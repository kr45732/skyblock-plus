package com.SkyblockBot.Apply;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ApplyGuild extends ListenerAdapter {
    public ApplyGuild(Message reactMessage, String channelPrefix, JsonElement currentSettings) {
        this.reactMessage = reactMessage;
        this.channelPrefix = channelPrefix;
        this.currentSettings = currentSettings;
    }

    Message reactMessage;
    String channelPrefix;
    JsonElement currentSettings;

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        event.getReaction().removeReaction(event.getUser()).queue();
        if (!event.getReactionEmote().getName().equals("✅")) {
            return;
        }

        if (event.getGuild()
                .getTextChannelsByName(channelPrefix + "-" + event.getUser().getName().replace(" ", "-"), true)
                .size() > 0) {
            return;
        }

        event.getJDA().addEventListener(new ApplyUser(event, event.getUser(), currentSettings));
    }
}
