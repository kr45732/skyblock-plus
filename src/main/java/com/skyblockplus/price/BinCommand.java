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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class BinCommand extends Command {

	public BinCommand() {
		this.name = "bin";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "lbin" };
	}

	public static EmbedBuilder getLowestBin(String item) {
		JsonElement lowestBinJson = getLowestBinJson();
		if (lowestBinJson == null) {
			return invalidEmbed("Error fetching auctions");
		}

		String itemId = nameToId(item);
		if (higherDepth(lowestBinJson, itemId) != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			eb.addField(idToName(itemId), formatNumber(higherDepth(lowestBinJson, itemId, 0L)), false);
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
			return eb;
		}

		for (String i : ENCHANT_NAMES) {
			if (itemId.contains(i)) {
				try {
					String enchantedBookId = i + ";" + Integer.parseInt(itemId.replaceAll("\\D+", ""));
					if (higherDepth(lowestBinJson, enchantedBookId) != null) {
						EmbedBuilder eb = defaultEmbed("Lowest bin");
						eb.addField(idToName(enchantedBookId), formatNumber(higherDepth(lowestBinJson, enchantedBookId, 0L)), false);
						eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					}
				} catch (NumberFormatException e) {
					EmbedBuilder eb = defaultEmbed("Lowest bin");
					for (int j = 10; j > 0; j--) {
						String enchantedBookId = i + ";" + j;
						if (higherDepth(lowestBinJson, enchantedBookId) != null) {
							eb.addField(idToName(enchantedBookId), formatNumber(higherDepth(lowestBinJson, enchantedBookId, 0L)), false);
						}
					}

					if (eb.getFields().size() != 0) {
						eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					}
				}
			}
		}

		JsonElement petJson = getPetNumsJson();
		for (String i : PET_NAMES) {
			if (itemId.contains(i)) {
				String petId = i;
				boolean raritySpecified = false;
				for (Entry<String, String> j : RARITY_TO_NUMBER_MAP.entrySet()) {
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
						if (higherDepth(lowestBinJson, petId + RARITY_TO_NUMBER_MAP.get(j)) != null) {
							petId += RARITY_TO_NUMBER_MAP.get(j);
							break;
						}
					}
				}

				if (higherDepth(lowestBinJson, petId) != null) {
					EmbedBuilder eb = defaultEmbed("Lowest bin");
					eb.addField(idToName(petId), formatNumber(higherDepth(lowestBinJson, petId, 0L)), false);
					eb.setThumbnail(getPetUrl(petId.split(";")[0]));
					return eb;
				}
			}
		}

		String closestMatch = getClosestMatch(itemId, getJsonKeys(lowestBinJson));
		if (closestMatch != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			if (ENCHANT_NAMES.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
			} else if (PET_NAMES.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail(getPetUrl(closestMatch.split(";")[0].trim()));
			} else {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + closestMatch);
			}

			eb.addField(idToName(closestMatch), formatNumber(higherDepth(lowestBinJson, closestMatch, 0L)), false);
			return eb;
		}

		return defaultEmbed("No bin found for " + idToName(item));
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				if (args.length == 2) {
					embed(getLowestBin(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
