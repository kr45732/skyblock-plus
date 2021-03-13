package com.skyblockplus.api.discordserversettings.automatedguildroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class GuildRank {
    private String rankName = "";
    private String roleId = "";

    public GuildRank(){

    }
}
