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
import static com.skyblockplus.utils.ApiHandler.playerFromUuid;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.*;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.exceptionhandler.ExceptionExecutor;
import com.skyblockplus.utils.structs.*;
import java.awt.*;
import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import okhttp3.OkHttpClient;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	/* Constants */
	public static final Color botColor = new Color(223, 5, 5);
	public static final int globalCooldown = 4;
	public static final String DISCORD_SERVER_INVITE_LINK = "https://discord.gg/Z4Fn3eNDXT";
	public static final String BOT_INVITE_LINK_REQUIRED_SLASH =
		"https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot%20applications.commands";
	public static final String FORUM_POST_LINK = "https://hypixel.net/threads/3980092";
	private static final Pattern patternControlCode = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
	public static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
	public static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	public static final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
	public static final ExecutorService executor = new ExceptionExecutor();
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
	public static final ScriptEngine jsScriptEngine = new ScriptEngineManager().getEngineByName("js");
	public static final AtomicInteger remainingLimit = new AtomicInteger(120);
	public static final AtomicInteger timeTillReset = new AtomicInteger(0);
	public static final ConcurrentHashMap<String, HypixelKeyInformation> keyCooldownMap = new ConcurrentHashMap<>();
	public static final Cache<String, HypixelGuildCache> hypixelGuildsCacheMap = Caffeine
		.newBuilder()
		.expireAfterWrite(15, TimeUnit.MINUTES)
		.build();
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	public static final Gson gson = new Gson();
	public static final Gson formattedGson = new GsonBuilder().setPrettyPrinting().create();
	/* Configuration File */
	public static String HYPIXEL_API_KEY = "";
	public static String BOT_TOKEN = "";
	public static String CLIENT_SECRET = "";
	public static String DATABASE_URL = "";
	public static String DATABASE_USERNAME = "";
	public static String DATABASE_PASSWORD = "";
	public static String API_USERNAME = "";
	public static String API_PASSWORD = "";
	public static String API_BASE_URL = "";
	public static String GITHUB_TOKEN = "";
	public static String DEFAULT_PREFIX = "";
	public static String CACHE_DATABASE_TOKEN = "";
	public static String AUCTION_API_KEY = "";
	public static boolean IS_API = false;
	/* JSON */
	private static JsonElement essenceCostsJson;
	private static JsonElement levelingJson;
	private static JsonObject collectionsJson;
	private static JsonElement skyCryptPetJson;
	private static JsonElement enchantsJson;
	private static JsonElement petNumsJson;
	private static JsonElement petsJson;
	private static JsonElement reforgeStonesJson;
	private static JsonElement bitsJson;
	private static JsonElement miscJson;
	private static JsonElement talismanJson;
	private static JsonElement lowestBinJson;
	private static JsonElement averageAuctionJson;
	private static JsonElement bazaarJson;
	private static JsonArray sbzPricesJson;
	public static JsonObject internalJsonMappings;
	private static JsonObject emojiMap;
	public static JsonObject priceOverrideJson;
	/* Miscellaneous */
	public static TextChannel botLogChannel;
	public static TextChannel errorLogChannel;
	public static JDAWebhookClient webhookClient;
	public static Instant lowestBinJsonLastUpdated = Instant.now();
	public static Instant averageAuctionJsonLastUpdated = Instant.now();
	public static Instant bazaarJsonLastUpdated = Instant.now();
	public static Instant sbzPricesJsonLastUpdated = Instant.now();

	/* Getters */
	public static JsonElement getLowestBinJson() {
		if (lowestBinJson == null || Duration.between(lowestBinJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
			lowestBinJsonLastUpdated = Instant.now();
		}

		return lowestBinJson;
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

	public static boolean getEmojiMap(boolean forceReload) {
		if (forceReload) {
			try {
				emojiMap =
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/IdToEmojiMappings.json")).getAsJsonObject();
			} catch (Exception ignored) {}
		}

		return emojiMap != null && forceReload;
	}

	public static JsonElement getAverageAuctionJson() {
		if (averageAuctionJson == null || Duration.between(averageAuctionJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			averageAuctionJson = getJson("https://moulberry.codes/auction_averages/3day.json");
			averageAuctionJsonLastUpdated = Instant.now();
		}

		return averageAuctionJson;
	}

	public static JsonElement getBazaarJson() {
		if (bazaarJson == null || Duration.between(bazaarJsonLastUpdated, Instant.now()).toMinutes() >= 1) {
			bazaarJson = getJson("https://api.hypixel.net/skyblock/bazaar");
			bazaarJsonLastUpdated = Instant.now();
		}

		return bazaarJson;
	}

	public static JsonArray getSbzPricesJson() {
		if (sbzPricesJson == null || Duration.between(sbzPricesJsonLastUpdated, Instant.now()).toMinutes() >= 15) {
			sbzPricesJson = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json").getAsJsonArray();
			sbzPricesJsonLastUpdated = Instant.now();
		}

		return sbzPricesJson;
	}

	public static JsonElement getMiscJson() {
		if (miscJson == null) {
			miscJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/misc.json");
		}

		return miscJson;
	}

	public static JsonElement getTalismanJson() {
		if (talismanJson == null) {
			talismanJson =
				parseJsString(
					"{" +
					getSkyCryptData("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/talismans.js")
						.replace("export const ", "")
						.replace(" = ", ": ")
						.replace(";", ",") +
					"}"
				);
		}

		return talismanJson;
	}

	public static JsonElement getBitsJson() {
		if (bitsJson == null) {
			bitsJson = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/BitPricesJson.json");
		}

		return bitsJson;
	}

	public static JsonElement getReforgeStonesJson() {
		if (reforgeStonesJson == null) {
			reforgeStonesJson =
				getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/reforgestones.json");
		}

		return reforgeStonesJson;
	}

	public static JsonElement getPetJson() {
		if (petsJson == null) {
			petsJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/pets.json");
		}
		return petsJson;
	}

	public static JsonElement getPetNumsJson() {
		if (petNumsJson == null) {
			petNumsJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/petnums.json");
		}
		return petNumsJson;
	}

	public static JsonElement getEnchantsJson() {
		if (enchantsJson == null) {
			enchantsJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/enchants.json");
		}
		return enchantsJson;
	}

	public static JsonElement getLevelingJson() {
		if (levelingJson == null) {
			levelingJson = getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
		}
		return levelingJson;
	}

	public static JsonElement getEssenceCostsJson() {
		if (essenceCostsJson == null) {
			essenceCostsJson =
				getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/essencecosts.json");
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

	public static JsonElement getSkyCryptPetJson() {
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
				);
		}

		return skyCryptPetJson;
	}

	/* Http requests */
	public static JsonElement getJson(String jsonUrl) {
		try {
			if (jsonUrl.contains(HYPIXEL_API_KEY) && remainingLimit.get() < 5) {
				log.info("Sleeping for " + timeTillReset + " seconds");
				TimeUnit.SECONDS.sleep(timeTillReset.get());
			}
		} catch (Exception ignored) {}

		try {
			HttpGet httpget = new HttpGet(jsonUrl);
			if (jsonUrl.contains("raw.githubusercontent.com")) {
				httpget.setHeader("Authorization", "token " + GITHUB_TOKEN);
			}
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				if (jsonUrl.toLowerCase().contains("api.hypixel.net") && jsonUrl.contains(HYPIXEL_API_KEY)) {
					try {
						remainingLimit.set(Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Remaining").getValue()));
						timeTillReset.set(Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Reset").getValue()));
					} catch (Exception ignored) {}
				}

				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static String getSkyCryptData(String dataUrl) {
		if (!dataUrl.contains("raw.githubusercontent.com")) {
			return null;
		}

		try {
			HttpGet httpget = new HttpGet(dataUrl);
			httpget.setHeader("Authorization", "token " + GITHUB_TOKEN);
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
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

	public static String makeHastePost(String body) {
		try {
			HttpPost httpPost = new HttpPost("https://hst.sh/documents");

			StringEntity entity = new StringEntity(body);
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

	public static DiscordInfoStruct getPlayerDiscordInfo(String username) {
		try {
			UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
			if (usernameUuidStruct.isNotValid()) {
				return new DiscordInfoStruct(usernameUuidStruct.getFailCause());
			}
			HypixelResponse response = playerFromUuid(usernameUuidStruct.getUuid());
			if (response.isNotValid()) {
				return new DiscordInfoStruct(response.getFailCause());
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
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()))
					.lines()
					.parallel()
					.collect(Collectors.joining("\n"));
			}
		} catch (Exception ignored) {}
		return null;
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

	public static void logCommand(String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		botLogChannel.sendMessageEmbeds(defaultEmbed(null).setDescription(commandInput).build()).queue();
	}

	/* Embeds and paginators */
	public static EmbedBuilder defaultEmbed(String title, String titleUrl) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(botColor);
		eb.setFooter("Created by CrypticPlasma", null);
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
			.setFinalAction(m -> m.editMessageComponents().queue())
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
		DecimalFormat df = new DecimalFormat("#.#");
		df.setRoundingMode(RoundingMode.HALF_UP);
		if (1000000000000D > number && number >= 1000000000) {
			df = new DecimalFormat("#.##");
			df.setRoundingMode(RoundingMode.HALF_UP);
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
			df = new DecimalFormat("#.##");
			df.setRoundingMode(RoundingMode.HALF_UP);
			formattedNumber = df.format(number);
		}
		return formattedNumber;
	}

	public static String capitalizeString(String str) {
		return Stream
			.of(str.trim().split("\\s"))
			.filter(word -> word.length() > 0)
			.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
			.collect(Collectors.joining(" "));
	}

	public static String parseMcCodes(String unformattedString) {
		return patternControlCode.matcher(unformattedString.replace("\u00A7ka", "")).replaceAll("");
	}

	public static String fixUsername(String username) {
		return username.replace("_", "\\_");
	}

	/* Miscellaneous */
	public static JsonElement higherDepth(JsonElement element, String path) {
		String[] paths = path.split("\\.");

		try {
			for (String key : paths) {
				if (key.startsWith("[") && key.endsWith("]") && key.length() >= 3) {
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
		if (internalJsonMappings == null) {
			internalJsonMappings =
				getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json").getAsJsonObject();
		}

		String internalName = itemName
			.trim()
			.toUpperCase()
			.replace(" ", "_")
			.replace("'S", "")
			.replace("FRAG", "FRAGMENT")
			.replace(".", "");

		switch (internalName) {
			case "GOD_POT":
				internalName = "GOD_POTION";
				break;
			case "AOTD":
				internalName = "ASPECT_OF_THE_DRAGON";
				break;
			case "AOTE":
				internalName = "ASPECT_OF_THE_END";
				break;
			case "AOTV":
				internalName = "ASPECT_OF_THE_VOID";
				break;
			case "AOTS:":
				internalName = "AXE_OF_THE_SHREDDED";
				break;
			case "LASR_EYE":
				internalName = "GIANT_FRAGMENT_LASER";
				break;
		}

		try {
			internalName = internalJsonMappings.getAsJsonArray(internalName).get(0).getAsString();
		} catch (Exception ignored) {}

		return internalName;
	}

	public static String idToName(String id) {
		if (internalJsonMappings == null) {
			try {
				internalJsonMappings =
					getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json")
						.getAsJsonObject();
			} catch (Exception ignored) {}
		}

		id = id.toUpperCase();

		for (Map.Entry<String, JsonElement> i : internalJsonMappings.entrySet()) {
			for (JsonElement j : i.getValue().getAsJsonArray()) {
				if (j.getAsString().equals(id)) {
					return capitalizeString(i.getKey().replace("_", " "));
				}
			}
		}

		return capitalizeString(id.replace("_", " "));
	}

	public static ArrayList<String> getJsonKeys(JsonElement jsonElement) {
		try {
			return new ArrayList<>(jsonElement.getAsJsonObject().keySet());
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	public static String profileNameToEmoji(String profileName) {
		switch (profileName) {
			case "apple":
				return "\uD83C\uDF4E";
			case "banana":
				return "\uD83C\uDF4C";
			case "blueberry":
				return "\uD83E\uDED0";
			case "coconut":
				return "\uD83E\uDD65";
			case "cucumber":
				return "\uD83E\uDD52";
			case "grapes":
				return "\uD83C\uDF47";
			case "kiwi":
				return "\uD83E\uDD5D";
			case "lemon":
				return "\uD83C\uDF4B";
			case "lime":
				return "lime:828632854174498837";
			case "mango":
				return "\uD83E\uDD6D";
			case "orange":
				return "orange:828634110360289331";
			case "papaya":
				return "papaya:828633125370200085";
			case "peach":
				return "\uD83C\uDF51";
			case "pear":
				return "\uD83C\uDF50";
			case "pineapple":
				return "\uD83C\uDF4D";
			case "pomegranate":
				return "pomegranate:828632397032456232";
			case "raspberry":
				return "raspberry:828632035127853064";
			case "strawberry":
				return "\uD83C\uDF53";
			case "tomato":
				return "\uD83C\uDF45";
			case "watermelon":
				return "\uD83C\uDF49";
			case "zucchini":
				return "zucchini:828636746358194206";
			default:
				return null;
		}
	}

	public static EmbedBuilder checkHypixelKey(String hypixelKey) {
		if (hypixelKey == null) {
			return invalidEmbed("You must set a Hypixel API key to use this command");
		}

		try {
			higherDepth(getJson("https://api.hypixel.net/key?key=" + hypixelKey), "record.key").getAsString();
		} catch (Exception e) {
			return invalidEmbed("The set Hypixel API key is invalid");
		}

		if (!keyCooldownMap.containsKey(hypixelKey)) {
			keyCooldownMap.put(hypixelKey, new HypixelKeyInformation());
		}

		return null;
	}

	public static String instantToDHM(Duration duration) {
		if (duration.toMinutes() == 0) {
			return instantToMS(duration);
		}

		long daysUntil = duration.toMinutes() / 1440;
		long hoursUntil = duration.toMinutes() / 60 % 24;
		long minutesUntil = duration.toMinutes() % 60;
		String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
		timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
		timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";

		return timeUntil.length() > 0 ? timeUntil.trim() : "0m";
	}

	public static String instantToMS(Duration duration) {
		long secondsDuration = duration.toMillis() / 1000;
		long minutesUntil = secondsDuration / 60 % 60;
		long secondsUntil = secondsDuration % 60;

		String timeUntil = minutesUntil > 0 ? minutesUntil + "m " : "";
		timeUntil += secondsUntil > 0 ? secondsUntil + "s " : "";

		return timeUntil.length() > 0 ? timeUntil.trim() : "0s";
	}

	public static void initialize() {
		Properties appProps = new Properties();
		try {
			IS_API = false;
			appProps.load(new FileInputStream("DevSettings.properties"));
			HYPIXEL_API_KEY = (String) appProps.get("HYPIXEL_API_KEY");
			BOT_TOKEN = (String) appProps.get("BOT_TOKEN");
			CLIENT_SECRET = (String) appProps.get("CLIENT_SECRET");
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
			API_BASE_URL = (String) appProps.get("API_BASE_URL");
			DEFAULT_PREFIX = (String) appProps.get("DEFAULT_PREFIX");
			CACHE_DATABASE_TOKEN = (String) appProps.get("CACHE_DATABASE_TOKEN");
			AUCTION_API_KEY = (String) appProps.get("AUCTION_API_KEY");
		} catch (IOException e) {
			HYPIXEL_API_KEY = System.getenv("HYPIXEL_API_KEY");
			BOT_TOKEN = System.getenv("BOT_TOKEN");
			CLIENT_SECRET = System.getenv("CLIENT_SECRET");
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
			API_BASE_URL = System.getenv("API_BASE_URL");
			IS_API = API_BASE_URL.equals("https://skyblock-plus.up.railway.app/");
			DEFAULT_PREFIX = System.getenv("DEFAULT_PREFIX");
			CACHE_DATABASE_TOKEN = System.getenv("CACHE_DATABASE_TOKEN");
			AUCTION_API_KEY = System.getenv("AUCTION_API_KEY");
		}
	}

	public static String getClosestMatch(String toMatch, List<String> matchFrom) {
		if (matchFrom == null || matchFrom.size() == 0) {
			return toMatch;
		}

		LevenshteinDistance matchCalc = LevenshteinDistance.getDefaultInstance();
		int minDistance = matchCalc.apply(matchFrom.get(0), toMatch);
		String closestMatch = matchFrom.get(0);
		for (String itemF : matchFrom) {
			int currentDistance = matchCalc.apply(itemF, toMatch);
			if (currentDistance < minDistance) {
				minDistance = currentDistance;
				closestMatch = itemF;
			}
		}

		return closestMatch;
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
								enchantsList.add(enchant.getKey() + ";" + enchant.getValue());
							}
							itemInfo.setEnchantsFormatted(enchantsList);
						}

						String itemSkinStr = item.getString("tag.ExtraAttributes.skin", "None");
						if (!itemSkinStr.equals("None")) {
							itemInfo.addExtraValue("PET_SKIN_" + itemSkinStr);
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

						for (int j = 0; j < item.getInt("tag.ExtraAttributes.farming_for_dummies_count", 0); j++) {
							itemInfo.addExtraValue("FARMING_FOR_DUMMIES");
						}

						if (item.containsTag("tag.ExtraAttributes.gems", TagType.COMPOUND)) {
							NBTCompound gems = item.getCompound("tag.ExtraAttributes.gems");
							for (Map.Entry<String, Object> gem : gems.entrySet()) {
								if (!gem.getKey().endsWith("_gem")) {
									if (gems.containsKey(gem.getKey() + "_gem")) {
										itemInfo.addExtraValue(gem.getValue() + "_" + gems.get(gem.getKey() + "_gem") + "_GEM");
									} else {
										itemInfo.addExtraValue(gem.getValue() + "_" + gem.getKey().split("_")[0] + "_GEM");
									}
								}
							}
						}

						for (int j = 0; j < item.getInt("tag.ExtraAttributes.gemstone_slots", 0); j++) {
							itemInfo.addExtraValue("GEMSTONE_CHAMBER");
						}

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

	public static void cacheApplyGuildUsers() {
		if (!DEFAULT_PREFIX.equals("+")) {
			return;
		}

		long startTime = System.currentTimeMillis();
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
							gson.toJson(applyUserList)
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
				ApplyUser currentApplyUserCache = gson.fromJson(applyUserCache, ApplyUser.class);
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
		scheduler.scheduleWithFixedDelay(Utils::updateLinkedAccounts, 1, 1, TimeUnit.MINUTES);
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
				.filter(linkedAccountModel ->
					Duration.between(Instant.ofEpochMilli(Long.parseLong(linkedAccountModel.getLastUpdated())), Instant.now()).toDays() > 1
				)
				.findAny()
				.ifPresent(notUpdated -> {
					try {
						DiscordInfoStruct discordInfo = getPlayerDiscordInfo(notUpdated.getMinecraftUsername());
						User updateUser = jda.retrieveUserById(notUpdated.getDiscordId()).complete();
						if (discordInfo.getDiscordTag().equals(updateUser.getAsTag())) {
							database.addLinkedUser(
								new LinkedAccountModel(
									"" + Instant.now().toEpochMilli(),
									updateUser.getId(),
									discordInfo.getUuid(),
									discordInfo.getUsername()
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
				});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static int petLevelFromXp(long petExp, String rarity) {
		int petRarityOffset = higherDepth(getPetJson(), "pet_rarity_offset." + rarity.toUpperCase()).getAsInt();
		JsonArray petLevelsXpPer = higherDepth(getPetJson(), "pet_levels").getAsJsonArray();
		long totalExp = 0;
		for (int i = petRarityOffset; i < petLevelsXpPer.size(); i++) {
			totalExp += petLevelsXpPer.get(i).getAsLong();
			if (totalExp >= petExp) {
				return (Math.min(i - petRarityOffset + 1, 100));
			}
		}
		return 100;
	}

	public static void refreshPriceOverrideJson() {
		JsonElement splitPriceOverrides = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/PriceOverrides.json")
			.getAsJsonObject();
		priceOverrideJson = higherDepth(splitPriceOverrides, "automatic").getAsJsonObject();
		for (Map.Entry<String, JsonElement> manualOverride : higherDepth(splitPriceOverrides, "manual").getAsJsonObject().entrySet()) {
			priceOverrideJson.add(manualOverride.getKey(), manualOverride.getValue());
		}
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
		return new Permission[] { Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION };
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
}
