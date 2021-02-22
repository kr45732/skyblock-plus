package com.skyblockplus.timeout;

import static com.skyblockplus.Main.jda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListenerDeleter extends ListenerAdapter {
    public static Map<String, Object> eventListeners = new HashMap<>();

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final Runnable channelDeleter = this::updateEventListeners;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(channelDeleter, 0, 50, TimeUnit.SECONDS);
    }

    public void updateEventListeners() {
        List<Object> registeredListeners = jda.getRegisteredListeners();
        Map<String, Object> tempMap = new HashMap<>();
        for (int i = 0; i < eventListeners.values().size(); i++) {
            String currentGuildChannel = eventListeners.keySet().toArray(new String[0])[i];
            Object currentEventListener = eventListeners.values().toArray(new Object[0])[i];
            if (registeredListeners.contains(currentEventListener)) {
                tempMap.put(currentGuildChannel, currentEventListener);
            }
        }
        eventListeners.clear();
        eventListeners.putAll(tempMap);
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        Object eventListener = eventListeners.get(event.getGuild().getId() + ":" + event.getChannel().getId());
        if (eventListener != null) {
            event.getJDA().removeEventListener(eventListener);
        }
    }

    public static void addEventListener(String guildId, String channelId, Object eventListener) {
        eventListeners.put((guildId + ":" + channelId), eventListener);
    }
}
