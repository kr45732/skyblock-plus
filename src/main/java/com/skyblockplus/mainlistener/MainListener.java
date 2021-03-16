package com.skyblockplus.mainlistener;

import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;


public class MainListener extends ListenerAdapter {
    private static final Map<String, AutomaticGuild> guildMap = new HashMap<>();

    public static String onApplyReload(String guildId) {
        String reloadStatus = "Error reloading";
        if (guildMap.containsKey(guildId)) {
            if (guildMap.get(guildId).allowApplyReload()) {
                reloadStatus = guildMap.get(guildId).reloadApplyConstructor(guildId);
            } else {
                reloadStatus = "Application in progress";
            }
        }

        return reloadStatus;
    }

    public static String onVerifyReload(String guildId) {
        String reloadStatus = "Error reloading";
        if (guildMap.containsKey(guildId)) {
            reloadStatus = guildMap.get(guildId).reloadVerifyConstructor(guildId);
        }

        return reloadStatus;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
            return;
        }
        if (isUniqueGuild(event.getGuild().getId())) {
            guildMap.put(event.getGuild().getId(), new AutomaticGuild(event));
        } else {
            System.out.println(event.getGuild().getId() + " is not unique");
        }
    }

    private boolean isUniqueGuild(String guildId) {
        return !guildMap.containsKey(guildId);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (guildMap.containsKey(event.getGuild().getId())) {
            guildMap.get(event.getGuild().getId()).onMessageReactionAdd(event);
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (guildMap.containsKey(event.getGuild().getId())) {
            guildMap.get(event.getGuild().getId()).onGuildMessageReceived(event);
        }
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        if (guildMap.containsKey(event.getGuild().getId())) {
            guildMap.get(event.getGuild().getId()).onTextChannelDelete(event);
        }
    }
}
