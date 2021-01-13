package com.SkyblockBot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;

public class BotUtils {
    public static String key = "75638239-56cc-4b96-b42a-8fe28c40f3a9";
    public static Color botColor = new Color(9, 92, 13);

    public static JsonElement higherDepth(JsonElement element, String value) {
        return element.getAsJsonObject().get(value);
    }

    public static JsonElement getJson(String jsonUrl) {
        try {
            URL url = new URL(jsonUrl);
            URLConnection request = url.openConnection();
            request.connect();
            JsonParser jp = new JsonParser();
            return jp.parse(new InputStreamReader(request.getInputStream()));
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

    public static String formatNumber(String number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }
}
