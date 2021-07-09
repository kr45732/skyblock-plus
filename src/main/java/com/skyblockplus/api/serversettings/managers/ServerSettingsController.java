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

	@GetMapping("/get/all")
	public List<ServerSettingsModel> getAllServerSettings() {
		return settingsService.getAllServerSettings();
	}

	@GetMapping("/get/byId")
	public ResponseEntity<?> getServerSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getServerSettingsById(serverId);
	}

	@GetMapping("/get/verify")
	public ResponseEntity<?> getVerifySettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getVerifySettings(serverId);
	}

	@GetMapping("/get/apply/all")
	public List<AutomatedApply> getAllApplySettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getAllApplySettings(serverId);
	}

	@GetMapping("/get/apply/byName")
	public ResponseEntity<?> getApplySettings(
		@RequestParam(value = "serverId") String serverId,
		@RequestParam(value = "name") String name
	) {
		return settingsService.getApplySettingsExt(serverId, name);
	}

	@GetMapping("/get/roles")
	public ResponseEntity<?> getRolesSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getRolesSettings(serverId);
	}

	@GetMapping("/get/role")
	public ResponseEntity<?> getRoleSettings(
		@RequestParam(value = "serverId") String serverId,
		@RequestParam(value = "roleName") String roleName
	) {
		return settingsService.getRoleSettings(serverId, roleName);
	}

	@GetMapping("/get/guild/all")
	public List<GuildRole> getAllGuildRoleSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getAllGuildRolesSettings(serverId);
	}

	@GetMapping("/get/guild/byName")
	public ResponseEntity<?> getGuildRoleSettings(
		@RequestParam(value = "serverId") String serverId,
		@RequestParam(value = "name") String name
	) {
		return settingsService.getGuildRoleSettingsExt(serverId, name);
	}

	@GetMapping("/get/event")
	public ResponseEntity<?> getSkyblockEventSettings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getSkyblockEventSettings(serverId);
	}

	@GetMapping("/get/mee6")
	public ResponseEntity<?> getMee6Settings(@RequestParam(value = "serverId") String serverId) {
		return settingsService.getMee6Settings(serverId);
	}
}
