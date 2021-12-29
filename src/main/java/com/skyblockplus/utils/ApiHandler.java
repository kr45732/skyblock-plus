/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiHandler {

	public static final Cache<String, String> uuidToUsernameCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
	public static final ConcurrentHashMap<String, Instant> uuidToTimeSkyblockProfiles = new ConcurrentHashMap<>();
	private static final Pattern minecraftUsernameRegex = Pattern.compile("^\\w+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern minecraftUuidRegex = Pattern.compile(
		"[0-9a-f]{32}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
	);
	private static final Logger log = LoggerFactory.getLogger(ApiHandler.class);
	public static Connection cacheDatabaseConnection;

	public static void initialize() {
		try {
			getCacheDatabaseConnection();
			initializeParties();
			scheduler.scheduleWithFixedDelay(ApiHandler::updateBotStatistics, 0, 3, TimeUnit.HOURS);
			initializeCommandUses();
			initializeJacobData();
			scheduler.scheduleWithFixedDelay(ApiHandler::updateCache, 60, 90, TimeUnit.SECONDS);
			scheduler.scheduleWithFixedDelay(ApiHandler::updateLinkedAccounts, 60, 30, TimeUnit.SECONDS);
		} catch (SQLException e) {
			log.error("Exception when connecting to cache database", e);
		} catch (Exception e) {
			log.error("Exception when initializing the ApiHandler", e);
		}
	}

	public static boolean useAlternativeApi = reloadSettingsJson();

	public static boolean reloadSettingsJson() {
		useAlternativeApi =
			higherDepth(
				getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/settings.json"),
				"useAlternativeApi",
				false
			);
		return useAlternativeApi;
	}

	public static Connection getCacheDatabaseConnection() throws SQLException {
		if (cacheDatabaseConnection == null || cacheDatabaseConnection.isClosed()) {
			cacheDatabaseConnection = DriverManager.getConnection(PLANET_SCALE_URL, PLANET_SCALE_USERNAME, PLANET_SCALE_PASSWORD);
		}

		return cacheDatabaseConnection;
	}

	public static void updateBotStatistics() {
		try {
			int serverCount = jda.getGuilds().size();
			jda.getPresence().setActivity(Activity.watching(DEFAULT_PREFIX + "help in " + serverCount + " servers"));

			if (!isMainBot()) {
				return;
			}

			String selfUserId = jda.getSelfUser().getId();

			JsonObject dscBotListJson = new JsonObject();
			dscBotListJson.addProperty("guilds", serverCount);
			dscBotListJson.addProperty("users", getUserCount());
			postJson(
				"https://discordbotlist.com/api/v1/bots/" + selfUserId + "/stats",
				dscBotListJson,
				new BasicHeader(
					"Authorization",
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0IjoxLCJpZCI6Ijc5Njc5MTE2NzM2NjU5NDU5MiIsImlhdCI6MTYzOTUyODcwNn0.eJ6ikA4fIPJI9W-lJkHs-oxNNGKsWPbH8cU7oVEbNYY"
				)
			);

			JsonObject dscBotsJson = new JsonObject();
			dscBotsJson.addProperty("guildCount", serverCount);
			postJson(
				"https://discord.bots.gg/api/v1/bots/" + selfUserId + "/stats",
				dscBotsJson,
				new BasicHeader(
					"Authorization",
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcGkiOnRydWUsImlkIjoiMzg1OTM5MDMxNTk2NDY2MTc2IiwiaWF0IjoxNjM5NTI5NjAyfQ.OsD5zgKVTgSh6IG34GGBsHPGoK7QTlkcHeksKRnIcWA"
				)
			);

			//	JsonObject discordsJson = new JsonObject();
			//	discordsJson.addProperty("server_count", serverCount);
			//	postJson("https://discords.com/bots/api/bot/" + selfUserId, discordsJson, new BasicHeader("Authorization", "c46ba888473968c1e1c9ddc7e11c515abf1b85ece6df62a21fed3e9dcae4efd4b62d2dc9e2637c11b3eda05dd668630444c33e6add140da5ec50a95521f38004"));

			JsonObject topGgJson = new JsonObject();
			topGgJson.addProperty("server_count", serverCount);
			postJson(
				"https://top.gg/api/bots/" + selfUserId + "/stats",
				topGgJson,
				new BasicHeader(
					"Authorization",
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Ijc5Njc5MTE2NzM2NjU5NDU5MiIsImJvdCI6dHJ1ZSwiaWF0IjoxNjM5NTMyMTI2fQ.YN6n3mXXgbENfQv1k8OylJV6tfZHqEFgciVGGt_Tsa0"
				)
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isValidMinecraftUsername(String username) {
		return username.length() > 2 && username.length() < 17 && minecraftUsernameRegex.matcher(username).find();
	}

	public static boolean isValidMinecraftUuid(String username) {
		return minecraftUuidRegex.matcher(username).matches();
	}

	public static UsernameUuidStruct usernameToUuid(String username) {
		if (!isValidMinecraftUsername(username)) {
			if (!isValidMinecraftUuid(username)) {
				return new UsernameUuidStruct("No user with the name '" + username + "' was found");
			}
		}

		Map.Entry<String, String> cachedResponse = uuidToUsernameCache
			.asMap()
			.entrySet()
			.stream()
			.filter(entry -> entry.getValue().equalsIgnoreCase(username))
			.findFirst()
			.orElse(null);
		if (cachedResponse != null) {
			return new UsernameUuidStruct(cachedResponse.getValue(), cachedResponse.getKey());
		}

		return uuidUsername(username);
	}

	public static UsernameUuidStruct uuidToUsername(String uuid) {
		String cachedResponse = uuidToUsernameCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			return new UsernameUuidStruct(cachedResponse, uuid);
		}

		return uuidUsername(uuid);
	}

	private static UsernameUuidStruct uuidUsername(String username) {
		try {
			if (!useAlternativeApi) {
				JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + username);
				try {
					UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
						higherDepth(usernameJson, "username").getAsString(),
						higherDepth(usernameJson, "uuid").getAsString().replace("-", "")
					);
					uuidToUsernameCache.put(usernameUuidStruct.uuid(), usernameUuidStruct.username());
					return usernameUuidStruct;
				} catch (Exception e) {
					return new UsernameUuidStruct(higherDepth(usernameJson, "reason").getAsString());
				}
			} else {
				JsonElement usernameJson = getJson("https://playerdb.co/api/player/minecraft/" + username);
				try {
					UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
						higherDepth(usernameJson, "data.player.username").getAsString(),
						higherDepth(usernameJson, "data.player.id").getAsString().replace("-", "")
					);
					uuidToUsernameCache.put(usernameUuidStruct.uuid(), usernameUuidStruct.username());
					return usernameUuidStruct;
				} catch (Exception e) {
					return new UsernameUuidStruct(higherDepth(usernameJson, "code").getAsString());
				}
			}
		} catch (Exception ignored) {}
		return new UsernameUuidStruct();
	}

	public static List<String> getNameHistory(String uuid) {
		try {
			List<String> nameHistory = new ArrayList<>();

			if (!useAlternativeApi) {
				JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + uuid);
				String username = higherDepth(usernameJson, "username").getAsString();
				for (JsonElement name : higherDepth(usernameJson, "username_history").getAsJsonArray()) {
					if (!higherDepth(name, "username").getAsString().equals(username)) {
						nameHistory.add(higherDepth(name, "username").getAsString());
					}
				}
			} else {
				JsonElement usernameJson = higherDepth(getJson("https://playerdb.co/api/player/minecraft/" + uuid), "data.player");
				String username = higherDepth(usernameJson, "username").getAsString();
				for (JsonElement name : higherDepth(usernameJson, "meta.name_history").getAsJsonArray()) {
					if (!higherDepth(name, "name").getAsString().equals(username)) {
						nameHistory.add(higherDepth(name, "name").getAsString());
					}
				}
			}
			return nameHistory;
		} catch (Exception ignored) {}
		return new ArrayList<>();
	}

	public static CompletableFuture<String> asyncUuidToUsername(String uuid) {
		CompletableFuture<String> future = new CompletableFuture<>();

		String cachedResponse = uuidToUsernameCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			future.complete(cachedResponse);
		} else {
			future =
				asyncHttpClient
					.prepareGet(
						(useAlternativeApi ? "https://playerdb.co/api/player/minecraft/" : "https://api.ashcon.app/mojang/v2/user/") + uuid
					)
					.execute()
					.toCompletableFuture()
					.thenApply(uuidToUsernameResponse -> {
						try {
							String username = Utils
								.higherDepth(
									JsonParser.parseString(uuidToUsernameResponse.getResponseBody()),
									(useAlternativeApi ? "data.player." : "") + "username"
								)
								.getAsString();
							uuidToUsernameCache.put(uuid, username);
							return username;
						} catch (Exception ignored) {}
						return null;
					});
		}

		return future;
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid) {
		return skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY);
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		JsonElement cachedResponse = getCachedJson(uuid);
		if (cachedResponse != null) {
			return new HypixelResponse(cachedResponse);
		}

		try {
			JsonElement profilesJson = getJson("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid);

			try {
				if (higherDepth(profilesJson, "profiles").isJsonNull()) {
					return new HypixelResponse("Player has no SkyBlock profiles");
				}

				JsonArray profileArray = processSkyblockProfilesArray(higherDepth(profilesJson, "profiles").getAsJsonArray());
				cacheJson(uuid, profileArray);
				return new HypixelResponse(profileArray);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(profilesJson, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static CompletableFuture<JsonElement> asyncSkyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		CompletableFuture<JsonElement> future = new CompletableFuture<>();

		JsonElement cachedResponse = getCachedJson(uuid);
		if (cachedResponse != null) {
			future.complete(cachedResponse);
		} else {
			future =
				asyncHttpClient
					.prepareGet("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid)
					.execute()
					.toCompletableFuture()
					.thenApply(profilesResponse -> {
						try {
							try {
								keyCooldownMap
									.get(hypixelApiKey)
									.remainingLimit()
									.set(Integer.parseInt(profilesResponse.getHeader("RateLimit-Remaining")));
								keyCooldownMap
									.get(hypixelApiKey)
									.timeTillReset()
									.set(Integer.parseInt(profilesResponse.getHeader("RateLimit-Reset")));
							} catch (Exception ignored) {}

							JsonArray profileArray = processSkyblockProfilesArray(
								higherDepth(JsonParser.parseString(profilesResponse.getResponseBody()), "profiles").getAsJsonArray()
							);

							cacheJson(uuid, profileArray);

							return profileArray;
						} catch (Exception ignored) {}
						return null;
					});
		}

		return future;
	}

	public static HypixelResponse playerFromUuid(String uuid) {
		try {
			JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + uuid);

			try {
				if (higherDepth(playerJson, "player").isJsonNull()) {
					return new HypixelResponse("Player has not played on Hypixel");
				}

				JsonObject playerObject = higherDepth(playerJson, "player").getAsJsonObject();
				return new HypixelResponse(playerObject);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(playerJson, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static HypixelResponse getAuctionGeneric(String query) {
		try {
			JsonElement auctionResponse = getJson("https://api.hypixel.net/skyblock/auction?key=" + HYPIXEL_API_KEY + query);
			try {
				return new HypixelResponse(higherDepth(auctionResponse, "auctions").getAsJsonArray());
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(auctionResponse, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static HypixelResponse getAuctionFromPlayer(String playerUuid) {
		return getAuctionGeneric("&player=" + playerUuid);
	}

	public static HypixelResponse getAuctionFromUuid(String auctionUuid) {
		HypixelResponse response = getAuctionGeneric("&uuid=" + auctionUuid);
		return response.isNotValid() ? response : (response.get("[0]") != null ? response : new HypixelResponse("Invalid auction UUID"));
	}

	public static HypixelResponse getGuildGeneric(String query) {
		try {
			JsonElement guildResponse = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + query);

			try {
				if (higherDepth(guildResponse, "guild").isJsonNull()) {
					if (query.startsWith("&player=")) {
						return new HypixelResponse("Player is not in a guild");
					} else if (query.startsWith("&id=")) {
						return new HypixelResponse("Invalid guild id");
					} else if (query.startsWith("&name=")) {
						return new HypixelResponse("Invalid guild name");
					}
				}
				return new HypixelResponse(higherDepth(guildResponse, "guild").getAsJsonObject());
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(guildResponse, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static HypixelResponse getGuildFromPlayer(String playerUuid) {
		return getGuildGeneric("&player=" + playerUuid);
	}

	public static HypixelResponse getGuildFromId(String guildId) {
		return getGuildGeneric("&id=" + guildId);
	}

	public static HypixelResponse getGuildFromName(String guildName) {
		return getGuildGeneric("&name=" + guildName.replace(" ", "%20").replace("_", "%20"));
	}

	public static JsonArray getBidsFromPlayer(String uuid) {
		try {
			HttpGet httpget = new HttpGet("http://venus.arcator.co.uk:1194/query");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI())
				.addParameter("bids", uuid)
				.addParameter("limit", "-1")
				.addParameter("key", AUCTION_API_KEY)
				.build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = Utils.httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryLowestBin(String query) {
		try {
			HttpGet httpget = new HttpGet("http://venus.arcator.co.uk:1194/query");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI())
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("item_name", "%" + query + "%")
				.addParameter("bin", "true")
				.addParameter("sort", "ASC")
				.addParameter("limit", "1")
				.addParameter("key", AUCTION_API_KEY)
				.build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryLowestBinPet(String petName, String rarity) {
		try {
			HttpGet httpGet = new HttpGet("http://venus.arcator.co.uk:1194/query");
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URIBuilder uri = new URIBuilder(httpGet.getURI())
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("item_name", "%" + petName + "%")
				.addParameter("item_id", "PET")
				.addParameter("bin", "true")
				.addParameter("sort", "ASC")
				.addParameter("limit", "1")
				.addParameter("key", AUCTION_API_KEY);
			if (!rarity.equals("ANY")) {
				uri.addParameter("tier", rarity);
			}
			httpGet.setURI(uri.build());

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryLowestBinEnchant(String enchantId, int enchantLevel) {
		try {
			HttpGet httpGet = new HttpGet("http://venus.arcator.co.uk:1194/query");
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpGet.getURI())
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("item_id", "ENCHANTED_BOOK")
				.addParameter("enchants", enchantId.toUpperCase() + ";" + enchantLevel)
				.addParameter("bin", "true")
				.addParameter("sort", "ASC")
				.addParameter("limit", "1")
				.addParameter("key", AUCTION_API_KEY)
				.build();
			httpGet.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray getAuctionPetsByName(String query) {
		try {
			HttpGet httpget = new HttpGet("http://venus.arcator.co.uk:1194/pets");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI()).addParameter("query", query).addParameter("key", AUCTION_API_KEY).build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static void cacheJson(String playerUuid, JsonElement json) {
		executor.submit(() -> {
			try (Statement statement = getCacheDatabaseConnection().createStatement()) {
				Instant now = Instant.now();
				uuidToTimeSkyblockProfiles.put(playerUuid, now);

				statement.executeUpdate(
					"INSERT INTO profiles VALUES ('" +
					playerUuid +
					"', " +
					now.toEpochMilli() +
					", '" +
					json +
					"') ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), time = VALUES(time), data = VALUES(data)"
				);
			} catch (Exception ignored) {}
		});
	}

	public static JsonElement getCachedJson(String playerUuid) {
		Instant lastUpdated = uuidToTimeSkyblockProfiles.getOrDefault(playerUuid, null);
		if (lastUpdated != null && Duration.between(lastUpdated, Instant.now()).toMillis() > 90000) {
			deleteCachedJson(playerUuid);
		} else {
			try (Statement statement = getCacheDatabaseConnection().createStatement()) {
				try (ResultSet response = statement.executeQuery("SELECT * FROM profiles where uuid = '" + playerUuid + "'")) {
					if (response.next()) {
						Instant lastUpdatedResponse = Instant.ofEpochMilli(response.getLong("time"));
						if (Duration.between(lastUpdatedResponse, Instant.now()).toMillis() > 90000) {
							deleteCachedJson(playerUuid);
						} else {
							uuidToTimeSkyblockProfiles.put(playerUuid, lastUpdatedResponse);
							return JsonParser.parseString(response.getString("data"));
						}
					}
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	public static void deleteCachedJson(String... playerUuids) {
		if (playerUuids.length == 0) {
			return;
		}

		executor.submit(() -> {
			StringBuilder query = new StringBuilder();
			for (String playerUuid : playerUuids) {
				uuidToTimeSkyblockProfiles.remove(playerUuid);
				query.append("'").append(playerUuid).append("',");
			}
			if (query.charAt(query.length() - 1) == ',') {
				query.deleteCharAt(query.length() - 1);
			}

			try (Statement statement = getCacheDatabaseConnection().createStatement()) {
				statement.executeUpdate("DELETE FROM profiles WHERE uuid IN (" + query + ")");
			} catch (Exception ignored) {}
		});
	}

	public static void updateCache() {
		long now = Instant.now().minusSeconds(90).toEpochMilli();
		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			try (ResultSet response = statement.executeQuery("SELECT uuid FROM profiles WHERE time < " + now + "")) {
				List<String> expiredCacheUuidList = new ArrayList<>();
				while (response.next()) {
					expiredCacheUuidList.add(response.getString("uuid"));
				}
				deleteCachedJson(expiredCacheUuidList.toArray(new String[0]));
			}
		} catch (Exception ignored) {}

		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			statement.executeUpdate("DELETE FROM profiles WHERE time < " + now + "");
		} catch (Exception ignored) {}
	}

	public static JsonArray processSkyblockProfilesArray(JsonArray array) {
		for (int i = 0; i < array.size(); i++) {
			JsonObject currentProfile = array.get(i).getAsJsonObject();
			currentProfile.remove("community_upgrades");

			JsonObject currentProfileMembers = higherDepth(currentProfile, "members").getAsJsonObject();
			for (String currentProfileMemberUuid : currentProfileMembers.keySet()) {
				JsonObject currentProfileMember = currentProfileMembers.getAsJsonObject(currentProfileMemberUuid);
				currentProfileMember.remove("stats");
				currentProfileMember.remove("objectives");
				currentProfileMember.remove("tutorial");
				currentProfileMember.remove("quests");
				currentProfileMember.remove("visited_zones");
				currentProfileMember.remove("griffin");
				currentProfileMember.remove("experimentation");
				currentProfileMember.remove("unlocked_coll_tiers");
				currentProfileMember.remove("backpack_icons");
				currentProfileMember.remove("achievement_spawned_island_types");
				currentProfileMember.remove("slayer_quest");

				currentProfileMembers.add(currentProfileMemberUuid, currentProfileMember);
			}

			currentProfile.add("members", currentProfileMembers);
			array.set(i, currentProfile);
		}
		return array;
	}

	public static void updateLinkedAccounts() {
		try {
			database
				.getLinkedUsers()
				.stream()
				.filter(linkedAccountModel ->
					Duration.between(Instant.ofEpochMilli(Long.parseLong(linkedAccountModel.getLastUpdated())), Instant.now()).toDays() > 5
				)
				.limit(10)
				.forEach(o ->
					asyncUuidToUsername(o.getMinecraftUuid())
						.thenApply(username -> {
							if (username != null) {
								database.addLinkedUser(
									new LinkedAccountModel(
										"" + Instant.now().toEpochMilli(),
										o.getDiscordId(),
										o.getMinecraftUuid(),
										username
									)
								);
							}
							return null;
						})
				);
		} catch (Exception e) {
			log.error("Exception when updating linked accounts", e);
		}
	}

	public static int cachePartySettings(String guildId, String json) {
		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			statement.executeUpdate(
				"INSERT INTO party VALUES ('" +
				guildId +
				"', '" +
				json +
				"') ON DUPLICATE KEY UPDATE guild_id = VALUES(guild_id), data = VALUES(data)"
			);
			return 200;
		} catch (Exception e) {
			return 400;
		}
	}

	public static int cacheCommandUseDb(String json) {
		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			statement.executeUpdate("INSERT INTO commands VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			return 200;
		} catch (Exception e) {
			return 400;
		}
	}

	public static int cacheJacobDataDb(String json) {
		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			statement.executeUpdate("INSERT INTO jacob VALUES (0, '" + json + "') ON DUPLICATE KEY UPDATE data = VALUES(data)");
			return 200;
		} catch (Exception e) {
			return 400;
		}
	}

	public static void initializeCommandUses() {
		if (!isMainBot()) {
			return;
		}

		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			try (ResultSet response = statement.executeQuery("SELECT * FROM commands")) {
				Type typeMapStringInt = new TypeToken<Map<String, Integer>>() {}.getType();
				response.next();
				Map<String, Integer> commandUsage = gson.fromJson(response.getString("data"), typeMapStringInt);
				slashCommandClient.setCommandUses(commandUsage);
				log.info("Retrieved command uses");
			}
		} catch (Exception e) {
			log.error("initializeCommandUses", e);
		}
	}

	public static void initializeJacobData() {
		if (!isMainBot()) {
			return;
		}

		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			try (ResultSet response = statement.executeQuery("SELECT * FROM jacob")) {
				response.next();
				JacobData data = gson.fromJson(response.getString("data"), JacobData.class);
				JacobHandler.setJacobData(data);
				log.info("Retrieved jacob data");
			}
		} catch (Exception e) {
			log.error("initializeJacobData", e);
		}
	}

	public static void initializeParties() {
		if (!isMainBot()) {
			return;
		}

		List<String> toDeleteIds = new ArrayList<>();
		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			try (ResultSet response = statement.executeQuery("SELECT * FROM party")) {
				Type partyListType = new TypeToken<List<Party>>() {}.getType();
				while (response.next()) {
					String guildId = null;
					try {
						guildId = response.getString("guild_id");
						toDeleteIds.add(guildId);
						List<Party> partyList = gson.fromJson(response.getString("data"), partyListType);
						guildMap.get(guildId).setPartyList(partyList);
						log.info("Retrieved party cache (" + partyList.size() + ") - guildId={" + guildId + "}");
					} catch (Exception e) {
						log.error("initializeParties guildId={" + guildId + "}", e);
					}
				}
			}
		} catch (Exception ignored) {}

		try (Statement statement = getCacheDatabaseConnection().createStatement()) {
			statement.executeUpdate("DELETE FROM party WHERE guild_id IN (" + String.join(",", toDeleteIds) + ")");
		} catch (Exception ignored) {}
	}
}
