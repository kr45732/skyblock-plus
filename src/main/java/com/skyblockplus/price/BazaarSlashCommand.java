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
import static com.skyblockplus.utils.Utils.getItemThumbnail;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class BazaarSlashCommand extends SlashCommand {

	public BazaarSlashCommand() {
		this.name = "bazaar";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.embed(getBazaarItem(event.getOptionStr("item")));
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get bazaar prices of an item").addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), getBazaarItems());
		}
	}

	public static EmbedBuilder getBazaarItem(String itemNameU) {
		JsonElement bazaarItems = getBazaarJson();
		if (bazaarItems == null) {
			return invalidEmbed("Error getting bazaar data");
		}

		String itemId = nameToId(itemNameU);
		if (higherDepth(bazaarItems, itemId) == null) {
			itemId = getClosestMatchFromIds(itemId, getJsonKeys(bazaarItems));
		}

		JsonElement itemInfo = higherDepth(bazaarItems, itemId);
		return defaultEmbed(idToName(itemId), "https://bazaartracker.com/product/" + itemId)
			.addField("Buy Price (Per)", simplifyNumber(higherDepth(itemInfo, "buy_summary.[0].pricePerUnit", 0.0)), true)
			.addField("Sell Price (Per)", simplifyNumber(higherDepth(itemInfo, "sell_summary.[0].pricePerUnit", 0.0)), true)
			.addBlankField(true)
			.addField("Buy Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.buyVolume", 0L)), true)
			.addField("Sell Volume", simplifyNumber(higherDepth(itemInfo, "quick_status.sellVolume", 0L)), true)
			.addBlankField(true)
			.setThumbnail(getItemThumbnail(itemId));
	}
}
