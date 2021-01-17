package com.SkyblockBot.Skills;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class SkillsCommands extends Command {
    public SkillsCommands() {
        this.name = "skills";
        this.guildOnly = false;
        this.cooldown = 5;
    }

    @Override
    protected void execute(CommandEvent event) {
        final EmbedBuilder[] eb = { defaultEmbed("Loading skill data...", null) };

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 3) {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            eb[0] = getPlayerSkill(args[2]);
        } else {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());

    }

    public EmbedBuilder getPlayerSkill(String username) {
        String profile = "";
        // key = "75638239-56cc-4b96-b42a-8fe28c40f3a9";
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

        double skillAverage = 0;
        EmbedBuilder eb = defaultEmbed("Skills", null);
        for (String skill : skills) {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                    "experience_skill_" + skill).getAsLong();
            SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
            eb.addField(skillInfo.skillName + " (" + skillInfo.skillLevel + ")",
                    skillInfo.expCurrent + "/" + skillInfo.expForNext + "\nTotal XP:" + skillInfo.totalSkillExp
                            + "\nProgress: " + skillInfo.progressToNext,
                    true);
            if (!skill.equals("runecrafting") && !skill.equals("carpentry")) {
                skillAverage += skillInfo.skillLevel;
            }

        }
        skillAverage /= (skills.size() - 2);
        eb.setDescription("True skill Average: " + skillAverage);
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
