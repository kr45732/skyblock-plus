package com.skyblockplus.utils;

import com.skyblockplus.guilds.UsernameUuidStruct;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BotUtils {
    public static final Color botColor = new Color(9, 92, 13);
    public static final int globalCooldown = 2;
    public static String key = "";
    public static String botToken = "";
    public static String botPrefix = "";
    private static int remainingLimit = 120;
    private static int timeTillReset = 60;

    public static String getBotPrefix() {
        String botPrefix = "";
        if (System.getenv("BOT_PREFIX") == null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader("DevSettings.txt"));
                br.readLine();
                br.readLine();
                botPrefix = br.readLine().split("=")[1];
                br.close();
            } catch (Exception ignored) {
            }
        } else {
            botPrefix = System.getenv("BOT_PREFIX");
        }
        return botPrefix;
    }

    public static void setBotSettings(String prefix) {
        String botTokenL = "";
        if (System.getenv("BOT_TOKEN") == null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader("DevSettings.txt"));
                botTokenL = br.readLine().split("=")[1];
                br.close();
            } catch (Exception ignored) {
            }
        } else {
            botTokenL = System.getenv("BOT_TOKEN");
        }
        botToken = botTokenL;

        String apiKey = "";
        if (System.getenv("API_KEY") == null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader("DevSettings.txt"));
                br.readLine();
                apiKey = br.readLine().split("=")[1];
                br.close();
            } catch (Exception ignored) {
            }
        } else {
            apiKey = System.getenv("API_KEY");
        }
        key = apiKey;

        botPrefix = prefix;
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
        return "Invalid input. Type `" + botPrefix + "help " + name + "` for help";
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
            return new UsernameUuidStruct(higherDepth(usernameJson, "name").getAsString(), higherDepth(usernameJson, "id").getAsString());
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
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(number * 100) + "%";
    }

    public static String simplifyNumber(double number) {
        String formattedNumber = "" + number;
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.HALF_UP);
        if (number >= 1000000000) {
            formattedNumber = df.format(number / 1000000000) + "B";
        } else if (number >= 1000000) {
            formattedNumber = df.format(number / 1000000) + "M";
        } else if (number >= 1000) {
            DecimalFormat df1 = new DecimalFormat("#");
            df1.setRoundingMode(RoundingMode.HALF_UP);
            formattedNumber = df1.format(number / 1000) + "K";
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
        JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + key + "&name=" + username);

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
            return new String[]{discordID, higherDepth(higherDepth(playerJson, "player"), "displayname").getAsString()};
        } catch (Exception e) {
            return null;
        }
    }


}
