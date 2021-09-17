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

import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class BidsCommand extends Command {

	public BidsCommand() {
		this.name = "bids";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getPlayerBids(String username) {
		return defaultEmbed("This command is temporarily disabled");
		//		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		//		if (usernameUuidStruct.isNotValid()) {
		//			return invalidEmbed(usernameUuidStruct.failCause);
		//		}
		//
		//		JsonArray bids = getBidsFromPlayer(usernameUuidStruct.playerUuid);
		//		if (bids == null || bids.size() == 0) {
		//			return defaultEmbed("No bids found for " + usernameUuidStruct.playerUsername);
		//		}
		//
		//		EmbedBuilder eb = defaultEmbed(
		//			usernameUuidStruct.playerUsername,
		//			"https://auctions.craftlink.xyz/players/" + usernameUuidStruct.playerUuid
		//		);
		//
		//		for (JsonElement bid : bids) {
		//			String auctionDesc;
		//			String itemName;
		//			boolean isPet = higherDepth(bid, "item_id").getAsString().equals("PET");
		//
		//			Instant endingAt = Instant.ofEpochMilli(higherDepth(bid, "end").getAsLong());
		//			Duration duration = Duration.between(Instant.now(), endingAt);
		//			String timeUntil = instantToDHM(duration);
		//
		//			if (higherDepth(bid, "item_name").getAsString().equals("Enchanted Book")) {
		//				itemName = parseMcCodes(higherDepth(bid, "item_lore").getAsString().split("\n")[0]);
		//			} else {
		//				itemName =
		//					(isPet ? capitalizeString(higherDepth(bid, "tier").getAsString().toLowerCase()) + " " : "") +
		//					higherDepth(bid, "item_name").getAsString();
		//			}
		//
		//			long highestBid = higherDepth(bid, "highest_bid_amount", 0L);
		//			JsonArray bidsArr = higherDepth(bid, "bids").getAsJsonArray();
		//			if (duration.toMillis() > 0) {
		//				auctionDesc = "Current bid: " + simplifyNumber(highestBid);
		//				auctionDesc += " | Ending in " + timeUntil;
		//				auctionDesc +=
		//					"\nHighest bidder: " +
		//					uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).playerUsername;
		//				for (int i = bidsArr.size() - 1; i >= 0; i--) {
		//					JsonElement curBid = bidsArr.get(i);
		//					if (higherDepth(curBid, "bidder").getAsString().equals(usernameUuidStruct.playerUuid)) {
		//						auctionDesc += "\nYour highest bid: " + simplifyNumber(higherDepth(curBid, "amount").getAsDouble());
		//						break;
		//					}
		//				}
		//			} else {
		//				auctionDesc = "Auction sold for " + simplifyNumber(highestBid) + " coins";
		//				auctionDesc +=
		//					"\n " +
		//					uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).playerUsername +
		//					" won the auction";
		//			}
		//
		//			eb.setThumbnail("https://cravatar.eu/helmavatar/" + usernameUuidStruct.playerUuid + "/64.png");
		//			eb.addField(itemName, auctionDesc, false);
		//		}
		//
		//		return eb;
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

					embed(getPlayerBids(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
