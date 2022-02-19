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

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class PriceCommand extends Command {

	public PriceCommand() {
		this.name = "price";
		this.cooldown = globalCooldown + 1;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder queryAuctions(String query, AuctionType auctionType, PaginatorEvent event) {
		JsonArray lowestBinArr = null;
		String tempName = null;
		for (String enchantId : ENCHANT_NAMES) {
			if (query.replace(" ", "_").toUpperCase().contains(enchantId)) {
				int enchantLevel;
				try {
					enchantLevel = Integer.parseInt(query.replaceAll("\\D+", "").trim());
				} catch (NumberFormatException e) {
					enchantLevel = 1;
				}

				lowestBinArr = queryLowestBinEnchant(enchantId, enchantLevel, auctionType);
				if (lowestBinArr == null) {
					return invalidEmbed("Error fetching auctions data");
				}
				tempName = idToName(enchantId + ";" + enchantLevel);
				break;
			}
		}

		if (lowestBinArr == null) {
			for (String pet : PET_NAMES) {
				if (query.replace(" ", "_").toUpperCase().contains(pet)) {
					query = query.toLowerCase();

					String rarity = "ANY";
					for (String rarityName : RARITY_TO_NUMBER_MAP.keySet()) {
						if (query.contains(rarityName.toLowerCase())) {
							rarity = rarityName;
							query = query.replace(rarityName.toLowerCase(), "").trim().replaceAll("\\s+", " ");
							break;
						}
					}

					lowestBinArr = queryLowestBinPet(query, rarity, auctionType);
					if (lowestBinArr == null) {
						return invalidEmbed("Error fetching auctions data");
					}
					break;
				}
			}
		}

		if (lowestBinArr == null) {
			String finalQuery = query;
			if(getQueryItems().stream().noneMatch(q -> q.equalsIgnoreCase(finalQuery))) {
				query = getClosestMatch(query, getQueryItems());
			}
			lowestBinArr = queryLowestBin(query, auctionType);
			if (lowestBinArr == null) {
				return invalidEmbed("Error fetching auctions data");
			}
		}



		if (lowestBinArr.size() == 0) {

			return invalidEmbed("No " + auctionType.getName() + " matching '" + query + "' found");
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(5);
		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS);
		for (JsonElement lowestBinAuction : lowestBinArr) {
			EmbedBuilder eb = defaultEmbed("Query Auctions");

			String lowestBinStr = "";
			lowestBinStr += "**Name:** " + (tempName == null ? higherDepth(lowestBinAuction, "item_name").getAsString() : tempName);
			lowestBinStr += "\n**Rarity:** " + higherDepth(lowestBinAuction, "tier").getAsString().toLowerCase();
			lowestBinStr += "\n**Price:** " + simplifyNumber(higherDepth(lowestBinAuction, "starting_bid").getAsDouble());
			//			lowestBinStr += "\n**Seller:** " + uuidToUsername(higherDepth(lowestBinAuction, "auctioneer").getAsString()).username();
			lowestBinStr += "\n**Auction:** `/viewauction " + higherDepth(lowestBinAuction, "uuid").getAsString() + "`";
			lowestBinStr +=
				"\n**Ends:** <t:" + Instant.ofEpochMilli(higherDepth(lowestBinAuction, "end_t").getAsLong()).getEpochSecond() + ":R>";

			String itemId = higherDepth(lowestBinAuction, "item_id").getAsString();
			if (itemId.equals("PET")) {
				if (!higherDepth(lowestBinAuction, "item_name").getAsString().startsWith("Mystery ")) {
					eb.setThumbnail(
						getPetUrl(higherDepth(lowestBinAuction, "item_name").getAsString().split("] ")[1].toUpperCase().replace(" ", "_"))
					);
				}
			} else {
				eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
			}

			extras.addEmbedField("Lowest Bin", lowestBinStr, false);
		}

		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				AuctionType auctionType = AuctionType.BIN;
				for (int i = 0; i < args.length; i++) {
					if (args[i].startsWith("type:")) {
						try {
							auctionType = AuctionType.valueOf(args[i].split("type:")[1].toUpperCase());
							removeArg(i);
						} catch (IllegalArgumentException e) {
							embed(invalidEmbed("Invalid auction type provided"));
							return;
						}
					}
				}
				if (args.length == 2) {
					paginate(queryAuctions(args[1], auctionType, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}

	public enum AuctionType {
		BIN,
		AUCTION,
		BOTH;

		public String getName() {
			return switch (this) {
				case BIN -> "bins";
				case AUCTION -> "auctions";
				case BOTH -> "bins or auctions";
			};
		}
	}
}
