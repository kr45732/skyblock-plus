package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class AverageAuctionCommand extends Command {

	public AverageAuctionCommand() {
		this.name = "average";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "avg" };
	}

	public static EmbedBuilder getAverageAuctionPrice(String item) {
		JsonElement averageAhJson = getAverageAuctionJson();
		if (averageAhJson == null) {
			return defaultEmbed("Error fetching auctions");
		}

		String itemId = nameToId(item);
		if (higherDepth(averageAhJson, itemId) != null) {
			JsonElement itemJson = higherDepth(averageAhJson, itemId);
			EmbedBuilder eb;

			if (higherDepth(itemJson, "clean_price") != null) {
				eb = defaultEmbed("Average auction (clean)");
				eb.addField(idToName(itemId), formatNumber(higherDepth(itemJson, "clean_price", 0L)), false);
			} else {
				eb = defaultEmbed("Average auction");
				eb.addField(idToName(itemId), formatNumber(higherDepth(itemJson, "price", 0L)), false);
			}

			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
			return eb;
		}

		for (String i : ENCHANT_NAMES) {
			if (itemId.contains(i)) {
				try {
					String enchantedBookId = i + ";" + Integer.parseInt(itemId.replaceAll("\\D+", ""));
					if (higherDepth(averageAhJson, enchantedBookId) != null) {
						JsonElement itemJson = higherDepth(averageAhJson, enchantedBookId);
						EmbedBuilder eb;

						if (higherDepth(itemJson, "clean_price") != null) {
							eb = defaultEmbed("Average auction (clean)");
							eb.addField(idToName(enchantedBookId), formatNumber(higherDepth(itemJson, "clean_price", 0L)), false);
						} else {
							eb = defaultEmbed("Average auction");
							eb.addField(idToName(enchantedBookId), formatNumber(higherDepth(itemJson, "price", 0L)), false);
						}

						eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					}
				} catch (NumberFormatException e) {
					EmbedBuilder eb = defaultEmbed("Average auction");
					for (int j = 10; j > 0; j--) {
						String enchantedBookId = i + ";" + j;
						if (higherDepth(averageAhJson, enchantedBookId) != null) {
							JsonElement itemJson = higherDepth(averageAhJson, enchantedBookId);

							if (higherDepth(itemJson, "clean_price") != null) {
								eb.setTitle("Average auction (clean)");
								eb.addField(idToName(enchantedBookId), formatNumber(higherDepth(itemJson, "clean_price", 0L)), false);
							} else {
								eb.setTitle("Average auction");
								eb.addField(idToName(enchantedBookId), formatNumber(higherDepth(itemJson, "price", 0L)), false);
							}
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
					EmbedBuilder eb;

					if (higherDepth(itemJson, "clean_price") != null) {
						eb = defaultEmbed("Average auction (clean)");
						eb.addField(idToName(petId), formatNumber(higherDepth(itemJson, "clean_price", 0L)), false);
					} else {
						eb = defaultEmbed("Average auction");
						eb.addField(idToName(petId), formatNumber(higherDepth(itemJson, "price", 0L)), false);
					}

					eb.setThumbnail(getPetUrl(petId.split(";")[0]));
					return eb;
				}
			}
		}

		String closestMatch = getClosestMatch(itemId, getJsonKeys(averageAhJson));
		if (closestMatch != null) {
			EmbedBuilder eb = defaultEmbed("Average Auction");
			JsonElement itemJson = higherDepth(averageAhJson, closestMatch);

			if (ENCHANT_NAMES.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
			} else if (PET_NAMES.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail(getPetUrl(closestMatch.split(";")[0].trim()));
			} else {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + closestMatch);
			}

			if (higherDepth(itemJson, "clean_price") != null) {
				eb = defaultEmbed("Average auction (clean)");
				eb.addField(idToName(closestMatch), formatNumber(higherDepth(itemJson, "clean_price", 0L)), false);
			} else {
				eb = defaultEmbed("Average auction");
				eb.addField(idToName(closestMatch), formatNumber(higherDepth(itemJson, "price", 0L)), false);
			}
			return eb;
		}

		return defaultEmbed("No auctions found for " + idToName(item));
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				if (args.length == 2) {
					embed(getAverageAuctionPrice(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
