/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.ApiHandler.queryLowestBin;
import static com.skyblockplus.utils.Constants.allAttributes;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.utils.Utils.setTriUnion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.rendering.LoreRenderer;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class AttributesSlashCommand extends SlashCommand {

	public AttributesSlashCommand() {
		this.name = "attributes";
	}

	public static Object getAttributes(String item, String attrOne, String attrTwo) {
		JsonObject averageAuctionJson = getAverageAuctionJson();
		JsonObject averageBinJson = getAverageBinJson();
		JsonObject lowestBinJson = getLowestBinJson();
		if (averageAuctionJson == null || averageBinJson == null || lowestBinJson == null) {
			return defaultEmbed("Error fetching auction prices");
		}

		Set<String> keys = setTriUnion(averageAuctionJson.keySet(), averageBinJson.keySet(), lowestBinJson.keySet())
			.stream()
			.filter(e -> e.contains("+"))
			.collect(Collectors.toCollection(HashSet::new));

		String itemId = nameToId(item, true);
		if (itemId == null) {
			itemId = getClosestMatchFromIds(item, keys);
		}

		attrOne = attrOne.replace(" ", "_").toUpperCase();
		if (!allAttributes.contains(attrOne)) {
			attrOne = getClosestMatchFromIds(attrOne, allAttributes);
		}

		attrTwo = attrTwo.replace(" ", "_").toUpperCase();
		if (!allAttributes.contains(attrTwo)) {
			attrTwo = getClosestMatchFromIds(attrTwo, allAttributes);
		}

		if (attrOne.compareTo(attrTwo) > 0) {
			String tempAttrOne = attrOne;
			attrOne = attrTwo;
			attrTwo = tempAttrOne;
		}

		EmbedBuilder eb = defaultEmbed(idToName(itemId))
			.appendDescription(
				"**Attribute One:** " +
				capitalizeString(attrOne.replace("_", " ")) +
				"\n**Attribute Two:** " +
				capitalizeString(attrTwo.replace("_", " "))
			);

		String formattedId = itemId + "+ATTRIBUTE_SHARD_" + attrOne + "+ATTRIBUTE_SHARD_" + attrTwo;
		if (!keys.contains(formattedId)) {
			return eb.setTitle("Error").appendDescription("\n\nProvided attributes were not found for item");
		}

		eb.setThumbnail(getItemThumbnail(itemId));

		JsonElement aucItemJson = higherDepth(averageAuctionJson, formattedId);
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

		JsonElement avgBinItemJson = higherDepth(averageBinJson, formattedId);
		if (avgBinItemJson != null) {
			int sales = higherDepth(avgBinItemJson, "sales").getAsInt();
			eb.addField(
				"Average Bin Price",
				"Cost: " +
				roundAndFormat(higherDepth(avgBinItemJson, "price").getAsDouble()) +
				"\nSales Per Hour: " +
				(sales < 1 ? "less than one" : formatNumber(sales)),
				false
			);
		}

		JsonElement lowestBinItemJson = queryLowestBin(formattedId);
		if (lowestBinItemJson != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				BufferedImage loreRender = LoreRenderer.renderLore(
					Arrays.stream(higherDepth(lowestBinItemJson, "lore").getAsString().split("\n")).toList()
				);
				ImageIO.write(loreRender, "png", baos);
			} catch (Exception ignored) {}

			return new MessageEditBuilder()
				.setEmbeds(
					eb
						.addField(
							"Lowest Bin Price",
							"Cost: " +
							roundAndFormat(higherDepth(lowestBinItemJson, "starting_bid").getAsDouble()) +
							"\nCommand: `/viewauction " +
							higherDepth(lowestBinItemJson, "uuid").getAsString() +
							"`",
							false
						)
						.setImage("attachment://lore.png")
						.build()
				)
				.setFiles(FileUpload.fromData(baos.toByteArray(), "lore.png"));
		}

		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getAttributes(event.getOptionStr("item"), event.getOptionStr("attribute_one"), event.getOptionStr("attribute_two")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the average attribute price of an item")
			.addOption(OptionType.STRING, "item", "Item name", true, true)
			.addOption(OptionType.STRING, "attribute_one", "Attribute one", true, true)
			.addOption(OptionType.STRING, "attribute_two", "Attribute two", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			if (getLowestBinJson() != null && getAverageAuctionJson() != null && getAverageBinJson() != null) {
				event.replyClosestMatch(
					event.getFocusedOption().getValue(),
					setTriUnion(getLowestBinJson().keySet(), getAverageAuctionJson().keySet(), getAverageBinJson().keySet())
						.stream()
						.filter(e -> e.contains("+"))
						.map(e -> idToName(e.split("\\+")[0]))
						.distinct()
						.collect(Collectors.toCollection(ArrayList::new))
				);
			}
		} else if (
			event.getFocusedOption().getName().equals("attribute_one") || event.getFocusedOption().getName().equals("attribute_two")
		) {
			String itemName = event.getOption("item", OptionMapping::getAsString);
			if (itemName != null && getLowestBinJson() != null && getAverageAuctionJson() != null && getAverageBinJson() != null) {
				Set<String> keys = setTriUnion(getLowestBinJson().keySet(), getAverageAuctionJson().keySet(), getAverageBinJson().keySet())
					.stream()
					.filter(e -> e.contains("+"))
					.collect(Collectors.toCollection(HashSet::new));

				String itemId = nameToId(itemName, true);
				if (itemId == null) {
					itemId = getClosestMatchFromIds(itemName, keys);
				}
				String finalItemId = itemId;

				Set<String> options = allAttributes
					.stream()
					.filter(attribute ->
						keys
							.stream()
							.anyMatch(e ->
								e.startsWith(finalItemId + "+ATTRIBUTE_SHARD_" + attribute) ||
								(e.startsWith(finalItemId + "+ATTRIBUTE_SHARD_") && e.endsWith("+ATTRIBUTE_SHARD_" + attribute))
							)
					)
					.map(e -> capitalizeString(e.replace("_", " ")))
					.collect(Collectors.toCollection(HashSet::new));

				if (!options.isEmpty()) {
					event.replyClosestMatch(event.getFocusedOption().getValue(), options);
					return;
				}
			}

			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				allAttributes.stream().map(e -> capitalizeString(e.replace("_", " "))).collect(Collectors.toCollection(ArrayList::new))
			);
		}
	}
}
