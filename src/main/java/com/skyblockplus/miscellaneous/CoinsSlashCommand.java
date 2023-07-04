/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.cleanMcCodes;
import static com.skyblockplus.utils.utils.StringUtils.simplifyNumber;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class CoinsSlashCommand extends SlashCommand {

	public CoinsSlashCommand() {
		this.name = "coins";
	}

	public static EmbedBuilder getPlayerBalance(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = player
				.defaultPlayerPaginator(PaginatorExtras.PaginatorType.EMBED_PAGES, event.getUser())
				.setItemsPerPage(20);

			double playerBankBalance = player.getBankBalance();
			double playerPurseCoins = player.getPurseCoins();

			double auctionCoins = 0;
			HypixelResponse playerAuctions = getAuctionFromPlayer(player.getUuid());
			if (playerAuctions.isValid()) {
				List<String> validProfileIds = player
					.getProfiles()
					.stream()
					.map(prof -> higherDepth(prof.getOuterProfileJson(), "profile_id").getAsString().replace("-", ""))
					.collect(Collectors.toCollection(ArrayList::new));
				for (JsonElement currentAuction : playerAuctions.response().getAsJsonArray()) {
					long highestBid = higherDepth(currentAuction, "highest_bid_amount", 0);
					long startingBid = higherDepth(currentAuction, "starting_bid", 0);
					if (
						!higherDepth(currentAuction, "claimed").getAsBoolean() &&
						validProfileIds.contains(higherDepth(currentAuction, "profile_id").getAsString()) &&
						higherDepth(currentAuction, "end").getAsLong() <= Instant.now().toEpochMilli() &&
						highestBid >= startingBid
					) {
						auctionCoins += highestBid;
					}
				}
			}

			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.setDescription(
				"**Total Coins:** " +
				simplifyNumber(playerBankBalance + playerPurseCoins) +
				" (" +
				simplifyNumber(playerBankBalance + playerPurseCoins + auctionCoins) +
				")"
			);
			eb.addField(
				getEmoji("PIGGY_BANK") + " Bank Balance",
				playerBankBalance == -1 ? "Banking API disabled" : simplifyNumber(playerBankBalance) + " coins",
				false
			);
			eb.addField(getEmoji("ENCHANTED_GOLD") + " Purse Coins", simplifyNumber(playerPurseCoins) + " coins", false);
			eb.addField(getEmoji("GOLD_BARDING") + " Sold Auctions Value", simplifyNumber(auctionCoins), false);
			paginateBuilder.getExtras().addEmbedPage(eb);

			JsonArray bankHistoryArray = player.getBankHistory();
			if (bankHistoryArray == null) {
				paginateBuilder.addStrings("Player banking API disabled");
			} else if (bankHistoryArray.isEmpty()) {
				paginateBuilder.addStrings("Bank history empty");
			} else {
				for (int i = bankHistoryArray.size() - 1; i >= 0; i--) {
					JsonElement currentTransaction = bankHistoryArray.get(i);
					paginateBuilder.addStrings(
						"**<t:" +
						(Instant.ofEpochMilli(higherDepth(currentTransaction, "timestamp").getAsLong()).getEpochSecond()) +
						":D>:** " +
						simplifyNumber(higherDepth(currentTransaction, "amount", 0L)) +
						" " +
						(higherDepth(currentTransaction, "action").getAsString().equals("DEPOSIT") ? "deposited" : "withdrawn") +
						" by " +
						cleanMcCodes(higherDepth(currentTransaction, "initiator_name").getAsString())
					);
				}
				paginateBuilder
					.getExtras()
					.setEveryPageText(
						"**Last Transaction Time:** " +
						"<t:" +
						Instant
							.ofEpochMilli(higherDepth(bankHistoryArray.get(bankHistoryArray.size() - 1), "timestamp").getAsLong())
							.getEpochSecond() +
						":D>" +
						"\n"
					);
			}

			paginateBuilder
				.getExtras()
				.addReactiveButtons(
					new PaginatorExtras.ReactiveButton(
						Button.primary("reactive_coins_history", "Show History"),
						action -> {
							action
								.paginator()
								.getExtras()
								.setType(PaginatorExtras.PaginatorType.DEFAULT)
								.toggleReactiveButton("reactive_coins_history", false)
								.toggleReactiveButton("reactive_coins_current", true);
						},
						true
					),
					new PaginatorExtras.ReactiveButton(
						Button.primary("reactive_coins_current", "Show Current"),
						action -> {
							action
								.paginator()
								.getExtras()
								.setType(PaginatorExtras.PaginatorType.EMBED_PAGES)
								.toggleReactiveButton("reactive_coins_history", true)
								.toggleReactiveButton("reactive_coins_current", false);
						},
						false
					)
				);

			event.paginate(paginateBuilder);
			return null;
		}
		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerBalance(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	protected SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's coins and bank history")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
