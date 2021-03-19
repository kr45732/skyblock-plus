package com.skyblockplus.api.discordserversettings.settingsmanagers;

import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRole;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;

@Data
@AllArgsConstructor
@Entity
@Transactional
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

    @Embedded
    private GuildRole automaticGuildRoles = new GuildRole();

    public ServerSettingsModel() {
    }

    public ServerSettingsModel(String serverName, String serverId) {
        this.serverName = serverName;
        this.serverId = serverId;
    }
}
