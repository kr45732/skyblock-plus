package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.text.similarity.LevenshteinDistance;

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

		if (higherDepth(averageAhJson, item) != null) {
			JsonElement itemJson = higherDepth(averageAhJson, item);
			EmbedBuilder eb;

			if (higherDepth(itemJson, "clean_price") != null) {
				eb = defaultEmbed("Average auction (clean)");
				eb.addField(capitalizeString(item.toLowerCase()), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
			} else {
				eb = defaultEmbed("Average auction");
				eb.addField(capitalizeString(item.toLowerCase()), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
			}

			eb.setThumbnail("https://sky.lea.moe/item.gif/" + item);
			return eb;
		}

		String internalName = convertToInternalName(item);

		if (higherDepth(averageAhJson, internalName) != null) {
			JsonElement itemJson = higherDepth(averageAhJson, internalName);
			EmbedBuilder eb;

			if (higherDepth(itemJson, "clean_price") != null) {
				eb = defaultEmbed("Average auction (clean)");
				eb.addField(capitalizeString(item.toLowerCase()), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
			} else {
				eb = defaultEmbed("Average auction");
				eb.addField(capitalizeString(item.toLowerCase()), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
			}

			eb.setThumbnail("https://sky.lea.moe/item.gif/" + internalName);
			return eb;
		}

		JsonElement enchantsJson = higherDepth(getEnchantsJson(), "enchants_min_level");

		List<String> enchantNames = enchantsJson
			.getAsJsonObject()
			.entrySet()
			.stream()
			.map(i -> i.getKey().toUpperCase())
			.collect(Collectors.toCollection(ArrayList::new));
		enchantNames.add("ULTIMATE_JERRY");

		Map<String, String> rarityMap = new HashMap<>();
		rarityMap.put("LEGENDARY", ";4");
		rarityMap.put("EPIC", ";3");
		rarityMap.put("RARE", ";2");
		rarityMap.put("UNCOMMON", ";1");
		rarityMap.put("COMMON", ";0");

		String formattedName;
		for (String i : enchantNames) {
			if (internalName.contains(i)) {
				String enchantName;
				try {
					int enchantLevel = Integer.parseInt(internalName.replaceAll("\\D+", ""));
					enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
					formattedName = i + ";" + enchantLevel;

					JsonElement itemJson = higherDepth(averageAhJson, formattedName);
					EmbedBuilder eb;

					if (higherDepth(itemJson, "clean_price") != null) {
						eb = defaultEmbed("Average auction (clean)");
						eb.addField(capitalizeString(enchantName), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
					} else {
						eb = defaultEmbed("Average auction");
						eb.addField(capitalizeString(enchantName), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
					}

					eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
					return eb;
				} catch (NumberFormatException e) {
					try {
						EmbedBuilder eb = defaultEmbed("Average auction");
						for (int j = 10; j > 0; j--) {
							try {
								formattedName = i + ";" + j;
								enchantName = i.toLowerCase().replace("_", " ") + " " + j;

								JsonElement itemJson = higherDepth(averageAhJson, formattedName);

								if (higherDepth(itemJson, "clean_price") != null) {
									eb.setTitle("Average auction (clean)");
									eb.addField(
										capitalizeString(enchantName),
										formatNumber(higherDepth(itemJson, "clean_price").getAsLong()),
										false
									);
								} else {
									eb.setTitle("Average auction");
									eb.addField(
										capitalizeString(enchantName),
										formatNumber(higherDepth(itemJson, "price").getAsLong()),
										false
									);
								}
							} catch (NullPointerException ignored) {}
						}
						if (eb.getFields().size() == 0) {
							return defaultEmbed("No auctions found for " + capitalizeString(item.toLowerCase()));
						}
						eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					} catch (NullPointerException ex) {
						return defaultEmbed("No auctions found for " + capitalizeString(item.toLowerCase()));
					}
				} catch (NullPointerException e) {
					return defaultEmbed("No auctions found for " + capitalizeString(item.toLowerCase()));
				}
			}
		}

		JsonElement petJson = getPetNumsJson();

		List<String> petNames = petJson
			.getAsJsonObject()
			.entrySet()
			.stream()
			.map(Map.Entry::getKey)
			.collect(Collectors.toCollection(ArrayList::new));

		for (String i : petNames) {
			if (internalName.contains(i)) {
				String petName = "";
				formattedName = i;
				boolean raritySpecified = false;
				for (Map.Entry<String, String> j : rarityMap.entrySet()) {
					if (internalName.contains(j.getKey())) {
						petName = j.getKey().toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
						formattedName += j.getValue();
						raritySpecified = true;
						break;
					}
				}

				if (!raritySpecified) {
					List<String> petRarities = higherDepth(petJson, formattedName)
						.getAsJsonObject()
						.entrySet()
						.stream()
						.map(j -> j.getKey().toUpperCase())
						.collect(Collectors.toCollection(ArrayList::new));

					for (String j : petRarities) {
						if (higherDepth(averageAhJson, formattedName + rarityMap.get(j)) != null) {
							petName = j.toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
							formattedName += rarityMap.get(j);
							break;
						}
					}
				}
				JsonElement itemJson = higherDepth(averageAhJson, formattedName);
				EmbedBuilder eb;

				if (higherDepth(itemJson, "clean_price") != null) {
					eb = defaultEmbed("Average auction (clean)");
					eb.addField(capitalizeString(petName + " pet"), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
				} else {
					eb = defaultEmbed("Average auction");
					eb.addField(capitalizeString(petName + " pet"), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
				}

				eb.setThumbnail(getPetUrl(formattedName.split(";")[0]));
				return eb;
			}
		}

		LevenshteinDistance matchCalc = LevenshteinDistance.getDefaultInstance();
		List<String> items = getJsonKeys(averageAhJson);
		int minDistance = matchCalc.apply(items.get(0), internalName);
		String closestMatch = items.get(0);
		for (String itemF : items) {
			int currentDistance = matchCalc.apply(itemF, internalName);
			if (currentDistance < minDistance) {
				minDistance = currentDistance;
				closestMatch = itemF;
			}
		}

		if (closestMatch != null && higherDepth(averageAhJson, closestMatch) != null) {
			EmbedBuilder eb = defaultEmbed("Average Auction");
			JsonElement itemJson = higherDepth(averageAhJson, closestMatch);

			if (enchantNames.contains(closestMatch.split(";")[0].trim())) {
				String itemName = closestMatch.toLowerCase().replace("_", " ").replace(";", " ");
				if (higherDepth(itemJson, "clean_price") != null) {
					eb = defaultEmbed("Average auction (clean)");
					eb.addField(capitalizeString(itemName), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
				} else {
					eb = defaultEmbed("Average auction");
					eb.addField(capitalizeString(itemName), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
				}

				eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
			} else if (petNames.contains(closestMatch.split(";")[0].trim())) {
				Map<String, String> rarityMapRev = new HashMap<>();
				rarityMapRev.put("4", "LEGENDARY");
				rarityMapRev.put("3", "EPIC");
				rarityMapRev.put("2", "RARE");
				rarityMapRev.put("1", "UNCOMMON");
				rarityMapRev.put("0", "COMMON");
				eb.setThumbnail(getPetUrl(closestMatch.split(";")[0]));
				String[] itemS = closestMatch.toLowerCase().replace("_", " ").split(";");

				if (higherDepth(itemJson, "clean_price") != null) {
					eb = defaultEmbed("Average auction (clean)");
					eb.addField(
						capitalizeString(rarityMapRev.get(itemS[1].toUpperCase()) + " " + itemS[0]),
						formatNumber(higherDepth(itemJson, "clean_price").getAsLong()),
						false
					);
				} else {
					eb = defaultEmbed("Average auction");
					eb.addField(
						capitalizeString(rarityMapRev.get(itemS[1].toUpperCase()) + " " + itemS[0]),
						formatNumber(higherDepth(itemJson, "price").getAsLong()),
						false
					);
				}
			} else {
				if (higherDepth(itemJson, "clean_price") != null) {
					eb = defaultEmbed("Average auction (clean)");
					eb.addField(
						capitalizeString(closestMatch.toLowerCase().replace("_", " ")),
						formatNumber(higherDepth(itemJson, "clean_price").getAsLong()),
						false
					);
				} else {
					eb = defaultEmbed("Average auction");
					eb.addField(
						capitalizeString(closestMatch.toLowerCase().replace("_", " ")),
						formatNumber(higherDepth(itemJson, "price").getAsLong()),
						false
					);
				}
				eb.setThumbnail("https://sky.lea.moe/item.gif/" + closestMatch);
			}

			return eb;
		}

		return defaultEmbed("No auctions found for " + capitalizeString(item.toLowerCase()));
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String args = event.getMessage().getContentRaw();

				logCommand(event.getGuild(), event.getAuthor(), args);

				if (args.split(" ").length >= 2) {
					ebMessage.editMessage(getAverageAuctionPrice(args.split(" ", 2)[1]).build()).queue();
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		)
			.start();
	}
}
