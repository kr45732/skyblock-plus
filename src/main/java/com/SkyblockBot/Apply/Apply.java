package com.SkyblockBot.Apply;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.util.List;

import static com.SkyblockBot.Utils.BotUtils.higherDepth;

public class Apply extends ListenerAdapter {
    Message reactMessage;
    String channelPrefix;
    boolean validGuild = false;
    JsonElement currentSettings;

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
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
                    List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                    reactChannel.deleteMessages(deleteMessages).complete();

                    String applyText = higherDepth(currentSettings, "apply_text").getAsString();
                    reactMessage = reactChannel.sendMessage(applyText).complete();
                    reactMessage.addReaction("✅").queue();
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
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

        if (event.getGuild()
                .getTextChannelsByName(channelPrefix + "-" + event.getUser().getName().replace(" ", "-"), true)
                .size() > 0) {
            return;
        }

        event.getJDA().addEventListener(new ApplyUser(event, event.getUser(), currentSettings));
    }
}