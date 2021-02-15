package com.skyblockplus.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.guilds.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BotUtils {
    public static final Color botColor = new Color(9, 92, 13);
    public static final int globalCooldown = 2;
    public static String HYPIXEL_API_KEY = "";
    public static String BOT_TOKEN = "";
    public static String BOT_PREFIX = "";
    public static String CLIENT_ID = "";
    public static String CLIENT_SECRET = "";
    public static String DATABASE_URL = "";
    public static String DATABASE_USERNAME = "";
    public static String DATABASE_PASSWORD = "";
    private static int remainingLimit = 120;
    private static int timeTillReset = 60;

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
            DATABASE_URL = "jdbc:postgresql://" + database_url_unformatted[2].split("@")[1] + "?sslmode=require&user="
                    + DATABASE_USERNAME + "&password=" + DATABASE_PASSWORD;
        } catch (IOException e) {
            BOT_PREFIX = System.getenv("BOT_PREFIX");
            HYPIXEL_API_KEY = System.getenv("HYPIXEL_API_KEY");
            BOT_TOKEN = System.getenv("BOT_TOKEN");
            CLIENT_ID = System.getenv("CLIENT_ID");
            CLIENT_SECRET = System.getenv("CLIENT_SECRET");
            String[] database_url_unformatted = System.getenv("DATABASE_URL").split(":", 3);
            DATABASE_USERNAME = database_url_unformatted[1].replace("/", "");
            DATABASE_PASSWORD = database_url_unformatted[2].split("@")[0];
            DATABASE_URL = "jdbc:postgresql://" + database_url_unformatted[2].split("@")[1] + "?sslmode=require&user="
                    + DATABASE_USERNAME + "&password=" + DATABASE_PASSWORD;
        }
    }

    public static JsonElement higherDepth(JsonElement element, String value) {
        try {
            return element.getAsJsonObject().get(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonElement getJson(String jsonUrl) {
        try {
            if (remainingLimit < 5) {
                TimeUnit.SECONDS.sleep(timeTillReset);
                System.out.println("Sleeping for " + timeTillReset + " seconds");
            }
            URLConnection request = new URL(jsonUrl).openConnection();
            request.connect();
            if (jsonUrl.toLowerCase().contains("api.hypixel.net")) {
                remainingLimit = Integer.parseInt(request.getHeaderField("RateLimit-Remaining"));
                timeTillReset = Integer.parseInt(request.getHeaderField("RateLimit-Reset"));
            }
            return JsonParser.parseReader(new InputStreamReader(request.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String errorMessage(String name) {
        return "Invalid input. Type `" + BOT_PREFIX + "help " + name + "` for help";
    }

    public static String uuidToUsername(String uuid) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/user/profiles/" + uuid + "/names");
            JsonArray usernameArr = usernameJson.getAsJsonArray();
            if (usernameArr.size() > 0) {
                JsonElement currentName = usernameArr.get(usernameArr.size() - 1);
                return higherDepth(currentName, "name").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;

    }

    public static String usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            return higherDepth(usernameJson, "id").getAsString();
        } catch (Exception ignored) {
        }
        return null;

    }

    public static UsernameUuidStruct usernameToUuidUsername(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            return new UsernameUuidStruct(higherDepth(usernameJson, "name").getAsString(),
                    higherDepth(usernameJson, "id").getAsString());
        } catch (Exception ignored) {
        }
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

    public static String fixUsername(String username) {
        return username.replace("_", "\\_");
    }

    public static String formatNumber(int number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }

    public static String formatNumber(long number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }

    public static String roundSkillAverage(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(number);
    }

    public static String roundProgress(double number) {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(number * 100) + "%";
    }

    public static String simplifyNumber(double number) {
        String formattedNumber = "" + number;
        DecimalFormat df = new DecimalFormat("#.#");
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
        }
        return formattedNumber;
    }

    public static String capitalizeString(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static String skyblockStatsLink(String username, String profileName) {
        return ("https://sky.shiiyu.moe/stats/" + username + "/" + profileName);
    }

    public static ArrayList<String> getJsonKeys(JsonElement jsonElement) {
        return jsonElement.getAsJsonObject().entrySet().stream().map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static String[] getPlayerDiscordInfo(String username) {
        JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&name=" + username);

        if (playerJson == null) {
            return null;
        }

        if (higherDepth(playerJson, "player").isJsonNull()) {
            return null;
        }
        try {
            String discordID = higherDepth(
                    higherDepth(higherDepth(higherDepth(playerJson, "player"), "socialMedia"), "links"), "DISCORD")
                    .getAsString();
            return new String[]{discordID,
                    higherDepth(higherDepth(playerJson, "player"), "displayname").getAsString()};
        } catch (Exception e) {
            return null;
        }
    }
}
