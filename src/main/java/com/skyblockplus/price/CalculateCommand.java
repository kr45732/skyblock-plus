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

import static com.skyblockplus.utils.ApiHandler.getAuctionFromUuid;
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.networth.NetworthExecute;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.InvItem;
import java.time.Duration;
import java.time.Instant;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;

public class CalculateCommand extends Command {

	public CalculateCommand() {
		this.name = "calculate";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "calc" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder calculatePriceFromUuid(String auctionUuid) {
		HypixelResponse auctionResponse = getAuctionFromUuid(auctionUuid);

		if (auctionResponse.isNotValid()) {
			return invalidEmbed(auctionResponse.failCause());
		}

		JsonElement auction = auctionResponse.get("[0]");
		try {
			InvItem item = getGenericInventoryMap(NBTReader.readBase64(higherDepth(auction, "item_bytes.data").getAsString())).get(0);
			double price = new NetworthExecute().initPrices().calculateItemPrice(item);
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

			String ebStr = "**Item name:** " + itemName;
			ebStr += "\n**Seller:** " + uuidToUsername(higherDepth(auction, "auctioneer").getAsString()).username();
			ebStr += "\n**Command:** `/viewauction " + higherDepth(auction, "uuid").getAsString() + "`";
			long highestBid = higherDepth(auction, "highest_bid_amount", 0L);
			long startingBid = higherDepth(auction, "starting_bid", 0L);
			JsonArray bidsArr = higherDepth(auction, "bids").getAsJsonArray();
			boolean bin = higherDepth(auction, "bin") != null;

			if (duration.toMillis() > 0) {
				if (bin) {
					ebStr += "\n**BIN:** " + simplifyNumber(startingBid) + " coins | Ending <t:" + endingAt.getEpochSecond() + ":R>";
				} else {
					ebStr += "\n**Current bid:** " + simplifyNumber(highestBid) + " | Ending <t:" + endingAt.getEpochSecond() + ":R>";
					ebStr +=
						bidsArr.size() > 0
							? "\n**Highest bidder:** " +
							uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).username()
							: "";
				}
			} else {
				if (highestBid >= startingBid) {
					ebStr +=
						"\n**Auction sold** for " +
						simplifyNumber(highestBid) +
						" coins to " +
						uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).username();
				} else {
					ebStr = "\n**Auction did not sell**";
				}
			}

			return defaultEmbed("Auction Price Calculator")
				.setThumbnail("https://sky.shiiyu.moe/item.gif/" + item.getId())
				.setDescription(ebStr)
				.appendDescription("\n**Calculated Price:** " + roundAndFormat(price));
		} catch (Exception e) {
			e.printStackTrace();
			return defaultEmbed("Error parsing data");
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					embed(calculatePriceFromUuid(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
