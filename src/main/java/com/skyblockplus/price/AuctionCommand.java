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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.networth.NetworthExecute;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

public class AuctionCommand extends Command {

	public AuctionCommand() {
		this.name = "auctions";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "ah", "auction" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerAuction(
		String username,
		AuctionFilterType filterType,
		AuctionSortType sortType,
		boolean verbose,
		PaginatorEvent event
	) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.getFailCause());
		}

		HypixelResponse auctionsResponse = getAuctionFromPlayer(usernameUuidStruct.getUuid());
		if (auctionsResponse.isNotValid()) {
			return invalidEmbed(auctionsResponse.getFailCause());
		}

		JsonArray auctionsArray = auctionsResponse.getResponse().getAsJsonArray();
		Stream<JsonElement> stream = streamJsonArray(auctionsArray);
		if (filterType == AuctionFilterType.SOLD || filterType == AuctionFilterType.UNSOLD) {
			stream =
				stream.filter(auction ->
					(filterType == AuctionFilterType.SOLD) ==
					(higherDepth(auction, "highest_bid_amount", 0) >= higherDepth(auction, "starting_bid", 0))
				);
		}
		if (sortType == AuctionSortType.LOW || sortType == AuctionSortType.HIGH) {
			stream =
				stream.sorted(
					Comparator.comparingLong(auction ->
						(sortType == AuctionSortType.LOW ? 1 : -1) *
						Math.max(higherDepth(auction, "highest_bid_amount", 0L), higherDepth(auction, "starting_bid", 0))
					)
				);
		}
		auctionsArray = collectJsonArray(stream);

		long totalSoldValue = 0;
		long totalPendingValue = 0;
		long failedToSell = 0;

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(10);
		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS);

		NetworthExecute calc = null;
		if (verbose) {
			calc = new NetworthExecute().initPrices().setVerbose(true);
			paginateBuilder.setItemsPerPage(7);
		}

		for (int i = 0; i < auctionsArray.size(); i++) {
			JsonElement currentAuction = auctionsArray.get(i);
			if (!higherDepth(currentAuction, "claimed").getAsBoolean()) {
				String auctionName;
				String auction;
				boolean isPet = higherDepth(currentAuction, "item_lore").getAsString().toLowerCase().contains("pet");
				boolean bin = higherDepth(currentAuction, "bin", false);

				Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
				Duration duration = Duration.between(Instant.now(), endingAt);
				String timeUntil = instantToDHM(duration);

				if (higherDepth(currentAuction, "item_name").getAsString().equals("Enchanted Book")) {
					auctionName = parseMcCodes(higherDepth(currentAuction, "item_lore").getAsString().split("\n")[0]);
				} else {
					auctionName =
						(isPet ? capitalizeString(higherDepth(currentAuction, "tier").getAsString().toLowerCase()) + " " : "") +
						higherDepth(currentAuction, "item_name").getAsString();
				}

				long highestBid = higherDepth(currentAuction, "highest_bid_amount", 0);
				long startingBid = higherDepth(currentAuction, "starting_bid", 0);
				if (duration.toMillis() > 0) {
					if (bin) {
						auction = "BIN: " + simplifyNumber(startingBid) + " coins";
						totalPendingValue += startingBid;
					} else {
						auction = "Current bid: " + simplifyNumber(highestBid);
						totalPendingValue += highestBid;
					}
					auction += " | Ending in " + timeUntil;
				} else {
					if (highestBid >= startingBid) {
						auction = "Auction sold for " + simplifyNumber(highestBid) + " coins";
						totalSoldValue += highestBid;
					} else {
						auction = "Auction did not sell";
						failedToSell += startingBid;
					}
				}
				if (verbose) {
					String estimatedPrice = "error calculating";
					try {
						estimatedPrice =
							formatNumber(
								calc.calculateItemPrice(
									getGenericInventoryMap(
										NBTReader.readBase64(higherDepth(currentAuction, "item_bytes.data").getAsString())
									)
										.get(0)
								)
							);
					} catch (Exception ignored) {}
					auction +=
						"\nEstimated value: " +
						estimatedPrice +
						"\nCommand: `/viewauction " +
						higherDepth(currentAuction, "uuid").getAsString() +
						"`";
				}

				extras.addEmbedField(auctionName, auction, false);
			}
		}

		if (extras.getEmbedFields().size() == 0) {
			return invalidEmbed("No auctions found for " + usernameUuidStruct.getUsername());
		}

		extras
			.setEveryPageTitle(usernameUuidStruct.getUsername())
			.setEveryPageTitleUrl(skyblockStatsLink(usernameUuidStruct.getUsername(), null))
			.setEveryPageThumbnail(usernameUuidStruct.getAvatarlUrl())
			.setEveryPageText(
				(totalSoldValue > 0 ? "**Sold Auctions Value:** " + simplifyNumber(totalSoldValue) : "") +
				(totalPendingValue > 0 ? "\n**Unsold Auctions Value:** " + simplifyNumber(totalPendingValue) : "") +
				(failedToSell > 0 ? "\n**Did Not Sell Auctions Value:** " + simplifyNumber(failedToSell) : "")
					+ (verbose ? "\n**Verbose JSON:** " + makeHastePost(formattedGson.toJson(calc.getVerboseJson())) + ".json": "")
			);

		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}

	public static EmbedBuilder getAuctionByUuid(String auctionUuid) {
		HypixelResponse auctionResponse = getAuctionFromUuid(auctionUuid);
		if (auctionResponse.isNotValid()) {
			return invalidEmbed(auctionResponse.getFailCause());
		}

		JsonElement auctionJson = auctionResponse.get("[0]");
		EmbedBuilder eb = defaultEmbed("Auction from UUID");
		String itemName = higherDepth(auctionJson, "item_name").getAsString();

		String itemId = "None";
		try {
			itemId =
				NBTReader
					.readBase64(higherDepth(auctionJson, "item_bytes").getAsString())
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
		String timeUntil = instantToDHM(duration);

		String ebStr = "**Item name:** " + itemName;
		ebStr += "\n**Seller:** " + uuidToUsername(higherDepth(auctionJson, "auctioneer").getAsString()).getUsername();
		ebStr += "\n**Command:** `/viewauction " + higherDepth(auctionJson, "uuid").getAsString() + "`";
		long highestBid = higherDepth(auctionJson, "highest_bid_amount", 0L);
		long startingBid = higherDepth(auctionJson, "starting_bid", 0L);
		JsonArray bidsArr = higherDepth(auctionJson, "bids").getAsJsonArray();
		boolean bin = higherDepth(auctionJson, "bin") != null;

		if (duration.toMillis() > 0) {
			if (bin) {
				ebStr += "\n**BIN:** " + simplifyNumber(startingBid) + " coins | Ending in " + timeUntil;
			} else {
				ebStr += "\n**Current bid:** " + simplifyNumber(highestBid) + " | Ending in " + timeUntil;
				ebStr +=
					bidsArr.size() > 0
						? "\n**Highest bidder:** " +
						uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).getUsername()
						: "";
			}
		} else {
			if (highestBid >= startingBid) {
				ebStr +=
					"\n**Auction sold** for " +
					simplifyNumber(highestBid) +
					" coins to " +
					uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).getUsername();
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

				if (args.length == 3 && args[1].equals("uuid")) {
					embed(getAuctionByUuid(args[2]));
					return;
				} else if (args.length == 4 || args.length == 3 || args.length == 2 || args.length == 1) {
					AuctionFilterType filterType = AuctionFilterType.NONE;
					for (int i = 0; i < args.length; i++) {
						if (args[i].startsWith("filter:")) {
							try {
								filterType = AuctionFilterType.valueOf(args[i].split("filter:")[1].toUpperCase(Locale.ROOT));
								removeArg(i);
							} catch (IllegalArgumentException e) {
								embed(invalidEmbed("Invalid filter type provided"));
								return;
							}
						}
					}
					AuctionSortType sortType = AuctionSortType.NONE;
					for (int i = 0; i < args.length; i++) {
						if (args[i].startsWith("sort:")) {
							try {
								sortType = AuctionSortType.valueOf(args[i].split("sort:")[1].toUpperCase());
								removeArg(i);
							} catch (IllegalArgumentException e) {
								embed(invalidEmbed("Invalid sort type provided"));
								return;
							}
						}
					}
					boolean verbose = false;
					for (int i = 0; i < args.length; i++) {
						if (args[i].equals("--verbose")) {
							verbose = true;
							removeArg(i);
						}
					}

					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerAuction(username, filterType, sortType, verbose, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public enum AuctionFilterType {
		NONE,
		SOLD,
		UNSOLD,
	}

	public enum AuctionSortType {
		NONE,
		LOW,
		HIGH,
	}
}
