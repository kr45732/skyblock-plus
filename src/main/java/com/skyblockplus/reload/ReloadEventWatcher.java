package com.skyblockplus.reload;

import com.google.gson.JsonElement;
import com.skyblockplus.apply.ApplyGuild;
import com.skyblockplus.verify.VerifyGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.higherDepth;

public class ReloadEventWatcher extends ListenerAdapter {
    private static final Map<String, ReloadEventWatcherClass> applyGuildEventListeners = new HashMap<>();
    private static final Map<String, ReloadEventWatcherClass> verifyGuildEventListeners = new HashMap<>();

    public static boolean isUniqueApplyGuild(String guildId) {
        return !applyGuildEventListeners.containsKey(guildId);
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

    public static void removeApplyDeletedEventListeners() {
        List<Object> registeredListeners = jda.getRegisteredListeners();
        System.out.println(registeredListeners);
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

                JsonElement currentSettings = database.getApplySettings(guildId);
                if (currentSettings != null) {
                    if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                        TextChannel reactChannel = jda.getGuildById(guildId)
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        EmbedBuilder eb = defaultEmbed("Apply For Guild");
                        eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());
                        Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                        reactMessage.addReaction("✅").queue();

                        jda.removeEventListener(applyGuildListenerObject.getGuildEventListener());
                        applyGuildEventListeners.remove(guildId);

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
        return !verifyGuildEventListeners.containsKey(guildId);
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
                JsonElement currentSettings = database.getVerifySettings(guildId);
                if (currentSettings != null) {
                    if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                        TextChannel reactChannel = jda.getGuildById(guildId)
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        String verifyText = higherDepth(currentSettings, "messageText").getAsString();
                        reactChannel.sendMessage(verifyText).queue();
                        Message reactMessage = reactChannel
                                .sendFile(new File("src/main/java/com/skyblockplus/verify/Link_Discord_To_Hypixel.mp4"))
                                .complete();
                        reactMessage.addReaction("✅").queue();

                        jda.removeEventListener(verifyGuildListenerObject.getGuildEventListener());
                        verifyGuildEventListeners.remove(guildId);

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

    @Override
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();
    }
}
