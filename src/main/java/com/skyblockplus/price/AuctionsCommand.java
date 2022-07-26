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
import static com.skyblockplus.utils.Constants.RARITY_TO_NUMBER_MAP;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class AuctionsCommand extends Command {

	public AuctionsCommand() {
		this.name = "auctions";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "ah", "auction" };
		this.botPermissions = defaultPerms();
	}

	public static Object getPlayerAuction(
		String username,
		AuctionFilterType filterType,
		AuctionSortType sortType,
		boolean verbose,
		PaginatorEvent event
	) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (!usernameUuidStruct.isValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}

		HypixelResponse auctionsResponse = getAuctionFromPlayer(usernameUuidStruct.uuid());
		if (!auctionsResponse.isValid()) {
			return invalidEmbed(auctionsResponse.failCause());
		}

		JsonArray auctionsArray = auctionsResponse.response().getAsJsonArray();
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

		if (!verbose) {
			long totalSoldValue = 0;
			long totalPendingValue = 0;
			long failedToSell = 0;
			long auctionTax = 0;

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(9);
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS);

			for (JsonElement currentAuction : auctionsArray) {
				if (!higherDepth(currentAuction, "claimed", false)) {
					InvItem item = nbtToItem(higherDepth(currentAuction, "item_bytes.data").getAsString());

					String aucTitle =
						getEmoji(
							item.getId().equals("PET")
								? item.getName().split("] ")[1].toUpperCase().replace(" ", "_") + RARITY_TO_NUMBER_MAP.get(item.getRarity())
								: item.getId()
						) +
						" ";
					if (item.getId().equals("ENCHANTED_BOOK")) {
						aucTitle += parseMcCodes(higherDepth(currentAuction, "item_lore").getAsString().split("\n")[0]);
					} else {
						aucTitle +=
							(
								item.getId().equals("PET")
									? capitalizeString(higherDepth(currentAuction, "tier").getAsString().toLowerCase()) + " "
									: (item.getCount() > 1 ? item.getCount() + "x " : "")
							) +
							higherDepth(currentAuction, "item_name").getAsString();
					}

					String desc;
					Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
					Duration duration = Duration.between(Instant.now(), endingAt);
					long highestBid = higherDepth(currentAuction, "highest_bid_amount", 0);
					long startingBid = higherDepth(currentAuction, "starting_bid", 0);
					if (duration.toMillis() > 0) {
						if (higherDepth(currentAuction, "bin", false)) {
							desc = "BIN: " + simplifyNumber(startingBid) + " coins";
							totalPendingValue += startingBid;
						} else {
							desc = "Current bid: " + simplifyNumber(highestBid);
							totalPendingValue += highestBid;
						}
						desc += " | Ending <t:" + endingAt.getEpochSecond() + ":R>";
					} else {
						if (highestBid >= startingBid) {
							desc = "Auction sold for " + simplifyNumber(highestBid) + " coins";
							totalSoldValue += highestBid;
							auctionTax +=
								(highestBid > 1000000)
									? ((0.99 * highestBid < 1000000) ? (highestBid - 1000000) : (long) (0.01 * highestBid))
									: 0;
						} else {
							desc = "Auction did not sell";
							failedToSell += startingBid;
						}
					}

					extras.addEmbedField(aucTitle, desc, false);
				}
			}

			UsernameUuidStruct curTrack = AuctionTracker.commandAuthorToTrackingUser.getOrDefault(event.getUser().getId(), null);
			Button button;
			if (curTrack != null && curTrack.uuid().equals(usernameUuidStruct.uuid())) {
				button =
					Button.primary(
						"track_auctions_stop_" + event.getUser().getId() + "_" + usernameUuidStruct.uuid(),
						"Stop Tracking Auctions"
					);
			} else {
				button =
					Button.primary("track_auctions_start_" + event.getUser().getId() + "_" + usernameUuidStruct.uuid(), "Track Auctions");
			}
			if (extras.getEmbedFields().size() == 0) {
				return new MessageBuilder()
					.setEmbeds(invalidEmbed("No auctions found for " + usernameUuidStruct.usernameFixed()).build())
					.setActionRows(ActionRow.of(button));
			}

			extras
				.setEveryPageTitle(usernameUuidStruct.usernameFixed())
				.setEveryPageTitleUrl(usernameUuidStruct.getAuctionUrl())
				.setEveryPageThumbnail(usernameUuidStruct.getAvatarlUrl())
				.setEveryPageText(
					(
						totalSoldValue > 0
							? "**Sold Auctions Value:** " +
							simplifyNumber(totalSoldValue) +
							(auctionTax > 0 ? " - " + simplifyNumber(auctionTax) + " = " + simplifyNumber(totalSoldValue - auctionTax) : "")
							: ""
					) +
					(totalPendingValue > 0 ? "\n**Unsold Auctions Value:** " + simplifyNumber(totalPendingValue) : "") +
					(failedToSell > 0 ? "\n**Did Not Sell Auctions Value:** " + simplifyNumber(failedToSell) : "")
				)
				.addButton(button);

			event.paginate(paginateBuilder.setPaginatorExtras(extras));
		} else {
			CustomPaginator.Builder paginateBuilder = event.getPaginator();
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

			long totalSoldValue = 0;
			long totalPendingValue = 0;
			long failedToSell = 0;
			long auctionTax = 0;

			NetworthExecute calc = new NetworthExecute().initPrices().setVerbose(true);

			for (JsonElement currentAuction : auctionsArray) {
				EmbedBuilder eb = defaultEmbed(usernameUuidStruct.usernameFixed(), usernameUuidStruct.getAuctionUrl());
				if (!higherDepth(currentAuction, "claimed").getAsBoolean()) {
					String auctionName;

					boolean isPet = higherDepth(currentAuction, "item_lore").getAsString().toLowerCase().contains("pet");
					boolean bin = higherDepth(currentAuction, "bin", false);

					Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
					Duration duration = Duration.between(Instant.now(), endingAt);

					if (higherDepth(currentAuction, "item_name").getAsString().equals("Enchanted Book")) {
						auctionName = parseMcCodes(higherDepth(currentAuction, "item_lore").getAsString().split("\n")[0]);
					} else {
						auctionName =
							(isPet ? capitalizeString(higherDepth(currentAuction, "tier").getAsString().toLowerCase()) + " " : "") +
							higherDepth(currentAuction, "item_name").getAsString();
					}
					eb.addField("Item Name", auctionName, false);

					long highestBid = higherDepth(currentAuction, "highest_bid_amount", 0);
					long startingBid = higherDepth(currentAuction, "starting_bid", 0);
					String auction;
					long aucTax;
					if (duration.toMillis() > 0) {
						if (bin) {
							auction = "BIN: " + simplifyNumber(startingBid) + " coins";
							totalPendingValue += startingBid;
						} else {
							auction = "Current bid: " + simplifyNumber(highestBid) + " coins";
							totalPendingValue += highestBid;
						}
						auction += " | Ending <t:" + endingAt.getEpochSecond() + ":R>";
						eb.addField("Status", auction, false);
						eb.addField("Command", "`/viewauction " + higherDepth(currentAuction, "uuid").getAsString() + "`", false);
					} else {
						if (highestBid >= startingBid) {
							totalSoldValue += highestBid;
							aucTax =
								(highestBid > 1000000)
									? ((0.99 * highestBid < 1000000) ? (highestBid - 1000000) : (long) (0.01 * highestBid))
									: 0;
							auctionTax += aucTax;
							eb.addField(
								"Status",
								"Sold for " +
								formatNumber(highestBid) +
								(aucTax > 0 ? " - " + formatNumber(aucTax) + " = " + formatNumber(highestBid - aucTax) : "") +
								" coins",
								false
							);
						} else {
							eb.addField("Status", "Failed to sell", false);
							failedToSell += startingBid;
						}
					}

					try {
						double calculatedPrice = calc.calculateItemPrice(
							getGenericInventoryMap(NBTReader.readBase64(higherDepth(currentAuction, "item_bytes.data").getAsString()))
								.get(0)
						);
						eb.addField("Estimated Price", roundAndFormat(calculatedPrice), false);
						try {
							JsonObject verboseObj = calc.getVerboseJson().getAsJsonArray().get(0).getAsJsonObject();
							verboseObj.remove("nbt_tag");
							eb.addField("Estimated Price Breakdown", "```json\n" + formattedGson.toJson(verboseObj) + "\n```", false);
						} catch (Exception ignored) {}
						calc.resetVerboseJson();
					} catch (Exception e) {
						e.printStackTrace();
					}

					extras.addEmbedPage(eb);
				}
			}

			if (extras.getEmbedPages().size() == 0) {
				return invalidEmbed("No auctions found for " + usernameUuidStruct.usernameFixed());
			}

			for (int i = 0; i < extras.getEmbedPages().size(); i++) {
				extras
					.getEmbedPages()
					.set(
						i,
						extras
							.getEmbedPages()
							.get(i)
							.setTitle(usernameUuidStruct.usernameFixed(), usernameUuidStruct.getAuctionUrl())
							.setThumbnail(usernameUuidStruct.getAvatarlUrl())
							.setDescription(
								(
									totalSoldValue > 0
										? "**Sold Auctions Value:** " +
										simplifyNumber(totalSoldValue) +
										(
											auctionTax > 0
												? " - " + simplifyNumber(auctionTax) + " = " + simplifyNumber(totalSoldValue - auctionTax)
												: ""
										)
										: ""
								) +
								(totalPendingValue > 0 ? "\n**Unsold Auctions Value:** " + simplifyNumber(totalPendingValue) : "") +
								(failedToSell > 0 ? "\n**Did Not Sell Auctions Value:** " + simplifyNumber(failedToSell) : "")
							)
					);
			}

			event.paginate(paginateBuilder.setPaginatorExtras(extras));
		}
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				AuctionFilterType filterType = AuctionFilterType.NONE;
				for (int i = 0; i < args.length; i++) {
					if (args[i].startsWith("filter:")) {
						try {
							filterType = AuctionFilterType.valueOf(args[i].split("filter:")[1].toUpperCase());
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
				boolean verbose = getBooleanOption("--verbose");

				if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
					return;
				}

				paginate(getPlayerAuction(player, filterType, sortType, verbose, getPaginatorEvent()));
			}
		}
			.queue();
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
