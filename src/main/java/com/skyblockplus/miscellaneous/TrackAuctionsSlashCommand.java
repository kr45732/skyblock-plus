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

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class TrackAuctionsSlashCommand extends SlashCommand {

	public TrackAuctionsSlashCommand() {
		this.name = "track";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "auctions" -> {
				if (event.invalidPlayerOption()) {
					return;
				}
				event.embed(TrackAuctionsCommand.trackAuctions(event.player, event.getUser().getId()));
			}
			case "stop" -> event.embed(TrackAuctionsCommand.stopTrackingAuctions(event.getUser().getId()));
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Main track command")
			.addSubcommands(
				new SubcommandData(
					"auctions",
					"Track the auctions of a certain player & receive a DM from the bot when the their auctions sell"
				)
					.addOption(OptionType.STRING, "player", "Player username or mention"),
				new SubcommandData("stop", "Stop tracking a players auctions")
			);
	}
}
