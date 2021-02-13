package com.skyblockplus.reload;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.higherDepth;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.apply.ApplyGuild;
import com.skyblockplus.verify.VerifyGuild;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ReloadEventWatcher extends ListenerAdapter {
    public static Map<String, ReloadEventWatcherClass> applyGuildEventListeners = new HashMap<>();
    public static Map<String, ReloadEventWatcherClass> verifyGuildEventListeners = new HashMap<>();
    public static JDA jda;

    @Override
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();
    }

    public static boolean isUniqueApplyGuild(String guildId) {
        if (!applyGuildEventListeners.containsKey(guildId)) {
            return true;
        }
        return false;
    }

    public static void addApplyGuild(String guildId, Object applyGuildEventListener) {
        if (!applyGuildEventListeners.containsKey(guildId)) {
            ReloadEventWatcherClass newApplyGuild = new ReloadEventWatcherClass(guildId, applyGuildEventListener);
            applyGuildEventListeners.put(guildId, newApplyGuild);
        }
    }

    public static void addApplySubEventListener(String guildId, Object applySubEventListener) {
        if (applyGuildEventListeners.get(guildId) != null) {
            applyGuildEventListeners.replace(guildId,
                    applyGuildEventListeners.get(guildId).addSubEventListener(applySubEventListener));
        }
    }

    public static void removeApplySubEventListener(String guildId, Object applySubEventListener) {
        if (applyGuildEventListeners.get(guildId) != null) {
            applyGuildEventListeners.replace(guildId,
                    applyGuildEventListeners.get(guildId).removeSubEventListener(applySubEventListener));
        }
    }

    public static void removeApplyDeletedEventListeners() {
        List<Object> registeredListeners = jda.getRegisteredListeners();
        for (ReloadEventWatcherClass currentGuild : applyGuildEventListeners.values()) {
            String currentGuildId = currentGuild.getGuildId();
            List<Object> currentApplySubListeners = currentGuild.getSubEventListeners();

            List<Object> tempApplySubEventListenersList = new ArrayList<>();
            for (Object currentApplyUserListener : currentApplySubListeners) {
                if (registeredListeners.contains(currentApplyUserListener)) {
                    tempApplySubEventListenersList.add(currentApplyUserListener);
                }
            }
            applyGuildEventListeners.replace(currentGuildId,
                    applyGuildEventListeners.get(currentGuildId).setGuildEventListener(tempApplySubEventListenersList));
        }
    }

    public static String onApplyReload(String guildId) {
        removeApplyDeletedEventListeners();
        try {
            ReloadEventWatcherClass applyGuildListenerObject = applyGuildEventListeners.get(guildId);
            if (applyGuildListenerObject.getSubEventListeners().size() == 0) {
                jda.removeEventListener(applyGuildListenerObject.getGuildEventListener());
                applyGuildEventListeners.remove(guildId);

                JsonElement settings = JsonParser
                        .parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json"));
                if (higherDepth(settings, guildId) != null) {
                    if (higherDepth(higherDepth(higherDepth(settings, guildId), "automatedApplication"), "enable")
                            .getAsBoolean()) {
                        JsonElement currentSettings = higherDepth(higherDepth(settings, guildId),
                                "automatedApplication");

                        TextChannel reactChannel = jda.getGuildById(guildId)
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        EmbedBuilder eb = defaultEmbed("Apply For Guild", null);
                        eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());
                        Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                        reactMessage.addReaction("✅").queue();

                        jda.addEventListener(new ApplyGuild(reactMessage, currentSettings));

                        return "Apply settings successfully reloaded";

                    }
                }
            } else {
                return "Apply settings not reloaded. There is currently an application in progress";
            }
        } catch (Exception ignored) {
        }
        return "There was a problem reloading the apply settings...";

    }

    public static boolean isUniqueVerifyGuild(String guildId) {
        if (!verifyGuildEventListeners.containsKey(guildId)) {
            return true;
        }
        return false;
    }

    public static void addVerifyGuild(String guildId, Object verifyGuildEventListener) {
        if (!verifyGuildEventListeners.containsKey(guildId)) {
            ReloadEventWatcherClass newVerifyGuild = new ReloadEventWatcherClass(guildId, verifyGuildEventListener);
            verifyGuildEventListeners.put(guildId, newVerifyGuild);
        }
    }

    public static void addVerifySubEventListener(String guildId, Object verifySubEventListener) {
        if (verifyGuildEventListeners.get(guildId) != null) {
            verifyGuildEventListeners.replace(guildId,
                    verifyGuildEventListeners.get(guildId).addSubEventListener(verifySubEventListener));
        }
    }

    public static void removeVerifySubEventListener(String guildId, Object verifySubEventListener) {
        if (verifyGuildEventListeners.get(guildId) != null) {
            verifyGuildEventListeners.replace(guildId,
                    verifyGuildEventListeners.get(guildId).removeSubEventListener(verifySubEventListener));
        }
    }

    public static void removeVerifyDeletedEventListeners() {
        List<Object> registeredListeners = jda.getRegisteredListeners();
        for (ReloadEventWatcherClass currentGuild : verifyGuildEventListeners.values()) {
            String currentGuildId = currentGuild.getGuildId();
            List<Object> currentVerifySubListeners = currentGuild.getSubEventListeners();

            List<Object> tempVerifySubEventListenersList = new ArrayList<>();
            for (Object currentVerifyUserListener : currentVerifySubListeners) {
                if (registeredListeners.contains(currentVerifyUserListener)) {
                    tempVerifySubEventListenersList.add(currentVerifyUserListener);
                }
            }
            verifyGuildEventListeners.replace(currentGuildId, verifyGuildEventListeners.get(currentGuildId)
                    .setGuildEventListener(tempVerifySubEventListenersList));
        }
    }

    public static String onVerifyReload(String guildId) {
        removeVerifyDeletedEventListeners();
        try {
            ReloadEventWatcherClass verifyGuildListenerObject = verifyGuildEventListeners.get(guildId);
            if (verifyGuildListenerObject.getSubEventListeners().size() == 0) {
                jda.removeEventListener(verifyGuildListenerObject.getGuildEventListener());
                verifyGuildEventListeners.remove(guildId);

                JsonElement settings = JsonParser
                        .parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json"));
                if (higherDepth(settings, guildId) != null) {
                    if (higherDepth(higherDepth(higherDepth(settings, guildId), "automatedVerify"), "enable")
                            .getAsBoolean()) {
                        JsonElement currentSettings = higherDepth(higherDepth(settings, guildId), "automatedVerify");

                        TextChannel reactChannel = jda.getGuildById(guildId)
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

                        jda.addEventListener(new VerifyGuild(reactMessage, currentSettings));

                        return "Verify settings successfully reloaded";

                    }
                }
            } else {
                return "Verify settings not reloaded. There is currently an application in progress";
            }
        } catch (Exception ignored) {
        }
        return "There was a problem reloading the verify settings...";

    }
}
