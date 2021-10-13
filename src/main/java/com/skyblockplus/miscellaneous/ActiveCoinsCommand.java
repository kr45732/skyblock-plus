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

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class ActiveCoinsCommand extends Command {

	public ActiveCoinsCommand() {
		this.name = "active-coins";
		this.aliases = new String[] { "ac" };
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getActiveCoins(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			double bankBalance = player.getBankBalance();
			double purseCoins = player.getPurseCoins();
			double auctionCoins = 0;
			HypixelResponse playerAuctions = getAuctionFromPlayer(player.getUuid());
			if (!playerAuctions.isNotValid()) {
				for (JsonElement currentAuction : playerAuctions.getResponse().getAsJsonArray()) {
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
			eb.setDescription("**Total active coins:** " + simplifyNumber(Math.max(bankBalance, 0) + purseCoins + auctionCoins));
			eb.addField("Bank balance", bankBalance == -1 ? "API Disabled" : simplifyNumber(bankBalance), false);
			eb.addField("Purse coins", simplifyNumber(purseCoins), false);
			eb.addField("Sold auction(s) value", simplifyNumber(auctionCoins), false);

			return eb;
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getActiveCoins(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
