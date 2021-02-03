package com.SkyblockBot.Miscellaneous;

import com.SkyblockBot.Utils.Player;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.SkyblockBot.Utils.BotUtils.*;

public class StatsCommand extends Command {
    Message ebMessage;

    public StatsCommand() {
        this.name = "stats";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...", null);
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

        if (args[1].equals("player")) {
            if (args.length == 4) {
                eb = getPlayerStats(args[2], args[3]);
            } else
                eb = getPlayerStats(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerStats(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            double playerBankBalance = player.getBankBalance();
            double playerPurseCoins = player.getPurseCoins();
            int fairySouls = player.getFairySouls();
            int minionSlots = player.getNumberMinionSlots();
            double skillAverage = player.getSkillAverage();


            EmbedBuilder eb = defaultEmbed("Player stats for " + player.getUsername(), skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.addField("Slayer", "Wolf: " + player.getSlayerLevel("sven") + "\nRev: " + player.getSlayerLevel("rev") + "\nTara: " + player.getSlayerLevel("tara"), true);
            eb.addField("Coins", "Bank: " + simplifyNumber(playerBankBalance) + " coins\nPurse: " + simplifyNumber(playerPurseCoins) + " coins", true);
            eb.addField("Current Minion Slots", minionSlots + " slots", true);
            eb.addField("Fairy Souls", fairySouls + " souls", true);
            eb.addField("Skill Average", roundSkillAverage(skillAverage), true);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
