package com.skyblockplus.api.discordserversettings.settingsmanagers;

import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRole;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.discordserversettings.linkedaccounts.LinkedAccount;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Entity
public class ServerSettingsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String serverName;
    private String serverId;

    @Embedded
    private AutomatedVerify automatedVerify = new AutomatedVerify();

    @Embedded
    private AutomatedApplication automatedApplication = new AutomatedApplication();

    @Embedded
    private AutomatedRoles automatedRoles = new AutomatedRoles();

    @ElementCollection
    private List<LinkedAccount> linkedAccounts = new ArrayList<>();

    @Embedded
    private GuildRole automaticGuildRoles = new GuildRole();

    public ServerSettingsModel() {
    }

    public ServerSettingsModel(String serverName, String serverId) {
        this.serverName = serverName;
        this.serverId = serverId;
    }
}
