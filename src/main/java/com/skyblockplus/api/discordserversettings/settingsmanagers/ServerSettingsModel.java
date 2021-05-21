package com.skyblockplus.api.discordserversettings.settingsmanagers;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRole;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.discordserversettings.skyblockevent.SbEvent;

import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.Data;

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
    private AutomatedApplication automatedApplicationOne = null;

    @Embedded
    private AutomatedApplication automatedApplicationTwo = null;

    @Embedded
    private AutomatedRoles automatedRoles = new AutomatedRoles();

    @Embedded
    private GuildRole automaticGuildRoles = new GuildRole();

    @Embedded
    private SbEvent sbEvent = new SbEvent();

    public ServerSettingsModel() {
    }

    public ServerSettingsModel(String serverName, String serverId) {
        this.serverName = serverName;
        this.serverId = serverId;
    }
}
