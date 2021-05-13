package com.skyblockplus.price;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        JsonElement bazaarItems = getJson("https://api.slothpixel.me/api/skyblock/bazaar");
        List<String> itemsUnformated = getJsonKeys(bazaarItems);
        String formattedItemName = itemName.replace(" ", "_").toUpperCase();
        Map<String, String> itemsMap = new HashMap<>();

        for (String item : itemsUnformated) {
            String itemNameFormatted;
            try {
                itemNameFormatted = higherDepth(bazaarItems, item + ".name").getAsString();
            } catch (Exception e) {
                itemNameFormatted = capitalizeString(item.replace("_", " "));
            }

            itemsMap.put(itemNameFormatted, item);

            if (itemNameFormatted.toUpperCase().replace(" ", "_").equals(formattedItemName)) {
                JsonElement itemInfo = higherDepth(bazaarItems, item);
                EmbedBuilder eb = defaultEmbed(itemNameFormatted, "https://bazaartracker.com/product/" + item);
                eb.addField("Buy Price (Per)", simplifyNumber(
                        higherDepth(higherDepth(itemInfo, "buy_summary").getAsJsonArray().get(0), "pricePerUnit")
                                .getAsDouble()),
                        true);
                eb.addField("Sell Price (Per)", simplifyNumber(
                        higherDepth(higherDepth(itemInfo, "sell_summary").getAsJsonArray().get(0), "pricePerUnit")
                                .getAsDouble()),
                        true);
                eb.addBlankField(true);
                eb.addField("Buy Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.buyVolume").getAsDouble()),
                        true);
                eb.addField("Sell Volume",
                        simplifyNumber(higherDepth(itemInfo, "quick_status.sellVolume").getAsDouble()), true);
                eb.addBlankField(true);
                eb.setThumbnail("https://sky.lea.moe/item.gif/" + item);
                return eb;
            }
        }

        List<String> items = new ArrayList<>(itemsMap.keySet());
        LevenshteinDistance matchCalc = LevenshteinDistance.getDefaultInstance();
        int minDistance = matchCalc.apply(items.get(0), itemName);
        String closestMatch = items.get(0);
        for (String itemF : items) {
            int currentDistance = matchCalc.apply(itemF, itemName);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                closestMatch = itemF;
            }
        }

        if (closestMatch != null && higherDepth(bazaarItems, closestMatch) != null) {
            closestMatch = itemsMap.get(closestMatch);

            JsonElement itemInfo = higherDepth(bazaarItems, closestMatch);
            EmbedBuilder eb = defaultEmbed(higherDepth(bazaarItems, closestMatch + ".name").getAsString(),
                    "https://bazaartracker.com/product/" + closestMatch);
            eb.addField("Buy Price (Per)",
                    simplifyNumber(
                            higherDepth(higherDepth(itemInfo, "buy_summary").getAsJsonArray().get(0), "pricePerUnit")
                                    .getAsDouble()),
                    true);
            eb.addField("Sell Price (Per)",
                    simplifyNumber(
                            higherDepth(higherDepth(itemInfo, "sell_summary").getAsJsonArray().get(0), "pricePerUnit")
                                    .getAsDouble()),
                    true);
            eb.addBlankField(true);
            eb.addField("Buy Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.buyVolume").getAsDouble()),
                    true);
            eb.addField("Sell Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.sellVolume").getAsDouble()),
                    true);
            eb.addBlankField(true);
            eb.setThumbnail("https://sky.lea.moe/item.gif/" + closestMatch);
            return eb;
        }

        return defaultEmbed("Unable to find item");
    }
}
