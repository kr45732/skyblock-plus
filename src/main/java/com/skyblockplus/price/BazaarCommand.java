package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class BazaarCommand extends Command {

	public BazaarCommand() {
		this.name = "bazaar";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "bz" };
	}

	public static EmbedBuilder getBazaarItem(String itemName) {
		JsonElement bazaarItems = getJson("https://api.slothpixel.me/api/skyblock/bazaar");
		List<String> itemsUnformatted = getJsonKeys(bazaarItems);
		String formattedItemName = itemName.replace(" ", "_").toUpperCase();
		Map<String, String> itemsMap = new HashMap<>();

		for (String item : itemsUnformatted) {
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
				eb.addField(
					"Buy Price (Per)",
					simplifyNumber(higherDepth(higherDepth(itemInfo, "buy_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble()),
					true
				);
				eb.addField(
					"Sell Price (Per)",
					simplifyNumber(
						higherDepth(higherDepth(itemInfo, "sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble()
					),
					true
				);
				eb.addBlankField(true);
				eb.addField("Buy Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.buyVolume").getAsDouble()), true);
				eb.addField("Sell Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.sellVolume").getAsDouble()), true);
				eb.addBlankField(true);
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + item);
				return eb;
			}
		}

		String closestMatch = getClosestMatch(itemName, new ArrayList<>(itemsMap.keySet()));
		closestMatch = itemsMap.get(closestMatch);

		if (closestMatch != null && higherDepth(bazaarItems, closestMatch) != null) {
			JsonElement itemInfo = higherDepth(bazaarItems, closestMatch);
			EmbedBuilder eb = defaultEmbed(
				higherDepth(bazaarItems, closestMatch + ".name").getAsString(),
				"https://bazaartracker.com/product/" + closestMatch
			);
			eb.addField(
				"Buy Price (Per)",
				simplifyNumber(higherDepth(higherDepth(itemInfo, "buy_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble()),
				true
			);
			eb.addField(
				"Sell Price (Per)",
				simplifyNumber(higherDepth(higherDepth(itemInfo, "sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble()),
				true
			);
			eb.addBlankField(true);
			eb.addField("Buy Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.buyVolume").getAsDouble()), true);
			eb.addField("Sell Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.sellVolume").getAsDouble()), true);
			eb.addBlankField(true);
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + closestMatch);
			return eb;
		}

		return invalidEmbed("Invalid item name");
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ", 2);

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					ebMessage.editMessageEmbeds(getBazaarItem(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
