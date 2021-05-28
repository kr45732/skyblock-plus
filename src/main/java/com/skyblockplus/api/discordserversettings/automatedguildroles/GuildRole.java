package com.skyblockplus.api.discordserversettings.automatedguildroles;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Embeddable
public class GuildRole {
    private String name;
    private String enableGuildRole = "false";
    private String guildId = "";
    private String roleId = "";

    private String enableGuildRanks = "";
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<GuildRank> guildRanks = new ArrayList<>();

    public GuildRole() {
    }

    public GuildRole(String name) {
        this.name = name;
    }
}
