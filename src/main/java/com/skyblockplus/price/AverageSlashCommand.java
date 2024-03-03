/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.utils.StringUtils;
import java.util.ArrayList;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Component;

@Component
public class AverageSlashCommand extends SlashCommand {

	public AverageSlashCommand() {
		this.name = "average";
	}

	public static EmbedBuilder getAverageAuctionPrice(String item) {
		JsonObject averageAuctionJson = getAverageAuctionJson();
		JsonObject averageBinJson = getAverageBinJson();
		if (averageAuctionJson == null || averageBinJson == null) {
			return defaultEmbed("Error fetching average auction prices");
		}

		String itemId = nameToId(item);
		if (higherDepth(averageAuctionJson, itemId) == null && higherDepth(averageBinJson, itemId) == null) {
			SetUtils.SetView<String> avgKeys = SetUtils.union(averageAuctionJson.keySet(), averageBinJson.keySet());
			itemId = getClosestMatchFromIds(itemId, avgKeys);
		}

		EmbedBuilder eb = defaultEmbed(idToName(itemId)).setThumbnail(getItemThumbnail(itemId));

		JsonElement aucItemJson = higherDepth(averageAuctionJson, itemId);
		if (aucItemJson != null) {
			int sales = higherDepth(aucItemJson, "sales").getAsInt();
			eb.addField(
				"Average Auction Price",
				"Cost: " +
				roundAndFormat(higherDepth(aucItemJson, "price").getAsDouble()) +
				"\nSales Per Hour: " +
				(sales < 1 ? "less than one" : formatNumber(sales)),
				false
			);
		}

		JsonElement binItemJson = higherDepth(averageBinJson, itemId);
		if (binItemJson != null) {
			int sales = higherDepth(binItemJson, "sales").getAsInt();
			eb.addField(
				"Average Bin Price",
				"Cost: " +
				roundAndFormat(higherDepth(binItemJson, "price").getAsDouble()) +
				"\nSales Per Hour: " +
				(sales < 1 ? "less than one" : formatNumber(sales)),
				false
			);
		}

		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getAverageAuctionPrice(event.getOptionStr("item")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the average auction price of an item")
			.addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			if (getAverageAuctionJson() != null && getAverageBinJson() != null) {
				event.replyClosestMatch(
					event.getFocusedOption().getValue(),
					SetUtils
						.union(getAverageAuctionJson().keySet(), getAverageBinJson().keySet())
						.stream()
						.filter(e -> !e.contains("+"))
						.map(StringUtils::idToName)
						.distinct()
						.collect(Collectors.toCollection(ArrayList::new))
				);
			}
		}
	}
}
