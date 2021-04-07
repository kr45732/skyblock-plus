package com.skyblockplus.utils;

public class DiscordInfoStruct {
    public String discordTag;
    public String minecraftUsername;

    public DiscordInfoStruct(String discordTag, String minecraftUsername, String minecraftUuid) {
        this.discordTag = discordTag;
        this.minecraftUsername = minecraftUsername;
        this.minecraftUuid = minecraftUuid;
    }

    public String minecraftUuid;


}
