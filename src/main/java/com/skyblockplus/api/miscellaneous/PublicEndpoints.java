/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.api.miscellaneous;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.cacheDatabase;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.general.help.HelpSlashCommand;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.oauth.TokenData;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.apache.groovy.util.Maps;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/public")
public class PublicEndpoints {

	private static List<Map<String, Object>> apiCommandList;

	public static void initialize() {
		apiCommandList =
			HelpSlashCommand.helpDataList
				.stream()
				.map(helpData -> DataObject.fromJson(gson.toJson(helpData)).toMap())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@GetMapping("/get/commands")
	public ResponseEntity<?> getAllCommands() {
		return new ResponseEntity<>(apiCommandList, HttpStatus.OK);
	}

	@GetMapping("/get/stats")
	public ResponseEntity<?> getStats() {
		Map<String, Integer> commandUses = getCommandUses();

		return new ResponseEntity<>(
			DataObject
				.empty()
				.put("guild_count", jda.getGuildCache().size())
				.put("user_count", getUserCount())
				.put("total_command_uses", commandUses.values().stream().mapToInt(Integer::intValue).sum())
				.put(
					"command_uses",
					commandUses
						.entrySet()
						.stream()
						.sorted(Comparator.comparingInt(e -> -e.getValue()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
				)
				.toMap(),
			HttpStatus.OK
		);
	}

	@PostMapping(value = "/post/jacob", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> postJacobData(@RequestBody JacobData jacobData, @RequestHeader String key) {
		if (key.equals("2d7569ff0decff164a46e8d417e7b692")) {
			if (jacobData.getContests().isEmpty()) {
				return new ResponseEntity<>(
					DataObject.empty().put("success", false).put("cause", "Contests list empty").toMap(),
					HttpStatus.BAD_REQUEST
				);
			}

			JacobHandler.setJacobData(jacobData);
			cacheDatabase.cacheJacobData();
			jda.getTextChannelById("937894945564545035").sendMessage(client.getSuccess() + " Received jacob data").queue();
			return new ResponseEntity<>(DataObject.empty().put("success", true).toMap(), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(
				DataObject.empty().put("success", false).put("cause", "Not authorized").toMap(),
				HttpStatus.FORBIDDEN
			);
		}
	}

	@GetMapping("/get/sbg/data")
	public ResponseEntity<?> getSbgEventData() {
		if (guildMap.containsKey("602137436490956820")) {
			return new ResponseEntity<>(guildMap.get("602137436490956820").eventMemberList, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/get/sbg/last-updated")
	public ResponseEntity<?> getSbgEventLastUpdate() {
		if (guildMap.containsKey("602137436490956820")) {
			Instant lastUpdated = guildMap.get("602137436490956820").eventMemberListLastUpdated;
			return new ResponseEntity<>(Maps.of("last_updated", lastUpdated == null ? -1 : lastUpdated.toEpochMilli()), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/discord/verify")
	public ResponseEntity<?> getDiscordVerify(HttpServletResponse res, HttpServletRequest req) {
		try {
			String redirectUri = ServletUriComponentsBuilder
				.fromRequest(req)
				.replacePath("/api/public/discord/oauth")
				.build()
				.toUriString();

			String state = oAuthClient.generateState(redirectUri);
			Cookie stateCookie = new Cookie("clientState", state);
			stateCookie.setMaxAge(1000 * 60 * 5);
			res.addCookie(stateCookie);

			return ResponseEntity.status(HttpStatus.FOUND).location(oAuthClient.createAuthorizationUri(state)).build();
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/discord/oauth")
	public ResponseEntity<?> getDiscordOauth(
		HttpServletResponse res,
		HttpServletRequest req,
		@RequestParam(value = "code") String code,
		@RequestParam(value = "state") String state,
		@CookieValue(value = "clientState") String clientState
	) {
		try {
			if (!Objects.equals(clientState, state)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			String redirectUri = oAuthClient.consumeState(state);
			if (redirectUri == null) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			TokenData tokenData = oAuthClient.postToken(code, redirectUri);
			String userId = oAuthClient.getDiscord(tokenData);

			LinkedAccount linkedAccount = database.getByDiscord(userId);
			Player.Profile player = null;
			if (linkedAccount != null) {
				player = Player.create(linkedAccount.uuid());
			}
			if (TokenData.updateLinkedRolesMetadata(userId, linkedAccount, player, false).get()) {
				res.sendRedirect("/success.html");
				return new ResponseEntity<>(HttpStatus.FOUND);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
