package com.skyblockplus.reload;

import java.util.ArrayList;
import java.util.List;

public class ReloadEventWatcherClass {
    private String guildId;
    private Object guildEventListener;
    private List<Object> subEventListeners = new ArrayList<>();

    public ReloadEventWatcherClass(String guildId, Object guildEventListener) {
        this.guildEventListener = guildEventListener;
        this.guildId = guildId;
    }

    public String getGuildId() {
        return this.guildId;
    }

    public Object getGuildEventListener() {
        return this.guildEventListener;
    }

    public ReloadEventWatcherClass setGuildEventListener(List<Object> newGuildEventListeners) {
        this.subEventListeners = newGuildEventListeners;
        return this;
    }

    public List<Object> getSubEventListeners() {
        return this.subEventListeners;
    }

    public ReloadEventWatcherClass addSubEventListener(Object subEventListener) {
        this.subEventListeners.add(subEventListener);
        return this;
    }

    public ReloadEventWatcherClass removeSubEventListener(Object subEventListener) {
        this.subEventListeners.remove(subEventListeners);
        return this;
    }

}
