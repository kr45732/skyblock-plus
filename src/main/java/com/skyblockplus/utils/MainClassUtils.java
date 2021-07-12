package com.skyblockplus.utils;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainClassUtils {

	private static final Logger log = LoggerFactory.getLogger(MainClassUtils.class);

	public static void cacheApplyGuildUsers() {
		long startTime = System.currentTimeMillis();
		if (!DEFAULT_PREFIX.equals("+")) {
			return;
		}

		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			List<ApplyGuild> applySettings = automaticGuild.getValue().applyGuild;
			for (ApplyGuild applySetting : applySettings) {
				try {
					database.deleteApplyCacheSettings(
						automaticGuild.getKey(),
						higherDepth(applySetting.currentSettings, "name").getAsString()
					);
					List<ApplyUser> applyUserList = applySetting.applyUserList;
					if (applyUserList.size() > 0) {
						int code = database.setApplyCacheSettings(
							automaticGuild.getKey(),
							higherDepth(applySetting.currentSettings, "name").getAsString(),
							new Gson().toJson(applyUserList)
						);

						if (code == 200) {
							log.info("Successfully cached ApplyUser | " + automaticGuild.getKey() + " | " + applyUserList.size());
						}
					}
				} catch (Exception e) {
					log.error("cacheApplyGuildUsers - " + automaticGuild.getKey(), e);
				}
			}
		}
		log.info("Cached apply users in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
	}

	public static List<ApplyUser> getApplyGuildUsersCache(String guildId, String name) {
		if (!DEFAULT_PREFIX.equals("+")) {
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
				log.info("Retrieved cache (" + applyUsersCacheList.size() + ") - guildId={" + guildId + "}, name={" + name + "}");
				database.deleteApplyCacheSettings(guildId, name);
				return applyUsersCacheList;
			}
		} catch (Exception e) {
			log.error("getApplyGuildUsersCache(guildId={" + guildId + "}, name={" + name + "})", e);
		}

		return new ArrayList<>();
	}

	public static void scheduleUpdateLinkedAccounts() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(MainClassUtils::updateLinkedAccounts, 1, 1, TimeUnit.MINUTES);
	}

	public static void closeAsyncHttpClient() {
		try {
			asyncHttpClient.close();
			log.info("Successfully Closed Async Http Client");
		} catch (Exception e) {
			log.error("closeAsyncHttpClient()", e);
		}
	}

	public static void closeHttpClient() {
		try {
			httpClient.close();
			log.info("Successfully Closed Http Client");
		} catch (Exception e) {
			log.error("closeHttpClient()", e);
		}
	}

	public static void updateLinkedAccounts() {
		try {
			database
				.getLinkedUsers()
				.stream()
				.filter(
					linkedAccountModel ->
						Duration
							.between(Instant.ofEpochMilli(Long.parseLong(linkedAccountModel.getLastUpdated())), Instant.now())
							.toDays() >
						1
				)
				.findAny()
				.ifPresent(
					notUpdated -> {
						try {
							DiscordInfoStruct discordInfo = getPlayerDiscordInfo(notUpdated.getMinecraftUsername());
							User updateUser = jda.retrieveUserById(notUpdated.getDiscordId()).complete();
							if (discordInfo.discordTag.equals(updateUser.getAsTag())) {
								database.addLinkedUser(
									new LinkedAccountModel(
										"" + Instant.now().toEpochMilli(),
										updateUser.getId(),
										discordInfo.minecraftUuid,
										discordInfo.minecraftUsername
									)
								);
								try {
									logCommand("Updated linked user: " + notUpdated.getMinecraftUsername());
								} catch (Exception ignored) {}
								return;
							}
						} catch (Exception ignored) {}
						database.deleteLinkedUserByMinecraftUsername(notUpdated.getMinecraftUsername());
						try {
							logCommand("Error updating linked user: " + notUpdated.getMinecraftUsername());
						} catch (Exception ignored) {}
					}
				);
		} catch (Exception e) {
			log.error("updateLinkedAccounts()", e);
		}
	}
}
