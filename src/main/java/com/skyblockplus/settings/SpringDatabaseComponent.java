package com.skyblockplus.settings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.discordserversettings.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class SpringDatabaseComponent {
    private final ServerSettingsService settingsService;
    private final Gson gson = new Gson();

    @Autowired
    public SpringDatabaseComponent(ServerSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public int addLinkedUser(String serverId, LinkedAccount newUser) {
        return settingsService.addLinkedUser(serverId, newUser).getStatusCodeValue();
    }

    public JsonElement getLinkedUser(String serverId, String discordId) {
        return gson.toJsonTree(settingsService.getLinkedUser(serverId, discordId).getBody());
    }

    public int removeLinkedUser(String serverId, String discordId) {
        return settingsService.removeLinkedUser(serverId, discordId).getStatusCodeValue();
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

    public int removeServerSettings(String serverId) {
        return settingsService.removeServerSettings(serverId).getStatusCodeValue();
    }

    public JsonElement getVerifySettings(String serverId) {
        return gson.toJsonTree(settingsService.getVerifySettings(serverId).getBody());
    }

    public int updateVerifySettings(String serverId, JsonElement newVerifySettings) {
        return settingsService.updateVerifySettings(serverId, gson.fromJson(newVerifySettings, AutomatedVerify.class)).getStatusCodeValue();
    }

    public JsonElement getApplySettings(String serverId) {
        return gson.toJsonTree(settingsService.getApplySettings(serverId).getBody());
    }

    public int updateApplySettings(String serverId, JsonElement newApplySettings) {
        return settingsService.updateApplySettings(serverId, gson.fromJson(newApplySettings, AutomatedApplication.class)).getStatusCodeValue();
    }

    public JsonElement getRolesSettings(String serverId) {
        return gson.toJsonTree(settingsService.getRolesSettings(serverId).getBody());
    }

    public int updateRolesSettings(String serverId, JsonElement newRoleSettings) {
        return settingsService.updateRolesSettings(serverId, gson.fromJson(newRoleSettings, AutomatedRoles.class)).getStatusCodeValue();
    }


    public JsonElement getRoleSettings(String serverId, String roleName) {
        return gson.toJsonTree(settingsService.getRoleSettings(serverId, roleName).getBody());
    }

    public int updateRoleSettings(String serverId, String roleName, JsonElement newRoleSettings) {
        return settingsService.updateRoleSettings(serverId, gson.fromJson(newRoleSettings, RoleModel.class), roleName).getStatusCodeValue();
    }
}
