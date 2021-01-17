package com.SkyblockBot.Apply;

import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ApplyStaff extends ListenerAdapter {
    ApplyUser applyUser;
    User user;
    TextChannel channelTest;
    EmbedBuilder ebMain;
    Message reactMessage;
    TextChannel applicationChannel;

    public ApplyStaff(ApplyUser applyUser, User user, TextChannel channelTest, EmbedBuilder ebMain) {
        this.applyUser = applyUser;
        this.user = user;
        this.channelTest = channelTest;
        this.ebMain = ebMain;
        applicationChannel = channelTest.getJDA().getTextChannelsByName("applications", true).get(0);

        ebMain.addField("To accept the application,", "React with ✅", true);
        ebMain.addBlankField(true);
        ebMain.addField("To deny the application,", "React with ❌", true);
        applicationChannel.sendMessage(ebMain.build()).queue(message -> {
            message.addReaction("✅").queue();
            message.addReaction("❌").queue();
            this.reactMessage = message;
        });
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong())
            return;
        if (event.getUser().isBot())
            return;

        if (event.getReactionEmote().getName().equals("❌")) {
            applicationChannel.sendMessage(user.getName() + " was denied").queue();
            channelTest
                    .sendMessage(user.getAsMention()
                            + "\nSorry to inform you but you have been denied.\nChannel closing in 30 seconds...")
                    .queue();
            channelTest.delete().reason("Application denied").queueAfter(30, TimeUnit.SECONDS);
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
        } else if (event.getReactionEmote().getName().equals("✅")) {
            applicationChannel.sendMessage(user.getName() + " was accepted").queue();
            channelTest.sendMessage(user.getAsMention()
                    + "\nYou have been accepted!\nPlease make sure to leave your current guild.\nYou will recieve an invite shortly.\nChannel closing in 30 seconds...")
                    .queue();
            ;
            channelTest.delete().reason("Application accepted").queueAfter(30, TimeUnit.SECONDS);
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
        }
    }

}
