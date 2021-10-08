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

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class AuctionsSlashCommand extends SlashCommand {

	public AuctionsSlashCommand() {
		this.name = "auctions";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "player":
				if (event.invalidPlayerOption()) {
					return;
				}

				event.paginate(
					AuctionCommand.getPlayerAuction(
						event.player,
						AuctionCommand.AuctionFilterType.valueOf(event.getOptionStr("filter", "none").toUpperCase()),
						AuctionCommand.AuctionSortType.valueOf(event.getOptionStr("sort", "none").toUpperCase()),
						new PaginatorEvent(event)
					)
				);
				break;
			case "uuid":
				event.embed(AuctionCommand.getAuctionByUuid(event.getOptionStr("uuid")));
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}

	@Override
	public CommandData getCommandData() {
		return new CommandData(name, "Main auctions command")
			.addSubcommands(
				new SubcommandData("player", "Get player's active (not claimed) auctions on all profiles")
					.addOption(OptionType.STRING, "player", "Player username or mention")
					.addOptions(
						new OptionData(OptionType.STRING, "filter", "How the auctions should be filtered")
							.addChoice("Sold", "sold")
							.addChoice("Unsold", "Unsold")
					)
					.addOptions(
						new OptionData(OptionType.STRING, "sort", "How the auctions should be sorted")
							.addChoice("Low", "low")
							.addChoice("High", "high")
					),
				new SubcommandData("uuid", "Get an auction by it's UUID").addOption(OptionType.STRING, "uuid", "Auction UUID", true)
			);
	}
}
