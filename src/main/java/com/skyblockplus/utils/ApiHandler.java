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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.InputStreamReader;
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiHandler {

	public static final Cache<String, String> uuidToUsernameCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
	public static final ConcurrentHashMap<String, Instant> uuidToTimeSkyblockProfiles = new ConcurrentHashMap<>();
	private static final Pattern minecraftUsernameRegex = Pattern.compile("^\\w+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern minecraftUuidRegex = Pattern.compile(
		"[0-9a-f]{32}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
	);

	public static Connection cacheDatabaseConnection;
	private static final Logger log = LoggerFactory.getLogger(ApiHandler.class);
	public static boolean useAlternativeApi = reloadSettingsJson();

	public static void initialize() {
		try {
			getCacheDatabaseConnection();
			scheduler.scheduleWithFixedDelay(ApiHandler::updateCache, 60, 90, TimeUnit.SECONDS);
			scheduler.scheduleWithFixedDelay(ApiHandler::updateLinkedAccounts, 60, 30, TimeUnit.SECONDS);
		} catch (SQLException e) {
			log.error("Exception when connecting to cache database", e);
		} catch (Exception e) {
			log.error("Exception when initializing the ApiHandler", e);
		}
	}

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
					uuidToUsernameCache.put(usernameUuidStruct.getUuid(), usernameUuidStruct.getUsername());
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
					uuidToUsernameCache.put(usernameUuidStruct.getUuid(), usernameUuidStruct.getUsername());
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
									.getRemainingLimit()
									.set(Integer.parseInt(profilesResponse.getHeader("RateLimit-Remaining")));
								keyCooldownMap
									.get(hypixelApiKey)
									.getTimeTillReset()
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

	//	public static JsonArray getBidsFromPlayer(String uuid) {
	//		try {
	//			HttpGet httpget = new HttpGet("https://query-api.kr45732.repl.co/");
	//			httpget.addHeader("content-type", "application/json; charset=UTF-8");
	//
	//			URI uri = new URIBuilder(httpget.getURI())
	//				.addParameter("query", "{\"bids\":{\"$elemMatch\":{\"bidder\":\"" + uuid + "\"}}}")
	//				.build();
	//			httpget.setURI(uri);
	//
	//			try (CloseableHttpResponse httpResponse = Utils.httpClient.execute(httpget)) {
	//				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
	//			}
	//		} catch (Exception ignored) {}
	//		return null;
	//	}

	public static JsonArray queryLowestBin(String query) {
		try {
			HttpGet httpget = new HttpGet("https://auctions.tyman.tech/query");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI())
				.addParameter(
					"query",
					"end_t > " + Instant.now().toEpochMilli()
				).addParameter("name", "%" + query + "%")
				.addParameter("sort", "starting_bid")
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
			HttpGet httpGet = new HttpGet("https://auctions.tyman.tech/query");
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpGet.getURI())
				.addParameter(
						"query",
						"end_t > " + Instant.now().toEpochMilli() +
								" AND item_id = 'PET'"
								+ (!rarity.equalsIgnoreCase("any") ? " AND tier = '" + rarity.toUpperCase()  +"'" :"")
				)
				.addParameter("name", "%" + petName + "%")
				.addParameter("sort", "starting_bid")
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

	public static JsonArray queryLowestBinEnchant(String enchantId, int enchantLevel) {
		try {
			HttpGet httpGet = new HttpGet("https://auctions.tyman.tech/query");
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpGet.getURI())
				.addParameter(
						"query",
						"end_t > " + Instant.now().toEpochMilli() +
								" AND item_id = 'ENCHANTED_BOOK' AND '" + enchantId.toUpperCase() +
								";" +
								enchantLevel
								+ "' = ANY (enchants)"
				)
				.addParameter("sort", "starting_bid")
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
		// Query should be 'name','name','name'...
		try {
			HttpGet httpget = new HttpGet("https://auctions.tyman.tech/pets");
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
}
