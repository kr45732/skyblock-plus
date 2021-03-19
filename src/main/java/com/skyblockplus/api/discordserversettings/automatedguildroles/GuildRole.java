package com.skyblockplus.api.discordserversettings.automatedguildroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class GuildRole {
//    @ElementCollection
//    List<GuildRank> guildRankRoles = new ArrayList<>();
    private String enableGuildRole = "false";
    private String guildId = "";
    private String roleId = "";
    private String enableGuildRanks = "false";

    public GuildRole() {
    }
}
