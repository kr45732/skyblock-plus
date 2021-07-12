package com.skyblockplus.utils;

import static com.skyblockplus.utils.Utils.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Hypixel {

	public static final Cache<String, String> uuidToUsernameCache = CacheBuilder
		.newBuilder()
		.expireAfterAccess(30, TimeUnit.MINUTES)
		.recordStats()
		.build();

	//	public static final Cache<String, JsonElement> uuidToSkyblockProfilesCache = CacheBuilder
	//		.newBuilder()
	//		.expireAfterWrite(90, TimeUnit.SECONDS)
	//		.recordStats()
	//		.build();
	//
	//	public static final Cache<String, JsonElement> uuidToPlayerCache = CacheBuilder
	//		.newBuilder()
	//		.expireAfterWrite(90, TimeUnit.SECONDS)
	//		.recordStats()
	//		.build();

	public static UsernameUuidStruct usernameToUuid(String username) {
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

		try {
			JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + username);
			try {
				UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
					higherDepth(usernameJson, "username").getAsString(),
					higherDepth(usernameJson, "uuid").getAsString().replace("-", "")
				);
				uuidToUsernameCache.put(usernameUuidStruct.playerUuid, usernameUuidStruct.playerUsername);
				return usernameUuidStruct;
			} catch (Exception e) {
				return new UsernameUuidStruct(higherDepth(usernameJson, "reason").getAsString());
			}
		} catch (Exception ignored) {}
		return new UsernameUuidStruct();
	}

	public static UsernameUuidStruct uuidToUsername(String uuid) {
		String cachedResponse = uuidToUsernameCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			return new UsernameUuidStruct(cachedResponse, uuid);
		}

		try {
			JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + uuid);
			try {
				UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
					higherDepth(usernameJson, "username").getAsString(),
					higherDepth(usernameJson, "uuid").getAsString().replace("-", "")
				);
				uuidToUsernameCache.put(usernameUuidStruct.playerUuid, usernameUuidStruct.playerUsername);
				return usernameUuidStruct;
			} catch (Exception e) {
				return new UsernameUuidStruct(higherDepth(usernameJson, "reason").getAsString());
			}
		} catch (Exception ignored) {}
		return new UsernameUuidStruct();
	}

	public static CompletableFuture<String> asyncUuidToUsername(String uuid) {
		CompletableFuture<String> future = new CompletableFuture<>();

		String cachedResponse = uuidToUsernameCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			future.complete(cachedResponse);
		} else {
			future =
				asyncHttpClient
					.prepareGet("https://api.ashcon.app/mojang/v2/user/" + uuid)
					.execute()
					.toCompletableFuture()
					.thenApply(
						uuidToUsernameResponse -> {
							try {
								String username = Utils
									.higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()), "username")
									.getAsString();
								uuidToUsernameCache.put(uuid, username);
								return username;
							} catch (Exception ignored) {}
							return null;
						}
					);
		}

		return future;
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid) {
		return skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY);
	}

	public static HypixelResponse skyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		//		JsonElement cachedResponse = uuidToSkyblockProfilesCache.getIfPresent(uuid);
		//		if (cachedResponse != null) {
		//			return new HypixelResponse(cachedResponse);
		//		}

		try {
			JsonElement profilesJson = getJson("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid);

			try {
				if (higherDepth(profilesJson, "profiles").isJsonNull()) {
					return new HypixelResponse("Player has no SkyBlock profiles");
				}

				JsonArray profileArray = higherDepth(profilesJson, "profiles").getAsJsonArray();
				//				uuidToSkyblockProfilesCache.put(uuid, profileArray);
				return new HypixelResponse(profileArray);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(profilesJson, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static CompletableFuture<JsonElement> asyncSkyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		CompletableFuture<JsonElement> future = new CompletableFuture<>();

		//		JsonElement cachedResponse = uuidToSkyblockProfilesCache.getIfPresent(uuid);
		//		if (cachedResponse != null) {
		//			future.complete(cachedResponse);
		//		} else {
		future =
			asyncHttpClient
				.prepareGet("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid)
				.execute()
				.toCompletableFuture()
				.thenApply(
					profilesResponse -> {
						try {
							try {
								keyCooldownMap
									.get(hypixelApiKey)
									.remainingLimit.set(Integer.parseInt(profilesResponse.getHeader("RateLimit-Remaining")));
								keyCooldownMap
									.get(hypixelApiKey)
									.timeTillReset.set(Integer.parseInt(profilesResponse.getHeader("RateLimit-Reset")));
							} catch (Exception ignored) {}

							JsonArray profileArray = higherDepth(JsonParser.parseString(profilesResponse.getResponseBody()), "profiles")
								.getAsJsonArray();
							//								uuidToSkyblockProfilesCache.put(uuid, profileArray);

							return profileArray;
						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
					}
				);
		//		}

		return future;
	}

	public static HypixelResponse playerFromUuid(String uuid) {
		//		JsonElement cachedResponse = uuidToPlayerCache.getIfPresent(uuid);
		//		if (cachedResponse != null) {
		//			return new HypixelResponse(cachedResponse);
		//		}

		try {
			JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + uuid);

			try {
				if (higherDepth(playerJson, "player").isJsonNull()) {
					return new HypixelResponse("Player has not played on Hypixel");
				}

				JsonObject playerObject = higherDepth(playerJson, "player").getAsJsonObject();
				//				uuidToPlayerCache.put(uuid, playerObject);
				return new HypixelResponse(playerObject);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(playerJson, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static HypixelResponse getSkyblockAuctionGeneric(String query) {
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

	public static HypixelResponse getSkyblockAuctionFromPlayer(String playerUuid) {
		return getSkyblockAuctionGeneric("&player=" + playerUuid);
	}

	public static HypixelResponse getSkyblockAuctionFromUuid(String auctionUuid) {
		return getSkyblockAuctionGeneric("&uuid=" + auctionUuid);
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
		return getGuildGeneric("&name=" + guildName.replace(" ", "%20"));
	}
}
