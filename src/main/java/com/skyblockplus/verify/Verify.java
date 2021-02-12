package com.skyblockplus.verify;

import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueVerifyGuild;
import static com.skyblockplus.utils.BotUtils.higherDepth;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Verify extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        try {
            JsonElement settings = JsonParser
                    .parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json"));
            if (higherDepth(settings, event.getGuild().getId()) != null) {
                if (higherDepth(higherDepth(higherDepth(settings, event.getGuild().getId()), "automated_verify"),
                        "enable").getAsBoolean()) {
                    if (isUniqueVerifyGuild(event.getGuild().getId())) {
                        JsonElement currentSettings = higherDepth(higherDepth(settings, event.getGuild().getId()),
                                "automated_verify");
                        TextChannel reactChannel = event.getGuild().getTextChannelById(
                                higherDepth(higherDepth(currentSettings, "react_channel"), "id").getAsString());

                        String channelPrefix = higherDepth(currentSettings, "new_channel_prefix").getAsString();
                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        String verifyText = higherDepth(currentSettings, "verify_text").getAsString();
                        reactChannel.sendMessage(verifyText).queue();
                        Message reactMessage = reactChannel
                                .sendFile(new File("src/main/java/com/skyblockplus/verify/Link Discord To Hypixel.mp4"))
                                .complete();
                        reactMessage.addReaction("✅").queue();

                        event.getJDA().addEventListener(new VerifyGuild(reactMessage, channelPrefix, currentSettings));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

}