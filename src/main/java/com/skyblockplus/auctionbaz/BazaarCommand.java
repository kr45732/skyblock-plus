package com.skyblockplus.auctionbaz;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class BazaarCommand extends Command {
    public BazaarCommand() {
        this.name = "bazaar";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"bz"};
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ", 2);

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2) {
                ebMessage.editMessage(getBazaarItem(args[1]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getBazaarItem(String itemName) {
        JsonElement bazaarItems = higherDepth(getJson("https://api.hypixel.net/skyblock/bazaar?key=" + HYPIXEL_API_KEY), "products");
        String formattedItemName = itemName.replace(" ", "_").toUpperCase();
        if (higherDepth(bazaarItems, formattedItemName) != null) {
            JsonElement itemInfo = higherDepth(higherDepth(bazaarItems, formattedItemName), "quick_status");
            EmbedBuilder eb = defaultEmbed(itemName, "https://bazaartracker.com/product/" + formattedItemName);
            eb.addField("Buy Price (Per)", simplifyNumber(higherDepth(itemInfo, "buyPrice").getAsDouble()), true);
            eb.addField("Sell Price (Per)", simplifyNumber(higherDepth(itemInfo, "sellPrice").getAsDouble()), true);
            eb.addBlankField(true);
            eb.addField("Buy Volume", simplifyNumber(higherDepth(itemInfo, "buyVolume").getAsDouble()), true);
            eb.addField("Sell Volume", simplifyNumber(higherDepth(itemInfo, "sellVolume").getAsDouble()), true);
            eb.addBlankField(true);
            eb.setThumbnail("https://sky.lea.moe/item.gif/" + formattedItemName);
            return eb;
        }

        return defaultEmbed("Unable to find item");
    }
}
