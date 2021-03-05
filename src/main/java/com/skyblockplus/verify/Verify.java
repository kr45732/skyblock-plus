package com.skyblockplus.verify;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueVerifyGuild;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;

public class Verify extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        try {
            JsonElement currentSettings = database.getVerifySettings(event.getGuild().getId());
            if (currentSettings != null) {
                if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                    if (isUniqueVerifyGuild(event.getGuild().getId())) {
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage(loadingEmbed().build()).complete();
                        reactChannel.sendMessage(loadingEmbed().build()).complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        String verifyText = higherDepth(currentSettings, "messageText").getAsString();
                        reactChannel.sendMessage(verifyText).queue();
                        Message reactMessage = reactChannel
                                .sendFile(new File("src/main/java/com/skyblockplus/verify/Link_Discord_To_Hypixel.mp4"))
                                .complete();
                        reactMessage.addReaction("✅").queue();

                        event.getJDA().addEventListener(new VerifyGuild(reactMessage, currentSettings));
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        event.getJDA().addEventListener(new VerifyGuild(event.getGuild().getId()));
    }

}