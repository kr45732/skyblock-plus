package com.SkyblockBot.Dungeons;

import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.SkyblockBot.Miscellaneous.BotUtils.*;
import static com.SkyblockBot.Skills.SkillsCommands.skillInfoFromExp;

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
            eb[0] = getPlayerCatacombs(args[2]);
        } else {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());

    }

    public EmbedBuilder getPlayerCatacombs(String username) {
        String profile = "";
        key = "75638239-56cc-4b96-b42a-8fe28c40f3a9";
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
        double skillExp = higherDepth(higherDepth(higherDepth(
                higherDepth(higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                        "dungeons"),
                "dungeon_types"), "catacombs"), "experience").getAsLong();
        SkillsStruct skillInfo = skillInfoFromExp(skillExp, "catacombs");

        eb.addField(skillInfo.skillName + " (" + skillInfo.skillLevel + ")",
                skillInfo.expCurrent + "/" + skillInfo.expForNext + "\nTotal XP:" + skillInfo.totalSkillExp
                        + "\nProgress: " + skillInfo.progressToNext,
                false);

        eb.setDescription("Catacombs level: " + skillInfo.skillLevel);
        return eb;

    }
}
