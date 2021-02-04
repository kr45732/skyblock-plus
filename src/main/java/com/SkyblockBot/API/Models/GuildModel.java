package com.SkyblockBot.API.Models;

public class GuildModel {
    private final String name;
    private final String id;

    public GuildModel(String guildName, String id) {
        this.name = guildName;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
