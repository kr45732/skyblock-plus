package com.skyblockplus.settings;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.api.discordserversettings.automatedapplication.ApplyRequirements;
import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRole;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsService;
import com.skyblockplus.api.discordserversettings.skyblockevent.EventMember;
import com.skyblockplus.api.discordserversettings.skyblockevent.SbEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.api.linkedaccounts.LinkedAccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SpringDatabaseComponent {
    private final ServerSettingsService settingsService;
    private final LinkedAccountService linkedAccountService;
    private final Gson gson = new Gson();

    @Autowired
    public SpringDatabaseComponent(ServerSettingsService settingsService, LinkedAccountService linkedAccountService) {
        this.settingsService = settingsService;
        this.linkedAccountService = linkedAccountService;
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

    public int updateVerifySettings(String serverId, JsonElement newVerifySettings) {
        return settingsService.updateVerifySettings(serverId, gson.fromJson(newVerifySettings, AutomatedVerify.class))
                .getStatusCodeValue();
    }

    public JsonElement getRolesSettings(String serverId) {
        return gson.toJsonTree(settingsService.getRolesSettings(serverId).getBody());
    }

    public int updateRolesSettings(String serverId, JsonElement newRoleSettings) {
        return settingsService.updateRolesSettings(serverId, gson.fromJson(newRoleSettings, AutomatedRoles.class))
                .getStatusCodeValue();
    }

    public boolean serverByServerIdExists(String serverId) {
        return settingsService.serverByServerIdExists(serverId);
    }

    public JsonElement getRoleSettings(String serverId, String roleName) {
        return gson.toJsonTree(settingsService.getRoleSettings(serverId, roleName).getBody());
    }

    public int updateRoleSettings(String serverId, String roleName, JsonElement newRoleSettings) {
        return settingsService.updateRoleSettings(serverId, gson.fromJson(newRoleSettings, RoleModel.class), roleName)
                .getStatusCodeValue();
    }

    public JsonElement getGuildRoleSettings(String serverId) {
        return gson.toJsonTree(settingsService.getGuildRolesSettings(serverId).getBody());
    }

    public int updateGuildRoleSettings(String serverId, JsonObject currentSettings) {
        return settingsService.updateGuildRoleSettings(serverId, gson.fromJson(currentSettings, GuildRole.class))
                .getStatusCodeValue();
    }

    public int updateApplyCacheSettings(String serverId, String name, String currentSettings) {
        return settingsService.updateApplyUsersCache(serverId, name, currentSettings).getStatusCodeValue();
    }

    public JsonArray getApplyCacheSettings(String serverId, String name) {
        try {
            return JsonParser.parseString((String) settingsService.getApplyUsersCache(serverId, name).getBody())
                    .getAsJsonArray();
        } catch (Exception e) {
            return JsonParser.parseString("[]").getAsJsonArray();
        }
    }

    public int deleteApplyCacheSettings(String serverId, String name) {
        return settingsService.updateApplyUsersCache(serverId, name, "[]").getStatusCodeValue();
    }

    public int updateSkyblockEventSettings(String serverId, SbEvent currentSettings) {
        return settingsService.updateSkyblockEventSettings(serverId, currentSettings).getStatusCodeValue();
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

    public int removeEventMemberToRunningEvent(String serverId, String minecraftUuid) {
        return settingsService.removeEventMemberToRunningEvent(serverId, minecraftUuid).getStatusCodeValue();
    }

    public boolean eventHasMemberByUuid(String serverId, String minecraftUuid) {
        return settingsService.eventHasMemberByUuid(serverId, minecraftUuid);
    }

    public JsonElement getApplyReqs(String serverId, String name) {
        return gson.toJsonTree(settingsService.getApplyReqs(serverId, name).getBody());
    }

    public int updateApplyReqs(String serverId, String name, JsonArray newApplyReqs) {
        return settingsService.updateApplyReqs(serverId, name, gson.fromJson(newApplyReqs, ApplyRequirements[].class))
                .getStatusCodeValue();
    }

    public List<AutomatedApplication> getAllApplySettings(String serverId) {
        return settingsService.getAllApplySettings(serverId);
    }

    public JsonElement getApplySettings(String serverId, String name) {
        return gson.toJsonTree(settingsService.getApplySettingsExt(serverId, name).getBody());
    }

    public int updateApplySettings(String serverId, AutomatedApplication newSettings) {
        return settingsService.updateApplySettings(serverId, newSettings).getStatusCodeValue();
    }

    public int updateApplySettings(String serverId, JsonElement newSettings) {
        return settingsService.updateApplySettings(serverId, gson.fromJson(newSettings, AutomatedApplication.class))
                .getStatusCodeValue();
    }
}
