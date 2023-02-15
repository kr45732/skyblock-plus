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
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class CoinsSlashCommand extends SlashCommand {

	public CoinsSlashCommand() {
		this.name = "coins";
	}

	public static class PlayerSubcommand extends Subcommand {

		public PlayerSubcommand() {
			this.name = "player";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.embed(getPlayerBalance(event.player, event.getOptionStr("profile")));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("player", "Get a player's bank and purse coins")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}

		public static EmbedBuilder getPlayerBalance(String username, String profileName) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				double playerBankBalance = player.getBankBalance();
				double playerPurseCoins = player.getPurseCoins();
				double auctionCoins = 0;
				HypixelResponse playerAuctions = getAuctionFromPlayer(player.getUuid());
				if (playerAuctions.isValid()) {
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
				return eb;
			}
			return player.getFailEmbed();
		}
	}

	public static class HistorySubcommand extends Subcommand {

		public HistorySubcommand() {
			this.name = "history";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerBankHistory(event.player, event.getOptionStr("profile"), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("history", "Get a player's bank transaction history")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}

		public static EmbedBuilder getPlayerBankHistory(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				JsonArray bankHistoryArray = player.getBankHistory();
				if (bankHistoryArray != null) {
					if (bankHistoryArray.isEmpty()) {
						return player.defaultPlayerEmbed().setDescription("Bank history empty");
					}
					CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(20);

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

					event.paginate(paginateBuilder);
					return null;
				} else {
					return invalidEmbed("Player banking API disabled");
				}
			}
			return player.getFailEmbed();
		}
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main coins command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
