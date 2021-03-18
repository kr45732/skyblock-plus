package com.skyblockplus.eventlisteners.apply;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.skills.SkillsStruct;
import com.skyblockplus.utils.Player;
import com.skyblockplus.weight.Weight;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import static com.skyblockplus.utils.Utils.*;

public class PlayerCustom implements Serializable {
    private boolean validPlayer = false;
    private String profileJsonString;
    private String levelTablesString;
    private String outerProfileJsonString;
    private String playerUuid;
    private String playerUsername;
    private String profileName;

    public PlayerCustom(String username) {
        if (usernameToUuid(username)) {
            return;
        }

        try {
            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();

            if (getLatestProfile(profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public PlayerCustom(String username, String profileName) {
        if (usernameToUuid(username)) {
            return;
        }

        try {

            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();
            if (!profileIdFromName(profileName, profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public boolean usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            this.playerUsername = higherDepth(usernameJson, "name").getAsString();
            this.playerUuid = higherDepth(usernameJson, "id").getAsString();
            return false;
        } catch (Exception ignored) {
        }
        return true;

    }

    public boolean isValid() {
        return validPlayer;
    }

    public int getSlayer() {
        return getWolfXp() + getZombieXp() + getSpiderXp();
    }

    public int getWolfXp() {
        JsonElement profileSlayer = higherDepth(JsonParser.parseString(profileJsonString), "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : 0;
    }

    public int getZombieXp() {
        JsonElement profileSlayer = higherDepth(JsonParser.parseString(profileJsonString), "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : 0;
    }

    public int getSpiderXp() {
        JsonElement profileSlayer = higherDepth(JsonParser.parseString(profileJsonString), "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : 0;
    }

    public SkillsStruct getCatacombsSkill() {
        if (this.levelTablesString == null) {
            this.levelTablesString = new Gson().toJson(getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json"));
        }

        try {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(JsonParser.parseString(profileJsonString), "dungeons"), "dungeon_types"), "catacombs"),
                    "experience").getAsDouble();
            return skillInfoFromExp(skillExp, "catacombs");
        } catch (Exception e) {
            return null;
        }

    }

    public double getSkillAverage() {
        if (this.levelTablesString == null) {
            this.levelTablesString = new Gson().toJson(getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json"));
        }

        JsonElement skillsCap = higherDepth(JsonParser.parseString(levelTablesString), "leveling_caps");

        List<String> skills = getJsonKeys(skillsCap);
        skills.remove("catacombs");
        skills.remove("runecrafting");
        skills.remove("carpentry");

        double progressSA = 0;
        for (String skill : skills) {
            try {
                double skillExp = higherDepth(JsonParser.parseString(profileJsonString), "experience_skill_" + skill).getAsDouble();
                SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
                progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
            } catch (Exception ignored) {
            }
        }
        progressSA /= skills.size();
        return progressSA;
    }

    public SkillsStruct skillInfoFromExp(double skillExp, String skill) {
        JsonElement skillsCap = higherDepth(JsonParser.parseString(levelTablesString), "leveling_caps");

        JsonArray skillsTable;
        if (skill.equals("catacombs")) {
            skillsTable = higherDepth(JsonParser.parseString(levelTablesString), "catacombs").getAsJsonArray();
        } else if (skill.equals("runecrafting")) {
            skillsTable = higherDepth(JsonParser.parseString(levelTablesString), "runecrafting_xp").getAsJsonArray();
        } else {
            skillsTable = higherDepth(JsonParser.parseString(levelTablesString), "leveling_xp").getAsJsonArray();
        }
        int maxLevel;
        try {
            maxLevel = higherDepth(skillsCap, skill).getAsInt();
        } catch (Exception e) {
            maxLevel = 50;
        }

        if (skill.equals("farming")) {
            maxLevel += getFarmingCapUpgrade();
        }

        long xpTotal = 0L;
        int level = 1;
        for (int i = 0; i < maxLevel; i++) {
            xpTotal += skillsTable.get(i).getAsLong();

            if (xpTotal > skillExp) {
                xpTotal -= skillsTable.get(i).getAsLong();
                break;
            } else {
                level = (i + 1);
            }
        }

        long xpCurrent = (long) Math.floor(skillExp - xpTotal);
        long xpForNext = 0;
        if (level < maxLevel)
            xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());

        double progress = xpForNext > 0 ? Math.max(0, Math.min(((double) xpCurrent) / xpForNext, 1)) : 0;

        return new SkillsStruct(skill, level, maxLevel, (long) skillExp, xpCurrent, xpForNext, progress);
    }

    public boolean getLatestProfile(JsonArray profilesArray) {
        try {
            String lastProfileSave = "";
            for (int i = 0; i < profilesArray.size(); i++) {
                String lastSaveLoop;
                try {
                    lastSaveLoop = higherDepth(
                            higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid), "last_save")
                            .getAsString();
                } catch (Exception e) {
                    continue;
                }

                if (i == 0) {
                    this.profileJsonString = new Gson().toJson(higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid));
                    this.outerProfileJsonString = new Gson().toJson(profilesArray.get(i));
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                } else if (Instant.ofEpochMilli(Long.parseLong(lastSaveLoop))
                        .isAfter(Instant.ofEpochMilli(Long.parseLong(lastProfileSave)))) {
                    this.profileJsonString = new Gson().toJson(higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid));
                    this.outerProfileJsonString = new Gson().toJson(profilesArray.get(i));
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                }
            }
            return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
        try {
            for (int i = 0; i < profilesArray.size(); i++) {
                String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                if (currentProfileName.equalsIgnoreCase(profileName)) {
                    this.profileName = currentProfileName;
                    this.outerProfileJsonString = new Gson().toJson(profilesArray.get(i));
                    this.profileJsonString = new Gson().toJson(higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid));
                    return true;
                }
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    public String getUsername() {
        return this.playerUsername;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getUuid() {
        return this.playerUuid;
    }

    public int getFarmingCapUpgrade() {
        try {
            return higherDepth(higherDepth(higherDepth(JsonParser.parseString(profileJsonString), "jacob2"), "perks"), "farming_level_cap")
                    .getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public double getWeight() {
        Weight playerWeight = new Weight(new Player(playerUsername, profileName));
        return playerWeight.getTotalWeight();
    }

    public EmbedBuilder defaultPlayerEmbed() {
        return defaultEmbed(getUsername() + (higherDepth(JsonParser.parseString(outerProfileJsonString), "game_mode") != null ? " ♻️" : ""),
                skyblockStatsLink(getUsername(), getProfileName()));
    }

    public String skyblockStatsLink(String username, String profileName) {
        return ("https://sky.shiiyu.moe/stats/" + username + "/" + profileName);
    }
}
