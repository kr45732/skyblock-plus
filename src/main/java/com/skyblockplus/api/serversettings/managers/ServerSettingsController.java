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

package com.skyblockplus.api.serversettings.managers;

import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private/server-settings")
public class ServerSettingsController {

	private final ServerSettingsService settingsService;

	@Autowired
	public ServerSettingsController(ServerSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/get/all")
	public List<ServerSettingsModel> getAllServerSettings() {
		return settingsService.getAllServerSettings();
	}

	@GetMapping("/get/byId")
	public ResponseEntity<?> getServerSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getServerSettingsById(serverId);
	}

	@GetMapping("/get/verify")
	public ResponseEntity<?> getVerifySettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getVerifySettings(serverId);
	}

	@GetMapping("/get/roles")
	public ResponseEntity<?> getRolesSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getRolesSettings(serverId);
	}

	@GetMapping("/get/role")
	public ResponseEntity<?> getRoleSettings(
		@RequestParam(value = "guildId") String serverId,
		@RequestParam(value = "roleName") String roleName
	) {
		return settingsService.getRoleSettings(serverId, roleName);
	}

	@GetMapping("/get/guild/all")
	public List<AutomatedGuild> getAllGuildSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getAllGuildSettings(serverId);
	}

	@GetMapping("/get/guild/byName")
	public ResponseEntity<?> getGuildSettings(@RequestParam(value = "guildId") String serverId, @RequestParam(value = "name") String name) {
		return settingsService.getGuildSettings(serverId, name);
	}

	@GetMapping("/get/event")
	public ResponseEntity<?> getSkyblockEventSettings(@RequestParam(value = "guildId") String serverId) {
		return settingsService.getSkyblockEventSettings(serverId);
	}
}
