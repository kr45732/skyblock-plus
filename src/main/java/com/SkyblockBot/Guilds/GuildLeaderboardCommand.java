package com.SkyblockBot.Guilds;

import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class GuildLeaderboardCommand extends Command {
    Message ebMessage;

    public GuildLeaderboardCommand() {
        this.name = "guildlb";
        this.guildOnly = false;
//        this.cooldown = 30;
//        this.aliases = new String[]{"guild lb"};
    }

    public SkillsStruct skillInfoFromExp(double skillExp, String skill, JsonElement levelTables) {
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

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading guild data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 2) { // No args or too many args are given
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        getLeaderboard(args[1]);
    }

    public void getLeaderboard(String username) {
        String playerUuid = usernameToUuid(username);
        JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + key + "&player=" + playerUuid);
        String guildId = higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString();
        List<String> staffRankNames = new ArrayList<>();
        try {
            JsonElement settings = new JsonParser().parse(new FileReader("src/main/java/com/SkyblockBot/json/GuildSettings.json"));
            JsonArray staffRanksArr = higherDepth(higherDepth(higherDepth(settings, guildId), "guild_leaderboard"), "staff_ranks").getAsJsonArray();
            for (JsonElement i : staffRanksArr) {
                staffRankNames.add(i.getAsString());
            }
        } catch (Exception e) {
            staffRankNames = null;
        }
        JsonElement levelTables = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        HashMap<String, String> guildRank; // uuid, rank
        HashMap<Integer, GuildMemberInfoStruct> guildSlayer = new HashMap<>(); // customStruct is (uuid, username)
        HashMap<Double, GuildMemberInfoStruct> guildSkills = new HashMap<>(); // customStruct is (uuid, username)
        HashMap<Integer, GuildMemberInfoStruct> guildCatacombs = new HashMap<>(); // customStruct is (uuid, username)

        int counter = 0;
        long starTime = System.currentTimeMillis();
        for (JsonElement guildMember : guildMembers) {
            String memberRank = higherDepth(guildMember, "rank").getAsString();
            if (!staffRankNames.contains(memberRank)) {
                String memberUuid = higherDepth(guildMember, "uuid").getAsString();
                String memberUsername = uuidToUsername(memberUuid);
                try {
                    JsonArray skyblockProfilesArr = higherDepth(getJson(
                            "https://api.hypixel.net/skyblock/profiles?key=" + key + "&uuid=" + memberUuid), "profiles").getAsJsonArray();
                    if (skyblockProfilesArr != null) {
                        JsonElement latestProfile = getLatestProfile(memberUuid, skyblockProfilesArr);
                        int playerSlayer = getPlayerSlayer(latestProfile);
                        double playerSkillAverage = getPlayerSkills(latestProfile, levelTables);
                        int playerCatacombs = getPlayerCatacombs(latestProfile, levelTables);
                        guildSlayer.put(playerSlayer, new GuildMemberInfoStruct(memberUuid, memberUsername));
                        guildSkills.put(playerSkillAverage, new GuildMemberInfoStruct(memberUuid, memberUsername));
                        guildCatacombs.put(playerCatacombs, new GuildMemberInfoStruct(memberUuid, memberUsername));
//                        System.out.println(memberUsername + " " + playerSlayer + " " + playerSkillAverage + " " + playerCatacombs);
//                    break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        System.out.println((System.currentTimeMillis() - starTime) / 1000);
    }

    public int getPlayerSlayer(JsonElement profileJson) {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        int slayerWolf = higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : 0;
        int slayerZombie = higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : 0;
        int slayerSpider = higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : 0;

        return (slayerWolf + slayerZombie + slayerSpider);
    }

    public int getPlayerCatacombs(JsonElement profileJson, JsonElement levelTables) {

        double skillExp = higherDepth(higherDepth(higherDepth(higherDepth(profileJson,
                "dungeons"), "dungeon_types"), "catacombs"), "experience").getAsLong();
        SkillsStruct skillInfo = skillInfoFromExp(skillExp, "catacombs", levelTables);
        return skillInfo.skillLevel;
    }

    public double getPlayerSkills(JsonElement profileJson, JsonElement levelTables) {
        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

        List<String> skills = skillsCap.getAsJsonObject().entrySet().stream().map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
        skills.remove("catacombs");
        skills.remove("runecrafting");
        skills.remove("carpentry");

        double progressSA = 0;

        for (String skill : skills) {
            try {
                double skillExp = higherDepth(profileJson, "experience_skill_" + skill).getAsLong();
                SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill, levelTables);
                progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
            } catch (Exception ignored) {
            }
        }
        progressSA /= skills.size();
        return progressSA;
    }

    public JsonElement getLatestProfile(String uuidPlayer, JsonArray profilesJson) {
        try {
            String lastProfileSave = "";
            JsonElement currentProfile = null;
            for (int i = 0; i < profilesJson.size(); i++) {
                String lastSave = higherDepth(higherDepth(higherDepth(profilesJson.get(i), "members"), uuidPlayer), "last_save").getAsString();
                if (i == 0) {
                    currentProfile = higherDepth(higherDepth(profilesJson.get(i), "members"), uuidPlayer);
                    lastProfileSave = lastSave;
                } else if (Instant.ofEpochMilli(Long.parseLong(lastSave))
                        .isAfter(Instant.ofEpochMilli(Long.parseLong(lastProfileSave)))) {
                    currentProfile = higherDepth(higherDepth(profilesJson.get(i), "members"), uuidPlayer);
                    lastProfileSave = lastSave;
                }
            }
            return currentProfile;
        } catch (Exception e) {
            return null;
        }
    }

}
