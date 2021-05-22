package com.skyblockplus.utils;

import static com.skyblockplus.Main.asyncHttpClient;
import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.eventlisteners.MainListener.getGuildMap;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.getPlayerDiscordInfo;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.logCommand;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.eventlisteners.AutomaticGuild;
import com.skyblockplus.eventlisteners.apply.ApplyGuild;
import com.skyblockplus.eventlisteners.apply.ApplyUser;
import com.skyblockplus.utils.structs.DiscordInfoStruct;

import net.dv8tion.jda.api.entities.User;

public class MainClassUtils {
    public static void cacheApplyGuildUsers() {
        long startTime = System.currentTimeMillis();
        if (!BOT_PREFIX.equals("+")) {
            return;
        }

        for (Map.Entry<String, AutomaticGuild> automaticGuild : getGuildMap().entrySet()) {
            List<ApplyGuild> applySettings = automaticGuild.getValue().getApplyGuild();
            for (ApplyGuild applySetting : applySettings) {
                try {
                    database.deleteApplyCacheSettings(automaticGuild.getKey(),
                            higherDepth(applySetting.currentSettings, "name").getAsString());
                    List<ApplyUser> applyUserList = applySetting.getApplyUserList();
                    if (applyUserList.size() > 0) {
                        int code = database.updateApplyCacheSettings(automaticGuild.getKey(),
                                higherDepth(applySetting.currentSettings, "name").getAsString(),
                                new Gson().toJson(applyUserList));

                        if (code == 200) {
                            System.out.println("Successfully cached ApplyUser | " + automaticGuild.getKey() + " | "
                                    + applyUserList.size());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("== Stack Trace (Cache ApplyUser - " + automaticGuild.getKey() + ")");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("== Cached apply users in " + ((System.currentTimeMillis() - startTime) / 1000) + "s ==");
    }

    public static List<ApplyUser> getApplyGuildUsersCache(String guildId, String name) {
        if (!BOT_PREFIX.equals("+")) {
            return new ArrayList<>();
        }

        JsonArray applyUsersCache = database.getApplyCacheSettings(guildId, name);

        try {
            List<ApplyUser> applyUsersCacheList = new ArrayList<>();
            for (JsonElement applyUserCache : applyUsersCache) {
                ApplyUser currentApplyUserCache = new Gson().fromJson(applyUserCache, ApplyUser.class);
                applyUsersCacheList.add(currentApplyUserCache);
            }
            if (applyUsersCacheList.size() > 0) {
                System.out.println("Retrieved cache (" + applyUsersCacheList.size() + ") - " + guildId);
                database.deleteApplyCacheSettings(guildId, name);
                return applyUsersCacheList;
            }
        } catch (Exception e) {
            System.out.println("== Stack Trace (Get cache ApplyUser - " + guildId + ")");
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public static void scheduleUpdateLinkedAccounts() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(MainClassUtils::updateLinkedAccounts, 0, 1, TimeUnit.MINUTES);
    }

    public static void closeAsyncHttpClient() {
        try {
            asyncHttpClient.close();
            System.out.println("== Successfully Closed Async Http Client ==");
        } catch (Exception e) {
            System.out.println("== Stack Trace (Close Async Http Client)");
            e.printStackTrace();
        }
    }

    public static void updateLinkedAccounts() {
        try {
            database.getLinkedUsers().stream().filter(linkedAccountModel -> Duration
                    .between(Instant.ofEpochMilli(Long.parseLong(linkedAccountModel.getLastUpdated())), Instant.now())
                    .toDays() > 1).findAny().ifPresent(notUpdated -> {
                        try {
                            DiscordInfoStruct discordInfo = getPlayerDiscordInfo(notUpdated.getMinecraftUsername());
                            User updateUser = jda.retrieveUserById(notUpdated.getDiscordId()).complete();
                            if (discordInfo.discordTag.equals(updateUser.getAsTag())) {
                                database.addLinkedUser(new LinkedAccountModel("" + Instant.now().toEpochMilli(),
                                        updateUser.getId(), discordInfo.minecraftUuid, discordInfo.minecraftUsername));
                                try {
                                    logCommand("Updated linked user: " + notUpdated.getMinecraftUsername());
                                } catch (Exception ignored) {
                                }
                                return;
                            }
                        } catch (Exception ignored) {
                        }
                        database.deleteLinkedUserByMinecraftUsername(notUpdated.getMinecraftUsername());
                        try {
                            logCommand("Error updating linked user: " + notUpdated.getMinecraftUsername());
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            System.out.println("== Stack Trace (updateGuildRoles) ==");
            e.printStackTrace();
        }
    }
}
