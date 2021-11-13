/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
		this.botPermissions = defaultPerms();
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
		eb.addField("Buy Price (Per)", simplifyNumber(higherDepth(itemInfo, "buy_summary.[0].pricePerUnit").getAsDouble()), true);
		eb.addField("Sell Price (Per)", simplifyNumber(higherDepth(itemInfo, "sell_summary.[0].pricePerUnit").getAsDouble()), true);
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
			.queue();
	}
}
