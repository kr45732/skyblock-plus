package com.SkyblockBot.Slayer;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.fixUsername;
import static com.SkyblockBot.Miscellaneous.BotUtils.formatNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.simplifyNumber;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SlayerCommands extends Command {
    public SlayerCommands() {
        this.name = "slayer";
        this.guildOnly = false;
        this.cooldown = 2;
    }

    @Override
    protected void execute(CommandEvent event) {
        final EmbedBuilder[] eb = { defaultEmbed("Loading slayer data...", null) };

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

        if ((args[1].equals("leaderboard") || args[1].equals("lb")) && args.length > 2) {
            eb[0].setTitle("Guild leaderboard");
            eb[0].addField("WIP", "WORK IN PROGRESS", false);
        } else if (args[1].equals("player") && args.length > 2) {
            if (args.length == 4) { // Profile specified
                eb[0] = getPlayerSlayer(args[2], args[3]);
            } else
                eb[0] = getPlayerSlayer(args[2], null);

        } else {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
    }

    public EmbedBuilder getPlayerSlayer(String username, String profile) {
        profile = profile != null ? profile : "";

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

        // String[] profileName = new String[outputStr.length];
        int profileIndex = 0;
        for (int i = 0; i < outputStr.length; i++) {
            String currentProfile = outputStr[i].substring(outputStr[i].indexOf("name") + 7, outputStr[i].length() - 1);
            // profileName[i] = currentProfile;
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

        JsonElement profileSlayer = higherDepth(
                higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer), "slayer_bosses");

        int slayerWolf = higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : -1;
        int slayerZombie = higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : -1;
        int slayerSpider = higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : -1;
        int totalSlayer = ((slayerWolf != -1 ? slayerWolf : 0) + (slayerZombie != -1 ? slayerZombie : 0)
                + (slayerSpider != -1 ? slayerSpider : 0));

        EmbedBuilder eb = defaultEmbed("Slayer for " + fixUsername(username),
                "https://sky.shiiyu.moe/stats/" + username + "/" + profile);
        eb.setDescription("**Total slayer:** "
                + ((slayerWolf != -1 && slayerZombie != -1 && slayerSpider != -1) ? formatNumber(totalSlayer) + " XP"
                        : "None"));
        eb.addField("<:sven_packmaster:800002277648891914>  Wolf",
                (slayerWolf != -1 ? simplifyNumber(slayerWolf) + " XP" : "None"), true);
        eb.addField("<:revenant_horror:800002290987302943>  Zombie",
                (slayerZombie != -1 ? simplifyNumber(slayerZombie) + " XP" : "None"), true);
        eb.addField("<:tarantula_broodfather:800002277262884874>  Spider",
                (slayerSpider != -1 ? simplifyNumber(slayerSpider) + " XP" : "None"), true);
        eb.setThumbnail("https://cravatar.eu/helmhead/" + uuidPlayer);
        return eb;
    }
}
