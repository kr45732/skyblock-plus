package com.skyblockplus.slayer;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class SlayerCommand extends Command {
    public SlayerCommand() {
        this.name = "slayer";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 3) {
                ebMessage.editMessage(getPlayerSlayer(args[1], args[2]).build()).queue();
                return;
            } else if (args.length == 2) {
                ebMessage.editMessage(getPlayerSlayer(args[1], null).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerSlayer(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = player.defaultPlayerEmbed();

            JsonElement slayer = higherDepth(player.getProfileJson(), "slayer_bosses");

            int svenOneKills = higherDepth(slayer, "wolf.boss_kills_tier_0") != null
                    ? higherDepth(slayer, "wolf.boss_kills_tier_0").getAsInt()
                    : 0;
            int svenTwoKills = higherDepth(slayer, "wolf.boss_kills_tier_1") != null
                    ? higherDepth(slayer, "wolf.boss_kills_tier_1").getAsInt()
                    : 0;
            int svenThreeKills = higherDepth(slayer, "wolf.boss_kills_tier_2") != null
                    ? higherDepth(slayer, "wolf.boss_kills_tier_2").getAsInt()
                    : 0;
            int svenFourKills = higherDepth(slayer, "wolf.boss_kills_tier_3") != null
                    ? higherDepth(slayer, "wolf.boss_kills_tier_3").getAsInt()
                    : 0;

            int revOneKills = higherDepth(slayer, "zombie.boss_kills_tier_0") != null
                    ? higherDepth(slayer, "zombie.boss_kills_tier_0").getAsInt()
                    : 0;
            int revTwoKills = higherDepth(slayer, "zombie.boss_kills_tier_1") != null
                    ? higherDepth(slayer, "zombie.boss_kills_tier_1").getAsInt()
                    : 0;
            int revThreeKills = higherDepth(slayer, "zombie.boss_kills_tier_2") != null
                    ? higherDepth(slayer, "zombie.boss_kills_tier_2").getAsInt()
                    : 0;
            int revFourKills = higherDepth(slayer, "zombie.boss_kills_tier_3") != null
                    ? higherDepth(slayer, "zombie.boss_kills_tier_3").getAsInt()
                    : 0;
            int revFiveKills = higherDepth(slayer, "zombie.boss_kills_tier_4") != null
                    ? higherDepth(slayer, "zombie.boss_kills_tier_4").getAsInt()
                    : 0;

            int taraOneKills = higherDepth(slayer, "spider.boss_kills_tier_0") != null
                    ? higherDepth(slayer, "spider.boss_kills_tier_0").getAsInt()
                    : 0;
            int taraTwoKills = higherDepth(slayer, "spider.boss_kills_tier_1") != null
                    ? higherDepth(slayer, "spider.boss_kills_tier_1").getAsInt()
                    : 0;
            int taraThreeKills = higherDepth(slayer, "spider.boss_kills_tier_2") != null
                    ? higherDepth(slayer, "spider.boss_kills_tier_2").getAsInt()
                    : 0;
            int taraFourKills = higherDepth(slayer, "spider.boss_kills_tier_3") != null
                    ? higherDepth(slayer, "spider.boss_kills_tier_3").getAsInt()
                    : 0;

            String svenKills = "**Tier 1:** " + svenOneKills + "\n**Tier 2:** " + svenTwoKills + "\n**Tier 3:** "
                    + svenThreeKills + "\n**Tier 4:** " + svenFourKills;

            String revKills = "**Tier 1:** " + revOneKills + "\n**Tier 2:** " + revTwoKills + "\n**Tier 3:** "
                    + revThreeKills + "\n**Tier 4:** " + revFourKills + "\n**Tier 5:** " + revFiveKills;

            String taraKills = "**Tier 1:** " + taraOneKills + "\n**Tier 2:** " + taraTwoKills + "\n**Tier 3:** "
                    + taraThreeKills + "\n**Tier 4:** " + taraFourKills;

            long coinsSpentOnSlayers = 100L * (svenOneKills + revOneKills + taraOneKills)
                    + 2000L * (svenTwoKills + revTwoKills + taraTwoKills)
                    + 10000L * (svenThreeKills + revThreeKills + taraThreeKills)
                    + 50000L * (svenFourKills + revFourKills + taraFourKills) + 100000L * revFiveKills;
            eb.setDescription("**Total slayer:** " + formatNumber(player.getSlayer()) + " XP\n**Total coins spent:** "
                    + simplifyNumber(coinsSpentOnSlayers));
            eb.addField("<:sven_packmaster:800002277648891914> Wolf (" + player.getSlayerLevel("sven") + ")",
                    simplifyNumber(player.getWolfXp()) + " XP", true);
            eb.addField("<:revenant_horror:800002290987302943> Zombie (" + player.getSlayerLevel("rev") + ")",
                    simplifyNumber(player.getZombieXp()) + " XP", true);
            eb.addField("<:tarantula_broodfather:800002277262884874> Spider (" + player.getSlayerLevel("tara") + ")",
                    simplifyNumber(player.getSpiderXp()) + " XP", true);

            eb.addField("Boss Kills", svenKills, true);
            eb.addField("Boss Kills", revKills, true);
            eb.addField("Boss Kills", taraKills, true);

            eb.setThumbnail(player.getThumbnailUrl());
            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
