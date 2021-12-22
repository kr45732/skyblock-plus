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

package com.skyblockplus.api.linkedaccounts;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private/linkedAccounts")
public class LinkedAccountController {

	private final LinkedAccountService settingsService;

	@Autowired
	public LinkedAccountController(LinkedAccountService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/get/all")
	public List<LinkedAccountModel> getAllServerSettings() {
		return settingsService.getAllLinkedAccounts();
	}

	@GetMapping("/get/by/discordId")
	public ResponseEntity<?> getByDiscordId(@RequestParam(value = "discordId") String discordId) {
		return settingsService.getByDiscordId(discordId);
	}

	@GetMapping("/get/by/minecraftUuid")
	public ResponseEntity<?> getByMinecraftUuid(@RequestParam(value = "minecraftUuid") String minecraftUuid) {
		return settingsService.getByMinecraftUuid(minecraftUuid);
	}

	@GetMapping("/get/by/minecraftUsername")
	public ResponseEntity<?> getByMinecraftUsername(@RequestParam(value = "minecraftUsername") String minecraftUsername) {
		return settingsService.getByMinecraftUsername(minecraftUsername);
	}
}
