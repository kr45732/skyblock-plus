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

package com.skyblockplus.api.serversettings.managers;

import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private/serverSettings")
public class ServerSettingsController {

	private final ServerSettingsService settingsService;

	@Autowired
	public ServerSettingsController(ServerSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/getFormatted/all")
	public List<ServerSettingsModel> getAllServerSettings() {
		return settingsService.getAllServerSettings();
	}

	@GetMapping("/getFormatted/byId")
	public ResponseEntity<?> getServerSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getServerSettingsById(serverId);
	}

	@GetMapping("/getFormatted/verify")
	public ResponseEntity<?> getVerifySettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getVerifySettings(serverId);
	}

	@GetMapping("/getFormatted/apply/all")
	public List<AutomatedApply> getAllApplySettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getAllApplySettings(serverId);
	}

	@GetMapping("/getFormatted/apply/byName")
	public ResponseEntity<?> getApplySettings(
		@RequestParam(value = "serverId") String serverId,
		@RequestParam(value = "name") String name
	) {
		return settingsService.getApplySettingsExt(serverId, name);
	}

	@GetMapping("/getFormatted/roles")
	public ResponseEntity<?> getRolesSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getRolesSettings(serverId);
	}

	@GetMapping("/getFormatted/role")
	public ResponseEntity<?> getRoleSettings(
		@RequestParam(value = "serverId") String serverId,
		@RequestParam(value = "roleName") String roleName
	) {
		return settingsService.getRoleSettings(serverId, roleName);
	}

	@GetMapping("/getFormatted/guild/all")
	public List<GuildRole> getAllGuildRoleSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getAllGuildRolesSettings(serverId);
	}

	@GetMapping("/getFormatted/guild/byName")
	public ResponseEntity<?> getGuildRoleSettings(
		@RequestParam(value = "serverId") String serverId,
		@RequestParam(value = "name") String name
	) {
		return settingsService.getGuildRoleSettingsExt(serverId, name);
	}

	@GetMapping("/getFormatted/event")
	public ResponseEntity<?> getSkyblockEventSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getSkyblockEventSettings(serverId);
	}

	@GetMapping("/getFormatted/mee6")
	public ResponseEntity<?> getMee6Settings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getMee6Settings(serverId);
	}
}
