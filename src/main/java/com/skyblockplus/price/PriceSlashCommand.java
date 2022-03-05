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

import static com.skyblockplus.utils.Utils.getQueryItems;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PriceSlashCommand extends SlashCommand {

	public PriceSlashCommand() {
		this.name = "price";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.paginate(
			PriceCommand.queryAuctions(
				event.getOptionStr("item"),
				PriceCommand.AuctionType.valueOf(event.getOptionStr("auction_type", "bin").toUpperCase()),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Query the auction house for the price of an item")
			.addOption(OptionType.STRING, "item", "Item name", true, true)
			.addOptions(
				new OptionData(OptionType.STRING, "auction_type", "Which type of auctions to show")
					.addChoice("Bin", "bin")
					.addChoice("Regular auctions", "auction")
					.addChoice("Bin & regular auctions", "bin")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), getQueryItems());
		}
	}
}
