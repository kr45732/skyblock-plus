package com.SkyblockBot.Miscellaneous;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.dv8tion.jda.api.EmbedBuilder;

public class BotUtils {
    public static String key = "";
    public static String botToken = "";
    public static Color botColor = new Color(9, 92, 13);

    public static void setBotSettings() {
        String botTokenL = "";
        if (System.getenv("BOT_TOKEN") == null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader("DevSettings.txt"));
                botTokenL = br.readLine().split("=")[1];
                br.close();
            } catch (Exception e) {
            }
        } else {
            botTokenL = System.getenv("BOT_TOKEN");
        }
        System.out.println(botTokenL);
        botToken = botTokenL;

        String apiKey = "";
        if (System.getenv("API_KEY") == null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader("DevSettings.txt"));
                br.readLine();
                apiKey = br.readLine().split("=")[1];
                br.close();
            } catch (Exception e) {
            }
        } else {
            apiKey = System.getenv("API_KEY");
        }
        System.out.println(apiKey);
        key = apiKey;
    }

    public static JsonElement higherDepth(JsonElement element, String value) {
        return element.getAsJsonObject().get(value);
    }

    public static JsonElement getJson(String jsonUrl) {
        try {
            URLConnection request = new URL(jsonUrl).openConnection();
            request.connect();
            return new JsonParser().parse(new InputStreamReader(request.getInputStream()));
        } catch (IOException e) {
            System.out.println("IOException - getJson - " + jsonUrl);
            e.printStackTrace();
        }
        return null;
    }

    public static String uuidToUsername(String uuid) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/user/profiles/" + uuid + "/names");
            JsonArray usernameArr = usernameJson.getAsJsonArray();
            if (usernameArr.size() > 0) {
                JsonElement currentName = usernameArr.get(usernameArr.size() - 1);
                return higherDepth(currentName, "name").getAsString();
            }
        } catch (Exception e) {
            System.out.println("Null - uuid - " + uuid);
        }
        return null;

    }

    public static JsonElement getJsonFromPath(String path) {
        try {
            return new JsonParser().parse(new FileReader(path));
        } catch (Exception e) {
            return null;
        }
    }

    public static String usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            return higherDepth(usernameJson, "id").getAsString();
        } catch (Exception e) {
            System.out.println("Null - uuid - " + username);
        }
        return null;

    }

    public static EmbedBuilder defaultEmbed(String title, String titleUrl) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(botColor);
        eb.setFooter("Created by CrypticPlasma#0820", null);
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

    public static String formatNumber(String number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }
}
