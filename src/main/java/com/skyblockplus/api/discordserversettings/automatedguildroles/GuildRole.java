package com.skyblockplus.api.discordserversettings.automatedguildroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class GuildRole {
    private String enableGuildRole = "false";
    private String guildId = "";
    private String roleId = "";

    public GuildRole() {
    }
}
