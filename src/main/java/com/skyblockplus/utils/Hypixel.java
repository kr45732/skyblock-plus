package com.skyblockplus.utils;

import static com.skyblockplus.utils.Utils.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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

	public static final Cache<String, JsonElement> uuidToSkyblockProfilesCache = CacheBuilder
		.newBuilder()
		.expireAfterWrite(90, TimeUnit.SECONDS)
		.recordStats()
		.build();

	public static final Cache<String, JsonElement> uuidToPlayerCache = CacheBuilder
		.newBuilder()
		.expireAfterWrite(90, TimeUnit.SECONDS)
		.recordStats()
		.build();

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
			JsonElement usernameJson = Utils.getJson("https://api.ashcon.app/mojang/v2/user/" + username);
			UsernameUuidStruct usernameUuidStruct = new UsernameUuidStruct(
				Utils.higherDepth(usernameJson, "username").getAsString(),
				Utils.higherDepth(usernameJson, "uuid").getAsString().replace("-", "")
			);
			uuidToUsernameCache.put(usernameUuidStruct.playerUuid, usernameUuidStruct.playerUsername);
			return usernameUuidStruct;
		} catch (Exception ignored) {}
		return null;
	}

	public static String uuidToUsername(String uuid) {
		String cachedResponse = uuidToUsernameCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			return cachedResponse;
		}

		try {
			JsonElement usernameJson = Utils.getJson("https://api.ashcon.app/mojang/v2/user/" + uuid);
			String responseUUID = Utils.higherDepth(usernameJson, "uuid").getAsString().replace("-", "");
			uuidToUsernameCache.put(responseUUID, Utils.higherDepth(usernameJson, "username").getAsString());
			return responseUUID;
		} catch (Exception ignored) {}
		return null;
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

	public static JsonElement skyblockProfilesFromUuid(String uuid) {
		return skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY, true);
	}

	public static JsonElement skyblockProfilesFromUuid(String uuid, String hypixelApiKey, boolean checkIfValid) {
		JsonElement cachedResponse = uuidToSkyblockProfilesCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			return cachedResponse;
		}

		JsonElement profilesJson = getJson("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid);

		if (!checkIfValid) {
			return profilesJson;
		}

		if (profilesJson != null && higherDepth(profilesJson, "profiles") != null && !higherDepth(profilesJson, "profiles").isJsonNull()) {
			uuidToSkyblockProfilesCache.put(uuid, profilesJson);
			return profilesJson;
		}

		return null;
	}

	public static CompletableFuture<JsonElement> asyncSkyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		CompletableFuture<JsonElement> future = new CompletableFuture<>();

		JsonElement cachedResponse = uuidToSkyblockProfilesCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			future.complete(cachedResponse);
		} else {
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

								JsonElement profilesJson = JsonParser.parseString(profilesResponse.getResponseBody());
								if (
									profilesJson != null &&
									higherDepth(profilesJson, "profiles") != null &&
									!higherDepth(profilesJson, "profiles").isJsonNull()
								) {
									uuidToSkyblockProfilesCache.put(uuid, profilesJson);
								}

								return profilesJson;
							} catch (Exception e) {
								e.printStackTrace();
							}
							return null;
						}
					);
		}

		return future;
	}

	public static JsonElement playerFromUuid(String uuid) {
		JsonElement cachedResponse = uuidToPlayerCache.getIfPresent(uuid);
		if (cachedResponse != null) {
			return cachedResponse;
		}

		JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + uuid);
		if (playerJson != null && higherDepth(playerJson, "player") != null && !higherDepth(playerJson, "player").isJsonNull()) {
			uuidToPlayerCache.put(uuid, playerJson);
			return playerJson;
		}

		return null;
	}
}
