package com.skyblockplus.utils;

import static com.skyblockplus.Main.jda;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.awt.*;
import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class Utils {

	public static final Color botColor = new Color(223, 5, 5);
	public static final int globalCooldown = 4;
	public static final ScriptEngine jsScriptEngine = new ScriptEngineManager().getEngineByName("js");
	public static String HYPIXEL_API_KEY = "";
	public static String BOT_TOKEN = "";
	public static String BOT_PREFIX = "";
	public static String CLIENT_ID = "";
	public static String CLIENT_SECRET = "";
	public static String DATABASE_URL = "";
	public static String DATABASE_USERNAME = "";
	public static String DATABASE_PASSWORD = "";
	public static String API_USERNAME = "";
	public static String API_PASSWORD = "";
	public static String API_BASE_URL = "";
	public static MessageChannel botLogChannel;
	public static JsonElement essenceCostsJson;
	public static JsonElement levelingJson;
	public static int remainingLimit = 120;
	public static int timeTillReset = 60;
	public static String GITHUB_TOKEN = "";
	public static JsonElement collectionsJson;
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
	private static Instant lowestBinJsonLastUpdated = Instant.now();
	private static Instant averageAuctionJsonLastUpdated = Instant.now();
	private static Instant bazaarJsonLastUpdated = Instant.now();
	private static Instant sbzPricesJsonLastUpdated = Instant.now();
	private static JsonElement internalJsonMappings;

	public static JsonElement getLowestBinJson() {
		if (lowestBinJson == null || Duration.between(lowestBinJsonLastUpdated, Instant.now()).toMinutes() > 1) {
			lowestBinJson = getJson("https://moulberry.codes/lowestbin.json");
			lowestBinJsonLastUpdated = Instant.now();
		}

		return lowestBinJson;
	}

	public static JsonElement getAverageAuctionJson() {
		if (averageAuctionJson == null || Duration.between(averageAuctionJsonLastUpdated, Instant.now()).toMinutes() > 1) {
			averageAuctionJson = getJson("https://moulberry.codes/auction_averages/3day.json");
			averageAuctionJsonLastUpdated = Instant.now();
		}

		return averageAuctionJson;
	}

	public static JsonElement getBazaarJson() {
		if (bazaarJson == null || Duration.between(bazaarJsonLastUpdated, Instant.now()).toMinutes() > 1) {
			bazaarJson = getJson("https://api.hypixel.net/skyblock/bazaar");
			bazaarJsonLastUpdated = Instant.now();
		}

		return bazaarJson;
	}

	public static JsonArray getSbzPricesJson() {
		if (sbzPricesJson == null || Duration.between(sbzPricesJsonLastUpdated, Instant.now()).toMinutes() > 5) {
			sbzPricesJson = getJson("https://raw.githubusercontent.com/skyblockz/pricecheckbot/master/data.json").getAsJsonArray();
			sbzPricesJsonLastUpdated = Instant.now();
		}

		return sbzPricesJson;
	}

	public static void setApplicationSettings() {
		Properties appProps = new Properties();
		try {
			appProps.load(new FileInputStream("DevSettings.properties"));
			BOT_PREFIX = (String) appProps.get("BOT_PREFIX");
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
		} catch (IOException e) {
			BOT_PREFIX = System.getenv("BOT_PREFIX");
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
		}
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
			bitsJson = getJson("https://raw.githubusercontent.com/plun1331/SkyKings/main/bot-data/bit-prices.json");
		}

		return bitsJson;
	}

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

	public static String toRomanNumerals(int number) {
		return join("", nCopies(number, "i")).replace("iiiii", "v").replace("iiii", "iv").replace("vv", "x").replace("viv", "ix");
	}

	public static String getSkyCryptData(String dataUrl) {
		if (!dataUrl.contains("raw.githubusercontent.com")) {
			return null;
		}

		try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {
			HttpGet httpget = new HttpGet(dataUrl);
			httpget.setHeader("Authorization", "token " + GITHUB_TOKEN);
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {
				InputStream inputStream = httpresponse.getEntity().getContent();
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				for (int length; (length = inputStream.read(buffer)) != -1;) {
					result.write(buffer, 0, length);
				}
				return result.toString().split("module.exports = ")[1];
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement parseJsString(String jsString) {
		try {
			return JsonParser.parseString(jsScriptEngine.eval(String.format("JSON.stringify(%s);", jsString)).toString());
		} catch (Exception e) {
			e.printStackTrace();
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

	public static String convertSkyblockIdName(String itemName) {
		if (collectionsJson == null) {
			collectionsJson =
				parseJsString(
					getSkyCryptData("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/collections.js")
						.replace(";", "")
				);
		}

		try {
			JsonArray collectionsArray = higherDepth(collectionsJson, "collection_data").getAsJsonArray();
			for (JsonElement collection : collectionsArray) {
				try {
					if (higherDepth(collection, "skyblockId").getAsString().equals(itemName)) {
						return higherDepth(collection, "name").getAsString();
					}
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}
		return capitalizeString(itemName.replace("_", " ").toLowerCase());
	}

	public static JsonElement getJson(String jsonUrl) {
		try {
			if (remainingLimit < 5) {
				System.out.println("Sleeping for " + timeTillReset + " seconds");
				TimeUnit.SECONDS.sleep(timeTillReset);
			}
		} catch (Exception ignored) {}

		try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {
			HttpGet httpget = new HttpGet(jsonUrl);
			if (jsonUrl.contains("raw.githubusercontent.com")) {
				httpget.setHeader("Authorization", "token " + GITHUB_TOKEN);
			}
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {
				if (jsonUrl.toLowerCase().contains("api.hypixel.net")) {
					try {
						remainingLimit = Integer.parseInt(httpresponse.getFirstHeader("RateLimit-Remaining").getValue());
						timeTillReset = Integer.parseInt(httpresponse.getFirstHeader("RateLimit-Reset").getValue());
					} catch (Exception ignored) {}
				}

				return JsonParser.parseReader(new InputStreamReader(httpresponse.getEntity().getContent()));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static String makeHastePost(String body) {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost httpPost = new HttpPost("https://hst.sh/documents");

			StringEntity entity = new StringEntity(body);
			httpPost.setEntity(entity);

			try (CloseableHttpResponse response = client.execute(httpPost)) {
				return (
					"https://hst.sh/" +
					higherDepth(JsonParser.parseReader(new InputStreamReader(response.getEntity().getContent())), "key").getAsString()
				);
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static EmbedBuilder errorMessage(String name) {
		return defaultEmbed("Invalid input. Type `" + BOT_PREFIX + "help " + name + "` for help");
	}

	public static String uuidToUsername(String uuid) {
		try {
			JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + uuid);
			return higherDepth(usernameJson, "username").getAsString();
		} catch (Exception ignored) {}
		return null;
	}

	public static String convertToInternalName(String itemName) {
		if (internalJsonMappings == null) {
			try {
				internalJsonMappings =
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/InternalNameMappings.json"));
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
			internalName = higherDepth(internalJsonMappings, internalName).getAsString();
		} catch (Exception ignored) {}

		return internalName;
	}

	public static UsernameUuidStruct usernameToUuid(String username) {
		try {
			JsonElement usernameJson = getJson("https://api.ashcon.app/mojang/v2/user/" + username);
			return new UsernameUuidStruct(
				higherDepth(usernameJson, "username").getAsString(),
				higherDepth(usernameJson, "uuid").getAsString().replace("-", "")
			);
		} catch (Exception ignored) {}
		return null;
	}

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

	public static String fixUsername(String username) {
		return username.replace("_", "\\_");
	}

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

	public static ArrayList<String> getJsonKeys(JsonElement jsonElement) {
		try {
			return new ArrayList<>(jsonElement.getAsJsonObject().keySet());
		} catch (Exception e) {
			return null;
		}
	}

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

		botLogChannel.sendMessage(eb.build()).queue();
	}

	public static void logCommand(Guild guild, String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		EmbedBuilder eb = defaultEmbed(null);
		eb.setAuthor(guild.getName() + " (" + guild.getId() + ")", null, guild.getIconUrl());
		eb.setDescription(commandInput);
		botLogChannel.sendMessage(eb.build()).queue();
	}

	public static void logCommand(String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		EmbedBuilder eb = defaultEmbed(null);
		eb.setDescription(commandInput);
		botLogChannel.sendMessage(eb.build()).queue();
	}

	public static DiscordInfoStruct getPlayerDiscordInfo(String username) {
		try {
			JsonElement playerJson = getJson(
				"https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameToUuid(username).playerUuid
			);

			String discordTag = higherDepth(playerJson, "player.socialMedia.links.DISCORD").getAsString();
			String minecraftUsername = higherDepth(playerJson, "player.displayname").getAsString();
			String minecraftUuid = higherDepth(playerJson, "player.uuid").getAsString();

			return new DiscordInfoStruct(discordTag, minecraftUsername, minecraftUuid);
		} catch (Exception e) {
			return null;
		}
	}

	public static String parseMcCodes(String unformattedString) {
		return unformattedString.replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|§7|§8|§l|§o|§b|§2|§e|§r|§3|§1|§ka", "");
	}

	public static CustomPaginator.Builder defaultPaginator(EventWaiter waiter, User eventAuthor) {
		CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder()
			.setColumns(1)
			.setItemsPerPage(1)
			.showPageNumbers(true)
			.setFinalAction(
				m -> {
					try {
						m.clearReactions().queue();
					} catch (PermissionException ex) {
						m.delete().queue();
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
}
