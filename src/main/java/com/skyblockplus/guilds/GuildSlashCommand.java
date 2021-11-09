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

package com.skyblockplus.guilds;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class GuildSlashCommand extends SlashCommand {

	public GuildSlashCommand() {
		this.name = "guild";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "player":
				event.embed(GuildCommand.getGuildPlayer(event.player));
				break;
			case "information":
				event.embed(GuildCommand.getGuildInfo(event.player));
				break;
			case "members":
				event.paginate(GuildCommand.getGuildMembersFromPlayer(event.player, new PaginatorEvent(event)));
				break;
			case "experience":
				OptionMapping numDays = event.getEvent().getOption("days");
				event.paginate(
					GuildCommand.getGuildExpFromPlayer(event.player, numDays != null ? numDays.getAsLong() : 7, new PaginatorEvent(event))
				);
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}

	@Override
	public CommandData getCommandData() {
		return new CommandData(name, "Main guild command")
			.addSubcommands(
				new SubcommandData("player", "Find what guild a player is in")
					.addOption(OptionType.STRING, "player", "Player username or mention"),
				new SubcommandData("information", "Get information and statistics about a player's guild")
					.addOption(OptionType.STRING, "player", "Player username or mention"),
				new SubcommandData("members", "Get a list of all members in a player's guild")
					.addOption(OptionType.STRING, "player", "Player username or mention"),
				new SubcommandData("experience", "Get the experience leaderboard for a player's guild")
					.addOption(OptionType.STRING, "player", "Player username or mention")
					.addOptions(new OptionData(OptionType.INTEGER, "days", "Number of days").setRequiredRange(1, 7))
			);
	}
}
