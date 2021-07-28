package com.skyblockplus.utils;

import static com.skyblockplus.utils.Utils.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.*;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

public class Hypixel {

	public static final Cache<String, String> uuidToUsernameCache = CacheBuilder
		.newBuilder()
		.expireAfterAccess(30, TimeUnit.MINUTES)
		.recordStats()
		.build();

	public static final ConcurrentHashMap<String, Instant> uuidToTimeSkyblockProfiles = new ConcurrentHashMap<>();

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
	private static final String databaseUrl = "https://cache-skyblockplus.harperdbcloud.com";

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
		JsonElement cachedResponse = getCachedJson("profiles", uuid);
		if (cachedResponse != null) {
			return new HypixelResponse(cachedResponse);
		}

		try {
			JsonElement profilesJson = getJson("https://api.hypixel.net/skyblock/profiles?key=" + hypixelApiKey + "&uuid=" + uuid);

			try {
				if (higherDepth(profilesJson, "profiles").isJsonNull()) {
					return new HypixelResponse("Player has no SkyBlock profiles");
				}

				JsonArray profileArray = higherDepth(profilesJson, "profiles").getAsJsonArray();
				cacheJson("profiles", uuid, profileArray);
				return new HypixelResponse(profileArray);
			} catch (Exception e) {
				return new HypixelResponse(higherDepth(profilesJson, "cause").getAsString());
			}
		} catch (Exception ignored) {}

		return new HypixelResponse();
	}

	public static CompletableFuture<JsonElement> asyncSkyblockProfilesFromUuid(String uuid, String hypixelApiKey) {
		CompletableFuture<JsonElement> future = new CompletableFuture<>();

		JsonElement cachedResponse = getCachedJson("profiles", uuid);
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

								JsonArray profileArray = higherDepth(JsonParser.parseString(profilesResponse.getResponseBody()), "profiles")
									.getAsJsonArray();

								cacheJson("profiles", uuid, profileArray);

								return profileArray;
							} catch (Exception e) {
								e.printStackTrace();
							}
							return null;
						}
					);
		}

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
		return getAuctionGeneric("&uuid=" + auctionUuid);
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

	public static JsonArray getAuctionsByQuery(String query) {
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			query = query.replace("[", "\\\\[");
			URI uri = new URIBuilder(httpget.getURI())
				// .addParameter("query", "{\"item_name\":{\"$regex\":\"" + query
				// +
				// "\",\"$options\":\"i\"},\"$or\":[{\"bin\":true},{\"bids\":{\"$lt\":{\"$size\":0}}}]}")
				.addParameter("query", "{\"item_name\":{\"$regex\":\"" + query + "\",\"$options\":\"i\"},\"bin\":true}")
				.addParameter("sort", "{\"starting_bid\":1}")
				.build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray getBidsFromUuid(String uuid) {
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI()).addParameter("query", "{\"bids.bidder\":\"" + uuid + "\"}").build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonArray getAuctionPetsByName(String query) {
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI())
				.addParameter("query", "{\"item_name\":{\"$in\":[" + query + "]},\"bin\":true}")
				.addParameter("sort", "{\"starting_bid\":1}")
				.build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	@SuppressWarnings("EmptyTryBlock")
	public static void cacheJson(String type, String playerUuid, JsonElement json) {
		executor.submit(
			() -> {
				try {
					uuidToTimeSkyblockProfiles.put(playerUuid, Instant.now());

					JsonObject obj = new JsonObject();
					obj.addProperty("uuid", playerUuid);
					obj.add("data", json);

					RequestBody body = RequestBody.create(
						MediaType.parse("application/json"),
						"{\"operation\":\"insert\",\"schema\":\"dev\",\"table\":\"" + type + "\",\"records\":[" + obj + "]}"
					);
					Request request = new Request.Builder()
						.url(databaseUrl)
						.method("POST", body)
						.addHeader("Content-Type", "application/json")
						.addHeader("Authorization", "Basic " + CACHE_DATABASE_TOKEN)
						.build();
					try (Response ignored = okHttpClient.newCall(request).execute()) {}
				} catch (Exception ignored) {}
			}
		);
	}

	public static JsonElement getCachedJson(String type, String playerUuid) {
		Instant lastUpdated = uuidToTimeSkyblockProfiles.getOrDefault(playerUuid, null);
		if (lastUpdated != null && Duration.between(lastUpdated, Instant.now()).toMillis() > 90000) {
			deleteCachedJson(type, playerUuid);
		} else {
			RequestBody body = RequestBody.create(
				MediaType.parse("application/json"),
				"{\"operation\":\"sql\",\"sql\":\"SELECT * FROM dev." + type + " where uuid = '" + playerUuid + "'\"}"
			);
			Request request = new Request.Builder()
				.url(databaseUrl)
				.method("POST", body)
				.addHeader("Content-Type", "application/json")
				.addHeader("Authorization", "Basic " + CACHE_DATABASE_TOKEN)
				.build();

			try (Response response = okHttpClient.newCall(request).execute()) {
				JsonElement jsonResponse = JsonParser.parseString(response.body().string());
				Instant lastUpdatedResponse = Instant.ofEpochMilli(higherDepth(jsonResponse, "__updatedtime__").getAsLong());
				if (Duration.between(lastUpdatedResponse, Instant.now()).toMillis() > 90000) {
					deleteCachedJson(type, playerUuid);
				} else {
					uuidToTimeSkyblockProfiles.put(playerUuid, lastUpdatedResponse);
					return higherDepth(jsonResponse, "data");
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	@SuppressWarnings("EmptyTryBlock")
	public static void deleteCachedJson(String type, String playerUuid) {
		executor.submit(
			() -> {
				uuidToTimeSkyblockProfiles.remove(playerUuid);

				RequestBody body = RequestBody.create(
					MediaType.parse("application/json"),
					"{\"operation\":\"delete\",\"table\":\"" + type + "\",\"schema\":\"dev\",\"hash_values\":[" + playerUuid + "]}"
				);
				Request request = new Request.Builder()
					.url(databaseUrl)
					.method("POST", body)
					.addHeader("Content-Type", "application/json")
					.addHeader("Authorization", "Basic " + CACHE_DATABASE_TOKEN)
					.build();
				try (Response ignored = okHttpClient.newCall(request).execute()) {} catch (Exception ignored) {}
			}
		);
	}
}
