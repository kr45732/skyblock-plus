package com.SkyblockBot.Apply;

import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;

import java.io.FileReader;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Apply extends ListenerAdapter {
    Message reactMessage;
    String channelPrefix;
    boolean validGuild = false;
    JsonElement currentSettings;

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        try {
            JsonElement settings = new JsonParser()
                    .parse(new FileReader("src/main/java/com/SkyblockBot/json/GuildSettings.json"));
            if (higherDepth(settings, event.getGuild().getId()) != null) {
                if (higherDepth(higherDepth(higherDepth(settings, event.getGuild().getId()), "automated_applications"),
                        "enable").getAsBoolean()) {
                    currentSettings = higherDepth(higherDepth(settings, event.getGuild().getId()),
                            "automated_applications");
                    TextChannel reactChannel = event.getGuild().getTextChannelById(
                            higherDepth(higherDepth(currentSettings, "react_channel"), "id").getAsString());
                    validGuild = true;
                    channelPrefix = higherDepth(currentSettings, "new_channel_prefix").getAsString();

                    reactChannel.sendMessage("Loading...").complete();
                    reactChannel.sendMessage("Loading...").complete();
                    List<Message> deleteMessages = reactChannel.getHistory().retrievePast(10).complete();
                    reactChannel.deleteMessages(deleteMessages).complete();

                    String applyText = higherDepth(currentSettings, "apply_text").getAsString();
                    reactChannel.sendMessage(applyText).queue(message -> {
                        message.addReaction("✅").queue();
                        this.reactMessage = message;
                    });
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!validGuild) {
            return;
        }
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

        if (event.getGuild().getTextChannelsByName(channelPrefix + "-" + event.getUser().getName(), true).size() > 0) {
            return;
        }

        event.getJDA().addEventListener(new ApplyUser(event, event.getUser(), currentSettings));
    }
}