package com.skyblockplus.api.models;

public class GuildChannelModel {
    private final String channelName;
    private final String channelId;

    public GuildChannelModel(String channelName, String channelId) {
        this.channelName = channelName;
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelId() {
        return channelId;
    }
}
