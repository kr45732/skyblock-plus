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

package com.skyblockplus.settings;

import static com.skyblockplus.utils.Utils.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.api.linkedaccounts.LinkedAccountService;
import com.skyblockplus.api.serversettings.automatedguild.ApplyBlacklist;
import com.skyblockplus.api.serversettings.automatedguild.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.jacob.JacobSettings;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.managers.ServerSettingsService;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Database {

	public final ServerSettingsService settingsService;
	public final LinkedAccountService linkedAccountService;

	@Autowired
	public Database(ServerSettingsService settingsService, LinkedAccountService linkedAccountService) {
		this.settingsService = settingsService;
		this.linkedAccountService = linkedAccountService;
	}

	public int removeGuildSettings(String serverId, String name) {
		return settingsService.removeGuildSettings(serverId, name).getStatusCodeValue();
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

	public void newServerSettings(String serverId, ServerSettingsModel serverSettingsModel) {
		settingsService.addNewServerSettings(serverId, serverSettingsModel).getStatusCodeValue();
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

	public int setApplyCacheSettings(String serverId, String name, String currentSettings) {
		return settingsService.setApplyUsersCache(serverId, name, currentSettings).getStatusCodeValue();
	}

	public JsonArray getApplyCacheSettings(String serverId, String name) {
		try {
			return JsonParser.parseString((String) settingsService.getApplyUsersCache(serverId, name).getBody()).getAsJsonArray();
		} catch (Exception e) {
			return new JsonArray();
		}
	}

	public int deleteApplyCacheSettings(String serverId, String name) {
		return settingsService.setApplyUsersCache(serverId, name, "[]").getStatusCodeValue();
	}

	public int setSkyblockEventSettings(String serverId, EventSettings currentSettings) {
		return settingsService.setSkyblockEventSettings(serverId, currentSettings).getStatusCodeValue();
	}

	public int addMemberToSkyblockEvent(String serverId, EventMember newEventMember) {
		return settingsService.addMemberToSkyblockEvent(serverId, newEventMember).getStatusCodeValue();
	}

	public boolean getSkyblockEventActive(String serverId) {
		return settingsService.getSkyblockEventActive(serverId);
	}

	public JsonElement getSkyblockEventSettings(String serverId) {
		return gson.toJsonTree(settingsService.getSkyblockEventSettings(serverId).getBody());
	}

	public int removeMemberFromSkyblockEvent(String serverId, String minecraftUuid) {
		return settingsService.removeMemberFromSkyblockEvent(serverId, minecraftUuid).getStatusCodeValue();
	}

	public boolean eventHasMemberByUuid(String serverId, String minecraftUuid) {
		return settingsService.eventHasMemberByUuid(serverId, minecraftUuid);
	}

	public int setApplyReqs(String serverId, String name, JsonArray newApplyReqs) {
		return settingsService.setApplyReqs(serverId, name, gson.fromJson(newApplyReqs, ApplyRequirements[].class)).getStatusCodeValue();
	}

	public int setVerifyRolesSettings(String serverId, JsonArray newSettings) {
		return settingsService.setVerifyRolesSettings(serverId, gson.fromJson(newSettings, String[].class)).getStatusCodeValue();
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
		return settingsService.setMee6Settings(serverId, gson.fromJson(newSettings, RoleModel.class)).getStatusCodeValue();
	}

	public int setPrefix(String serverId, String prefix) {
		return settingsService.setPrefix(serverId, prefix).getStatusCodeValue();
	}

	public String getPrefix(String serverId) {
		return settingsService.getPrefix(serverId).getBody();
	}

	public JsonArray getApplyBlacklist(String serverId) {
		return gson.toJsonTree(settingsService.getApplyBlacklist(serverId).getBody()).getAsJsonArray();
	}

	public int setApplyBlacklist(String serverId, JsonArray newSettings) {
		return settingsService.setApplyBlacklist(serverId, gson.fromJson(newSettings, ApplyBlacklist[].class)).getStatusCodeValue();
	}

	public int setPartyFinderCategoryId(String serverId, String newSettings) {
		return settingsService.setPartyFinderCategoryId(serverId, newSettings).getStatusCodeValue();
	}

	public List<AutomatedGuild> getAllGuildSettings(String serverId) {
		return settingsService.getAllGuildSettings(serverId);
	}

	public JsonElement getGuildSettings(String serverId, String name) {
		return gson.toJsonTree(settingsService.getGuildSettings(serverId, name).getBody());
	}

	public int setGuildSettings(String serverId, JsonElement newSettings) {
		return settingsService.setGuildSettings(serverId, gson.fromJson(newSettings, AutomatedGuild.class)).getStatusCodeValue();
	}

	public int setApplyGuestRole(String serverId, String newSettings) {
		return settingsService.setApplyGuestRole(serverId, newSettings).getStatusCodeValue();
	}

	public JsonElement getJacobSettings(String serverId) {
		return gson.toJsonTree(settingsService.getJacobSettings(serverId).getBody());
	}

	public int setJacobSettings(String serverId, JsonElement newSettings) {
		return settingsService.setJacobSettings(serverId, gson.fromJson(newSettings, JacobSettings.class)).getStatusCodeValue();
	}

	public int setFetchurChannelId(String serverId, String newSettings) {
		return settingsService.setFetchurChannelId(serverId, newSettings).getStatusCodeValue();
	}

    public int setFetchurRole(String serverId, String newSettings) {
		return settingsService.setFetchurRole(serverId, newSettings).getStatusCodeValue();
    }
}
