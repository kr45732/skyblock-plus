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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.features.mayor.MayorHandler.currentMayor;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.ENCHANT_NAMES;
import static com.skyblockplus.utils.Constants.PET_NAMES;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.*;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandClient;
import com.skyblockplus.utils.database.Database;
import com.skyblockplus.utils.exceptionhandler.ExceptionExecutor;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import com.skyblockplus.utils.structs.*;
import java.awt.*;
import java.io.*;
import java.math.RoundingMode;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.OkHttpClient;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class Utils {

	/* Constants */
	public static final Color botColor = new Color(223, 5, 5);
	public static final int globalCooldown = 4;
	public static final String DISCORD_SERVER_INVITE_LINK = "https://discord.gg/Z4Fn3eNDXT";
	public static final String BOT_INVITE_LINK =
		"https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=395540032593&scope=bot%20applications.commands";
	public static final String FORUM_POST_LINK = "https://hypixel.net/threads/3980092";
	public static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
	public static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	public static final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
	public static final ExecutorService executor = new ExceptionExecutor();
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(7);
	public static final ScriptEngine jsScriptEngine = new ScriptEngineManager().getEngineByName("js");
	public static final AtomicInteger remainingLimit = new AtomicInteger(240);
	public static final AtomicInteger timeTillReset = new AtomicInteger(0);
	public static final ConcurrentHashMap<String, HypixelKeyRecord> keyCooldownMap = new ConcurrentHashMap<>();
	public static final Cache<String, HypixelGuildCache> hypixelGuildsCacheMap = Caffeine
		.newBuilder()
		.expireAfterWrite(15, TimeUnit.MINUTES)
		.build();
	public static final Gson gson = new Gson();
	public static final Gson formattedGson = new GsonBuilder().setPrettyPrinting().create();
	private static final Pattern mcColorPattern = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	public static final Consumer<Object> ignore = ignored -> {};
	public static final Pattern nicknameTemplatePattern = Pattern.compile("\\[(GUILD|PLAYER)\\.(\\w+)(?:\\.\\{(.*?)})?]");
	public static final JDAWebhookClient botStatusWebhook = new WebhookClientBuilder(
		"https://discord.com/api/webhooks/957659234827374602/HLXDdqX5XMaH2ZDX5HRHifQ6i71ISoCNcwVmwPQCyCvbKv2l0Q7NLj_lmzwfs4mdcOM1"
	)
		.buildJDA();
	/* Configuration File */
	public static String HYPIXEL_API_KEY = "";
	public static String BOT_TOKEN = "";
	public static String DATABASE_URL = "";
	public static String DATABASE_USERNAME = "";
	public static String DATABASE_PASSWORD = "";
	public static String API_USERNAME = "";
	public static String API_PASSWORD = "";
	public static String GITHUB_TOKEN = "";
	public static String DEFAULT_PREFIX = "";
	public static String AUCTION_API_KEY = "";
	public static String PLANET_SCALE_URL = "";
	public static String PLANET_SCALE_USERNAME = "";
	public static String PLANET_SCALE_PASSWORD = "";
	public static String SBZ_SCAMMER_DB_KEY = "";
	public static String LINKED_USER_URL = "";
	public static String LINKED_USER_USERNAME = "";
	public static String LINKED_USER_PASSWORD = "";
	public static String HEROKU_API_KEY = "";
	public static String LEADERBOARD_DB_URL = "";
	/* JSON */
	private static JsonObject essenceCostsJson;
	private static JsonObject levelingJson;
	private static JsonObject collectionsJson;
	private static JsonObject skyCryptPetJson;
	private static JsonObject enchantsJson;
	private static JsonObject petNumsJson;
	private static JsonObject petsJson;
	private static JsonObject reforgeStonesJson;
	private static JsonObject bitsJson;
	private static JsonObject miscJson;
	private static JsonObject talismanJson;
	private static JsonObject lowestBinJson;
	private static JsonObject averageAuctionJson;
	private static JsonObject bazaarJson;
	private static JsonArray sbzPricesJson;
	private static JsonObject emojiMap;
	public static JsonArray skyblockItemsJson;
	public static JsonObject internalJsonMappings;
	public static JsonObject priceOverrideJson;
	public static JsonObject bingoInfoJson;
	public static JsonObject dungeonLootJson;
	public static JsonObject dragonLootJson;
	/* Miscellaneous */
	public static TextChannel botLogChannel;
	public static TextChannel errorLogChannel;
	public static Instant lowestBinJsonLastUpdated = Instant.now();
	public static Instant averageAuctionJsonLastUpdated = Instant.now();
	public static Instant bingoJsonLastUpdated = Instant.now();
	public static Instant bazaarJsonLastUpdated = Instant.now();
	public static Instant sbzPricesJsonLastUpdated = Instant.now();
	public static Set<String> vanillaItems;
	private static Instant userCountLastUpdated = Instant.now();
	private static int userCount = -1;
	public static List<String> queryItems;
	public static ShardManager jda;
	public static Database database;
	public static EventWaiter waiter;
	public static GlobalExceptionHandler globalExceptionHandler;
	public static CommandClient client;
	public static SlashCommandClient slashCommandClient;
	public static JsonObject allServerSettings;
	public static ConfigurableApplicationContext springContext;
	public static String selfUserId;

	/* Getters */
	public static JsonObject getLowestBinJson() {
		if (lowestBinJson == null || Duration.between(lowestBinJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			if (currentMayor.equals("Derpy")) {
				return lowestBinJson = getJsonObject(getQueryApiUrl("lowestbin") + "?key=" + AUCTION_API_KEY);
			}

			lowestBinJson = getJsonObject("https://moulberry.codes/lowestbin.json");
			if (lowestBinJson == null) {
				lowestBinJson = getJsonObject(getQueryApiUrl("lowestbin") + "?key=" + AUCTION_API_KEY);
			}
			lowestBinJsonLastUpdated = Instant.now();
		}

		return lowestBinJson;
	}

	public static JsonObject getInternalJsonMappings() {
		if (internalJsonMappings == null) {
			internalJsonMappings =
				getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json");
		}

		return internalJsonMappings;
	}

	public static JsonObject getEmojiMap() {
		if (emojiMap == null) {
			try {
				emojiMap =
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/IdToEmojiMappings.json")).getAsJsonObject();
			} catch (Exception ignored) {}
		}

		return emojiMap;
	}

	public static String getEmoji(String id, String name) {
		return getEmoji(id).replaceAll("(?<before>:).*(?<after>:)", "${before}" + name + "${after}");
	}

	public static String getEmoji(String id) {
		return higherDepth(getEmojiMap(), id, "");
	}

	public static String getEmojiOr(String id, String defaultValue) {
		return higherDepth(getEmojiMap(), id, defaultValue);
	}

	public static JsonObject getAverageAuctionJson() {
		if (averageAuctionJson == null || Duration.between(averageAuctionJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			if (currentMayor.equals("Derpy")) {
				return averageAuctionJson = getJsonObject(getQueryApiUrl("average_auction") + "?key=" + AUCTION_API_KEY + "&time=1");
			}

			averageAuctionJson = getJsonObject("https://moulberry.codes/auction_averages/3day.json");
			if (averageAuctionJson == null) {
				averageAuctionJson =
					getJsonObject(
						getQueryApiUrl("average_auction") +
						"?key=" +
						AUCTION_API_KEY +
						"&time=" +
						Instant.now().minus(3, ChronoUnit.DAYS).toEpochMilli()
					);
			}
			averageAuctionJsonLastUpdated = Instant.now();
		}

		return averageAuctionJson;
	}

	public static JsonObject getBingoInfoJson() {
		if (bingoInfoJson == null || Duration.between(bingoJsonLastUpdated, Instant.now()).toMinutes() >= 5) {
			bingoInfoJson = getJsonObject("https://api.hypixel.net/resources/skyblock/bingo");
			bingoJsonLastUpdated = Instant.now();
		}

		return bingoInfoJson;
	}

	public static JsonObject getBazaarJson() {
		if (bazaarJson == null || Duration.between(bazaarJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			bazaarJson = getJsonObject("https://api.hypixel.net/skyblock/bazaar");
			bazaarJsonLastUpdated = Instant.now();
		}

		return bazaarJson;
	}

	public static List<String> getQueryItems() {
		if (queryItems == null) {
			try {
				HttpGet httpGet = new HttpGet(getQueryApiUrl("query_items"));
				httpGet.addHeader("content-type", "application/json; charset=UTF-8");

				URI uri = new URIBuilder(httpGet.getURI()).addParameter("key", AUCTION_API_KEY).build();
				httpGet.setURI(uri);

				try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
					queryItems =
						streamJsonArray(
							JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray()
						)
							.map(JsonElement::getAsString)
							.collect(Collectors.toList());
				}
			} catch (Exception ignored) {}
		}

		return queryItems;
	}

	public static JsonArray getSkyblockItemsJson() {
		if (skyblockItemsJson == null) {
			skyblockItemsJson = higherDepth(getJson("https://api.hypixel.net/resources/skyblock/items"), "items").getAsJsonArray();
		}

		return skyblockItemsJson;
	}

	public static double getNpcSellPrice(String id) {
		for (JsonElement npcSellPrice : getSkyblockItemsJson()) {
			if (higherDepth(npcSellPrice, "id").getAsString().equals(id)) {
				return higherDepth(npcSellPrice, "npc_sell_price", -1.0);
			}
		}

		return -1.0;
	}

	public static JsonArray getSbzPricesJson() {
		if (sbzPricesJson == null || Duration.between(sbzPricesJsonLastUpdated, Instant.now()).toMinutes() >= 30) {
			sbzPricesJson = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json").getAsJsonArray();
			sbzPricesJsonLastUpdated = Instant.now();
		}

		return sbzPricesJson;
	}

	public static JsonObject getMiscJson() {
		if (miscJson == null) {
			miscJson = getJsonObject("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/misc.json");
		}

		return miscJson;
	}

	public static JsonObject getDungeonLootJson() {
		if (dungeonLootJson == null) {
			dungeonLootJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/dungeon_loot.json");
		}

		return dungeonLootJson;
	}

	public static JsonObject getDragonLootJson() {
		if (dragonLootJson == null) {
			dragonLootJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/dragon_loot.json");
		}

		return dragonLootJson;
	}

	public static JsonObject getTalismanJson() {
		if (talismanJson == null) {
			talismanJson =
				parseJsString(
					"{" +
					getSkyCryptData("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/talismans.js")
						.replace("export const ", "")
						.replace(" = ", ": ")
						.replace(";", ",")
						.split("//")[0] +
					"}"
				)
					.getAsJsonObject();
		}

		return talismanJson;
	}

	public static JsonObject getBitsJson() {
		if (bitsJson == null) {
			bitsJson = getJsonObject("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/BitPricesJson.json");
		}

		return bitsJson;
	}

	public static JsonObject getReforgeStonesJson() {
		if (reforgeStonesJson == null) {
			reforgeStonesJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/reforgestones.json"
				);
		}

		return reforgeStonesJson;
	}

	public static JsonObject getPetJson() {
		if (petsJson == null) {
			petsJson = getJsonObject("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/pets.json");
		}
		return petsJson;
	}

	public static JsonObject getPetNumsJson() {
		if (petNumsJson == null) {
			petNumsJson =
				getJsonObject("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/petnums.json");
		}
		return petNumsJson;
	}

	public static JsonObject getEnchantsJson() {
		if (enchantsJson == null) {
			enchantsJson =
				getJsonObject("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/enchants.json");
		}
		return enchantsJson;
	}

	public static JsonObject getLevelingJson() {
		if (levelingJson == null) {
			levelingJson =
				getJsonObject("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/leveling.json");
		}
		return levelingJson;
	}

	public static JsonObject getEssenceCostsJson() {
		if (essenceCostsJson == null) {
			essenceCostsJson =
				getJsonObject(
					"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/essencecosts.json"
				);
		}
		return essenceCostsJson;
	}

	public static JsonObject getCollectionsJson() {
		if (collectionsJson == null) {
			collectionsJson = new JsonObject();
			JsonObject hypixelCollectionsJson = higherDepth(
				getJson("https://api.hypixel.net/resources/skyblock/collections"),
				"collections"
			)
				.getAsJsonObject();
			for (Map.Entry<String, JsonElement> collectionType : hypixelCollectionsJson.entrySet()) {
				JsonObject collectionItems = higherDepth(collectionType.getValue(), "items").getAsJsonObject();
				for (Map.Entry<String, JsonElement> item : collectionItems.entrySet()) {
					JsonArray tierAmounts = new JsonArray();
					for (JsonElement tierAmount : higherDepth(item.getValue(), "tiers").getAsJsonArray()) {
						tierAmounts.add(higherDepth(tierAmount, "amountRequired"));
					}
					JsonObject idAndTier = new JsonObject();
					idAndTier.add("name", higherDepth(item.getValue(), "name"));
					idAndTier.add("tiers", tierAmounts);
					collectionsJson.add(item.getKey(), idAndTier);
				}
			}
		}

		return collectionsJson;
	}

	public static JsonObject getSkyCryptPetJson() {
		if (skyCryptPetJson == null) {
			skyCryptPetJson =
				parseJsString(
					Pattern
						.compile("/\\*(.*)\\*/", Pattern.DOTALL)
						.matcher(
							"{" +
							getSkyCryptData("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/pets.js")
								.split("];")[1].replace("export const ", "")
								.replace(" = ", ": ")
								.replace(";", ",") +
							"}"
						)
						.replaceAll("")
						.replace("//(.*)", "")
						.replaceAll("(description: `)(.*?)(\\s*`,)", "")
				)
					.getAsJsonObject();
		}

		return skyCryptPetJson;
	}

	/* Http requests */
	public static JsonElement getJson(String jsonUrl) {
		return getJson(jsonUrl, HYPIXEL_API_KEY);
	}

	public static JsonElement getJson(String jsonUrl, String hypixelApiKey) {
		boolean isMain = hypixelApiKey.equals(HYPIXEL_API_KEY);
		try {
			if (
				jsonUrl.contains(hypixelApiKey) && (isMain ? remainingLimit : keyCooldownMap.get(hypixelApiKey).remainingLimit()).get() < 5
			) {
				int timeTillResetInt = (isMain ? timeTillReset : keyCooldownMap.get(hypixelApiKey).timeTillReset()).get();
				log.info("Sleeping for " + timeTillResetInt + " seconds (" + isMain + ")");
				TimeUnit.SECONDS.sleep(timeTillResetInt);
			}
		} catch (Exception ignored) {}

		try {
			HttpGet httpGet = new HttpGet(jsonUrl);
			if (jsonUrl.contains("raw.githubusercontent.com")) {
				httpGet.setHeader("Authorization", "token " + GITHUB_TOKEN);
			}
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				if (jsonUrl.toLowerCase().contains("api.hypixel.net")) {
					if (jsonUrl.contains(hypixelApiKey)) {
						try {
							(isMain ? remainingLimit : keyCooldownMap.get(hypixelApiKey).remainingLimit()).set(
									Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Remaining").getValue())
								);
							(isMain ? timeTillReset : keyCooldownMap.get(hypixelApiKey).timeTillReset()).set(
									Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Reset").getValue())
								);
						} catch (Exception ignored) {}
					}

					if (httpResponse.getStatusLine().getStatusCode() == 502) {
						return JsonParser.parseString("{\"cause\":\"Hypixel API returned 502 bad gateway\"}");
					}
				}

				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonObject getJsonObject(String url) {
		try {
			return getJson(url).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}

	public static String getSkyCryptData(String dataUrl) {
		if (!dataUrl.contains("raw.githubusercontent.com")) {
			return null;
		}

		try {
			HttpGet httpGet = new HttpGet(dataUrl);
			httpGet.setHeader("Authorization", "token " + GITHUB_TOKEN);
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				InputStream inputStream = httpResponse.getEntity().getContent();
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				for (int length; (length = inputStream.read(buffer)) != -1;) {
					result.write(buffer, 0, length);
				}
				return result.toString();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static String makeHastePost(Object body) {
		try {
			HttpPost httpPost = new HttpPost("https://hst.sh/documents");

			StringEntity entity = new StringEntity(body.toString(), "UTF-8");
			httpPost.setEntity(entity);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				return (
					"https://hst.sh/" +
					higherDepth(JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())), "key").getAsString()
				);
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement postJson(String url, JsonElement body, Header... headers) {
		try {
			HttpPost httpPost = new HttpPost(url);

			StringEntity entity = new StringEntity(body.toString(), "UTF-8");
			httpPost.setEntity(entity);
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeaders(headers);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement deleteUrl(String url, Header... headers) {
		try {
			HttpDelete httpDelete = new HttpDelete(url);

			httpDelete.setHeader("Content-Type", "application/json");
			httpDelete.setHeader("Accept", "application/json");
			httpDelete.setHeaders(headers);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpDelete)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static DiscordInfoStruct getPlayerDiscordInfo(String username) {
		try {
			UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
			if (usernameUuidStruct.isNotValid()) {
				return new DiscordInfoStruct(usernameUuidStruct.failCause());
			}
			HypixelResponse response = playerFromUuid(usernameUuidStruct.uuid());
			if (response.isNotValid()) {
				return new DiscordInfoStruct(response.failCause());
			}

			if (response.get("socialMedia.links.DISCORD") == null) {
				return new DiscordInfoStruct(
					"Player is not linked on Hypixel. For help on how to link view [__**this video**__](https://streamable.com/sdq8tp)."
				);
			}

			String discordTag = response.get("socialMedia.links.DISCORD").getAsString();
			String minecraftUsername = response.get("displayname").getAsString();
			String minecraftUuid = response.get("uuid").getAsString();

			return new DiscordInfoStruct(discordTag, minecraftUsername, minecraftUuid);
		} catch (Exception e) {
			return new DiscordInfoStruct();
		}
	}

	public static String getPetUrl(String petName) {
		if (skyCryptPetJson == null) {
			skyCryptPetJson = getSkyCryptPetJson();
		}
		try {
			return ("https://sky.shiiyu.moe" + higherDepth(skyCryptPetJson, "pet_data." + petName.toUpperCase() + ".head").getAsString());
		} catch (Exception e) {
			return null;
		}
	}

	public static String getUrl(String url) {
		try {
			HttpGet httpGet = new HttpGet(url);
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				return new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()))
					.lines()
					.parallel()
					.collect(Collectors.joining("\n"));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement getScammerJson(String uuid) {
		JsonElement scammerJson = getJson("https://api.robothanzo.dev/scammer/" + uuid + "?key=" + SBZ_SCAMMER_DB_KEY);
		return higherDepth(scammerJson, "success", false) ? scammerJson : null;
	}

	public static String getScammerReason(String uuid) {
		JsonElement scammerJson = getScammerJson(uuid);
		return scammerJson != null ? higherDepth(getScammerJson(uuid), "result.reason", "No reason provided") : null;
	}

	/* Logging */
	public static void logCommand(Guild guild, User user, String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		EmbedBuilder eb = defaultEmbed(null);

		if (guild != null) {
			eb.setAuthor(guild.getName() + " (" + guild.getId() + ")", null, guild.getIconUrl());
		}

		if (commandInput.length() > 1024) {
			eb.addField(user.getName() + " (" + user.getId() + ")", makeHastePost(commandInput) + ".json", false);
		} else {
			eb.addField(user.getName() + " (" + user.getId() + ")", "`" + commandInput + "`", false);
		}

		botLogChannel.sendMessageEmbeds(eb.build()).queue();
	}

	public static void logCommand(Guild guild, String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		EmbedBuilder eb = defaultEmbed(null);
		eb.setAuthor(guild.getName() + " (" + guild.getId() + ")", null, guild.getIconUrl());
		eb.setDescription(commandInput);
		botLogChannel.sendMessageEmbeds(eb.build()).queue();
	}

	/* Embeds and paginators */
	public static EmbedBuilder defaultEmbed(String title, String titleUrl) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(botColor);
		eb.setFooter("By CrypticPlasma • dsc.gg/sb+", null);
		if (titleUrl != null && titleUrl.length() <= MessageEmbed.URL_MAX_LENGTH && EmbedBuilder.URL_PATTERN.matcher(titleUrl).matches()) {
			eb.setTitle(title, titleUrl);
		} else {
			eb.setTitle(title);
		}
		eb.setTimestamp(Instant.now());
		return eb;
	}

	public static EmbedBuilder defaultEmbed(String title) {
		return defaultEmbed(title, null);
	}

	public static EmbedBuilder invalidEmbed(String failCause) {
		return defaultEmbed("Error").setDescription(failCause);
	}

	public static EmbedBuilder loadingEmbed() {
		return defaultEmbed(null).setImage("https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif");
	}

	public static EmbedBuilder errorEmbed(String name) {
		return defaultEmbed("Invalid input. Run `help " + name + "` for help");
	}

	public static CustomPaginator.Builder defaultPaginator(User... eventAuthor) {
		return new CustomPaginator.Builder()
			.setEventWaiter(waiter)
			.setColumns(1)
			.setItemsPerPage(1)
			.setFinalAction(m -> {
				if (m.getActionRows().isEmpty()) {
					return;
				}

				if (m.getActionRows().size() == 1 && m.getActionRows().get(0).getButtons().get(0).getId().startsWith("paginator_")) {
					m.editMessageComponents().queue(ignore, ignore);
					return;
				}

				if (m.getActionRows().size() == 2) {
					m.editMessageComponents(m.getActionRows().get(1)).queue(ignore, ignore);
				}
			})
			.setTimeout(30, TimeUnit.SECONDS)
			.setColor(botColor)
			.setUsers(eventAuthor);
	}

	/* Format numbers or text */
	public static String formatNumber(long number) {
		return NumberFormat.getInstance(Locale.US).format(number);
	}

	public static String formatNumber(double number) {
		return NumberFormat.getInstance(Locale.US).format(number);
	}

	public static String roundAndFormat(double number) {
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return formatNumber(Double.parseDouble(df.format(number)));
	}

	public static String roundProgress(double number) {
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return df.format(number * 100) + "%";
	}

	public static String simplifyNumber(double number) {
		String formattedNumber;
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		if (1000000000000D > number && number >= 1000000000) {
			number = number >= 999999999950D ? 999999999949D : number;
			formattedNumber = df.format(number / 1000000000) + "B";
		} else if (number >= 1000000) {
			number = number >= 999999950D ? 999999949D : number;
			formattedNumber = df.format(number / 1000000) + "M";
		} else if (number >= 1000) {
			number = number >= 999950D ? 999949D : number;
			formattedNumber = df.format(number / 1000) + "K";
		} else if (number < 1) {
			formattedNumber = "0";
		} else {
			formattedNumber = df.format(number);
		}
		return formattedNumber;
	}

	public static String capitalizeString(String str) {
		return str == null
			? null
			: Stream
				.of(str.trim().split("\\s"))
				.filter(word -> word.length() > 0)
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	public static String parseMcCodes(String unformattedString) {
		return mcColorPattern.matcher(unformattedString.replace("\u00A7ka", "")).replaceAll("");
	}

	public static String fixUsername(String username) {
		return username.replace("_", "\\_");
	}

	/* Miscellaneous */
	public static JsonElement higherDepth(JsonElement element, String path) {
		String[] paths = path.split("\\.");

		try {
			for (String key : paths) {
				if (key.length() >= 3 && key.startsWith("[") && key.endsWith("]")) {
					element = element.getAsJsonArray().get(Integer.parseInt(key.substring(1, key.length() - 1)));
				} else {
					element = element.getAsJsonObject().get(key);
				}
			}
			return element;
		} catch (Exception e) {
			return null;
		}
	}

	public static String higherDepth(JsonElement element, String path, String defaultValue) {
		try {
			return higherDepth(element, path).getAsString();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static boolean higherDepth(JsonElement element, String path, boolean defaultValue) {
		try {
			return higherDepth(element, path).getAsBoolean();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static long higherDepth(JsonElement element, String path, long defaultValue) {
		try {
			return higherDepth(element, path).getAsLong();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int higherDepth(JsonElement element, String path, int defaultValue) {
		try {
			return higherDepth(element, path).getAsInt();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static double higherDepth(JsonElement element, String path, double defaultValue) {
		try {
			return higherDepth(element, path).getAsDouble();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static String toRomanNumerals(int number) {
		return join("", nCopies(number, "i")).replace("iiiii", "v").replace("iiii", "iv").replace("vv", "x").replace("viv", "ix");
	}

	public static JsonElement parseJsString(String jsString) {
		try {
			return JsonParser.parseString(jsScriptEngine.eval(String.format("JSON.stringify(%s);", jsString)).toString());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public static String convertSkyblockIdName(String itemName) {
		try {
			return higherDepth(getCollectionsJson(), itemName + ".name").getAsString();
		} catch (Exception ignored) {}
		return capitalizeString(itemName.replace("_", " "));
	}

	public static String nameToId(String itemName) {
		return nameToId(itemName, false);
	}

	public static String nameToId(String itemName, boolean strict) {
		getInternalJsonMappings();

		String id = itemName.trim().toUpperCase().replace(" ", "_").replace("'S", "").replace("FRAG", "FRAGMENT").replace(".", "");

		switch (id) {
			case "GOD_POT":
				return "GOD_POTION_2";
			case "AOTD":
				return "ASPECT_OF_THE_DRAGON";
			case "AOTE":
				return "ASPECT_OF_THE_END";
			case "AOTV":
				return "ASPECT_OF_THE_VOID";
			case "AOTS:":
				return "AXE_OF_THE_SHREDDED";
			case "LASR_EYE":
				return "GIANT_FRAGMENT_LASER";
			case "HYPE":
				return "HYPERION";
			case "GS":
				return "GIANTS_SWORD";
			case "TERM":
				return "TERMINATOR";
			case "TREECAP":
				return "TREECAPITATOR_AXE";
			case "JUJU":
				return "JUJU_SHORTBOW";
			case "VALK":
				return "VALKYRIE";
			case "HANDLE":
				return "NECRON_HANDLE";
		}

		for (Map.Entry<String, JsonElement> entry : internalJsonMappings.entrySet()) {
			if (higherDepth(entry.getValue(), "name").getAsString().equalsIgnoreCase(itemName)) {
				return entry.getKey();
			}
		}

		return strict ? null : id;
	}

	public static String idToName(String id) {
		getInternalJsonMappings();

		id = id.toUpperCase();
		return higherDepth(internalJsonMappings, id + ".name", capitalizeString(id.replace("_", " ")));
	}

	public static ArrayList<String> getJsonKeys(JsonElement jsonElement) {
		try {
			return new ArrayList<>(jsonElement.getAsJsonObject().keySet());
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	public static String profileNameToEmoji(String profileName) {
		return switch (profileName) {
			case "apple" -> "\uD83C\uDF4E";
			case "banana" -> "\uD83C\uDF4C";
			case "blueberry" -> "\uD83E\uDED0";
			case "coconut" -> "\uD83E\uDD65";
			case "cucumber" -> "\uD83E\uDD52";
			case "grapes" -> "\uD83C\uDF47";
			case "kiwi" -> "\uD83E\uDD5D";
			case "lemon" -> "\uD83C\uDF4B";
			case "lime" -> "lime:828632854174498837";
			case "mango" -> "\uD83E\uDD6D";
			case "orange" -> "orange:828634110360289331";
			case "papaya" -> "papaya:828633125370200085";
			case "peach" -> "\uD83C\uDF51";
			case "pear" -> "\uD83C\uDF50";
			case "pineapple" -> "\uD83C\uDF4D";
			case "pomegranate" -> "pomegranate:828632397032456232";
			case "raspberry" -> "raspberry:828632035127853064";
			case "strawberry" -> "\uD83C\uDF53";
			case "tomato" -> "\uD83C\uDF45";
			case "watermelon" -> "\uD83C\uDF49";
			case "zucchini" -> "zucchini:828636746358194206";
			default -> null;
		};
	}

	public static EmbedBuilder checkHypixelKey(String hypixelKey) {
		return checkHypixelKey(hypixelKey, true);
	}

	public static EmbedBuilder checkHypixelKey(String hypixelKey, boolean checkRatelimit) {
		if (hypixelKey == null) {
			return invalidEmbed("You must set a valid Hypixel API key to use this feature or command");
		}

		try {
			HttpGet httpGet = new HttpGet("https://api.hypixel.net/key?key=" + hypixelKey);
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				int remainingLimit = Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Remaining").getValue());
				int timeTillReset = Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Reset").getValue());
				if (checkRatelimit && remainingLimit < 10) {
					return invalidEmbed("That command is on cooldown for " + timeTillReset + " more seconds");
				}

				higherDepth(JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())), "record.key")
					.getAsString();

				if (!keyCooldownMap.containsKey(hypixelKey)) {
					keyCooldownMap.put(
						hypixelKey,
						new HypixelKeyRecord(new AtomicInteger(remainingLimit), new AtomicInteger(timeTillReset))
					);
				}
			}
		} catch (Exception e) {
			return invalidEmbed("You must set a valid Hypixel API key to use this feature or command");
		}
		return null;
	}

	public static void initialize() {
		Properties appProps = new Properties();
		try {
			appProps.load(new FileInputStream("DevSettings.properties"));
			HYPIXEL_API_KEY = (String) appProps.get("HYPIXEL_API_KEY");
			BOT_TOKEN = (String) appProps.get("BOT_TOKEN");
			String[] database_url_unformatted = ((String) appProps.get("DATABASE_URL")).split(":", 3);
			DATABASE_USERNAME = database_url_unformatted[1].replace("/", "");
			DATABASE_PASSWORD = database_url_unformatted[2].split("@")[0];
			DATABASE_URL =
				"jdbc:postgresql://" +
				database_url_unformatted[2].split("@")[1] +
				"?sslmode=require&user=" +
				DATABASE_USERNAME +
				"&password=" +
				DATABASE_PASSWORD;
			GITHUB_TOKEN = (String) appProps.get("GITHUB_TOKEN");
			API_USERNAME = (String) appProps.get("API_USERNAME");
			API_PASSWORD = (String) appProps.get("API_PASSWORD");
			DEFAULT_PREFIX = (String) appProps.get("DEFAULT_PREFIX");
			AUCTION_API_KEY = (String) appProps.get("AUCTION_API_KEY");
			PLANET_SCALE_URL = (String) appProps.get("PLANET_SCALE_URL");
			PLANET_SCALE_USERNAME = (String) appProps.get("PLANET_SCALE_USERNAME");
			PLANET_SCALE_PASSWORD = (String) appProps.get("PLANET_SCALE_PASSWORD");
			SBZ_SCAMMER_DB_KEY = (String) appProps.get("SBZ_SCAMMER_DB_KEY");
			LINKED_USER_URL = (String) appProps.get("LINKED_USER_URL");
			LINKED_USER_USERNAME = (String) appProps.get("LINKED_USER_USERNAME");
			LINKED_USER_PASSWORD = (String) appProps.get("LINKED_USER_PASSWORD");
			HEROKU_API_KEY = (String) appProps.get("HEROKU_API_KEY");
			LEADERBOARD_DB_URL = (String) appProps.get("LEADERBOARD_DB_URL");
		} catch (IOException e) {
			HYPIXEL_API_KEY = System.getenv("HYPIXEL_API_KEY");
			BOT_TOKEN = System.getenv("BOT_TOKEN");
			String[] database_url_unformatted = System.getenv("DATABASE_URL").split(":", 3);
			DATABASE_USERNAME = database_url_unformatted[1].replace("/", "");
			DATABASE_PASSWORD = database_url_unformatted[2].split("@")[0];
			DATABASE_URL =
				"jdbc:postgresql://" +
				database_url_unformatted[2].split("@")[1] +
				"?sslmode=require&user=" +
				DATABASE_USERNAME +
				"&password=" +
				DATABASE_PASSWORD;
			GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
			API_USERNAME = System.getenv("API_USERNAME");
			API_PASSWORD = System.getenv("API_PASSWORD");
			DEFAULT_PREFIX = System.getenv("DEFAULT_PREFIX");
			AUCTION_API_KEY = System.getenv("AUCTION_API_KEY");
			PLANET_SCALE_URL = System.getenv("PLANET_SCALE_URL");
			PLANET_SCALE_USERNAME = System.getenv("PLANET_SCALE_USERNAME");
			PLANET_SCALE_PASSWORD = System.getenv("PLANET_SCALE_PASSWORD");
			SBZ_SCAMMER_DB_KEY = System.getenv("SBZ_SCAMMER_DB_KEY");
			LINKED_USER_URL = System.getenv("LINKED_USER_URL");
			LINKED_USER_USERNAME = System.getenv("LINKED_USER_USERNAME");
			LINKED_USER_PASSWORD = System.getenv("LINKED_USER_PASSWORD");
			HEROKU_API_KEY = System.getenv("HEROKU_API_KEY");
			LEADERBOARD_DB_URL = System.getenv("LEADERBOARD_DB_URL");
		}
	}

	/**
	 * @param toMatch name to match
	 * @param matchFrom list of ID (will convert to their names)
	 */
	public static String getClosestMatchFromIds(String toMatch, Collection<String> matchFrom) {
		if (matchFrom == null || matchFrom.isEmpty()) {
			return toMatch;
		}

		return FuzzySearch
			.extractOne(
				toMatch,
				matchFrom.stream().collect(Collectors.toMap(Function.identity(), Utils::idToName)).entrySet(),
				Map.Entry::getValue
			)
			.getReferent()
			.getKey();
	}

	public static String getClosestMatch(String toMatch, List<String> matchFrom) {
		if (matchFrom == null || matchFrom.isEmpty()) {
			return toMatch;
		}

		return FuzzySearch.extractOne(toMatch, matchFrom).getString();
	}

	public static List<String> getClosestMatch(String toMatch, List<String> matchFrom, int numMatches) {
		if (matchFrom == null || matchFrom.isEmpty()) {
			return List.of(toMatch);
		}

		return FuzzySearch.extractTop(toMatch, matchFrom, numMatches).stream().map(ExtractedResult::getString).collect(Collectors.toList());
	}

	public static String skyblockStatsLink(String username, String profileName) {
		if (username == null) {
			return null;
		}

		return (
			"https://sky.shiiyu.moe/stats/" +
			username +
			(profileName != null && !profileName.equalsIgnoreCase("Not Allowed To Quit Skyblock Ever Again") ? "/" + profileName : "")
		);
	}

	public static Map<Integer, InvItem> getGenericInventoryMap(NBTCompound parsedContents) {
		try {
			NBTList items = parsedContents.getList("i");
			Map<Integer, InvItem> itemsMap = new HashMap<>();

			for (int i = 0; i < items.size(); i++) {
				try {
					NBTCompound item = items.getCompound(i);
					if (!item.isEmpty()) {
						InvItem itemInfo = new InvItem();
						itemInfo.setName(parseMcCodes(item.getString("tag.display.Name", "None")));
						itemInfo.setLore(
							parseMcCodes(item.getString("tag.display.Lore", "None").replace(", ", "\n").replace("[", "").replace("]", ""))
						);
						itemInfo.setCount(Integer.parseInt(item.getString("Count", "0").replace("b", " ")));
						itemInfo.setId(item.getString("tag.ExtraAttributes.id", "None"));
						itemInfo.setCreationTimestamp(item.getString("tag.ExtraAttributes.timestamp", "None"));
						itemInfo.setHbpCount(item.getInt("tag.ExtraAttributes.hot_potato_count", 0));
						itemInfo.setRecombobulated(item.getInt("tag.ExtraAttributes.rarity_upgrades", 0) == 1);
						itemInfo.setModifier(item.getString("tag.ExtraAttributes.modifier", "None"));
						itemInfo.setDungeonFloor(Integer.parseInt(item.getString("tag.ExtraAttributes.item_tier", "-1")));
						itemInfo.setNbtTag(item.toString());

						if (item.containsTag("tag.ExtraAttributes.enchantments", TagType.COMPOUND)) {
							NBTCompound enchants = item.getCompound("tag.ExtraAttributes.enchantments");
							List<String> enchantsList = new ArrayList<>();
							for (Map.Entry<String, Object> enchant : enchants.entrySet()) {
								if (
									enchant.getKey().equals("efficiency") &&
									!itemInfo.getId().equals("STONK_PICKAXE") &&
									(int) enchant.getValue() > 5
								) {
									itemInfo.addExtraValues((int) enchant.getValue() - 5, "SIL_EX");
								}
								enchantsList.add(enchant.getKey() + ";" + enchant.getValue());
							}
							itemInfo.setEnchantsFormatted(enchantsList);
						}

						if (item.containsKey("tag.ExtraAttributes.skin")) {
							itemInfo.addExtraValue(
								(itemInfo.getId().equals("PET") ? "PET_SKIN_" : "") + item.getString("tag.ExtraAttributes.skin")
							);
						}

						if (item.containsKey("tag.ExtraAttributes.talisman_enrichment")) {
							itemInfo.addExtraValue("TALISMAN_ENRICHMENT_" + item.getString("tag.ExtraAttributes.talisman_enrichment"));
						}

						if (item.containsTag("tag.ExtraAttributes.ability_scroll", TagType.LIST)) {
							NBTList necronBladeScrolls = item.getList("tag.ExtraAttributes.ability_scroll");
							for (Object scroll : necronBladeScrolls) {
								itemInfo.addExtraValue((String) scroll);
							}
						}

						if (item.getInt("tag.ExtraAttributes.wood_singularity_count", 0) == 1) {
							itemInfo.addExtraValue("WOOD_SINGULARITY");
						}

						if (item.getInt("tag.ExtraAttributes.art_of_war_count", 0) == 1) {
							itemInfo.addExtraValue("THE_ART_OF_WAR");
						}

						if (item.getInt("tag.ExtraAttributes.dungeon_item_level", 0) > 5) {
							int masterStarCount = item.getInt("tag.ExtraAttributes.dungeon_item_level", 5) - 5;
							switch (masterStarCount) {
								case 5:
									itemInfo.addExtraValue("FIFTH_MASTER_STAR");
								case 4:
									itemInfo.addExtraValue("FOURTH_MASTER_STAR");
								case 3:
									itemInfo.addExtraValue("THIRD_MASTER_STAR");
								case 2:
									itemInfo.addExtraValue("SECOND_MASTER_STAR");
								case 1:
									itemInfo.addExtraValue("FIRST_MASTER_STAR");
							}
						}

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.farming_for_dummies_count", 0), "FARMING_FOR_DUMMIES");

						itemInfo.addExtraValues(
							Integer.parseInt(item.getString("tag.ExtraAttributes.ethermerge", "0").replace("b", " ")),
							"ETHERWARP_CONDUIT"
						);

						if (item.containsKey("tag.ExtraAttributes.drill_part_upgrade_module")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.drill_part_upgrade_module").toUpperCase());
						}
						if (item.containsKey("tag.ExtraAttributes.drill_part_fuel_tank")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.drill_part_fuel_tank").toUpperCase());
						}
						if (item.containsKey("tag.ExtraAttributes.drill_part_engine")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.drill_part_engine").toUpperCase());
						}

						if (item.containsKey("tag.ExtraAttributes.petInfo")) {
							JsonElement petInfoJson = JsonParser.parseString(item.getString("tag.ExtraAttributes.petInfo"));
							if (higherDepth(petInfoJson, "heldItem", null) != null) {
								itemInfo.addExtraValue(higherDepth(petInfoJson, "heldItem").getAsString());
							}
							if (higherDepth(petInfoJson, "tier", null) != null) {
								itemInfo.setRarity(higherDepth(petInfoJson, "tier").getAsString());
							}
						}

						if (item.containsTag("tag.ExtraAttributes.gems", TagType.COMPOUND)) {
							NBTCompound gems = item.getCompound("tag.ExtraAttributes.gems");
							for (Map.Entry<String, Object> gem : gems.entrySet()) {
								if (!gem.getKey().endsWith("_gem")) {
									if (gem.getKey().equals("unlocked_slots")) {
										if (
											itemInfo.getId().equals("DIVAN_HELMET") ||
											itemInfo.getId().equals("DIVAN_CHESTPLATE") ||
											itemInfo.getId().equals("DIVAN_LEGGINGS") ||
											itemInfo.getId().equals("DIVAN_BOOTS")
										) {
											itemInfo.addExtraValues(gems.getList(gem.getKey()).size(), "GEMSTONE_CHAMBER");
										}
									} else if (gems.containsKey(gem.getKey() + "_gem")) {
										itemInfo.addExtraValue(gem.getValue() + "_" + gems.get(gem.getKey() + "_gem") + "_GEM");
									} else {
										itemInfo.addExtraValue(gem.getValue() + "_" + gem.getKey().split("_")[0] + "_GEM");
									}
								}
							}
						}

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.gemstone_slots", 0), "GEMSTONE_CHAMBER");

						try {
							byte[] backpackContents = item.getByteArray("tag.ExtraAttributes." + itemInfo.getId().toLowerCase() + "_data");
							NBTCompound parsedContentsBackpack = NBTReader.read(new ByteArrayInputStream(backpackContents));
							itemInfo.setBackpackItems(getGenericInventoryMap(parsedContentsBackpack).values());
						} catch (Exception ignored) {}

						itemsMap.put(i, itemInfo);
						continue;
					}
				} catch (Exception ignored) {}
				itemsMap.put(i, null);
			}

			return itemsMap;
		} catch (Exception ignored) {}

		return null;
	}

	public static InvItem nbtToItem(String rawContents) {
		try {
			return getGenericInventoryMap(NBTReader.readBase64(rawContents)).get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public static void cacheApplyGuildUsers() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			List<ApplyGuild> applySettings = automaticGuild.getValue().applyGuild;
			for (ApplyGuild applySetting : applySettings) {
				try {
					String name = higherDepth(applySetting.currentSettings, "guildName").getAsString();
					List<ApplyUser> applyUserList = applySetting.applyUserList
						.stream()
						.filter(a -> {
							try {
								return jda.getTextChannelById(a.applicationChannelId) != null;
							} catch (Exception e) {
								return false;
							}
						})
						.collect(Collectors.toList());
					database.setApplyCacheSettings(automaticGuild.getKey(), name, gson.toJson(applyUserList));

					if (applyUserList.size() > 0) {
						log.info(
							"Cached ApplyUser - size={" +
							applyUserList.size() +
							"}, guildId={" +
							automaticGuild.getKey() +
							"}, name={" +
							name +
							"}"
						);
					}
				} catch (Exception e) {
					log.error(
						"guildId={" +
						automaticGuild.getKey() +
						"}, name={" +
						higherDepth(applySetting.currentSettings, "guildName", "null") +
						"}",
						e
					);
				}
			}
		}
		log.info("Cached apply users in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
	}

	public static void cacheParties() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			try {
				List<Party> partyList = automaticGuild.getValue().partyList;
				if (partyList.size() > 0) {
					String partySettingsJson = gson.toJson(partyList);
					if (cacheDatabase.cachePartyData(automaticGuild.getValue().guildId, partySettingsJson)) {
						log.info("Successfully cached PartyList | " + automaticGuild.getKey() + " | " + partyList.size());
					}
				}
			} catch (Exception e) {
				log.error(automaticGuild.getKey(), e);
			}
		}
		log.info("Cached parties in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
	}

	public static List<ApplyUser> getApplyGuildUsersCache(String guildId, String name) {
		if (!isMainBot()) {
			return new ArrayList<>();
		}

		JsonArray applyUsersCache = database.getApplyCacheSettings(guildId, name);

		try {
			List<ApplyUser> applyUsersCacheList = streamJsonArray(applyUsersCache)
				.map(u -> gson.fromJson(u, ApplyUser.class))
				.filter(u -> {
					try {
						return jda.getTextChannelById(u.applicationChannelId) != null;
					} catch (Exception e) {
						return false;
					}
				})
				.collect(Collectors.toList());

			if (applyUsersCacheList.size() > 0) {
				log.info(
					"Retrieved ApplyUser cache - size={" + applyUsersCacheList.size() + "}, guildId={" + guildId + "}, name={" + name + "}"
				);
				return applyUsersCacheList;
			}
		} catch (Exception e) {
			log.error("guildId={" + guildId + "}, name={" + name + "}", e);
		}

		return new ArrayList<>();
	}

	public static void cacheCommandUses() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		if (cacheDatabase.cacheCommandUsage(gson.toJson(getCommandUses()))) {
			log.info("Cached command uses in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
		} else {
			log.error("Failed to cache command uses in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
		}
	}

	public static void cacheAhTracker() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		if (cacheDatabase.cacheAhTracker(gson.toJson(AuctionTracker.commandAuthorToTrackingUser))) {
			log.info(
				"Cached auction tracker in " +
				((System.currentTimeMillis() - startTime) / 1000) +
				"s | " +
				AuctionTracker.commandAuthorToTrackingUser.size()
			);
		} else {
			log.error("Failed to cache auction tracker in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
		}
	}

	public static void cacheJacobData() {
		if (!isMainBot()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		if (cacheDatabase.cacheJacobData(gson.toJson(JacobHandler.getJacobData()))) {
			log.info("Cached jacob data in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
		} else {
			log.error("Failed to cache jacob data in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
		}
	}

	public static void closeAsyncHttpClient() {
		try {
			asyncHttpClient.close();
			log.info("Successfully Closed Async Http Client");
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public static void closeHttpClient() {
		try {
			httpClient.close();
			log.info("Successfully Closed Http Client");
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public static int petLevelFromXp(long petExp, String rarity, String id) {
		int petRarityOffset = higherDepth(getPetJson(), "pet_rarity_offset." + rarity.toUpperCase()).getAsInt();
		JsonArray petLevelsXpPer = higherDepth(getPetJson(), "pet_levels").getAsJsonArray().deepCopy();
		JsonElement customLevelingJson = higherDepth(getPetJson(), "custom_pet_leveling." + id);
		if (customLevelingJson != null) {
			switch (higherDepth(customLevelingJson, "type", 0)) {
				case 1 -> petLevelsXpPer.addAll(higherDepth(customLevelingJson, "pet_levels").getAsJsonArray());
				case 2 -> petLevelsXpPer = higherDepth(customLevelingJson, "pet_levels").getAsJsonArray();
			}
		}
		int maxLevel = higherDepth(customLevelingJson, "max_level", 100);
		long totalExp = 0;
		for (int i = petRarityOffset; i < petLevelsXpPer.size(); i++) {
			totalExp += petLevelsXpPer.get(i).getAsLong();
			if (totalExp >= petExp) {
				return (Math.min(i - petRarityOffset + 1, maxLevel));
			}
		}
		return maxLevel;
	}

	public static void refreshPriceOverrideJson() {
		JsonElement splitPriceOverrides = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/PriceOverrides.json")
			.getAsJsonObject();
		priceOverrideJson = higherDepth(splitPriceOverrides, "automatic").getAsJsonObject();
		for (Map.Entry<String, JsonElement> manualOverride : higherDepth(splitPriceOverrides, "manual").getAsJsonObject().entrySet()) {
			priceOverrideJson.add(manualOverride.getKey(), manualOverride.getValue());
		}
		priceOverrideJson.remove("ENCHANTED_BOOK");
	}

	public static boolean isVanillaItem(String id) {
		if (vanillaItems == null) {
			vanillaItems =
				higherDepth(
					getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/PriceOverrides.json").getAsJsonObject(),
					"automatic"
				)
					.getAsJsonObject()
					.keySet();
		}

		return vanillaItems.contains(id);
	}

	public static double getPriceOverride(String itemId) {
		if (priceOverrideJson == null) {
			refreshPriceOverrideJson();
		}

		return priceOverrideJson.has(itemId) ? priceOverrideJson.get(itemId).getAsDouble() : -1;
	}

	public static double getMin(double val1, double val2) {
		val1 = val1 < 0 ? -1 : val1;
		val2 = val2 < 0 ? -1 : val2;

		if (val1 != -1 && val2 != -1) {
			return Math.max(Math.min(val1, val2), 0);
		} else if (val1 != -1) {
			return val1;
		} else {
			return val2;
		}
	}

	public static Permission[] defaultPerms() {
		return defaultPerms(false);
	}

	public static Permission[] defaultPerms(boolean isSlash) {
		return isSlash
			? new Permission[] { Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION }
			: new Permission[] { Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS };
	}

	public static Stream<JsonElement> streamJsonArray(JsonArray array) {
		List<JsonElement> list = new ArrayList<>();
		for (JsonElement element : array) {
			list.add(element);
		}
		return list.stream();
	}

	public static JsonArray collectJsonArray(Stream<JsonElement> list) {
		JsonArray array = new JsonArray();
		list.forEach(array::add);
		return array;
	}

	public static String nameMcHyperLink(String username, String uuid) {
		return "[**" + username + "**](https://mine.ly/" + uuid + ")";
	}

	public static boolean isMainBot() {
		return DEFAULT_PREFIX.equals("+");
	}

	public static Map<String, Integer> getCommandUses() {
		Map<String, Integer> commandUses = client
			.getCommands()
			.stream()
			.filter(command -> !command.isOwnerCommand())
			.collect(Collectors.toMap(Command::getName, command -> client.getCommandUses(command), (a, b) -> b));
		slashCommandClient
			.getCommands()
			.stream()
			.collect(Collectors.toMap(SlashCommand::getName, command -> slashCommandClient.getCommandUses(command), (a, b) -> b))
			.forEach((key, value) -> commandUses.merge(key, value, Integer::sum));
		return commandUses;
	}

	public static int getUserCount() {
		if (userCount == -1 || Duration.between(userCountLastUpdated, Instant.now()).toMinutes() >= 60) {
			userCount = jda.getGuilds().stream().mapToInt(Guild::getMemberCount).sum();
			userCountLastUpdated = Instant.now();
		}

		return userCount;
	}

	public static String getItemThumbnail(String id) {
		if (PET_NAMES.contains(id.split(";")[0].trim())) {
			return getPetUrl(id.split(";")[0].trim());
		} else if (ENCHANT_NAMES.contains(id.split(";")[0].trim())) {
			return "https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK";
		}
		return "https://sky.shiiyu.moe/item.gif/" + id;
	}
}
