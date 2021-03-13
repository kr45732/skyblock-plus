package com.skyblockplus.api.discordserversettings.automatedguildroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Embeddable
public class GuildRole {
    private String enableGuildRole = "false";
    private String guildId = "";
    private String roleId = "";

    private String enableGuildRanks = "false";

    @ElementCollection
    List<GuildRank> guildRankRoles = new ArrayList<>();

    public GuildRole() {
    }
}
