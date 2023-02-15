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

package com.skyblockplus.api.linkedaccounts;

import static com.skyblockplus.utils.Utils.database;

import java.util.List;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/private/linked-accounts")
public class LinkedAccountController {

	@GetMapping("/get/all")
	public List<LinkedAccount> getAllServerSettings() {
		return database.getAllLinkedAccounts();
	}

	@GetMapping("/get/by")
	public ResponseEntity<?> getByDiscordId(
		@RequestParam(value = "discord", required = false) String discord,
		@RequestParam(value = "uuid", required = false) String uuid,
		@RequestParam(value = "username", required = false) String username
	) {
		if (discord != null) {
			return new ResponseEntity<>(database.getByDiscord(discord), HttpStatus.OK);
		} else if (uuid != null) {
			return new ResponseEntity<>(database.getByUuid(uuid), HttpStatus.OK);
		} else if (username != null) {
			return new ResponseEntity<>(database.getByUsername(username), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(
				DataObject.empty().put("success", false).put("cause", "No parameter provided from: id, uuid, username").toMap(),
				HttpStatus.BAD_REQUEST
			);
		}
	}
}
