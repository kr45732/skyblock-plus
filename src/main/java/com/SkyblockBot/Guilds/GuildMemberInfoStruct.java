package com.SkyblockBot.Guilds;

public class GuildMemberInfoStruct {
    String username;
    String uuid;

    public GuildMemberInfoStruct(String username, String uuid) {
        this.uuid = uuid;
        this.username = username;
    }
}
