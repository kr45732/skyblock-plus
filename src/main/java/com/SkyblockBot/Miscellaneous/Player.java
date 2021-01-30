package com.SkyblockBot.Miscellaneous;

import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.util.List;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class Player {
    private boolean validPlayer = false;
    private JsonElement profileJson;
    private JsonElement levelTables;
    private JsonElement outerProfileJson;
    private String playerUuid;
    private String playerUsername;
    private String profileName;
    private String playerGuildRank;

    public Player(String username) {
        if (!usernameToUuid(username)) {
            return;
        }

        JsonArray profileArray = higherDepth(
                getJson("https://api.hypixel.net/skyblock/profiles?key=" + key + "&uuid=" + playerUuid), "profiles")
                .getAsJsonArray();

        if (!getLatestProfile(profileArray)) {
            return;
        }

        this.validPlayer = true;
    }

    public Player(String playerUuid, JsonElement levelTables, String playerGuildRank) {
        this.playerUuid = playerUuid;
        this.playerUsername = uuidToUsername(playerUuid);
        this.levelTables = levelTables;
        this.playerGuildRank = playerGuildRank;

        JsonArray profileArray = higherDepth(
                getJson("https://api.hypixel.net/skyblock/profiles?key=" + key + "&uuid=" + playerUuid), "profiles")
                .getAsJsonArray();

        if (!getLatestProfile(profileArray)) {
            return;
        }

        this.validPlayer = true;
    }

    public Player(String username, String profileName) {
        if (!usernameToUuid(username)) {
            return;
        }

        JsonArray profileArray = higherDepth(
                getJson("https://api.hypixel.net/skyblock/profiles?key=" + key + "&uuid=" + playerUuid), "profiles")
                .getAsJsonArray();

        if (!profileIdFromName(profileName, profileArray)) {
            return;
        }

        this.validPlayer = true;
    }

    public boolean usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            this.playerUsername = higherDepth(usernameJson, "name").getAsString();
            this.playerUuid = higherDepth(usernameJson, "id").getAsString();
            return true;
        } catch (Exception e) {
            System.out.println("Null - uuid - " + username);
        }
        return false;

    }

    public boolean isValidPlayer() {
        return validPlayer;
    }

    public int getPlayerSlayer() {
        return getPlayerWolfXp() + getPlayerZombieXp() + getPlayerSpiderXp();
    }

    public int getPlayerSlayer(String slayerName) {
        if (slayerName.equals("sven")) {
            return getPlayerWolfXp();
        } else if (slayerName.equals("rev")) {
            return getPlayerZombieXp();
        } else if (slayerName.equals("tara")) {
            return getPlayerSpiderXp();
        }
        return -1;
    }

    public int getPlayerWolfXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : 0;
    }

    public int getPlayerZombieXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : 0;
    }

    public int getPlayerSpiderXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : 0;
    }

    public double getPlayerCatacombsLevel() {
        SkillsStruct catacombsInfo = getPlayerCatacombs();
        if (catacombsInfo != null) {
            return catacombsInfo.skillLevel + catacombsInfo.progressToNext;
        }
        return 0;
    }

    public SkillsStruct getPlayerCatacombs() {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        try {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(profileJson, "dungeons"), "dungeon_types"), "catacombs"),
                    "experience").getAsLong();
            SkillsStruct skillInfo = skillInfoFromExp(skillExp, "catacombs");
            return skillInfo;
        } catch (Exception e) {
            return null;
        }

    }

    public SkillsStruct getPlayerSkill(String skillName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        try {
            double skillExp = higherDepth(profileJson, "experience_skill_" + skillName).getAsLong();
            return skillInfoFromExp(skillExp, skillName);
        } catch (Exception ignored) {
        }
        return null;
    }

    public double getPlayerSkillAverage() {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

        List<String> skills = getJsonKeys(skillsCap);
        skills.remove("catacombs");
        skills.remove("runecrafting");
        skills.remove("carpentry");

        double progressSA = 0;
        for (String skill : skills) {
            try {
                double skillExp = higherDepth(profileJson, "experience_skill_" + skill).getAsLong();
                SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
                progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
            } catch (Exception ignored) {
            }
        }
        progressSA /= skills.size();
        return progressSA;
    }

    public SkillsStruct skillInfoFromExp(double skillExp, String skill) {
        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");
        JsonArray skillsTable;
        if (skill.equals("catacombs")) {
            skillsTable = higherDepth(levelTables, "catacombs").getAsJsonArray();
        } else if (skill.equals("runecrafting")) {
            skillsTable = higherDepth(levelTables, "runecrafting_xp").getAsJsonArray();
        } else {
            skillsTable = higherDepth(levelTables, "leveling_xp").getAsJsonArray();
        }
        int maxLevel = higherDepth(skillsCap, skill).getAsInt();

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

    public double getPlayerBankBalance() {
        try {
            return higherDepth(higherDepth(outerProfileJson, "banking"), "balance")
                    .getAsDouble();
        } catch (Exception e) {
            return -1;
        }
    }

    public int getPlayerFairySouls() {
        try {
            return higherDepth(profileJson,
                    "fairy_souls_collected").getAsInt();
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean getLatestProfile(JsonArray profilesArray) {
        try {
            String lastProfileSave = "";
            for (int i = 0; i < profilesArray.size(); i++) {
                String lastSaveLoop = higherDepth(higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid), "last_save").getAsString();
                if (i == 0) {
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    this.outerProfileJson = profilesArray.get(i);
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                } else if (Instant.ofEpochMilli(Long.parseLong(lastSaveLoop))
                        .isAfter(Instant.ofEpochMilli(Long.parseLong(lastProfileSave)))) {
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    this.outerProfileJson = profilesArray.get(i);
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                }
            }
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
        try {
            for (int i = 0; i < profilesArray.size(); i++) {
                String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                if (currentProfileName.equalsIgnoreCase(profileName)) {
                    this.profileName = currentProfileName;
                    this.outerProfileJson = profilesArray.get(i);
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    return true;
                }
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    public String getPlayerUsername() {
        return this.playerUsername;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getPlayerUuid() {
        return this.playerUuid;
    }

    public String getPlayerGuildRank() {
        return playerGuildRank;
    }
}
