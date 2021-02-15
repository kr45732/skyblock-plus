package com.skyblockplus.skills;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.skyblockplus.utils.BotUtils.*;

public class SkillsCommands extends Command {
    Message ebMessage;
    JsonElement levelTables;

    public SkillsCommands() {
        this.name = "skills";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading skills data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        levelTables = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        if (levelTables == null) {
            eb = defaultEmbed("Error fetching data from github", null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        if (args[1].equals("player")) {
            if (args.length == 4) { // Profile specified
                eb = getPlayerSkill(args[2], args[3]);
            } else
                eb = getPlayerSkill(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    private EmbedBuilder getPlayerSkill(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);

        if (player.isValid()) {
            JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

            List<String> skills = getJsonKeys(skillsCap);
            skills.remove("catacombs");

            double trueSA = 0;
            double progressSA = 0;
            EmbedBuilder eb = defaultEmbed("Skills for " + player.getUsername(),
                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
            Map<String, String> skillsEmojiMap = new HashMap<>();
            skillsEmojiMap.put("taming", "<:taming:800462115365716018>");
            skillsEmojiMap.put("farming", "<:farming:800462115055992832>");
            skillsEmojiMap.put("foraging", "<:foraging:800462114829500477>");
            skillsEmojiMap.put("combat", "<:combat:800462115009855548>");
            skillsEmojiMap.put("alchemy", "<:alchemy:800462114589376564>");
            skillsEmojiMap.put("fishing", "<:fishing:800462114853617705>");
            skillsEmojiMap.put("enchanting", "<:enchanting:800462115193225256>");
            skillsEmojiMap.put("mining", "<:mining:800462115009069076>");
            skillsEmojiMap.put("carpentry", "<:carpentry:800462115156131880>");
            skillsEmojiMap.put("runecrafting", "<:runecrafting:800462115172909086>");

            for (String skill : skills) {
                SkillsStruct skillInfo = player.getSkill(skill);
                if (skillInfo != null) {
                    eb.addField(
                            skillsEmojiMap.get(skill) + " " + capitalizeString(skillInfo.skillName) + " ("
                                    + skillInfo.skillLevel + ")",
                            simplifyNumber(skillInfo.expCurrent) + " / " + simplifyNumber(skillInfo.expForNext)
                                    + "\nTotal XP: " + simplifyNumber(skillInfo.totalSkillExp) + "\nProgress: "
                                    + (skillInfo.skillLevel == skillInfo.maxSkillLevel ? "MAX"
                                            : roundProgress(skillInfo.progressToNext)),
                            true);
                    if (!skill.equals("runecrafting") && !skill.equals("carpentry")) {
                        trueSA += skillInfo.skillLevel;
                        progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
                    }
                } else {
                    eb.addField(skillsEmojiMap.get(skill) + " " + capitalizeString(skill) + " (?) ",
                            "Unable to retrieve", true);
                }
            }
            trueSA /= (skills.size() - 2);
            progressSA /= (skills.size() - 2);
            eb.setDescription("True skill average: " + roundSkillAverage(trueSA) + "\nProgress skill average: "
                    + roundSkillAverage(progressSA));
            return eb;
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
