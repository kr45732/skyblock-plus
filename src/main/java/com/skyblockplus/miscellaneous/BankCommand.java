package com.skyblockplus.miscellaneous;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.Utils.*;

public class BankCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public BankCommand(EventWaiter waiter) {
        this.name = "bank";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        if ((args.length == 3 || args.length == 4) && args[1].equals("history")) {
            this.event = event;
            if (args.length == 4) {
                eb = getPlayerBankHistory(args[2], args[3]);
            } else {
                eb = getPlayerBankHistory(args[2], null);
            }

            if (eb == null) {
                ebMessage.delete().queue();
            } else {
                ebMessage.editMessage(eb.build()).queue();
            }
            return;
        } else if (args.length == 2) {
            ebMessage.editMessage(getPlayerBalance(args[1], null).build()).queue();
            return;
        } else if (args.length == 3) {
            ebMessage.editMessage(getPlayerBalance(args[1], args[2]).build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getPlayerBalance(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            double playerBankBalance = player.getBankBalance();
            double playerPurseCoins = player.getPurseCoins();

            EmbedBuilder eb = defaultEmbed("Total coins for " + player.getUsername(),
                    skyblockStatsLink(player.getUsername(), player.getProfileName()));
            eb.setDescription("**Total coins:** " + simplifyNumber(playerBankBalance + playerPurseCoins));
            eb.addField("Bank balance",
                    playerBankBalance == -1 ? "Banking API disabled" : simplifyNumber(playerBankBalance) + " coins",
                    false);
            eb.addField("Purse coins",
                    playerPurseCoins == -1 ? "Banking API disabled" : simplifyNumber(playerPurseCoins) + " coins",
                    false);
            return eb;
        }
        return defaultEmbed("Unable to fetch player data");
    }

    private EmbedBuilder getPlayerBankHistory(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            JsonArray bankHistoryArray = player.getBankHistory();
            if (bankHistoryArray != null) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(Locale.US).withZone(ZoneId.systemDefault());

                ArrayList<String> pageTitles = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1)
                        .setItemsPerPage(20).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ex) {
                                m.delete().queue();
                            }
                        }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                        .setCommandUser(event.getAuthor());

                paginateBuilder.addItems("**Last Transaction Time:** " + dateTimeFormatter.format(Instant.ofEpochMilli(
                        higherDepth(bankHistoryArray.get(bankHistoryArray.size() - 1), "timestamp").getAsLong()))
                        + "\n");
                for (int i = bankHistoryArray.size() - 1; i >= 0; i--) {
                    pageTitles.add("Bank transaction history for " + player.getUsername());
                    JsonElement currentTransaction = bankHistoryArray.get(i);
                    String valueString = simplifyNumber(higherDepth(currentTransaction, "amount").getAsLong()) + " "
                            + (higherDepth(currentTransaction, "action").getAsString().equals("DEPOSIT") ? "deposited"
                            : "withdrawn")
                            + " by " + higherDepth(currentTransaction, "initiator_name").getAsString()
                            .replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|§7|§b", "");

                    String time = dateTimeFormatter
                            .format(Instant.ofEpochMilli(higherDepth(currentTransaction, "timestamp").getAsLong()));
                    paginateBuilder.addItems("**" + time + "**: " + valueString);
                }

                paginateBuilder.setPageTitles(pageTitles.toArray(new String[0]));
                paginateBuilder.build().paginate(event.getChannel(), 0);
                return null;
            } else {
                return defaultEmbed("Player banking API disabled");
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
