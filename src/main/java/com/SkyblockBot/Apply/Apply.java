package com.SkyblockBot.Apply;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Apply extends ListenerAdapter {
    String applyChannelName;
    String applyReactText;
    Message reactMessage;
    String emoji;
    String channelPrefix;

    public Apply(String applyChannelName, String applyReactText, String emoji, String channelPrefix) {
        this.applyChannelName = applyChannelName;
        this.applyReactText = applyReactText;
        this.emoji = emoji;
        this.channelPrefix = channelPrefix;
    }

    @Override
    public void onReady(ReadyEvent event) {
        for (TextChannel channel : event.getJDA().getTextChannels()) {
            if (channel.getName().equals(applyChannelName)) {
                channel.sendMessage("Loading...").complete();
                channel.sendMessage("Loading...").complete();
                List<Message> messages = channel.getHistory().retrievePast(10).complete();
                channel.deleteMessages(messages).complete();
                channel.sendMessage(applyReactText).queue(message -> {
                    message.addReaction(emoji).queue();
                    this.reactMessage = message;
                });
                break;
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }
        if (!event.getReactionEmote().getName().equals(emoji)) {
            return;
        }
        event.getReaction().removeReaction(event.getUser()).queue();
        event.getJDA().addEventListener(new ApplyUser(event.getUser(), event, channelPrefix, emoji));
    }
}