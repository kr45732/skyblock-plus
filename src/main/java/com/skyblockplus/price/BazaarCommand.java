package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class BazaarCommand extends Command {

	public BazaarCommand() {
		this.name = "bazaar";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "bz" };
	}

	public static EmbedBuilder getBazaarItem(String itemNameU) {
		JsonElement bazaarItems = getJson("https://api.slothpixel.me/api/skyblock/bazaar");
		if (bazaarItems == null) {
			return invalidEmbed("Error getting bazaar data");
		}

		String itemId = nameToId(itemNameU);
		if (higherDepth(bazaarItems, itemId) == null) {
			Map<String, String> itemNameToId = new HashMap<>();
			for (String itemIID : getJsonKeys(bazaarItems)) {
				String itemName;
				try {
					itemName = higherDepth(bazaarItems, itemIID + ".name").getAsString();
				} catch (Exception e) {
					itemName = capitalizeString(itemIID.replace("_", " "));
				}
				itemNameToId.put(itemName, itemIID);
			}

			itemId = itemNameToId.get(getClosestMatch(itemId, new ArrayList<>(itemNameToId.keySet())));
		}

		JsonElement itemInfo = higherDepth(bazaarItems, itemId);
		EmbedBuilder eb = defaultEmbed(idToName(itemId), "https://bazaartracker.com/product/" + itemId);
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
		eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				if (args.length == 2) {
					embed(getBazaarItem(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
