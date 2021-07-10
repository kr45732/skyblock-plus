package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.enchantNames;
import static com.skyblockplus.utils.Constants.petNames;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class BinCommand extends Command {

	public BinCommand() {
		this.name = "bin";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "lbin" };
	}

	public static EmbedBuilder getLowestBin(String item) {
		JsonElement lowestBinJson = getLowestBinJson();
		if (lowestBinJson == null) {
			return defaultEmbed("Error fetching auctions");
		}

		if (higherDepth(lowestBinJson, item) != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			eb.addField(capitalizeString(item), formatNumber(higherDepth(lowestBinJson, item).getAsLong()), false);
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + item);
			return eb;
		}

		String preFormattedItem = convertToInternalName(item);

		if (higherDepth(lowestBinJson, preFormattedItem) != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			eb.addField(
				capitalizeString(item.toLowerCase()),
				formatNumber(higherDepth(lowestBinJson, preFormattedItem).getAsLong()),
				false
			);
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + preFormattedItem);
			return eb;
		}

		String formattedName;
		for (String i : enchantNames) {
			if (preFormattedItem.contains(i)) {
				String enchantName;
				try {
					int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
					enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
					formattedName = i + ";" + enchantLevel;
					EmbedBuilder eb = defaultEmbed("Lowest bin");
					eb.addField(capitalizeString(enchantName), formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()), false);
					eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
					return eb;
				} catch (NumberFormatException e) {
					try {
						EmbedBuilder eb = defaultEmbed("Lowest bin");
						for (int j = 10; j > 0; j--) {
							try {
								formattedName = i + ";" + j;
								enchantName = i.toLowerCase().replace("_", " ") + " " + j;
								eb.addField(
									capitalizeString(enchantName),
									formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()),
									false
								);
							} catch (NullPointerException ignored) {}
						}
						if (eb.getFields().size() == 0) {
							return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
						}
						eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					} catch (NullPointerException ex) {
						return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
					}
				} catch (NullPointerException e) {
					return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
				}
			}
		}

		JsonElement petJson = getPetNumsJson();

		for (String i : petNames) {
			if (preFormattedItem.contains(i)) {
				String petName = "";
				formattedName = i;
				boolean raritySpecified = false;
				for (Entry<String, String> j : Constants.rarityToNumberMap.entrySet()) {
					if (preFormattedItem.contains(j.getKey())) {
						petName = j.getKey().toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
						formattedName += j.getValue();
						raritySpecified = true;
						break;
					}
				}

				if (!raritySpecified) {
					List<String> petRarities = higherDepth(petJson, formattedName)
						.getAsJsonObject()
						.keySet()
						.stream()
						.map(String::toUpperCase)
						.collect(Collectors.toCollection(ArrayList::new));

					for (String j : petRarities) {
						if (higherDepth(lowestBinJson, formattedName + Constants.rarityToNumberMap.get(j)) != null) {
							petName = j.toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
							formattedName += Constants.rarityToNumberMap.get(j);
							break;
						}
					}
				}
				EmbedBuilder eb = defaultEmbed("Lowest bin");

				try {
					eb.addField(
						capitalizeString(petName) + " pet",
						formatNumber(higherDepth(lowestBinJson, formattedName).getAsLong()),
						false
					);
					eb.setThumbnail(getPetUrl(formattedName.split(";")[0]));
					return eb;
				} catch (Exception ignored) {}
			}
		}

		String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(lowestBinJson));

		if (closestMatch != null && higherDepth(lowestBinJson, closestMatch) != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			if (enchantNames.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
				eb.addField(
					capitalizeString(closestMatch.toLowerCase().replace("_", " ").replace(";", " ")),
					formatNumber(higherDepth(lowestBinJson, closestMatch).getAsLong()),
					false
				);
			} else if (petNames.contains(closestMatch.split(";")[0].trim())) {
				Map<String, String> rarityMapRev = new HashMap<>();
				rarityMapRev.put("4", "LEGENDARY");
				rarityMapRev.put("3", "EPIC");
				rarityMapRev.put("2", "RARE");
				rarityMapRev.put("1", "UNCOMMON");
				rarityMapRev.put("0", "COMMON");
				String[] itemS = closestMatch.split(";");
				eb.setThumbnail(getPetUrl(itemS[0]));
				eb.addField(
					capitalizeString(rarityMapRev.get(itemS[1].toUpperCase()) + " " + itemS[0].replace("_", " ")),
					formatNumber(higherDepth(lowestBinJson, closestMatch).getAsLong()),
					false
				);
			} else {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + closestMatch);
				eb.addField(
					capitalizeString(closestMatch.toLowerCase().replace("_", " ")),
					formatNumber(higherDepth(lowestBinJson, closestMatch).getAsLong()),
					false
				);
			}

			return eb;
		}

		return defaultEmbed("No bin found for " + capitalizeString(item.toLowerCase()));
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String[] args = event.getMessage().getContentRaw().split(" ", 2);

				logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

				if (args.length == 2) {
					ebMessage.editMessageEmbeds(getLowestBin(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
