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
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class BidsCommand extends Command {

	public BidsCommand() {
		this.name = "bids";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerBids(String username, PaginatorEvent event) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
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
			String itemName = getEmoji(higherDepth(bid, "item_id").getAsString()) +  " ";
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerBids(player, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
