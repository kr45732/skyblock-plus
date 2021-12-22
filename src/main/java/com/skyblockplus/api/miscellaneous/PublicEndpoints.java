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

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.help.HelpCommand;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/public")
public class PublicEndpoints {

	private static List<Map<String, Object>> apiCommandList;
	private static Instant userCountLastUpdated = Instant.now();
	public static int userCount = -1;

	private static final List<String> ignoredGuilds = Arrays.asList("374071874222686211", "110373943822540800", "597450230430040076", "703967135961055314", "858695709393027102");

	public static void initialize() {
		apiCommandList =
			HelpCommand.helpDataList
				.stream()
				.map(helpData -> DataObject.fromJson(gson.toJson(helpData)).toMap())
				.collect(Collectors.toList());
	}

	@GetMapping("/get/commands")
	public ResponseEntity<?> getAllCommands() {
		return new ResponseEntity<>(apiCommandList, HttpStatus.OK);
	}

	@GetMapping("/get/stats")
	public ResponseEntity<?> getStats() {
		Map<String, Integer> commandUses = getCommandUses();
		if(userCount == -1 || Duration.between(userCountLastUpdated, Instant.now()).toHours() >= 1) {
			userCount = jda.getGuilds().stream().filter(g -> !ignoredGuilds.contains(g.getId())).map(Guild::getMemberCount).mapToInt(Integer::intValue).sum();
			userCountLastUpdated = Instant.now();
		}

		return new ResponseEntity<>(
			DataObject
				.empty()
				.put("guild_count", jda.getGuildCache().size())
				.put("user_count", userCount)
				.put("total_command_uses", commandUses.values().stream().mapToInt(Integer::intValue).sum())
				.put("command_uses", commandUses)
				.toMap(),
			HttpStatus.OK
		);
	}

	@PostMapping(value = "/post/jacob", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> postJacobData(@RequestBody JacobData jacobData, @RequestHeader String key) {
		if (key.equals("2d7569ff0decff164a46e8d417e7b692")) {
			if (JacobHandler.needsUpdate()) {
				JacobHandler.setJacobData(jacobData);
				return new ResponseEntity<>(DataObject.empty().put("success", true).toMap(), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
					DataObject.empty().put("success", false).put("cause", "Already have data for this year").toMap(),
					HttpStatus.OK
				);
			}
		} else {
			return new ResponseEntity<>(
				DataObject.empty().put("success", false).put("cause", "Not authorized").toMap(),
				HttpStatus.FORBIDDEN
			);
		}
	}
}
