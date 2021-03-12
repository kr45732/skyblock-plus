package com.skyblockplus.verify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueVerifyGuild;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

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
                        try {
                            Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                            if (reactMessage != null) {
                                reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                                event.getJDA().addEventListener(new VerifyGuild(reactMessage, currentSettings));
                                return;
                            }
                        } catch (Exception ignored) {
                        }

                        Message reactMessage = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                                .addFile(new File("src/main/java/com/skyblockplus/verify/Link_Discord_To_Hypixel.mp4"))
                                .complete();
                        reactMessage.addReaction("✅").queue();

                        JsonObject newSettings = currentSettings.getAsJsonObject();
                        newSettings.remove("previousMessageId");
                        newSettings.addProperty("previousMessageId", reactMessage.getId());
                        database.updateVerifySettings(event.getGuild().getId(), newSettings);

                        event.getJDA().addEventListener(new VerifyGuild(reactMessage, currentSettings));
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        event.getJDA().addEventListener(new VerifyGuild(event.getGuild().getId()));
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        try {
            JsonElement currentSettings = database.getVerifySettings(event.getGuild().getId());
            if (currentSettings != null) {
                if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                    if (isUniqueVerifyGuild(event.getGuild().getId())) {
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
                        try {
                            Message reactMessage = reactChannel.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString()).complete();
                            if (reactMessage != null) {
                                reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

                                event.getJDA().addEventListener(new VerifyGuild(reactMessage, currentSettings));
                                return;
                            }
                        } catch (Exception ignored) {
                        }

                        Message reactMessage = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
                                .addFile(new File("src/main/java/com/skyblockplus/verify/Link_Discord_To_Hypixel.mp4"))
                                .complete();
                        reactMessage.addReaction("✅").queue();

                        JsonObject newSettings = currentSettings.getAsJsonObject();
                        newSettings.remove("previousMessageId");
                        newSettings.addProperty("previousMessageId", reactMessage.getId());
                        database.updateVerifySettings(event.getGuild().getId(), newSettings);

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