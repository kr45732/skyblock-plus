/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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
import static com.skyblockplus.utils.ApiHandler.queryPet;
import static com.skyblockplus.utils.Constants.PET_NAMES;
import static com.skyblockplus.utils.Constants.RARITY_TO_NUMBER_MAP;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.ApiHandler;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.Comparator;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class PriceSlashCommand extends SlashCommand {

	public PriceSlashCommand() {
		this.name = "price";
	}

	public static EmbedBuilder queryAuctions(String query, AuctionType auctionType) {
		if (currentMayor.equals("Derpy")) {
			return errorEmbed("This command does not work during Derpy");
		}

		JsonArray auctionsArr = null;
		for (String pet : PET_NAMES) {
			if (query.replace(" ", "_").toUpperCase().contains(pet)) {
				String queryFmt = query.toLowerCase();

				String rarity = "ANY";
				for (String rarityName : RARITY_TO_NUMBER_MAP.keySet()) {
					if (queryFmt.contains(rarityName.toLowerCase())) {
						rarity = rarityName;
						queryFmt = queryFmt.replace(rarityName.toLowerCase(), "").trim().replaceAll("\\s+", " ");
						break;
					}
				}

				auctionsArr = queryPet(queryFmt, rarity, auctionType);
				if (auctionsArr == null) {
					return errorEmbed("Error fetching auctions");
				}
				break;
			}
		}

		String matchedQuery = null;
		if (auctionsArr == null || auctionsArr.isEmpty()) {
			String idStrict = nameToId(query, true);
			if (idStrict != null) {
				auctionsArr = ApiHandler.queryAuctions(idStrict, false, auctionType);
			} else {
				List<String> queryItems = getQueryItems();
				if (queryItems != null && queryItems.stream().noneMatch(q -> q.equalsIgnoreCase(query))) {
					matchedQuery = getClosestMatch(query, queryItems);
				}
				auctionsArr = ApiHandler.queryAuctions(matchedQuery != null ? matchedQuery : query, true, auctionType);
			}

			if (auctionsArr == null) {
				return errorEmbed("Error fetching auctions");
			}
		}

		if (auctionsArr.isEmpty()) {
			return errorEmbed("No " + auctionType.getName() + " matching '" + query + "' found");
		}

		EmbedBuilder eb = defaultEmbed("Auction Searcher (" + capitalizeString(auctionType.getName()) + ")");
		if (matchedQuery != null) {
			eb.setDescription(
				"Searched for '" + matchedQuery + "' since no " + auctionType.getName() + " matching '" + query + "' were found"
			);
		}

		for (JsonElement auction : streamJsonArray(auctionsArr)
			.sorted(
				Comparator
					.comparing((JsonElement e) -> higherDepth(e, "bin", false))
					.thenComparing(e -> Math.max(higherDepth(e, "starting_bid").getAsDouble(), higherDepth(e, "starting_bid").getAsDouble())
					)
			)
			.toList()) {
			boolean isBin = higherDepth(auction, "bin", false);
			double startingBid = higherDepth(auction, "starting_bid").getAsDouble();
			double highestBid = higherDepth(auction, "highest_bid").getAsDouble();
			int count = higherDepth(auction, "count", 1);

			String ahStr =
				"**Price:** " +
				roundAndFormat(Math.max(startingBid, highestBid)) +
				"\n**Rarity:** " +
				higherDepth(auction, "tier").getAsString().toLowerCase() +
				"\n**" +
				(isBin ? "Bin" : "Auction") +
				":** `/viewauction " +
				higherDepth(auction, "uuid").getAsString() +
				"`" +
				"\n**Ends:** " +
				getRelativeTimestamp(higherDepth(auction, "end_t").getAsLong());

			eb.addField(
				getEmoji(higherDepth(auction, "item_id").getAsString()) +
				" " +
				(count > 1 ? count + "x " : "") +
				higherDepth(auction, "item_name").getAsString(),
				ahStr,
				false
			);
		}

		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(
			queryAuctions(event.getOptionStr("item"), AuctionType.valueOf(event.getOptionStr("auction_type", "both").toUpperCase()))
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Query the auction house for the price of an item")
			.addOption(OptionType.STRING, "item", "Item name", true, true)
			.addOptions(
				new OptionData(OptionType.STRING, "auction_type", "Which type of auctions to show")
					.addChoice("Bin", "bin")
					.addChoice("Regular auctions", "auction")
					.addChoice("All auctions", "both")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), getQueryItems());
		}
	}

	public enum AuctionType {
		BIN,
		AUCTION,
		BOTH;

		public String getName() {
			return switch (this) {
				case BIN -> "bins";
				case AUCTION -> "auctions";
				case BOTH -> "bins and auctions";
			};
		}
	}
}
