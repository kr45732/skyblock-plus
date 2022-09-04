/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

import static com.skyblockplus.utils.Utils.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.price.PriceSlashCommand;
import com.skyblockplus.utils.database.CacheDatabase;
import com.skyblockplus.utils.database.LeaderboardDatabase;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
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
	public static final CacheDatabase cacheDatabase = new CacheDatabase();
	public static final LeaderboardDatabase leaderboardDatabase = new LeaderboardDatabase();
	private static final Pattern minecraftUsernameRegex = Pattern.compile("^\\w+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern minecraftUuidRegex = Pattern.compile(
		"[\\da-f]{32}|[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}"
	);
	private static final Logger log = LoggerFactory.getLogger(ApiHandler.class);
	public static boolean useAlternativeAhApi = false;
	public static boolean useAlternativeApi = false;
	public static ScheduledFuture<?> updateCacheTask;

	public static void initialize() {
		try {
			reloadSettingsJson();
			cacheDatabase.initializeParties();
			scheduler.scheduleWithFixedDelay(ApiHandler::updateBotStatistics, 0, 3, TimeUnit.HOURS);
			cacheDatabase.initializeCommandUses();
			cacheDatabase.initializeJacobData();
			cacheDatabase.initializeAhTracker();
			scheduler.scheduleWithFixedDelay(cacheDatabase::updateCache, 60, 90, TimeUnit.SECONDS);
			updateCacheTask =
				scheduler.scheduleWithFixedDelay(
					() -> {
						try {
							cacheApplyGuildUsers();
							cacheParties();
							cacheCommandUses();
							cacheAhTracker();
						} catch (Exception e) {
							log.error("Exception when interval caching", e);
						}
					},
					60,
					60,
					TimeUnit.MINUTES
				);
			scheduler.scheduleWithFixedDelay(ApiHandler::updateLinkedAccounts, 60, 30, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Exception when initializing the ApiHandler", e);
		}
	}

	public static void reloadSettingsJson() {
		JsonElement settings = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/settings.json");
		useAlternativeAhApi = higherDepth(settings, "useAlternativeAhApi", false);
		useAlternativeApi = higherDepth(settings, "useAlternativeApi", false);
	}

	public static void updateBotStatistics() {
		try {
			int serverCount = jda.getGuilds().size();
			jda.setActivity(Activity.watching("/help in " + serverCount + " servers"));

			if (!isMainBot()) {
				return;
			}

			JsonObject dscBotListJson = new JsonObject();
			dscBotListJson.addProperty("guilds", serverCount);
			dscBotListJson.addProperty("users", getUserCount());
			postJson(
				"https://discordbotlist.com/api/v1/bots/" + selfUserId + "/stats",
				dscBotListJson,
				new BasicHeader(
					"Authorization",
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0IjoxLCJpZCI6Ijc5Njc5MTE2NzM2NjU5NDU5MiIsImlhdCI6MTY2MDc0ODQ0OH0._yWDDG-qXfYAyKzjYw5n76hQgyqEQ5ysbLwu1nRmKOo"
				)
			);

			JsonObject dscBotsJson = new JsonObject();
			dscBotsJson.addProperty("guildCount", serverCount);
			postJson(
				"https://discord.bots.gg/api/v1/bots/" + selfUserId + "/stats",
				dscBotsJson,
				new BasicHeader(
					"Authorization",
					"eyJhbGciOiJIUzI1NiJ9.eyJhcGkiOnRydWUsImlkIjoiMzg1OTM5MDMxNTk2NDY2MTc2IiwiaWF0IjoxNjYwNzQ4NTk0fQ.F1r05If3Sizp9M4MfBpzvrxi854HrPpcw5fINZcpEj8"
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
				asyncGetJson(
					(useAlternativeApi ? "https://playerdb.co/api/player/minecraft/" : "https://api.ashcon.app/mojang/v2/user/") + uuid
				)
					.thenApplyAsync(
						uuidToUsernameJson -> {
							try {
								String username = Utils
									.higherDepth(uuidToUsernameJson, (useAlternativeApi ? "data.player." : "") + "username")
									.getAsString();
								uuidToUsernameCache.put(uuid, username);
								return username;
							} catch (Exception ignored) {}
							return null;
						},
						executor
					);
		}

		return future;
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid) {
		return skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY);
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		return skyblockProfilesFromUuid(uuid, hypixelApiKey, true, true);
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid, String hypixelApiKey, boolean useCache, boolean shouldCache) {
		if (useCache) {
			JsonElement cachedResponse = cacheDatabase.getCachedJson(uuid);
			if (cachedResponse != null) {
				return new HypixelResponse(cachedResponse);
			}
		}

		try {
			JsonElement profilesJson = getJson(
				"https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid,
				hypixelApiKey,
				true
			);

			try {
				if (higherDepth(profilesJson, "profiles").isJsonNull()) {
					String username = uuidToUsernameCache.getIfPresent(uuid);
					return new HypixelResponse((username != null ? username : "Player") + " has no SkyBlock profiles");
				}

				JsonArray profileArray = higherDepth(profilesJson, "profiles").getAsJsonArray();
				if (shouldCache) {
					cacheDatabase.cacheJson(uuid, profileArray);
				}
				return new HypixelResponse(profileArray);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(profilesJson, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	/**
	 * Does not cache the profiles json
	 */
	public static CompletableFuture<JsonElement> asyncSkyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		CompletableFuture<JsonElement> future = new CompletableFuture<>();

		JsonElement cachedResponse = cacheDatabase.getCachedJson(uuid);
		if (cachedResponse != null) {
			future.complete(cachedResponse);
		} else {
			future =
				asyncGet("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid)
					.thenApplyAsync(
						profilesResponse -> {
							try {
								try {
									keyCooldownMap
										.get(hypixelApiKey)
										.remainingLimit()
										.set(Integer.parseInt(profilesResponse.headers().firstValue("RateLimit-Remaining").get()));
									keyCooldownMap
										.get(hypixelApiKey)
										.timeTillReset()
										.set(Integer.parseInt(profilesResponse.headers().firstValue("RateLimit-Reset").get()));
								} catch (Exception ignored) {}

								try (
									InputStreamReader in = new InputStreamReader(profilesResponse.body());
									JsonReader jsonIn = new JsonReader(in)
								) {
									JsonElement profiles = SkyblockProfilesParser.parse(jsonIn, uuid);

									// Json parsing probably takes more memory than the HTTP request
									if (Runtime.getRuntime().totalMemory() > 1250000000) {
										System.gc();
									}

									return higherDepth(profiles, "profiles").getAsJsonArray();
								}
							} catch (Exception ignored) {}
							return null;
						},
						executor
					);
		}

		return future;
	}

	public static HypixelResponse playerFromUuid(String uuid) {
		try {
			JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + uuid);

			try {
				if (higherDepth(playerJson, "player").isJsonNull()) {
					String username = uuidToUsernameCache.getIfPresent(uuid);
					return new HypixelResponse((username != null ? username : "Player") + " has not played on Hypixel");
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
		return !response.isValid() ? response : (response.get("[0]") != null ? response : new HypixelResponse("Invalid auction UUID"));
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

	public static String getQueryApiUrl(String path) {
		return (useAlternativeAhApi ? "https://query-api.up.railway.app/" : "https://query-api.herokuapp.com/") + path;
	}

	public static JsonArray getBidsFromPlayer(String uuid) {
		try {
			HttpGet httpGet = new HttpGet(getQueryApiUrl("query"));
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpGet.getURI())
				.addParameter("bids", uuid)
				.addParameter("limit", "-1")
				.addParameter("key", AUCTION_API_KEY)
				.build();
			httpGet.setURI(uri);

			try (
				CloseableHttpResponse httpResponse = Utils.httpClient.execute(httpGet);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryLowestBin(String query, boolean isName, PriceSlashCommand.AuctionType auctionType) {
		try {
			HttpGet httpGet = new HttpGet(getQueryApiUrl("query"));
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URIBuilder uriBuilder = new URIBuilder(httpGet.getURI())
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("sort", "ASC")
				.addParameter("limit", "5")
				.addParameter("key", AUCTION_API_KEY);
			if (isName) {
				uriBuilder.addParameter("item_name", "%" + query + "%");
			} else {
				uriBuilder.addParameter("item_id", query);
			}
			if (auctionType == PriceSlashCommand.AuctionType.BIN) {
				uriBuilder.addParameter("bin", "true");
			} else if (auctionType == PriceSlashCommand.AuctionType.AUCTION) {
				uriBuilder.addParameter("bin", "false");
			}
			httpGet.setURI(uriBuilder.build());

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryLowestBinPet(String petName, String rarity, PriceSlashCommand.AuctionType auctionType) {
		try {
			HttpGet httpGet = new HttpGet(getQueryApiUrl("query"));
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URIBuilder uriBuilder = new URIBuilder(httpGet.getURI())
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("item_name", "%" + petName + "%")
				.addParameter("item_id", "PET")
				.addParameter("sort", "ASC")
				.addParameter("limit", "5")
				.addParameter("key", AUCTION_API_KEY);
			if (!rarity.equals("ANY")) {
				uriBuilder.addParameter("tier", rarity);
			}
			if (auctionType == PriceSlashCommand.AuctionType.BIN) {
				uriBuilder.addParameter("bin", "true");
			} else if (auctionType == PriceSlashCommand.AuctionType.AUCTION) {
				uriBuilder.addParameter("bin", "false");
			}
			httpGet.setURI(uriBuilder.build());

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray getAuctionPetsByName(String query) {
		try {
			HttpGet httpGet = new HttpGet(getQueryApiUrl("pets"));
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpGet.getURI()).addParameter("query", query).addParameter("key", AUCTION_API_KEY).build();
			httpGet.setURI(uri);

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static void updateLinkedAccounts() {
		try {
			database
				.getLinkedAccounts()
				.stream()
				.filter(linkedAccountModel ->
					Duration.between(Instant.ofEpochMilli(linkedAccountModel.lastUpdated()), Instant.now()).toDays() > 5
				)
				.limit(60)
				.forEach(o ->
					asyncUuidToUsername(o.uuid())
						.thenApplyAsync(
							username ->
								username != null &&
								database.insertLinkedAccount(
									new LinkedAccount(Instant.now().toEpochMilli(), o.discord(), o.uuid(), username)
								),
							executor
						)
				);
		} catch (Exception e) {
			log.error("Exception when updating linked accounts", e);
		}
	}
}
