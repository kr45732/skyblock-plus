package com.skyblockplus.settings;

import com.google.gson.*;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.api.linkedaccounts.LinkedAccountService;
import com.skyblockplus.api.serversettings.automatedapply.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.managers.ServerSettingsService;
import com.skyblockplus.api.serversettings.mee6roles.Mee6Data;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.RunningEvent;
import com.skyblockplus.api.serversettings.skyblockevent.SbEvent;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Database {

	public final ServerSettingsService settingsService;
	public final LinkedAccountService linkedAccountService;
	private final Gson gson = new Gson();

	@Autowired
	public Database(ServerSettingsService settingsService, LinkedAccountService linkedAccountService) {
		this.settingsService = settingsService;
		this.linkedAccountService = linkedAccountService;
	}

	public int removeApplySettings(String serverId, String name) {
		return settingsService.removeApplySettings(serverId, name).getStatusCodeValue();
	}

	public int addLinkedUser(LinkedAccountModel newUser) {
		return linkedAccountService.addNewLinkedAccount(newUser).getStatusCodeValue();
	}

	public JsonElement getLinkedUserByMinecraftUsername(String minecraftUsername) {
		return gson.toJsonTree(linkedAccountService.getByMinecraftUsername(minecraftUsername).getBody());
	}

	public JsonElement getLinkedUserByMinecraftUuid(String minecraftUuid) {
		return gson.toJsonTree(linkedAccountService.getByMinecraftUuid(minecraftUuid).getBody());
	}

	public JsonElement getLinkedUserByDiscordId(String discordId) {
		return gson.toJsonTree(linkedAccountService.getByDiscordId(discordId).getBody());
	}

	public void deleteLinkedUserByDiscordId(String discordId) {
		linkedAccountService.deleteByDiscordId(discordId);
	}

	public void deleteLinkedUserByMinecraftUsername(String minecraftUsername) {
		linkedAccountService.deleteByMinecraftUsername(minecraftUsername);
	}

	public void deleteLinkedUserByMinecraftUuid(String minecraftUuid) {
		linkedAccountService.deleteByMinecraftUuid(minecraftUuid);
	}

	public List<LinkedAccountModel> getLinkedUsers() {
		return linkedAccountService.getAllLinkedAccounts();
	}

	public List<ServerSettingsModel> getAllServerSettings() {
		return settingsService.getAllServerSettings();
	}

	public JsonElement getServerSettings(String serverId) {
		return gson.toJsonTree(settingsService.getServerSettingsById(serverId).getBody());
	}

	public int addNewServerSettings(String serverId, ServerSettingsModel serverSettingsModel) {
		return settingsService.addNewServerSettings(serverId, serverSettingsModel).getStatusCodeValue();
	}

	public int deleteServerSettings(String serverId) {
		return settingsService.deleteServerSettings(serverId).getStatusCodeValue();
	}

	public JsonElement getVerifySettings(String serverId) {
		return gson.toJsonTree(settingsService.getVerifySettings(serverId).getBody());
	}

	public int setVerifySettings(String serverId, JsonElement newVerifySettings) {
		return settingsService.setVerifySettings(serverId, gson.fromJson(newVerifySettings, AutomatedVerify.class)).getStatusCodeValue();
	}

	public JsonElement getRolesSettings(String serverId) {
		return gson.toJsonTree(settingsService.getRolesSettings(serverId).getBody());
	}

	public int setRolesSettings(String serverId, JsonElement newRoleSettings) {
		return settingsService.setRolesSettings(serverId, gson.fromJson(newRoleSettings, AutomatedRoles.class)).getStatusCodeValue();
	}

	public boolean serverByServerIdExists(String serverId) {
		return settingsService.serverByServerIdExists(serverId);
	}

	public JsonElement getRoleSettings(String serverId, String roleName) {
		return gson.toJsonTree(settingsService.getRoleSettings(serverId, roleName).getBody());
	}

	public int setRoleSettings(String serverId, String roleName, JsonElement newRoleSettings) {
		return settingsService.setRoleSettings(serverId, gson.fromJson(newRoleSettings, RoleModel.class), roleName).getStatusCodeValue();
	}

	public int setGuildRoleSettings(String serverId, JsonObject currentSettings) {
		return settingsService.setGuildRoleSettings(serverId, gson.fromJson(currentSettings, GuildRole.class)).getStatusCodeValue();
	}

	public int setGuildRoleSettings(String serverId, GuildRole currentSettings) {
		return settingsService.setGuildRoleSettings(serverId, currentSettings).getStatusCodeValue();
	}

	public int setApplyCacheSettings(String serverId, String name, String currentSettings) {
		return settingsService.setApplyUsersCache(serverId, name, currentSettings).getStatusCodeValue();
	}

	public JsonArray getApplyCacheSettings(String serverId, String name) {
		try {
			return JsonParser.parseString((String) settingsService.getApplyUsersCache(serverId, name).getBody()).getAsJsonArray();
		} catch (Exception e) {
			return JsonParser.parseString("[]").getAsJsonArray();
		}
	}

	public int deleteApplyCacheSettings(String serverId, String name) {
		return settingsService.setApplyUsersCache(serverId, name, "[]").getStatusCodeValue();
	}

	public int setSkyblockEventSettings(String serverId, SbEvent currentSettings) {
		return settingsService.setSkyblockEventSettings(serverId, currentSettings).getStatusCodeValue();
	}

	public int addEventMemberToRunningEvent(String serverId, EventMember newEventMember) {
		return settingsService.addEventMemberToRunningEvent(serverId, newEventMember).getStatusCodeValue();
	}

	public boolean getSkyblockEventActive(String serverId) {
		return settingsService.getSkyblockEventActive(serverId);
	}

	public String getSkyblockEventGuildId(String serverId) {
		return (String) settingsService.getSkyblockEventGuildId(serverId).getBody();
	}

	public JsonElement getRunningEventSettings(String serverId) {
		return gson.toJsonTree(settingsService.getRunningSkyblockEventSettings(serverId).getBody());
	}

	public int setRunningEventSettings(String serverId, JsonElement newSettings) {
		return settingsService.setSkyblockRunningEvent(serverId, gson.fromJson(newSettings, RunningEvent.class)).getStatusCodeValue();
	}

	public int removeEventMemberFromRunningEvent(String serverId, String minecraftUuid) {
		return settingsService.removeEventMemberFromRunningEvent(serverId, minecraftUuid).getStatusCodeValue();
	}

	public boolean eventHasMemberByUuid(String serverId, String minecraftUuid) {
		return settingsService.eventHasMemberByUuid(serverId, minecraftUuid);
	}

	public JsonElement getApplyReqs(String serverId, String name) {
		return gson.toJsonTree(settingsService.getApplyReqs(serverId, name).getBody());
	}

	public int setApplyReqs(String serverId, String name, JsonArray newApplyReqs) {
		return settingsService.setApplyReqs(serverId, name, gson.fromJson(newApplyReqs, ApplyRequirements[].class)).getStatusCodeValue();
	}

	public List<AutomatedApply> getAllApplySettings(String serverId) {
		return settingsService.getAllApplySettings(serverId);
	}

	public JsonElement getApplySettings(String serverId, String name) {
		return gson.toJsonTree(settingsService.getApplySettingsExt(serverId, name).getBody());
	}

	public int setApplySettings(String serverId, AutomatedApply newSettings) {
		return settingsService.setApplySettings(serverId, newSettings).getStatusCodeValue();
	}

	public int setApplySettings(String serverId, JsonElement newSettings) {
		return settingsService.setApplySettings(serverId, gson.fromJson(newSettings, AutomatedApply.class)).getStatusCodeValue();
	}

	public int setVerifyRolesSettings(String serverId, JsonArray newSettings) {
		return settingsService.setVerifyRolesSettings(serverId, gson.fromJson(newSettings, String[].class)).getStatusCodeValue();
	}

	public List<GuildRole> getAllGuildRoles(String serverId) {
		return settingsService.getAllGuildRolesSettings(serverId);
	}

	public JsonElement getGuildRoleSettings(String serverId, String name) {
		return gson.toJsonTree(settingsService.getGuildRoleSettingsExt(serverId, name).getBody());
	}

	public String getServerHypixelApiKey(String serverId) {
		Object response = settingsService.getServerHypixelApiKey(serverId).getBody();
		return response != null ? (String) response : null;
	}

	public int setServerHypixelApiKey(String serverId, String newKey) {
		return settingsService.setServerHypixelApiKey(serverId, newKey).getStatusCodeValue();
	}

	public JsonElement getMee6Settings(String serverId) {
		return gson.toJsonTree(settingsService.getMee6Settings(serverId).getBody());
	}

	public int setMee6Settings(String serverId, JsonElement newSettings) {
		return settingsService.setMee6Settings(serverId, gson.fromJson(newSettings, Mee6Data.class)).getStatusCodeValue();
	}

	public int setPrefix(String serverId, String prefix) {
		return settingsService.setPrefix(serverId, prefix).getStatusCodeValue();
	}

	public String getPrefix(String serverId) {
		return settingsService.getPrefix(serverId).getBody();
	}
}
