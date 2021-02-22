package com.skyblockplus.verify;

import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueVerifyGuild;
import static com.skyblockplus.utils.BotUtils.getJson;
import static com.skyblockplus.utils.BotUtils.*;

import java.io.File;
import java.util.List;

import com.google.gson.JsonElement;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Verify extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        try {
            JsonElement currentSettings = getJson(
                    API_BASE_URL + "api/discord/serverSettings/get/verify?serverId=" + event.getGuild().getId());
            if (currentSettings != null) {
                if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                    if (isUniqueVerifyGuild(event.getGuild().getId())) {
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        String verifyText = higherDepth(currentSettings, "messageText").getAsString();
                        reactChannel.sendMessage(verifyText).queue();
                        Message reactMessage = reactChannel.sendMessage("https://streamable.com/sdq8tp").complete();
                        reactMessage.addReaction("✅").queue();

                        event.getJDA().addEventListener(new VerifyGuild(reactMessage, currentSettings));
                    }
                } else {
                    event.getJDA().addEventListener(new VerifyGuild(event.getGuild().getId(), currentSettings));
                }
            }
        } catch (Exception ignored) {
        }
    }

}