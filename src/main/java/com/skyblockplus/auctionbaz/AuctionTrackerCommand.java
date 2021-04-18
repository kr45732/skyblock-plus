package com.skyblockplus.auctionbaz;

import static com.skyblockplus.utils.Utils.HYPIXEL_API_KEY;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.usernameToUuid;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class AuctionTrackerCommand extends Command {
    public AuctionTrackerCommand() {
        this.name = "track";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String[] args = event.getMessage().getContentRaw().split(" ");

            logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

            if (args.length == 2 && (args[1].equals("auction") || args[1].equals("ah"))) {
                ebMessage.editMessage(trackPlayerAuction(args[2]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder trackPlayerAuction(String username) {
        UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
        if (usernameUuidStruct == null) {
            return defaultEmbed("Error fetching player data");
        }

        String url = "https://api.hypixel.net/skyblock/auction?key=" + HYPIXEL_API_KEY + "&player="
                + usernameUuidStruct.playerUuid;

        JsonArray auctionsArray = higherDepth(getJson(url), "auctions").getAsJsonArray();

        List<String> auctionUuids = new ArrayList<>();
        String auctionNames = "";
        EmbedBuilder eb = defaultEmbed("Tracking Auctions of " + usernameUuidStruct.playerUsername,
                "https://auctions.craftlink.xyz/players/" + usernameUuidStruct.playerUuid);
        for (JsonElement auction : auctionsArray) {
            auctionUuids.add(higherDepth(auction, "uuid").getAsString());
            auctionNames += higherDepth(auction, "item_name").getAsString() + "\n";
        }

        eb.setDescription("Tracking " + auctionUuids.size() + " auctions\n" + auctionNames);
        return eb;
    }
}
