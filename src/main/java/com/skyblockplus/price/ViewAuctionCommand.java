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
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Duration;
import java.time.Instant;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class ViewAuctionCommand extends Command {

	public ViewAuctionCommand() {
		this.name = "viewauction";
		this.aliases = new String[] { "viewah" };
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getAuctionByUuid(String auctionUuid) {
		HypixelResponse auctionResponse = getAuctionFromUuid(auctionUuid);
		if (auctionResponse.isNotValid()) {
			return invalidEmbed(auctionResponse.failCause());
		}

		JsonElement auctionJson = auctionResponse.get("[0]");
		EmbedBuilder eb = defaultEmbed("Auction from UUID");
		String itemName = higherDepth(auctionJson, "item_name").getAsString();

		String itemId = "None";
		try {
			itemId =
				NBTReader
					.readBase64(higherDepth(auctionJson, "item_bytes.data").getAsString())
					.getList("i")
					.getCompound(0)
					.getString("tag.ExtraAttributes.id", "None");
		} catch (Exception ignored) {}

		if (itemId.equals("ENCHANTED_BOOK")) {
			itemName = parseMcCodes(higherDepth(auctionJson, "item_lore").getAsString().split("\n")[0]);
		} else {
			itemName =
				(itemId.equals("PET") ? capitalizeString(higherDepth(auctionJson, "tier").getAsString().toLowerCase()) + " " : "") +
				itemName;
		}

		Instant endingAt = Instant.ofEpochMilli(higherDepth(auctionJson, "end").getAsLong());
		Duration duration = Duration.between(Instant.now(), endingAt);

		String ebStr = "**Item name:** " + itemName;
		ebStr += "\n**Seller:** " + uuidToUsername(higherDepth(auctionJson, "auctioneer").getAsString()).username();
		ebStr += "\n**Command:** `/viewauction " + higherDepth(auctionJson, "uuid").getAsString() + "`";
		long highestBid = higherDepth(auctionJson, "highest_bid_amount", 0L);
		long startingBid = higherDepth(auctionJson, "starting_bid", 0L);
		JsonArray bidsArr = higherDepth(auctionJson, "bids").getAsJsonArray();
		boolean bin = higherDepth(auctionJson, "bin", false);

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

		eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
		return eb.setDescription(ebStr);
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					embed(getAuctionByUuid(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
