package com.skyblockplus.verify;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueVerifyGuild;
import static com.skyblockplus.utils.BotUtils.higherDepth;

public class Verify extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        try {
            JsonElement settings = JsonParser
                    .parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json"));
            if (higherDepth(settings, event.getGuild().getId()) != null) {
                if (higherDepth(higherDepth(higherDepth(settings, event.getGuild().getId()), "automatedVerify"),
                        "enable").getAsBoolean()) {
                    if (isUniqueVerifyGuild(event.getGuild().getId())) {
                        JsonElement currentSettings = higherDepth(higherDepth(settings, event.getGuild().getId()),
                                "automatedVerify");
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        String verifyText = higherDepth(currentSettings, "messageText").getAsString();
                        reactChannel.sendMessage(verifyText).queue();
                        Message reactMessage = reactChannel
                                .sendFile(new File("src/main/java/com/skyblockplus/verify/Link Discord To Hypixel.mp4"))
                                .complete();
                        reactMessage.addReaction("✅").queue();

                        event.getJDA().addEventListener(new VerifyGuild(reactMessage, currentSettings));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

}