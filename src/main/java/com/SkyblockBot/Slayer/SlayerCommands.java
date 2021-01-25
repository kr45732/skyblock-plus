package com.SkyblockBot.Slayer;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.errorMessage;
import static com.SkyblockBot.Miscellaneous.BotUtils.formatNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.getLatestProfile;
import static com.SkyblockBot.Miscellaneous.BotUtils.globalCooldown;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.profileIdFromName;
import static com.SkyblockBot.Miscellaneous.BotUtils.simplifyNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.skyblockStatsLink;

import com.SkyblockBot.Miscellaneous.LatestProfileStruct;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SlayerCommands extends Command {
    Message ebMessage;

    public SlayerCommands() {
        this.name = "slayer";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading slayer data...", null);
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

        if ((args[1].equals("leaderboard") || args[1].equals("lb")) && args.length > 2) {
            eb.setTitle("Guild leaderboard");
            eb.addField("WIP", "WORK IN PROGRESS", false);
        } else if (args[1].equals("player") && args.length > 2) {
            if (args.length == 4) { // Profile specified
                eb = getPlayerSlayer(args[2], args[3]);
            } else
                eb = getPlayerSlayer(args[2], null);

        } else {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerSlayer(String username, String profile) {
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

        EmbedBuilder eb = defaultEmbed("Slayer for " + username, skyblockStatsLink(username, profile));
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
