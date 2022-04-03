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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.getAuctionFromPlayer;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;

import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class CoinsCommand extends Command {

	public CoinsCommand() {
		this.name = "coins";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerBalance(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			double playerBankBalance = player.getBankBalance();
			double playerPurseCoins = player.getPurseCoins();
			double auctionCoins = 0;
			HypixelResponse playerAuctions = getAuctionFromPlayer(player.getUuid());
			if (!playerAuctions.isNotValid()) {
				for (JsonElement currentAuction : playerAuctions.response().getAsJsonArray()) {
					if (higherDepth(currentAuction, "claimed").getAsBoolean()) {
						continue;
					}
					Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
					Duration duration = Duration.between(Instant.now(), endingAt);
					long highestBid = higherDepth(currentAuction, "highest_bid_amount", 0);
					long startingBid = higherDepth(currentAuction, "starting_bid", 0);
					if (duration.toMillis() <= 0) {
						if (highestBid >= startingBid) {
							auctionCoins += highestBid;
						}
					}
				}
			}

			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.setDescription("**Total Coins:** " + simplifyNumber(playerBankBalance + playerPurseCoins) + " (" + simplifyNumber(playerBankBalance + playerPurseCoins + auctionCoins) + ")");
			eb.addField(
				"<:piggy_bank:939014681434161152> Bank Balance",
				playerBankBalance == -1 ? "Banking API disabled" : simplifyNumber(playerBankBalance) + " coins",
				false
			);
			eb.addField("<:enchanted_gold:939021206470926336> Purse Coins", simplifyNumber(playerPurseCoins) + " coins", false);
			eb.addField("<:gold_horse_armor:939021482481291314> Sold Auctions Value", simplifyNumber(auctionCoins), false);
			return eb;
		}
		return player.getFailEmbed();
	}

	public static EmbedBuilder getPlayerBankHistory(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			JsonArray bankHistoryArray = player.getBankHistory();
			if (bankHistoryArray != null) {
				if (bankHistoryArray.isEmpty()) {
					return player.defaultPlayerEmbed().setDescription("Bank history empty");
				}
				CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);

				paginateBuilder.addItems(
					"**Last Transaction Time:** " +
					"<t:" +
					Instant
						.ofEpochMilli(higherDepth(bankHistoryArray.get(bankHistoryArray.size() - 1), "timestamp").getAsLong())
						.getEpochSecond() +
					":D>" +
					"\n"
				);
				for (int i = bankHistoryArray.size() - 1; i >= 0; i--) {
					JsonElement currentTransaction = bankHistoryArray.get(i);
					String valueString =
						simplifyNumber(higherDepth(currentTransaction, "amount", 0L)) +
						" " +
						(higherDepth(currentTransaction, "action").getAsString().equals("DEPOSIT") ? "deposited" : "withdrawn") +
						" by " +
						parseMcCodes(higherDepth(currentTransaction, "initiator_name").getAsString());

					paginateBuilder.addItems(
						"**<t:" +
						Instant.ofEpochMilli(higherDepth(currentTransaction, "timestamp").getAsLong()).getEpochSecond() +
						":D>:** " +
						valueString
					);
				}

				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle(player.getUsername())
						.setEveryPageThumbnail(player.getThumbnailUrl())
						.setEveryPageTitleUrl(player.skyblockStatsLink())
				);

				event.paginate(paginateBuilder);
				return null;
			} else {
				return invalidEmbed("Player banking API disabled");
			}
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 4 || args.length == 3 || args.length == 2) && args[1].equals("history")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerBankHistory(player, args.length == 4 ? args[3] : null, new PaginatorEvent(event)));
					return;
				} else if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPlayerBalance(player, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
