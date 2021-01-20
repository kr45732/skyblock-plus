package com.SkyblockBot.Dungeons;

import static com.SkyblockBot.Miscellaneous.BotUtils.capitalizeString;
import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.roundProgress;
import static com.SkyblockBot.Miscellaneous.BotUtils.roundSkillAverage;
import static com.SkyblockBot.Miscellaneous.BotUtils.simplifyNumber;
import static com.SkyblockBot.Skills.SkillsCommands.skillInfoFromExp;

import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class CatacombsCommand extends Command {
    public CatacombsCommand() {
        this.name = "catacombs";
        this.guildOnly = false;
        this.cooldown = 5;
    }

    @Override
    protected void execute(CommandEvent event) {
        final EmbedBuilder[] eb = { defaultEmbed("Loading catacombs data...", null) };

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 1 || args.length > 4) {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            if (args.length == 4) { // Profile specified
                eb[0] = getPlayerCatacombs(args[2], args[3]);
            } else
                eb[0] = getPlayerCatacombs(args[2], null);
        } else {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());

    }

    public EmbedBuilder getPlayerCatacombs(String username, String profile) {
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

        EmbedBuilder eb = defaultEmbed("Dungeons", null);
        try {
            double skillExp = higherDepth(higherDepth(higherDepth(
                    higherDepth(higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                            "dungeons"),
                    "dungeon_types"), "catacombs"), "experience").getAsLong();
            SkillsStruct skillInfo = skillInfoFromExp(skillExp, "catacombs");

            eb.addField(capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel + ")",
                    simplifyNumber(skillInfo.expCurrent) + " / " + simplifyNumber(skillInfo.expForNext) + "\nTotal XP: "
                            + simplifyNumber(skillInfo.totalSkillExp) + "\nProgress: "
                            + roundProgress(skillInfo.progressToNext),
                    false);

            eb.setDescription("True catacombs level: " + skillInfo.skillLevel + "\nProgress catacombs level: "
                    + roundSkillAverage(skillInfo.skillLevel + skillInfo.progressToNext));
            return eb;
        } catch (NullPointerException e) {
            return defaultEmbed("Error fetching player dungeon data", null);
        }

    }
}
