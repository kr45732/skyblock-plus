package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.ArrayList;
import java.util.List;
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
			return invalidEmbed("Error fetching auctions");
		}

		String itemId = nameToId(item);
		if (higherDepth(lowestBinJson, itemId) != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			eb.addField(idToName(itemId), formatNumber(higherDepth(lowestBinJson, itemId).getAsLong()), false);
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
			return eb;
		}

		for (String i : enchantNames) {
			if (itemId.contains(i)) {
				try {
					String enchantedBookId = i + ";" + Integer.parseInt(itemId.replaceAll("\\D+", ""));
					if (higherDepth(lowestBinJson, enchantedBookId) != null) {
						EmbedBuilder eb = defaultEmbed("Lowest bin");
						eb.addField(
							idToName(enchantedBookId),
							formatNumber(higherDepth(lowestBinJson, enchantedBookId).getAsLong()),
							false
						);
						eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					}
				} catch (NumberFormatException e) {
					EmbedBuilder eb = defaultEmbed("Lowest bin");
					for (int j = 10; j > 0; j--) {
						String enchantedBookId = i + ";" + j;
						if (higherDepth(lowestBinJson, enchantedBookId) != null) {
							eb.addField(
								idToName(enchantedBookId),
								formatNumber(higherDepth(lowestBinJson, enchantedBookId).getAsLong()),
								false
							);
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
		for (String i : petNames) {
			if (itemId.contains(i)) {
				String petId = i;
				boolean raritySpecified = false;
				for (Entry<String, String> j : rarityToNumberMap.entrySet()) {
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
						if (higherDepth(lowestBinJson, petId + rarityToNumberMap.get(j)) != null) {
							petId += rarityToNumberMap.get(j);
							break;
						}
					}
				}

				if (higherDepth(lowestBinJson, petId) != null) {
					EmbedBuilder eb = defaultEmbed("Lowest bin");
					eb.addField(idToName(petId), formatNumber(higherDepth(lowestBinJson, petId).getAsLong()), false);
					eb.setThumbnail(getPetUrl(petId.split(";")[0]));
					return eb;
				}
			}
		}

		String closestMatch = getClosestMatch(itemId, getJsonKeys(lowestBinJson));
		if (closestMatch != null) {
			EmbedBuilder eb = defaultEmbed("Lowest bin");
			if (enchantNames.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
			} else if (petNames.contains(closestMatch.split(";")[0].trim())) {
				eb.setThumbnail(getPetUrl(closestMatch.split(";")[0].trim()));
			} else {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + closestMatch);
			}

			eb.addField(idToName(closestMatch), formatNumber(higherDepth(lowestBinJson, closestMatch).getAsLong()), false);
			return eb;
		}

		return defaultEmbed("No bin found for " + idToName(item));
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
