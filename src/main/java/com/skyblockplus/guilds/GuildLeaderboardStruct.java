package com.skyblockplus.guilds;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class GuildLeaderboardStruct {
    public String name;
    public double skills;
    public double slayer;
    public double catacombs;
    public double weight;
    public String guildRank;
}
