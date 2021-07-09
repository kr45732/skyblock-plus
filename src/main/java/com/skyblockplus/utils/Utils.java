package com.skyblockplus.utils;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.CustomPaginator.throwableConsumer;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelKeyInformation;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;

public class Utils {

	/* Constants */
	public static final Color botColor = new Color(223, 5, 5);
	public static final int globalCooldown = 4;
	public static final String DISCORD_SERVER_INVITE_LINK = "https://discord.gg/Z4Fn3eNDXT";
	public static final String BOT_INVITE_LINK_REQUIRED_NO_SLASH =
		"https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot";
	public static final String BOT_INVITE_LINK_REQUIRED_SLASH =
		"https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=403040368&scope=bot%20applications.commands";
	public static final String FORUM_POST_LINK = "https://hypixel.net/threads/3980092";
	public static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
	public static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	public static final ExecutorService executor = Executors.newCachedThreadPool();
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
	/* Script Engines */
	public static final ScriptEngine jsScriptEngine = new ScriptEngineManager().getEngineByName("js");
	public static final ScriptEngine es6ScriptEngine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
	public static final AtomicInteger remainingLimit = new AtomicInteger(120);
	public static final AtomicInteger timeTillReset = new AtomicInteger(0);
	public static final ConcurrentHashMap<String, HypixelKeyInformation> keyCooldownMap = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<String, HypixelGuildCache> hypixelGuildsCacheMap = new ConcurrentHashMap<>();
	/* Configuration File */
	public static String HYPIXEL_API_KEY = "";
	public static String BOT_TOKEN = "";
	public static String CLIENT_ID = "";
	public static String CLIENT_SECRET = "";
	public static String DATABASE_URL = "";
	public static String DATABASE_USERNAME = "";
	public static String DATABASE_PASSWORD = "";
	public static String API_USERNAME = "";
	public static String API_PASSWORD = "";
	public static String API_BASE_URL = "";
	public static String GITHUB_TOKEN = "";
	public static String DEFAULT_PREFIX = "";
	/* JSON */
	public static JsonElement essenceCostsJson;
	public static JsonElement levelingJson;
	public static JsonObject collectionsJson;
	public static JsonElement petUrlJson;
	public static JsonElement enchantsJson;
	public static JsonElement petNumsJson;
	public static JsonElement petsJson;
	public static JsonElement reforgeStonesJson;
	public static JsonElement bitsJson;
	public static JsonElement miscJson;
	public static JsonElement talismanJson;
	public static JsonElement lowestBinJson;
	public static JsonElement averageAuctionJson;
	public static JsonElement bazaarJson;
	public static JsonArray sbzPricesJson;
	public static JsonObject internalJsonMappings;
	public static JsonObject emojiMap;
	/* Miscellaneous */
	public static MessageChannel botLogChannel;
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
					getSkyCryptData("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/talismans.js")
				);
		}

		return talismanJson;
	}

	public static JsonElement getBitsJson() {
		if (bitsJson == null) {
			bitsJson = getJson("https://raw.githubusercontent.com/SkyKings-Guild/SkyKings/main/bot-data/bit-prices.json");
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

	/* Http requests */
	public static JsonElement getJson(String jsonUrl) {
		try {
			if (jsonUrl.contains(HYPIXEL_API_KEY) && remainingLimit.get() < 5) {
				System.out.println("Sleeping for " + timeTillReset + " seconds");
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
				return result.toString().split("module.exports = ")[1].replace(";", "");
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

	public static String makeJsonHastePost(String body) {
		String url = makeHastePost(body);
		if (url == null) {
			return null;
		}

		return "https://hst.sh/raw/" + url.split("https://hst.sh/")[1] + ".json";
	}

	public static DiscordInfoStruct getPlayerDiscordInfo(String username) {
		try {
			JsonElement playerJson = Hypixel.playerFromUuid(Hypixel.usernameToUuid(username).playerUuid);

			String discordTag = higherDepth(playerJson, "player.socialMedia.links.DISCORD").getAsString();
			String minecraftUsername = higherDepth(playerJson, "player.displayname").getAsString();
			String minecraftUuid = higherDepth(playerJson, "player.uuid").getAsString();

			return new DiscordInfoStruct(discordTag, minecraftUsername, minecraftUuid);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getPetUrl(String petName) {
		if (petUrlJson == null) {
			petUrlJson =
				parseJsString(
					getSkyCryptData("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/pets.js")
						.split("pet_value")[0] +
					"}"
				);
		}
		try {
			return ("https://sky.lea.moe" + higherDepth(petUrlJson, "pet_data." + petName.toUpperCase() + ".head").getAsString());
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
		eb.setAuthor(guild.getName() + " (" + guild.getId() + ")", null, guild.getIconUrl());
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

		EmbedBuilder eb = defaultEmbed(null);
		eb.setDescription(commandInput);
		botLogChannel.sendMessageEmbeds(eb.build()).queue();
	}

	/* Embeds and paginators */
	public static EmbedBuilder defaultEmbed(String title, String titleUrl) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(botColor);
		eb.setFooter("Created by CrypticPlasma", null);
		eb.setTitle(title, titleUrl);
		eb.setTimestamp(Instant.now());
		return eb;
	}

	public static EmbedBuilder defaultEmbed(String title) {
		return defaultEmbed(title, null);
	}

	public static EmbedBuilder loadingEmbed() {
		return defaultEmbed(null).setImage("https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif");
	}

	public static EmbedBuilder errorEmbed(String name) {
		return defaultEmbed("Invalid input. Run `help " + name + "` for help");
	}

	public static CustomPaginator.Builder defaultPaginator(EventWaiter waiter, User eventAuthor) {
		CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder()
			.setColumns(1)
			.setItemsPerPage(1)
			.showPageNumbers(true)
			.setFinalAction(
				m -> {
					try {
						m.clearReactions().queue(null, throwableConsumer);
					} catch (PermissionException ex) {
						m.delete().queue(null, throwableConsumer);
					}
				}
			)
			.setEventWaiter(waiter)
			.setTimeout(30, TimeUnit.SECONDS)
			.setColor(botColor);
		if (eventAuthor != null) {
			paginateBuilder.setUsers(eventAuthor);
		}
		return paginateBuilder;
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
		return unformattedString.replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|§7|§8|§l|§o|§b|§2|§e|§r|§3|§1|§ka", "");
	}

	public static String fixUsername(String username) {
		return username.replace("_", "\\_");
	}

	/* Miscellaneous */
	public static JsonElement higherDepth(JsonElement element, String path) {
		String[] paths = path.split("\\.");

		try {
			for (String key : paths) {
				element = element.getAsJsonObject().get(key);
			}
			return element;
		} catch (Exception e) {
			return null;
		}
	}

	public static String toRomanNumerals(int number) {
		return join("", nCopies(number, "i")).replace("iiiii", "v").replace("iiii", "iv").replace("vv", "x").replace("viv", "ix");
	}

	public static JsonElement parseJsString(String jsString) {
		try {
			return JsonParser.parseString(jsScriptEngine.eval(String.format("JSON.stringify(%s);", jsString)).toString());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String convertSkyblockIdName(String itemName) {
		try {
			return higherDepth(getCollectionsJson(), itemName + ".name").getAsString();
		} catch (Exception ignored) {}
		return capitalizeString(itemName.replace("_", " "));
	}

	public static String convertToInternalName(String itemName) {
		if (internalJsonMappings == null) {
			try {
				internalJsonMappings =
					JsonParser
						.parseReader(new FileReader("src/main/java/com/skyblockplus/json/InternalNameMappings.json"))
						.getAsJsonObject();
			} catch (Exception ignored) {}
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

	public static String convertFromInternalName(String internalName) {
		if (internalJsonMappings == null) {
			try {
				internalJsonMappings =
					JsonParser
						.parseReader(new FileReader("src/main/java/com/skyblockplus/json/InternalNameMappings.json"))
						.getAsJsonObject();
			} catch (Exception ignored) {}
		}

		internalName = internalName.toUpperCase();

		for (Map.Entry<String, JsonElement> i : internalJsonMappings.entrySet()) {
			for (JsonElement j : i.getValue().getAsJsonArray()) {
				if (j.getAsString().equals(internalName)) {
					return capitalizeString(i.getKey().replace("_", " "));
				}
			}
		}

		return capitalizeString(internalName.replace("_", " "));
	}

	public static ArrayList<String> getJsonKeys(JsonElement jsonElement) {
		try {
			return new ArrayList<>(jsonElement.getAsJsonObject().keySet());
		} catch (Exception e) {
			return null;
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

	public static String emojiToProfileName(String emoji) {
		switch (emoji) {
			case "\uD83C\uDF4E":
				return "apple";
			case "\uD83C\uDF4C":
				return "banana";
			case "\uD83E\uDED0":
				return "blueberry";
			case "\uD83E\uDD65":
				return "coconut";
			case "\uD83E\uDD52":
				return "cucumber";
			case "\uD83C\uDF47":
				return "grapes";
			case "\uD83E\uDD5D":
				return "kiwi";
			case "\uD83C\uDF4B":
				return "lemon";
			case "lime:828632854174498837":
				return "lime";
			case "\uD83E\uDD6D":
				return "mango";
			case "orange:828634110360289331":
				return "orange";
			case "papaya:828633125370200085":
				return "papaya";
			case "\uD83C\uDF51":
				return "peach";
			case "\uD83C\uDF50":
				return "pear";
			case "\uD83C\uDF4D":
				return "pineapple";
			case "pomegranate:828632397032456232":
				return "pomegranate";
			case "raspberry:828632035127853064":
				return "raspberry";
			case "\uD83C\uDF53":
				return "strawberry";
			case "\uD83C\uDF45":
				return "tomato";
			case "\uD83C\uDF49":
				return "watermelon";
			case "zucchini:828636746358194206":
				return "zucchini";
			default:
				return null;
		}
	}

	public static EmbedBuilder checkHypixelKey(String HYPIXEL_KEY) {
		if (HYPIXEL_KEY == null) {
			return defaultEmbed("Error").setDescription("You must set a Hypixel API key to use this command");
		}

		try {
			higherDepth(getJson("https://api.hypixel.net/key?key=" + HYPIXEL_KEY), "record.key").getAsString();
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("The set Hypixel API key is invalid");
		}

		if (!keyCooldownMap.containsKey(HYPIXEL_KEY)) {
			keyCooldownMap.put(HYPIXEL_KEY, new HypixelKeyInformation());
		}

		return null;
	}

	public static String instantToDHM(Duration duration) {
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

	public static void setApplicationSettings() {
		Properties appProps = new Properties();
		try {
			appProps.load(new FileInputStream("DevSettings.properties"));
			HYPIXEL_API_KEY = (String) appProps.get("HYPIXEL_API_KEY");
			BOT_TOKEN = (String) appProps.get("BOT_TOKEN");
			CLIENT_ID = (String) appProps.get("CLIENT_ID");
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
		} catch (IOException e) {
			HYPIXEL_API_KEY = System.getenv("HYPIXEL_API_KEY");
			BOT_TOKEN = System.getenv("BOT_TOKEN");
			CLIENT_ID = System.getenv("CLIENT_ID");
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
			DEFAULT_PREFIX = System.getenv("DEFAULT_PREFIX");
		}
	}

	public static String getClosestMatch(String toMatch, List<String> matchFrom) {
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
}
