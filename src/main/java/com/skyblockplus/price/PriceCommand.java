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

import static com.skyblockplus.features.mayor.MayorHandler.currentMayor;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class PriceCommand extends Command {

	public PriceCommand() {
		this.name = "price";
		this.cooldown = globalCooldown + 1;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder queryAuctions(String query, AuctionType auctionType) {
		if (currentMayor.equals("Derpy")) {
			return invalidEmbed("The price command does not work during Derpy");
		}

		JsonArray lowestBinArr = null;
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

		String matchedQuery = null;
		if (lowestBinArr == null) {
			String idStrict = nameToId(query, true);
			if (idStrict != null) {
				lowestBinArr = queryLowestBin(idStrict, false, auctionType);
			} else {
				String finalQuery = query;
				List<String> queryItems = getQueryItems();
				if (queryItems != null && queryItems.stream().noneMatch(q -> q.equalsIgnoreCase(finalQuery))) {
					matchedQuery = getClosestMatch(query, queryItems);
				}
				lowestBinArr = queryLowestBin(matchedQuery != null ? matchedQuery : query, true, auctionType);
			}

			if (lowestBinArr == null) {
				return invalidEmbed("Error fetching auctions data");
			}
		}

		if (lowestBinArr.size() == 0) {
			return invalidEmbed("No " + auctionType.getName() + " matching '" + query + "' found");
		}
		EmbedBuilder eb = defaultEmbed("Auction Searcher");
		if (matchedQuery != null) {
			eb.setDescription(
				"Searched for '" + matchedQuery + "' since no " + auctionType.getName() + " matching '" + query + "' were found"
			);
		}
		for (JsonElement auction : lowestBinArr) {
			String ahStr =
				"**Price:** " +
				roundAndFormat(higherDepth(auction, "starting_bid").getAsDouble()) +
				"\n**Rarity:** " +
				higherDepth(auction, "tier").getAsString().toLowerCase() +
				//	ahStr += "\n**Seller:** " + uuidToUsername(higherDepth(auction, "auctioneer").getAsString()).username();
				"\n**" +
				(higherDepth(auction, "bin", false) ? "Bin" : "Auction") +
				":** `/viewauction " +
				higherDepth(auction, "uuid").getAsString() +
				"`" +
				"\n**Ends:** <t:" +
				Instant.ofEpochMilli(higherDepth(auction, "end_t").getAsLong()).getEpochSecond() +
				":R>";

			eb.addField(
				getEmoji(higherDepth(auction, "item_id").getAsString()) + " " + higherDepth(auction, "item_name").getAsString(),
				ahStr,
				false
			);
		}

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

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

				setArgs(2);
				if (args.length == 2) {
					embed(queryAuctions(args[1], auctionType));
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
