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

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class BankCommand extends Command {

	public BankCommand() {
		this.name = "bank";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerBalance(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			double playerBankBalance = player.getBankBalance();
			double playerPurseCoins = player.getPurseCoins();

			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.setDescription("**Total coins:** " + simplifyNumber(playerBankBalance + playerPurseCoins));
			eb.addField(
				"Bank balance",
				playerBankBalance == -1 ? "Banking API disabled" : simplifyNumber(playerBankBalance) + " coins",
				false
			);
			eb.addField("Purse coins", simplifyNumber(playerPurseCoins) + " coins", false);
			return eb;
		}
		return invalidEmbed(player.getFailCause());
	}

	public static EmbedBuilder getPlayerBankHistory(
		String username,
		String profileName,
		User user,
		MessageChannel channel,
		InteractionHook hook
	) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			JsonArray bankHistoryArray = player.getBankHistory();
			if (bankHistoryArray != null) {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(1).setItemsPerPage(20);

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
						":D>**: " +
						valueString
					);
				}

				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle(player.getUsername())
						.setEveryPageThumbnail(player.getThumbnailUrl())
						.setEveryPageTitleUrl(player.skyblockStatsLink())
				);

				if (channel != null) {
					paginateBuilder.build().paginate(channel, 0);
				} else {
					paginateBuilder.build().paginate(hook, 0);
				}
				return null;
			} else {
				return invalidEmbed("Player banking API disabled");
			}
		}
		return invalidEmbed(player.getFailCause());
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

					paginate(
						getPlayerBankHistory(username, args.length == 4 ? args[3] : null, event.getAuthor(), event.getChannel(), null)
					);
					return;
				} else if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPlayerBalance(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
