package com.skyblockplus.api.discordserversettings.automatedapplication;

import com.skyblockplus.eventlisteners.apply.PlayerCustom;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Data
@AllArgsConstructor
@Embeddable
public class ApplyUsersCache {
    private String applyingUserId;
    private String applicationChannelId;
    private String currentSettingsString;
    private String guildId;
    private String reactMessageId;
    private String state = "0";
    private String staffChannelId;
    private String shouldDeleteChannel = "false";
    private String playerSlayer;
    private String playerSkills;
    private String playerCatacombs;
    private String playerWeight;

    @Embedded
    private PlayerCustomCache player = new PlayerCustomCache();

    public ApplyUsersCache(){}
}
