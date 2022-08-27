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
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class BidsSlashCommand extends SlashCommand {

	public BidsSlashCommand() {
		this.name = "bids";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerBids(event.player, new PaginatorEvent(event)));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's bids")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getPlayerBids(String username, PaginatorEvent event) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (!usernameUuidStruct.isValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}

		JsonArray bids = getBidsFromPlayer(usernameUuidStruct.uuid());
		if (bids == null || bids.size() == 0) {
			return defaultEmbed("No bids found for " + usernameUuidStruct.username());
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(10);
		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS);

		for (JsonElement bid : bids) {
			String auctionDesc;
			String itemName = getEmoji(higherDepth(bid, "item_id").getAsString()) + " ";
			boolean isPet = higherDepth(bid, "item_id").getAsString().equals("PET");

			Instant endingAt = Instant.ofEpochMilli(higherDepth(bid, "end_t").getAsLong());
			Duration duration = Duration.between(Instant.now(), endingAt);

			itemName +=
				(isPet ? capitalizeString(higherDepth(bid, "tier").getAsString()) + " " : "") + higherDepth(bid, "item_name").getAsString();

			JsonArray bidsArr = higherDepth(bid, "bids").getAsJsonArray();
			long highestBid = higherDepth(bidsArr, "[" + (bidsArr.size() - 1) + "].amount").getAsLong();
			if (duration.toMillis() > 0) {
				auctionDesc = "Current bid: " + simplifyNumber(highestBid);
				auctionDesc += " | Ending <t:" + endingAt.getEpochSecond() + ":R>";
				auctionDesc +=
					"\nHighest bidder: " + uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).username();
				for (int i = bidsArr.size() - 1; i >= 0; i--) {
					JsonElement curBid = bidsArr.get(i);
					if (higherDepth(curBid, "bidder").getAsString().equals(usernameUuidStruct.uuid())) {
						auctionDesc += "\nYour highest bid: " + simplifyNumber(higherDepth(curBid, "amount").getAsDouble());
						break;
					}
				}
			} else {
				auctionDesc = "Auction sold for " + simplifyNumber(highestBid) + " coins";
				auctionDesc +=
					"\n " +
					uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).username() +
					" won the auction";
			}

			extras.addEmbedField(itemName, auctionDesc, false);
		}

		extras
			.setEveryPageTitle(usernameUuidStruct.username())
			.setEveryPageTitleUrl(skyblockStatsLink(usernameUuidStruct.username(), null))
			.setEveryPageThumbnail(usernameUuidStruct.getAvatarlUrl());

		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}
}
