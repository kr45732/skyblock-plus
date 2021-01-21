package com.SkyblockBot.Apply;

import static com.SkyblockBot.Apply.ChannelDeleter.removeChannel;
import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;

import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ApplyStaff extends ListenerAdapter {
    User user;
    TextChannel applyChannel;
    EmbedBuilder ebMain;
    Message reactMessage;
    TextChannel staffChannel;
    JsonElement currentSettings;
    Message deleteChannelMessage;

    public ApplyStaff(User user, TextChannel applyChannel, EmbedBuilder ebMain, JsonElement currentSettings) {
        this.user = user;
        this.applyChannel = applyChannel;
        this.ebMain = ebMain;
        this.currentSettings = currentSettings;
        staffChannel = applyChannel.getJDA()
                .getTextChannelById(higherDepth(higherDepth(currentSettings, "staff_channel"), "id").getAsString());

        ebMain.addField("To accept the application,", "React with ✅", true);
        ebMain.addBlankField(true);
        ebMain.addField("To deny the application,", "React with ❌", true);
        staffChannel
                .sendMessage("<@&" + higherDepth(higherDepth(currentSettings, "staff_ping"), "id").getAsString() + ">")
                .complete();
        staffChannel.sendMessage(ebMain.build()).queue(message -> {
            message.addReaction("✅").queue();
            message.addReaction("❌").queue();
            this.reactMessage = message;
        });
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }
        try {
            if (event.getMessageIdLong() == deleteChannelMessage.getIdLong()) {
                if (event.getReactionEmote().getName().equals("✅")) {
                    deleteChannelMessage.clearReactions().queue();
                    EmbedBuilder eb = defaultEmbed("Channel Closing", null);
                    eb.addField("Reason", "User has read message", false);
                    applyChannel.sendMessage(eb.build()).queue();
                    applyChannel.delete().reason("Applicant read final message").queueAfter(15, TimeUnit.SECONDS);
                    removeChannel(applyChannel);
                    return;
                } else {
                    event.getReaction().removeReaction(user).queue();
                }
                return;
            }
        } catch (Exception ex) {

        }
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }

        if (event.getReactionEmote().getName().equals("❌")) {
            staffChannel.sendMessage(user.getAsMention() + " (" + user.getName() + ") was denied by "
                    + event.getUser().getAsMention() + " (" + event.getUser().getName() + ")").queue();
            reactMessage.clearReactions().queue();
            EmbedBuilder eb = defaultEmbed("Application Not Accepted", null);
            eb.setDescription(higherDepth(currentSettings, "deny_text").getAsString()
                    + "\n**React with ✅ to confirm that you have read this message and to close channel**");
            applyChannel.sendMessage(user.getAsMention()).queue();
            applyChannel.sendMessage(eb.build()).queue(denyM -> {
                denyM.addReaction("✅").queue();
                this.deleteChannelMessage = denyM;
            });
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
        } else if (event.getReactionEmote().getName().equals("✅")) {
            staffChannel.sendMessage(user.getName() + " was accepted by " + event.getUser().getName()).queue();
            reactMessage.clearReactions().queue();
            EmbedBuilder eb = defaultEmbed("Application Accepted", null);
            eb.setDescription(higherDepth(currentSettings, "accept_text").getAsString()
                    + "\n**React with ✅ to confirm that you have read this message and to close channel**");
            applyChannel.sendMessage(user.getAsMention()).queue();
            applyChannel.sendMessage(eb.build()).queue(denyM -> {
                denyM.addReaction("✅").queue();
                this.deleteChannelMessage = denyM;
            });
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
        }
    }

}
