package com.SkyblockBot.Skills;

import static com.SkyblockBot.Miscellaneous.BotUtils.capitalizeString;
import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.errorMessage;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.globalCooldown;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.roundProgress;
import static com.SkyblockBot.Miscellaneous.BotUtils.roundSkillAverage;
import static com.SkyblockBot.Miscellaneous.BotUtils.simplifyNumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SkillsCommands extends Command {
    Message ebMessage;

    public SkillsCommands() {
        this.name = "skills";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading skill data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 1 || args.length > 4) {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            if (args.length == 4) { // Profile specified
                eb = getPlayerSkill(args[2], args[3]);
            } else
                eb = getPlayerSkill(args[2], null);
        } else {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    public EmbedBuilder getPlayerSkill(String username, String profile) {
        JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + key + "&name=" + username);

        if (playerJson == null) {
            return defaultEmbed("Error fetching player data", null);
        }

        if (higherDepth(playerJson, "player").isJsonNull()) {
            return defaultEmbed("Player not found", null);
        }

        String userProfile = higherDepth(
                higherDepth(higherDepth(higherDepth(playerJson, "player"), "stats"), "SkyBlock"), "profiles")
                        .toString();
        userProfile = userProfile.substring(1, userProfile.length() - 2);
        String[] outputStr = userProfile.split("},");
        String[] profileId = new String[outputStr.length];

        for (int i = 0; i < outputStr.length; i++) {
            outputStr[i] = outputStr[i].substring(outputStr[i].indexOf(":{") + 2);
            profileId[i] = outputStr[i].substring(outputStr[i].indexOf("id") + 5, outputStr[i].indexOf("cute") - 3);
        }

        int profileIndex = 0;
        for (int i = 0; i < outputStr.length; i++) {
            String currentProfile = outputStr[i].substring(outputStr[i].indexOf("name") + 7, outputStr[i].length() - 1);
            if (currentProfile.equalsIgnoreCase(profile)) {
                profileIndex = i;
                break;
            }
        }

        JsonElement uuidJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);

        if (uuidJson == null) {
            return defaultEmbed("Error fetching player data", null);
        }

        String uuidPlayer = higherDepth(uuidJson, "id").getAsString();
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile="
                + profileId[profileIndex];
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return defaultEmbed("Error fetching player skyblock data", null);
        }

        JsonElement levelTabels = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");

        JsonElement skillsCap = higherDepth(levelTabels, "leveling_caps");

        List<String> skills = skillsCap.getAsJsonObject().entrySet().stream().map(i -> i.getKey())
                .collect(Collectors.toCollection(ArrayList::new));
        skills.remove("catacombs");

        double trueSA = 0;
        double progressSA = 0;
        EmbedBuilder eb = defaultEmbed("Skills", null);
        Map<String, String> skillsEmojiMap = new HashMap<String, String>();
        skillsEmojiMap.put("taming", "<:taming:800462115365716018>");
        skillsEmojiMap.put("farming", "<:farming:800462115055992832>");
        skillsEmojiMap.put("foraging", "<:foraging:800462114829500477>");
        skillsEmojiMap.put("combat", "<:combat:800462115009855548>");
        skillsEmojiMap.put("alchemy", "<:alchemy:800462114589376564>");
        skillsEmojiMap.put("fishing", "<:fishing:800462114853617705>");
        skillsEmojiMap.put("enchanting", "<:enchanting:800462115193225256>");
        skillsEmojiMap.put("mining", "<:mining:800462115009069076>");
        skillsEmojiMap.put("carpentry", "<:carpentery:800462115156131880>");
        skillsEmojiMap.put("runecrafting", "<:runecrafting:800462115172909086>");

        for (String skill : skills) {
            try {
                double skillExp = higherDepth(
                        higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                        "experience_skill_" + skill).getAsLong();
                SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
                eb.addField(
                        skillsEmojiMap.get(skill) + capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel
                                + ")",
                        simplifyNumber(skillInfo.expCurrent) + " / " + simplifyNumber(skillInfo.expForNext)
                                + "\nTotal XP: " + simplifyNumber(skillInfo.totalSkillExp) + "\nProgress: "
                                + (skillInfo.skillLevel == skillInfo.maxSkillLevel ? "max"
                                        : roundProgress(skillInfo.progressToNext)),
                        true);
                if (!skill.equals("runecrafting") && !skill.equals("carpentry")) {
                    trueSA += skillInfo.skillLevel;
                    progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
                }
            } catch (NullPointerException ex) {
                eb.addField(skillsEmojiMap.get(skill) + capitalizeString(skill) + " (?) ", "Unable to retrieve", true);
            }
        }
        trueSA /= (skills.size() - 2);
        progressSA /= (skills.size() - 2);
        eb.setDescription("True skill average: " + roundSkillAverage(trueSA) + "\nProgress skill average: "
                + roundSkillAverage(progressSA));
        return eb;

    }

    public static SkillsStruct skillInfoFromExp(double skillExp, String skill) {
        JsonElement levelTabels = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        JsonElement skillsCap = higherDepth(levelTabels, "leveling_caps");
        JsonArray skillsTable;
        if (skill.equals("catacombs")) {
            skillsTable = higherDepth(levelTabels, "catacombs").getAsJsonArray();
        } else if (skill.equals("runecrafting")) {
            skillsTable = higherDepth(levelTabels, "runecrafting_xp").getAsJsonArray();
        } else {
            skillsTable = higherDepth(levelTabels, "leveling_xp").getAsJsonArray();
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

}
