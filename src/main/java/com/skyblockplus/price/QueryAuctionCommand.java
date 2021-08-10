package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class QueryAuctionCommand extends Command {

	public QueryAuctionCommand() {
		this.name = "query";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder queryAuctions(String query) {
		JsonArray lowestBinArr = null;
		String tempName = null;
		for (String enchantId : enchantNames) {
			if (query.replace(" ", "_").toUpperCase().contains(enchantId)) {
				int enchantLevel;
				try {
					enchantLevel = Integer.parseInt(query.replaceAll("\\D+", "").trim());
				} catch (NumberFormatException e) {
					enchantLevel = 1;
				}

				lowestBinArr = queryLowestBinEnchant(enchantId, enchantLevel);
				if (lowestBinArr == null) {
					return invalidEmbed("Error fetching auctions data");
				}
				tempName = idToName(enchantId + ";" + enchantLevel);
				break;
			}
		}

		if (lowestBinArr == null) {
			for (String pet : petNames) {
				if (query.replace(" ", "_").toUpperCase().contains(pet)) {
					query = query.toLowerCase();

					String rarity = "ANY";
					for (String rarityName : rarityToNumberMap.keySet()) {
						if (query.contains(rarity.toLowerCase())) {
							rarity = rarityName;
							query = query.replace(rarityName.toLowerCase(), "").trim().replaceAll("\\s+", " ");
						}
					}

					lowestBinArr = queryLowestBinPet(query, rarity);
					if (lowestBinArr == null) {
						return invalidEmbed("Error fetching auctions data");
					}
					break;
				}
			}
		}

		if (lowestBinArr == null) {
			lowestBinArr = queryLowestBin(query);
			if (lowestBinArr == null) {
				return invalidEmbed("Error fetching auctions data");
			}
		}

		if (lowestBinArr.size() == 0) {
			return invalidEmbed("No auctions matching '" + query + "' found");
		}

		JsonElement lowestBinAuction = lowestBinArr.get(0);
		EmbedBuilder eb = defaultEmbed("Query Auctions");
		Duration duration = Duration.between(Instant.now(), Instant.ofEpochMilli(higherDepth(lowestBinAuction, "end").getAsLong()));

		String lowestBinStr = "";
		lowestBinStr += "**Name:** " + (tempName == null ? higherDepth(lowestBinAuction, "item_name").getAsString() : tempName);
		lowestBinStr += "\n**Rarity:** " + higherDepth(lowestBinAuction, "tier").getAsString();
		lowestBinStr += "\n**Price:** " + simplifyNumber(higherDepth(lowestBinAuction, "starting_bid").getAsDouble());
		lowestBinStr += "\n**Seller:** " + uuidToUsername(higherDepth(lowestBinAuction, "auctioneer").getAsString()).playerUsername;
		lowestBinStr += "\n**Auction:** `/viewauction " + higherDepth(lowestBinAuction, "uuid").getAsString() + "`";
		lowestBinStr += "\n**Ends in:** " + instantToDHM(duration);

		String itemId = higherDepth(lowestBinAuction, "item_id").getAsString();
		if (itemId.equals("ENCHANTED_BOOK")) {
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
		} else if (itemId.equals("PET")) {
			eb.setThumbnail(
				getPetUrl(higherDepth(lowestBinAuction, "item_name").getAsString().split("] ")[1].toUpperCase().replace(" ", "_"))
			);
		} else {
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
		}

		eb.addField("Lowest Bin", lowestBinStr, false);

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				if (args.length == 2) {
					embed(queryAuctions(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
