package com.SkyblockBot.Miscellaneous;

import com.SkyblockBot.Utils.Player;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.SkyblockBot.Utils.BotUtils.*;

public class CoinsBalanceCommand extends Command {
    Message ebMessage;

    public CoinsBalanceCommand(){
        this.name = "coins";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading banking and purse data...", null);
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
                eb = getPlayerBalance(args[2], args[3]);
            } else
                eb = getPlayerBalance(args[2], null);
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerBalance(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValidPlayer()) {
            double playerBankBalance = player.getBankBalance();
            double playerPurseCoins = player.getPurseCoins();


            EmbedBuilder eb = defaultEmbed("Bank and purse coins for " + player.getUsername(), skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.setDescription("**Total coins:** " + simplifyNumber(playerBankBalance + playerPurseCoins));
            eb.addField("Bank balance", playerBankBalance == -1 ? "Banking API disabled" : simplifyNumber(playerBankBalance) + " coins", false);
            eb.addField("Purse coins", playerPurseCoins == -1 ? "Banking API disabled" : simplifyNumber(playerPurseCoins) + " coins", false);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
