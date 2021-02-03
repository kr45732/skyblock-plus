package com.SkyblockBot.Verify;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class VerifyGuild extends ListenerAdapter {
    Message reactMessage;
    String channelPrefix;
    JsonElement currentSettings;

    public VerifyGuild(Message reactMessage, String channelPrefix, JsonElement currentSettings) {
        this.reactMessage = reactMessage;
        this.channelPrefix = channelPrefix;
        this.currentSettings = currentSettings;
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }


        event.getReaction().removeReaction(event.getUser()).queue();
        if (!event.getReactionEmote().getName().equals("✅")) {
            return;
        }

        if (event.getGuild().getTextChannelsByName(channelPrefix + "-" + event.getUser().getName(), true).size() > 0) {
            return;
        }


        event.getJDA().addEventListener(new VerifyUser(event, event.getUser(), currentSettings));
    }
}
