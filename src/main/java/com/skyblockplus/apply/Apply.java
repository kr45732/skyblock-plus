package com.skyblockplus.apply;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueApplyGuild;
import static com.skyblockplus.utils.Utils.*;

public class Apply extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        try {
            JsonElement currentSettings = database.getApplySettings(event.getGuild().getId());
            if (currentSettings != null) {
                if (isUniqueApplyGuild(event.getGuild().getId())) {
                    if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                        try {
                            Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                            if (reactMessage != null) {
                                event.getJDA().addEventListener(new ApplyGuild(reactMessage, currentSettings));
                                return;
                            }
                        } catch (Exception ignored) {
                        }

                        reactChannel.sendMessage(loadingEmbed().build()).complete();
                        reactChannel.sendMessage(loadingEmbed().build()).complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        EmbedBuilder eb = defaultEmbed("Apply For Guild");
                        eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());
                        Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                        reactMessage.addReaction("✅").queue();

                        JsonObject newSettings = currentSettings.getAsJsonObject();
                        newSettings.remove("previousMessageId");
                        newSettings.addProperty("previousMessageId", reactMessage.getId());
                        database.updateApplySettings(event.getGuild().getId(), newSettings);

                        event.getJDA().addEventListener(new ApplyGuild(reactMessage, currentSettings));
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        event.getJDA().addEventListener(new ApplyGuild(event.getGuild().getId()));
    }


    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        try {
            JsonElement currentSettings = database.getApplySettings(event.getGuild().getId());
            if (currentSettings != null) {
                if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                    if (isUniqueApplyGuild(event.getGuild().getId())) {
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                        try {
                            Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                            if (reactMessage != null) {
                                event.getJDA().addEventListener(new ApplyGuild(reactMessage, currentSettings));
                                return;
                            }
                        } catch (Exception ignored) {
                        }

                        reactChannel.sendMessage(loadingEmbed().build()).complete();
                        reactChannel.sendMessage(loadingEmbed().build()).complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        EmbedBuilder eb = defaultEmbed("Apply For Guild");
                        eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());
                        Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                        reactMessage.addReaction("✅").queue();

                        JsonObject newSettings = currentSettings.getAsJsonObject();
                        newSettings.remove("previousMessageId");
                        newSettings.addProperty("previousMessageId", reactMessage.getId());
                        database.updateApplySettings(event.getGuild().getId(), newSettings);

                        event.getJDA().addEventListener(new ApplyGuild(reactMessage, currentSettings));

                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        event.getJDA().addEventListener(new ApplyGuild(event.getGuild().getId()));
    }

}