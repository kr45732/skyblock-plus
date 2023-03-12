/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.api.controller;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.cacheDatabase;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.managers.ServerSettingsService;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.general.help.HelpSlashCommand;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.apache.groovy.util.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(headers = "X-Subdomain-Internal=api")
public class ApiController {

	private static List<Map<String, Object>> apiCommandList;
	private final ServerSettingsService settingsService;

	@Autowired
	public ApiController(ServerSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public static void initialize() {
		apiCommandList =
			HelpSlashCommand.helpDataList
				.stream()
				.map(helpData -> DataObject.fromJson(gson.toJson(helpData)).toMap())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@CrossOrigin({ "https://skyblock-plus.vercel.app", "https://skyblock-plus.ml" })
	@GetMapping("/commands")
	public ResponseEntity<?> getAllCommands() {
		return new ResponseEntity<>(apiCommandList, HttpStatus.OK);
	}

	@CrossOrigin({ "https://skyblock-plus.vercel.app", "https://skyblock-plus.ml" })
	@GetMapping("/stats")
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

	@GetMapping("/sbg/data")
	public ResponseEntity<?> getSbgEventData() {
		if (guildMap.containsKey("602137436490956820")) {
			return new ResponseEntity<>(guildMap.get("602137436490956820").eventMemberList, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/sbg/last-updated")
	public ResponseEntity<?> getSbgEventLastUpdate() {
		if (guildMap.containsKey("602137436490956820")) {
			Instant lastUpdated = guildMap.get("602137436490956820").eventMemberListLastUpdated;
			return new ResponseEntity<>(Maps.of("last_updated", lastUpdated == null ? -1 : lastUpdated.toEpochMilli()), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/jacob", consumes = "application/json", produces = "application/json")
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

	@GetMapping("/private/all")
	public List<ServerSettingsModel> getAllServerSettings() {
		return settingsService.getAllServerSettings();
	}

	@GetMapping("/private/byId")
	public ResponseEntity<?> getServerSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getServerSettingsById(serverId);
	}

	@GetMapping("/private/verify")
	public ResponseEntity<?> getVerifySettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getVerifySettings(serverId);
	}

	@GetMapping("/private/roles")
	public ResponseEntity<?> getRolesSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getRolesSettings(serverId);
	}

	@GetMapping("/private/role")
	public ResponseEntity<?> getRoleSettings(
		@RequestParam(value = "guildId") String serverId,
		@RequestParam(value = "roleName") String roleName
	) {
		return settingsService.getRoleSettings(serverId, roleName);
	}

	@GetMapping("/private/guild/all")
	public List<AutomatedGuild> getAllGuildSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getAllGuildSettings(serverId);
	}

	@GetMapping("/private/guild/byName")
	public ResponseEntity<?> getGuildSettings(@RequestParam(value = "guildId") String serverId, @RequestParam(value = "name") String name) {
		return settingsService.getGuildSettings(serverId, name);
	}

	@GetMapping("/private/event")
	public ResponseEntity<?> getSkyblockEventSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getSkyblockEventSettings(serverId);
	}
}
