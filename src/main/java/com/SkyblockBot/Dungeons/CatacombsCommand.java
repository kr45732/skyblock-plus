package com.SkyblockBot.Dungeons;

import static com.SkyblockBot.Miscellaneous.BotUtils.capitalizeString;
import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.errorMessage;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.getLatestProfile;
import static com.SkyblockBot.Miscellaneous.BotUtils.globalCooldown;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.profileIdFromName;
import static com.SkyblockBot.Miscellaneous.BotUtils.roundProgress;
import static com.SkyblockBot.Miscellaneous.BotUtils.roundSkillAverage;
import static com.SkyblockBot.Miscellaneous.BotUtils.simplifyNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.skyblockStatsLink;
import static com.SkyblockBot.Skills.SkillsCommands.skillInfoFromExp;

import com.SkyblockBot.Miscellaneous.LatestProfileStruct;
import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class CatacombsCommand extends Command {
    Message ebMessage;

    public CatacombsCommand() {
        this.name = "catacombs";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "cata" };
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading catacombs data...", null);

        Message message = event.getMessage();
        String content = message.getContentRaw();
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

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
                eb = getPlayerCatacombs(args[2], args[3]);
            } else
                eb = getPlayerCatacombs(args[2], null);
        } else {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    public EmbedBuilder getPlayerCatacombs(String username, String profile) {
        String profileID = "";

        if (profile == null) {
            LatestProfileStruct latestProfile = getLatestProfile(username);
            if (latestProfile == null) {
                return defaultEmbed("Error fetching latest Skyblock profile", null);
            }
            profileID = latestProfile.profileID;
            profile = latestProfile.profileName;
        } else {
            profileID = profileIdFromName(username, profile);
            if (profileID == null) {
                return defaultEmbed("Error fetching player catacombs data", null);
            }
        }

        JsonElement uuidJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
        if (uuidJson == null) {
            return defaultEmbed("Error fetching player data", null);
        }
        String uuidPlayer = higherDepth(uuidJson, "id").getAsString();
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileID;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return defaultEmbed("Error fetching player skyblock data", null);
        }

        EmbedBuilder eb = defaultEmbed("Dungeons for " + username, skyblockStatsLink(username, profile));
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
            return defaultEmbed("Error fetching player catacombs data", null);
        }
    }
}
