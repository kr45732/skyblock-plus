package com.skyblockplus.price;

import static com.skyblockplus.Main.executor;
import static com.skyblockplus.networth.NetworthExecute.isIgnoredItem;
import static com.skyblockplus.networth.NetworthExecute.queryAhApi;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItem;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class PriceCommand extends Command {

	private static JsonElement lowestBinJson;
	private static JsonElement averageAuctionJson;
	private static JsonElement bazaarJson;
	private static JsonArray sbzPrices;

	public PriceCommand() {
		this.name = "price";
		this.cooldown = globalCooldown + 1;
	}

	public static EmbedBuilder calculatePriceFromUuid(String auctionUuid) {
		JsonArray auctionJson = higherDepth(
			getJson("https://api.hypixel.net/skyblock/auction?key=" + HYPIXEL_API_KEY + "&uuid=" + auctionUuid),
			"auctions"
		)
			.getAsJsonArray();
		if (auctionJson.size() == 0) {
			return defaultEmbed("Error").setDescription("Invalid auction UUID");
		}

		JsonElement auction = auctionJson.get(0);

		try {
			NBTCompound itemNBTRaw = NBTReader.readBase64(higherDepth(auction, "item_bytes.data").getAsString());
			InvItem item = Player.getGenericInventoryMap(itemNBTRaw).get(0);
			double price = calculateItemPrice(item);
			String itemName = higherDepth(auction, "item_name").getAsString();
			if (item.getId().equals("ENCHANTED_BOOK")) {
				itemName = parseMcCodes(higherDepth(auction, "item_lore").getAsString().split("\n")[0]);
			} else {
				itemName =
					(item.getId().equals("PET") ? capitalizeString(higherDepth(auction, "tier").getAsString().toLowerCase()) + " " : "") +
					itemName;
			}

			Instant endingAt = Instant.ofEpochMilli(higherDepth(auction, "end").getAsLong());
			Duration duration = Duration.between(Instant.now(), endingAt);
			String timeUntil = instantToDHM(duration);

			String ebStr = "**Item name:** " + itemName;
			ebStr += "\n**Seller:** " + uuidToUsername(higherDepth(auction, "auctioneer").getAsString());
			ebStr += "\n**Command:** `/ah " + higherDepth(auction, "uuid").getAsString() + "`";
			long highestBid = higherDepth(auction, "highest_bid_amount").getAsInt();
			long startingBid = higherDepth(auction, "starting_bid").getAsInt();
			JsonArray bidsArr = higherDepth(auction, "bids").getAsJsonArray();
			boolean bin = higherDepth(auction, "bin") != null;

			if (timeUntil.length() > 0) {
				if (bin) {
					ebStr += "\n**BIN:** " + simplifyNumber(startingBid) + " coins | Ending in " + timeUntil;
				} else {
					ebStr += "\n**Current bid:** " + simplifyNumber(highestBid) + " | Ending in " + timeUntil;
					ebStr +=
						bidsArr.size() > 0
							? "\n**Highest bidder:** " +
							uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString())
							: "";
				}
			} else {
				if (highestBid >= startingBid) {
					ebStr +=
						"\n**Auction sold** for " +
						simplifyNumber(highestBid) +
						" coins to " +
						uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString());
				} else {
					ebStr = "\n**Auction did not sell**";
				}
			}

			return defaultEmbed("Auction Price Calculator")
				.setThumbnail("https://sky.lea.moe/item.gif/" + item.getId())
				.setDescription(ebStr)
				.appendDescription("\n**Calculated Price:** " + roundAndFormat(price));
		} catch (Exception e) {
			e.printStackTrace();
			return defaultEmbed("Error parsing data");
		}
	}

	private static double calculateItemPrice(InvItem item) {
		if (item == null) {
			return 0;
		}

		lowestBinJson = getLowestBinJson();
		averageAuctionJson = getAverageAuctionJson();
		bazaarJson = getBazaarJson();
		sbzPrices = getSbzPricesJson();

		double itemCost = 0;
		double itemCount = 1;
		double recombobulatedExtra = 0;
		double hbpExtras = 0;
		double enchantsExtras = 0;
		double fumingExtras = 0;
		double reforgeExtras = 0;
		double miscExtras = 0;
		double backpackExtras = 0;

		try {
			if (item.getId().equals("PET")) {
				return calculatePetPrice(item);
			} else {
				itemCost = getLowestPrice(item.getId().toUpperCase());
			}
		} catch (Exception ignored) {}

		try {
			itemCount = item.getCount();
		} catch (Exception ignored) {}

		try {
			if (
				item.isRecombobulated() &&
				(
					itemCost *
					2 >=
					higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
						.getAsDouble()
				)
			) {
				recombobulatedExtra =
					higherDepth(higherDepth(bazaarJson, "RECOMBOBULATOR_3000.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
						.getAsDouble();
			}
		} catch (Exception ignored) {}

		try {
			hbpExtras =
				item.getHbpCount() *
				higherDepth(higherDepth(bazaarJson, "HOT_POTATO_BOOK.sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble();
		} catch (Exception ignored) {}

		try {
			fumingExtras =
				item.getFumingCount() *
				higherDepth(higherDepth(bazaarJson, "FUMING_POTATO_BOOK.sell_summary").getAsJsonArray().get(0), "pricePerUnit")
					.getAsDouble();
		} catch (Exception ignored) {}

		try {
			List<String> enchants = item.getEnchantsFormatted();
			for (String enchant : enchants) {
				try {
					if (item.getDungeonFloor() != -1 && enchant.equalsIgnoreCase("scavenger;5")) {
						continue;
					}

					enchantsExtras += getLowestPriceEnchant(enchant.toUpperCase());
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}

		try {
			reforgeExtras = calculateReforgePrice(item.getModifier(), item.getRarity());
		} catch (Exception ignored) {}

		try {
			List<String> extraStats = item.getExtraStats();
			for (String extraItem : extraStats) {
				miscExtras += getLowestPrice(extraItem);
			}
		} catch (Exception ignored) {}

		try {
			List<InvItem> backpackItems = item.getBackpackItems();
			for (InvItem backpackItem : backpackItems) {
				backpackExtras += calculateItemPrice(backpackItem);
			}
		} catch (Exception ignored) {}

		return (
			itemCount *
			(itemCost + recombobulatedExtra + hbpExtras + enchantsExtras + fumingExtras + reforgeExtras + miscExtras + backpackExtras)
		);
	}

	private static double calculateReforgePrice(String reforgeName, String itemRarity) {
		JsonElement reforgesStonesJson = getReforgeStonesJson();
		List<String> reforgeStones = getJsonKeys(reforgesStonesJson);

		for (String reforgeStone : reforgeStones) {
			JsonElement reforgeStoneInfo = higherDepth(reforgesStonesJson, reforgeStone);
			if (higherDepth(reforgeStoneInfo, "reforgeName").getAsString().equalsIgnoreCase(reforgeName)) {
				String reforgeStoneName = higherDepth(reforgeStoneInfo, "internalName").getAsString();
				double reforgeStoneCost = getLowestPrice(reforgeStoneName);
				double reforgeApplyCost = higherDepth(reforgeStoneInfo, "reforgeCosts." + itemRarity.toUpperCase()).getAsDouble();
				return reforgeStoneCost + reforgeApplyCost;
			}
		}

		return 0;
	}

	private static double calculatePetPrice(InvItem pet) {
		String queryStr = "\"" + capitalizeString(pet.getName()).replace("lvl", "Lvl") + "\"";

		JsonArray ahQuery = queryAhApi(queryStr);

		if (ahQuery != null) {
			for (JsonElement auction : ahQuery) {
				String auctionName = higherDepth(auction, "item_name").getAsString();
				double auctionPrice = higherDepth(auction, "starting_bid").getAsDouble();
				String auctionRarity = higherDepth(auction, "tier").getAsString();

				if (pet.getName().equalsIgnoreCase(auctionName) && pet.getRarity().equalsIgnoreCase(auctionRarity)) {
					double miscExtras = 0;
					try {
						List<String> extraStats = pet.getExtraStats();
						for (String extraItem : extraStats) {
							miscExtras += getLowestPrice(extraItem);
						}
					} catch (Exception ignored) {}

					return auctionPrice + miscExtras;
				}
			}
		}

		Map<String, String> rarityMap = new HashMap<>();
		rarityMap.put("LEGENDARY", ";4");
		rarityMap.put("EPIC", ";3");
		rarityMap.put("RARE", ";2");
		rarityMap.put("UNCOMMON", ";1");
		rarityMap.put("COMMON", ";0");

		try {
			double auctionPrice = higherDepth(
				lowestBinJson,
				pet.getName().split("] ")[1].toLowerCase().trim() + rarityMap.get(pet.getRarity())
			)
				.getAsDouble();

			double miscExtras = 0;
			try {
				List<String> extraStats = pet.getExtraStats();
				for (String extraItem : extraStats) {
					miscExtras += getLowestPrice(extraItem);
				}
			} catch (Exception ignored) {}

			return auctionPrice + miscExtras;
		} catch (Exception ignored) {}
		return 0;
	}

	private static double getLowestPriceEnchant(String enchantId) {
		double lowestBin = -1;
		double averageAuction = -1;
		String enchantName = enchantId.split(";")[0];
		int enchantLevel = Integer.parseInt(enchantId.split(";")[1]);

		if (
			enchantName.equalsIgnoreCase("compact") ||
			enchantName.equalsIgnoreCase("expertise") ||
			enchantName.equalsIgnoreCase("cultivating")
		) {
			enchantLevel = 1;
		}

		for (int i = enchantLevel; i >= 1; i--) {
			try {
				lowestBin = higherDepth(lowestBinJson, enchantName + ";" + i).getAsDouble();
			} catch (Exception ignored) {}

			try {
				JsonElement avgInfo = higherDepth(averageAuctionJson, enchantName + ";" + i);
				averageAuction =
					higherDepth(avgInfo, "clean_price") != null
						? higherDepth(avgInfo, "clean_price").getAsDouble()
						: higherDepth(avgInfo, "price").getAsDouble();
			} catch (Exception ignored) {}

			if (lowestBin == -1 && averageAuction != -1) {
				return Math.pow(2, enchantLevel - i) * averageAuction;
			} else if (lowestBin != -1 && averageAuction == -1) {
				return Math.pow(2, enchantLevel - i) * lowestBin;
			} else if (lowestBin != -1 && averageAuction != -1) {
				return (Math.pow(2, enchantLevel - i) * Math.min(lowestBin, averageAuction));
			}
		}

		if (higherDepth(sbzPrices, enchantName + "_1") != null) {
			return (Math.pow(2, enchantLevel - 1) * higherDepth(sbzPrices, enchantName + "_1").getAsDouble());
		}

		if (higherDepth(sbzPrices, enchantName + "_i") != null) {
			return (Math.pow(2, enchantLevel - 1) * higherDepth(sbzPrices, enchantName + "_i").getAsDouble());
		}
		return 0;
	}

	private static double getLowestPrice(String itemId) {
		if (isIgnoredItem(itemId)) {
			return 0;
		}

		double lowestBin = -1;
		double averageAuction = -1;

		try {
			lowestBin = higherDepth(lowestBinJson, itemId).getAsDouble();
		} catch (Exception ignored) {}

		try {
			JsonElement avgInfo = higherDepth(averageAuctionJson, itemId);
			averageAuction =
				higherDepth(avgInfo, "clean_price") != null
					? higherDepth(avgInfo, "clean_price").getAsDouble()
					: higherDepth(avgInfo, "price").getAsDouble();
		} catch (Exception ignored) {}

		if (lowestBin == -1 && averageAuction != -1) {
			return averageAuction;
		} else if (lowestBin != -1 && averageAuction == -1) {
			return lowestBin;
		} else if (lowestBin != -1 && averageAuction != -1) {
			return Math.min(lowestBin, averageAuction);
		}

		try {
			return higherDepth(higherDepth(bazaarJson, itemId + "sell_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble();
		} catch (Exception ignored) {}

		try {
			itemId = itemId.toLowerCase();
			if (itemId.contains("generator")) {
				String minionName = itemId.split("_generator_")[0];
				int level = Integer.parseInt(itemId.split("_generator_")[1]);

				itemId = minionName + "_minion_" + toRomanNumerals(level);
			} else if (itemId.equals("magic_mushroom_soup")) {
				itemId = "magical_mushroom_soup";
			} else if (itemId.startsWith("theoretical_hoe_")) {
				String parseHoe = itemId.split("theoretical_hoe_")[1];
				String hoeType = parseHoe.split("_")[0];
				int hoeLevel = Integer.parseInt(parseHoe.split("_")[1]);

				for (JsonElement itemPrice : sbzPrices) {
					String itemNamePrice = higherDepth(itemPrice, "name").getAsString();
					if (itemNamePrice.startsWith("tier_" + hoeLevel) && itemNamePrice.endsWith(hoeType + "_hoe")) {
						return higherDepth(itemPrice, "low").getAsDouble();
					}
				}
			} else if (itemId.equals("mine_talisman")) {
				itemId = "mine_affinity_talisman";
			} else if (itemId.equals("village_talisman")) {
				itemId = "village_affinity_talisman";
			} else if (itemId.equals("coin_talisman")) {
				itemId = "talisman_of_coins";
			} else if (itemId.equals("melody_hair")) {
				itemId = "melodys_hair";
			} else if (itemId.equals("theoretical_hoe")) {
				itemId = "mathematical_hoe_blueprint";
			} else if (itemId.equals("dctr_space_helm")) {
				itemId = "dctrs_space_helmet";
			}

			for (JsonElement itemPrice : sbzPrices) {
				if (higherDepth(itemPrice, "name").getAsString().equalsIgnoreCase(itemId)) {
					return higherDepth(itemPrice, "low").getAsDouble();
				}
			}
		} catch (Exception ignored) {}
		return 0;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					eb = calculatePriceFromUuid(args[1]);

					ebMessage.editMessage(eb.build()).queue();
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
