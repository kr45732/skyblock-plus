package com.skyblockplus.api.models.discordoauth2;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscordUserModel {
    @JsonProperty("id")
    private final String id;

    @JsonProperty("username")
    private final String username;

    @JsonProperty("discriminator")
    private final int discriminator;

    @JsonProperty("avatar")
    private final String avatar;

    public DiscordUserModel(String id, String username, int discriminator, String avatar) {
        this.id = id;
        this.username = username;
        this.discriminator = discriminator;
        this.avatar = avatar;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public int getDiscriminator() {
        return discriminator;
    }

    public String getAvatar() {
        return avatar;
    }
}