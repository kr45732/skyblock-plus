package com.SkyblockBot.Auction;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.simplifyNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.usernameToUuid;

import java.time.Duration;
import java.time.Instant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class AuctionCommands extends Command {
    public AuctionCommands() {
        this.name = "auction";
        this.guildOnly = false;
        this.cooldown = 5;
    }

    @Override
    protected void execute(CommandEvent event) {
        final EmbedBuilder[] eb = { defaultEmbed("Loading auction data...", null) };

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 3) { // No args or too many args are given
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        if (args[1].equals("player")) {
            eb[0] = getPlayerAuction(args[2]);
        } else {
            eb[0].setTitle("Invalid input. Type !help for help");
            event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());
            return;
        }

        event.reply(eb[0].build(), m -> m.editMessage(eb[0].build()).queue());

    }

    public EmbedBuilder getPlayerAuction(String username) {
        // String username = "pinkishh";
        String uuid = usernameToUuid(username);
        if (uuid == null) {
            return defaultEmbed("Error fetching player data", null);
        }
        String url = "https://api.hypixel.net/skyblock/auction?key=" + key + "&player=" + uuid;
        System.out.println(url);
        JsonElement playerAuctions = getJson(url);
        JsonArray auctionsArray = higherDepth(playerAuctions, "auctions").getAsJsonArray();
        String[][] auctions = new String[auctionsArray.size()][2];

        for (int i = 0; i < auctionsArray.size(); i++) {
            JsonElement currentAuction = auctionsArray.get(i);
            if (!higherDepth(currentAuction, "claimed").getAsBoolean()) {

                String auction = "";
                Boolean isPet = higherDepth(currentAuction, "item_lore").getAsString().toLowerCase().contains("pet");
                Boolean bin = false;
                try {
                    bin = higherDepth(currentAuction, "bin").getAsBoolean();
                } catch (NullPointerException ex) {
                }

                Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
                Duration duration = Duration.between(Instant.now(), endingAt);
                long daysUntil = duration.toMinutes() / 1400;
                long hoursUntil = duration.toMinutes() / 60 % 24;
                long minutesUntil = duration.toMinutes() % 60;
                String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
                timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
                timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";

                auctions[i][0] = isPet ? higherDepth(currentAuction, "tier").getAsString().toLowerCase() + " "
                        : "" + higherDepth(currentAuction, "item_name").getAsString();

                // auction = timeUntil.length() > 0
                // ? ((bin ? "BIN: _ coins" : "Current bid: _") + " | Ending in " + timeUntil)
                // : "Auction ended at _ coins";

                int highestBid = higherDepth(currentAuction, "highest_bid_amount").getAsInt();
                int startingBid = higherDepth(currentAuction, "starting_bid").getAsInt();
                if (timeUntil.length() > 0) {
                    if (bin) {
                        auction = "BIN: " + simplifyNumber(startingBid) + " coins";
                    } else {
                        auction = "Current bid: " + simplifyNumber(highestBid);
                    }
                    auction += " | Ending in " + timeUntil;
                } else {
                    if (highestBid >= startingBid) { // Auction sold
                        auction = "Auction sold for " + simplifyNumber(highestBid) + " coins";
                    } else {
                        auction = "Auction did not sell";
                    }

                }
                auctions[i][1] = auction;
            }
        }

        EmbedBuilder eb = defaultEmbed(username + "'s auctions", null);
        for (int i = 0; i < auctions.length; i++) {
            if (auctions[i][0] != null) {

                for (int j = 0; j < auctions.length; j++) {
                    if (auctions[j][0] != null) {
                        eb.addField(auctions[j][0], auctions[j][1], false);
                    }
                }
                return eb;
            }
        }
        eb.setTitle("No auctions found for " + username, null);
        return eb;
    }

}
