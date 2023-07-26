/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.utils.HttpUtils.*;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.Utils.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.price.PriceSlashCommand;
import com.skyblockplus.utils.database.CacheDatabase;
import com.skyblockplus.utils.database.LeaderboardDatabase;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiHandler {

	public static final CacheDatabase cacheDatabase = new CacheDatabase();
	public static final LeaderboardDatabase leaderboardDatabase = new LeaderboardDatabase();
	private static final Cache<String, String> uuidToUsernameCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
	private static final Pattern minecraftUsernameRegex = Pattern.compile("^\\w+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern minecraftUuidRegex = Pattern.compile(
		"[\\da-f]{32}|[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}"
	);
	private static final Logger log = LoggerFactory.getLogger(ApiHandler.class);
	private static String ahApiUrl;
	private static int mojangApiNum = 0;
	private static boolean allowMojangApi = false;
	private static String neuBranch = null;

	private static String hasteUrl = null;

	public static void initialize() {
		try {
			cacheDatabase.initializeParties();
			scheduler.scheduleWithFixedDelay(ApiHandler::updateBotStatistics, 90, 3 * 60 * 60, TimeUnit.SECONDS);
			cacheDatabase.initializeCommandUses();
			cacheDatabase.initializeJacobData();
			cacheDatabase.initializeTokens();
			cacheDatabase.initializeAhTracker();
			scheduler.scheduleWithFixedDelay(cacheDatabase::updateCache, 60, 60, TimeUnit.SECONDS);
			scheduler.scheduleWithFixedDelay(ApiHandler::updateCaches, 60, 60, TimeUnit.MINUTES);
			if (isMainBot()) {
				scheduler.scheduleWithFixedDelay(ApiHandler::updateLinkedAccounts, 60, 30, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			log.error("Exception when initializing the ApiHandler", e);
		}
	}

	public static void updateCaches() {
		try {
			cacheApplyGuildUsers();
			cacheDatabase.cachePartyData();
			cacheDatabase.cacheCommandUsesData();
			cacheDatabase.cacheAhTrackerData();
			cacheDatabase.cacheTokensData();
		} catch (Exception e) {
			log.error("Exception when interval caching", e);
		}
	}

	public static void initializeConstants() {
		JsonElement settings = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/Settings.json");
		ahApiUrl = "https://vague-darkviolet-springtail.gigalixirapp.com/"; // higherDepth(settings, "ahApiUrl").getAsString();
		mojangApiNum = higherDepth(settings, "mojangApiNum", 0);
		allowMojangApi = higherDepth(settings, "allowMojangApi", false);
		hasteUrl = higherDepth(settings, "hasteUrl").getAsString();
		neuBranch = higherDepth(settings, "neuBranch").getAsString();
	}

	public static String getNeuBranch() {
		return neuBranch;
	}

	public static String getHasteUrl() {
		return hasteUrl;
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

			JsonObject discordsJson = new JsonObject();
			discordsJson.addProperty("server_count", serverCount);
			postJson(
				"https://discords.com/bots/api/bot/" + selfUserId,
				discordsJson,
				new BasicHeader(
					"Authorization",
					"e2574f9c2bbe92a80c387f4b4f9c43ef0b8d747189375eef19a98165ffc0e1341e7abd333b037eee5ca9e9e31ca906a945f6d30eb477fbf4acb7ca0e0938ff40"
				)
			);

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

		if (cachedResponse == null && !isValidMinecraftUsername(username) && isValidMinecraftUuid(username)) {
			return uuidToUsername(username);
		}

		if (cachedResponse != null) {
			return new UsernameUuidStruct(cachedResponse.getValue(), cachedResponse.getKey());
		}

		UsernameUuidStruct response = uuidUsername(username);
		return allowMojangApi && response.isRateLimited() ? uuidUsernameMojang(username) : response;
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
			switch (mojangApiNum) {
				case 1 -> {
					JsonElement usernameJson = getJson("https://playerdb.co/api/player/minecraft/" + username);
					try {
						UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
							higherDepth(usernameJson, "data.player.username").getAsString(),
							higherDepth(usernameJson, "data.player.id").getAsString()
						);
						uuidToUsernameCache.put(usernameUuidStruct.uuid(), usernameUuidStruct.username());
						return usernameUuidStruct;
					} catch (Exception e) {
						String message = higherDepth(usernameJson, "message").getAsString();
						return new UsernameUuidStruct(
							allowMojangApi && message.equals("Mojang API lookup failed.")
								? "Mojang has rate limited this request."
								: message
						);
					}
				}
				case 2 -> {
					JsonElement usernameJson = getJson("https://api.minetools.eu/uuid/" + username);
					try {
						UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
							higherDepth(usernameJson, "name").getAsString(),
							higherDepth(usernameJson, "id").getAsString()
						);
						uuidToUsernameCache.put(usernameUuidStruct.uuid(), usernameUuidStruct.username());
						return usernameUuidStruct;
					} catch (Exception e) {
						return new UsernameUuidStruct(higherDepth(usernameJson, "error").getAsString());
					}
				}
				default -> {
					JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + username);
					try {
						UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
							higherDepth(usernameJson, "username").getAsString(),
							higherDepth(usernameJson, "uuid").getAsString()
						);
						uuidToUsernameCache.put(usernameUuidStruct.uuid(), usernameUuidStruct.username());
						return usernameUuidStruct;
					} catch (Exception e) {
						return new UsernameUuidStruct(
							allowMojangApi && higherDepth(usernameJson, "error", "").equals("Too Many Requests")
								? "Mojang has rate limited this request."
								: higherDepth(usernameJson, "reason").getAsString()
						);
					}
				}
			}
		} catch (Exception e) {
			return new UsernameUuidStruct(e.getMessage());
		}
	}

	public static CompletableFuture<String> asyncUuidToUsername(String uuid) {
		String cachedResponse = uuidToUsernameCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			return CompletableFuture.completedFuture(cachedResponse);
		} else {
			return asyncGetJson(
				(
					switch (mojangApiNum) {
						case 1 -> "https://playerdb.co/api/player/minecraft/";
						case 2 -> "https://api.minetools.eu/uuid/";
						default -> "https://api.ashcon.app/mojang/v2/user/";
					}
				) +
				uuid
			)
				.thenApplyAsync(
					uuidToUsernameJson -> {
						try {
							String username = higherDepth(
								uuidToUsernameJson,
								switch (mojangApiNum) {
									case 1 -> "data.player.username";
									case 2, 3 -> "name";
									default -> "username";
								}
							)
								.getAsString();
							uuidToUsernameCache.put(uuid, username);
							return username;
						} catch (Exception ignored) {}
						return null;
					},
					executor
				);
		}
	}

	private static UsernameUuidStruct uuidUsernameMojang(String username) {
		try {
			// true ? uuid to username : else username to uuid
			JsonElement usernameJson = getJson(
				(
					!isValidMinecraftUsername(username) && isValidMinecraftUuid(username)
						? "https://sessionserver.mojang.com/session/minecraft/profile/"
						: "https://api.mojang.com/users/profiles/minecraft/"
				) +
				username
			);
			try {
				UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
					higherDepth(usernameJson, "name").getAsString(),
					higherDepth(usernameJson, "id").getAsString()
				);
				uuidToUsernameCache.put(usernameUuidStruct.uuid(), usernameUuidStruct.username());
				return usernameUuidStruct;
			} catch (Exception e) {
				return new UsernameUuidStruct(higherDepth(usernameJson, "errorMessage").getAsString());
			}
		} catch (Exception e) {
			return new UsernameUuidStruct(e.getMessage());
		}
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid) {
		return skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY);
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		return skyblockProfilesFromUuid(uuid, hypixelApiKey, true, true);
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid, String hypixelApiKey, boolean useCache, boolean shouldCache) {
		if (useCache) {
			JsonElement cachedResponse = cacheDatabase.getCachedJson(CacheDatabase.CacheType.SKYBLOCK_PROFILES, uuid);
			if (cachedResponse != null) {
				return new HypixelResponse(cachedResponse);
			}
		}

		try {
			JsonElement profilesJson = getJson(
				getHypixelApiUrl("/skyblock/profiles", hypixelApiKey).addParameter("uuid", uuid).toString(),
				hypixelApiKey
			);

			try {
				if (
					higherDepth(profilesJson, "profiles").isJsonNull() || higherDepth(profilesJson, "profiles").getAsJsonArray().isEmpty()
				) {
					String username = uuidToUsernameCache.getIfPresent(uuid);
					return new HypixelResponse(
						(username != null ? username : "Player") +
						" has no Skyblock profiles. Make sure this player has logged on after the past Skyblock maintenance (<t:1684270848:D>)"
					);
				}

				JsonArray profileArray = higherDepth(profilesJson, "profiles").getAsJsonArray();
				if (shouldCache) {
					cacheDatabase.cacheJson(new CacheDatabase.CacheId(CacheDatabase.CacheType.SKYBLOCK_PROFILES, uuid), profileArray);
				}
				return new HypixelResponse(profileArray);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(profilesJson, "cause").getAsString());
			}
		} catch (Exception e) {
			return new HypixelResponse(e.getMessage());
		}
	}

	/**
	 * Does not cache the profiles json
	 */
	public static CompletableFuture<HypixelResponse> asyncSkyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		JsonElement cachedResponse = cacheDatabase.getCachedJson(CacheDatabase.CacheType.SKYBLOCK_PROFILES, uuid);
		if (cachedResponse != null) {
			return CompletableFuture.completedFuture(new HypixelResponse(cachedResponse));
		} else {
			return asyncGet(getHypixelApiUrl("/skyblock/profiles", hypixelApiKey).addParameter("uuid", uuid).toString())
				.thenApplyAsync(
					httpResponse -> {
						try {
							if (httpResponse.statusCode() == 502) {
								return new HypixelResponse("Hypixel API returned 502 bad gateway. The API may be down.");
							} else if (httpResponse.statusCode() == 522) {
								return new HypixelResponse("Hypixel API returned 522 connection timed out. The API may be down.");
							}

							try (
								InputStreamReader in = new InputStreamReader(httpResponse.body());
								JsonReader jsonIn = new JsonReader(in)
							) {
								JsonElement profilesJson = SkyblockProfilesParser.parse(jsonIn, uuid);

								// Json parsing probably takes more memory than the HTTP request
								if (Runtime.getRuntime().totalMemory() > 1250000000) {
									System.gc();
								}

								try {
									hypixelRateLimiter.update(
										Integer.parseInt(httpResponse.headers().firstValue("RateLimit-Remaining").get()),
										Integer.parseInt(httpResponse.headers().firstValue("RateLimit-Reset").get())
									);
								} catch (Exception ignored) {}

								if (higherDepth(profilesJson, "throttle", false) && higherDepth(profilesJson, "global", false)) {
									return new HypixelResponse(
										"Hypixel API returned 429 too many requests. The API is globally throttled and may be down."
									);
								}

								try {
									if (
										higherDepth(profilesJson, "profiles").isJsonNull() ||
										higherDepth(profilesJson, "profiles").getAsJsonArray().isEmpty()
									) {
										String username = uuidToUsernameCache.getIfPresent(uuid);
										return new HypixelResponse(
											(username != null ? username : "Player") +
											" has no Skyblock profiles. Make sure this player has logged on after the past Skyblock maintenance (<t:1684270848:D>)"
										);
									}

									return new HypixelResponse(higherDepth(profilesJson, "profiles").getAsJsonArray());
								} catch (Exception e) {
									return new HypixelResponse(higherDepth(profilesJson, "cause").getAsString());
								}
							}
						} catch (Exception e) {
							return new HypixelResponse(e.getMessage());
						}
					},
					executor
				);
		}
	}

	public static HypixelResponse skyblockMuseumFromProfileId(String profileId, String uuid) {
		JsonElement cachedResponse = cacheDatabase.getCachedJson(CacheDatabase.CacheType.SKYBLOCK_MUSEUM, profileId);
		if (cachedResponse != null) {
			return new HypixelResponse(cachedResponse);
		}

		try {
			JsonElement museumJson = getJson(
				getHypixelApiUrl("/skyblock/museum", HYPIXEL_API_KEY).addParameter("profile", profileId).toString()
			);

			try {
				if (higherDepth(museumJson, "members." + uuid) == null) {
					String username = uuidToUsernameCache.getIfPresent(uuid);
					return new HypixelResponse((username != null ? username : "Player") + "'s museum API is disabled");
				}

				JsonObject membersObject = higherDepth(museumJson, "members").getAsJsonObject();
				cacheDatabase.cacheJson(new CacheDatabase.CacheId(CacheDatabase.CacheType.SKYBLOCK_MUSEUM, profileId), membersObject);
				return new HypixelResponse(membersObject);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(museumJson, "cause").getAsString());
			}
		} catch (Exception e) {
			return new HypixelResponse(e.getMessage());
		}
	}

	public static HypixelResponse playerFromUuid(String uuid) {
		return playerFromUuid(uuid, true);
	}

	public static HypixelResponse playerFromUuid(String uuid, boolean useCache) {
		if (useCache) {
			JsonElement cachedResponse = cacheDatabase.getCachedJson(CacheDatabase.CacheType.PLAYER, uuid);
			if (cachedResponse != null) {
				return new HypixelResponse(cachedResponse);
			}
		}

		try {
			JsonElement playerJson = getJson(getHypixelApiUrl("/player", HYPIXEL_API_KEY).addParameter("uuid", uuid).toString());

			try {
				if (higherDepth(playerJson, "player").isJsonNull()) {
					String username = uuidToUsernameCache.getIfPresent(uuid);
					return new HypixelResponse((username != null ? username : "Player") + " has not played on Hypixel");
				}

				JsonObject playerObject = higherDepth(playerJson, "player").getAsJsonObject();
				cacheDatabase.cacheJson(new CacheDatabase.CacheId(CacheDatabase.CacheType.PLAYER, uuid), playerObject);
				return new HypixelResponse(playerObject);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(playerJson, "cause").getAsString());
			}
		} catch (Exception e) {
			return new HypixelResponse(e.getMessage());
		}
	}

	public static HypixelResponse getAuctionGeneric(String param, String value) {
		try {
			JsonElement auctionResponse = getJson(
				getHypixelApiUrl("/skyblock/auction", HYPIXEL_API_KEY).addParameter(param, value).toString()
			);
			try {
				return new HypixelResponse(higherDepth(auctionResponse, "auctions").getAsJsonArray());
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(auctionResponse, "cause").getAsString());
			}
		} catch (Exception e) {
			return new HypixelResponse(e.getMessage());
		}
	}

	public static HypixelResponse getAuctionFromPlayer(String playerUuid) {
		return getAuctionGeneric("player", playerUuid);
	}

	public static HypixelResponse getAuctionFromUuid(String auctionUuid) {
		HypixelResponse response = getAuctionGeneric("uuid", auctionUuid);
		return !response.isValid() ? response : (response.get("[0]") != null ? response : new HypixelResponse("Invalid auction UUID"));
	}

	public static HypixelResponse getGuildGeneric(String param, String value) {
		JsonElement cachedResponse = cacheDatabase.getCachedJson(CacheDatabase.CacheType.GUILD, value);
		if (cachedResponse != null) {
			return new HypixelResponse(cachedResponse);
		}

		try {
			JsonElement guildResponse = getJson(getHypixelApiUrl("/guild", HYPIXEL_API_KEY).addParameter(param, value).toString());

			try {
				if (higherDepth(guildResponse, "guild").isJsonNull()) {
					switch (param) {
						case "player" -> {
							return new HypixelResponse("Player is not in a guild");
						}
						case "id" -> {
							return new HypixelResponse("Invalid guild id");
						}
						case "name" -> {
							return new HypixelResponse("Invalid guild name");
						}
					}
				}

				JsonObject guildObject = higherDepth(guildResponse, "guild").getAsJsonObject();
				cacheDatabase.cacheJson(
					new CacheDatabase.CacheId(
						CacheDatabase.CacheType.GUILD,
						higherDepth(guildObject, "_id").getAsString(),
						higherDepth(guildObject, "name").getAsString()
					)
						.addIds(
							streamJsonArray(higherDepth(guildObject, "members")).map(m -> higherDepth(m, "uuid").getAsString()).toList()
						),
					guildObject
				);
				return new HypixelResponse(guildObject);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(guildResponse, "cause").getAsString());
			}
		} catch (Exception e) {
			return new HypixelResponse(e.getMessage());
		}
	}

	public static HypixelResponse getGuildFromPlayer(String playerUuid) {
		return getGuildGeneric("player", playerUuid);
	}

	public static HypixelResponse getGuildFromId(String guildId) {
		return getGuildGeneric("id", guildId);
	}

	public static HypixelResponse getGuildFromName(String guildName) {
		return getGuildGeneric("name", guildName.replace("_", " "));
	}

	public static URIBuilder getHypixelApiUrl(String path, String hypixelApiKey) {
		try {
			URIBuilder uriBuilder = new URIBuilder("https://api.hypixel.net").setPath(path);
			if (hypixelApiKey != null) {
				uriBuilder.addParameter("key", hypixelApiKey);
			}
			return uriBuilder;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URIBuilder getQueryApiUrl(String path) {
		try {
			return new URIBuilder(ahApiUrl).setPath(path).addParameter("key", AUCTION_API_KEY);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static JsonArray getBidsFromPlayer(String uuid) {
		try {
			return getJson(getQueryApiUrl("query").addParameter("bids", uuid).addParameter("limit", "-1").toString()).getAsJsonArray();
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryAuctions(String query, boolean isName, PriceSlashCommand.AuctionType auctionType) {
		try {
			URIBuilder uriBuilder = getQueryApiUrl("query")
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("sort_by", "highest_bid")
				.addParameter("sort_order", "ASC")
				.addParameter("limit", "5");
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

			return getJson(uriBuilder.toString()).getAsJsonArray();
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonObject queryLowestBin(String query) {
		try {
			return getJson(
				getQueryApiUrl("query")
					.addParameter("end", "" + Instant.now().toEpochMilli())
					.addParameter("sort_by", "highest_bid")
					.addParameter("sort_order", "ASC")
					.addParameter("limit", "1")
					.addParameter("item_id", query)
					.addParameter("bin", "true")
					.toString()
			)
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject();
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray queryLowestBinPet(String petName, String rarity, PriceSlashCommand.AuctionType auctionType) {
		try {
			URIBuilder uriBuilder = getQueryApiUrl("query")
				.addParameter("end", "" + Instant.now().toEpochMilli())
				.addParameter("item_name", "%" + petName + "%")
				.addParameter("item_id", "PET")
				.addParameter("sort_by", "highest_bid")
				.addParameter("sort_order", "ASC")
				.addParameter("limit", "5");
			if (!rarity.equals("ANY")) {
				uriBuilder.addParameter("tier", rarity);
			}
			if (auctionType == PriceSlashCommand.AuctionType.BIN) {
				uriBuilder.addParameter("bin", "true");
			} else if (auctionType == PriceSlashCommand.AuctionType.AUCTION) {
				uriBuilder.addParameter("bin", "false");
			}

			return getJson(uriBuilder.toString()).getAsJsonArray();
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray getAuctionPetsByName(String query) {
		try {
			return getJson(getQueryApiUrl("pets").addParameter("query", query).toString()).getAsJsonArray();
		} catch (Exception ignored) {}
		return null;
	}

	public static void updateLinkedAccounts() {
		try {
			for (LinkedAccount o : database.getBeforeLastUpdated(Instant.now().minus(5, ChronoUnit.DAYS).toEpochMilli())) {
				UsernameUuidStruct uuidStruct = uuidToUsername(o.uuid());
				if (uuidStruct.isValid()) {
					database.insertLinkedAccount(
						new LinkedAccount(Instant.now().toEpochMilli(), o.discord(), o.uuid(), uuidStruct.username()),
						null,
						null
					);
				}
			}
		} catch (Exception e) {
			log.error("Exception when updating linked accounts", e);
		}
	}
}
