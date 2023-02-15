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

import static com.skyblockplus.utils.Constants.PET_NAMES;
import static com.skyblockplus.utils.Constants.RARITY_TO_NUMBER_MAP;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class AverageAuctionSlashCommand extends SlashCommand {

	public AverageAuctionSlashCommand() {
		this.name = "average";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getAverageAuctionPrice(event.getOptionStr("item")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the average auction price create an item")
			.addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			if (getAverageAuctionJson() != null) {
				event.replyClosestMatch(
					event.getFocusedOption().getValue(),
					getAverageAuctionJson()
						.keySet()
						.stream()
						.map(Utils::idToName)
						.distinct()
						.collect(Collectors.toCollection(ArrayList::new))
				);
			}
		}
	}

	public static EmbedBuilder getAverageAuctionPrice(String item) {
		JsonElement averageAhJson = getAverageAuctionJson();
		if (averageAhJson == null) {
			return defaultEmbed("Error fetching auctions");
		}

		EmbedBuilder eb = defaultEmbed("Average auction");
		String itemId = nameToId(item);
		if (higherDepth(averageAhJson, itemId) != null) {
			JsonElement itemJson = higherDepth(averageAhJson, itemId);
			eb.addField(idToName(itemId), formatNumber(higherDepth(itemJson, "clean_price", higherDepth(itemJson, "price", 0L))), false);
			eb.setThumbnail(getItemThumbnail(itemId));
			return eb;
		}

		JsonElement petJson = getPetNumsJson();
		for (String i : PET_NAMES) {
			if (itemId.contains(i)) {
				String petId = i;
				boolean raritySpecified = false;
				for (Map.Entry<String, String> j : RARITY_TO_NUMBER_MAP.entrySet()) {
					if (itemId.contains(j.getKey())) {
						petId += j.getValue();
						raritySpecified = true;
						break;
					}
				}

				if (!raritySpecified) {
					List<String> petRarities = higherDepth(petJson, petId)
						.getAsJsonObject()
						.keySet()
						.stream()
						.map(String::toUpperCase)
						.collect(Collectors.toCollection(ArrayList::new));

					for (String j : petRarities) {
						if (higherDepth(averageAhJson, petId + RARITY_TO_NUMBER_MAP.get(j)) != null) {
							petId += RARITY_TO_NUMBER_MAP.get(j);
							break;
						}
					}
				}

				if (higherDepth(averageAhJson, petId) != null) {
					JsonElement itemJson = higherDepth(averageAhJson, petId);
					eb.addField(
						idToName(petId),
						formatNumber(higherDepth(itemJson, "clean_price", higherDepth(itemJson, "price", 0L))),
						false
					);
					eb.setThumbnail(getItemThumbnail(petId));
					return eb;
				}
			}
		}

		String closestMatch = getClosestMatchFromIds(itemId, getJsonKeys(averageAhJson));
		if (closestMatch != null) {
			JsonElement itemJson = higherDepth(averageAhJson, closestMatch);
			eb.addField(
				idToName(closestMatch),
				formatNumber(higherDepth(itemJson, "clean_price", higherDepth(itemJson, "price", 0L))),
				false
			);
			eb.setThumbnail(getItemThumbnail(closestMatch));
			return eb;
		}

		return defaultEmbed("No auctions found for " + idToName(item));
	}
}
