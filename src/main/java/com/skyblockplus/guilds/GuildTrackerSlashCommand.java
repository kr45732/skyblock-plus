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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class GuildTrackerSlashCommand extends SlashCommand {

	public GuildTrackerSlashCommand() {
		this.name = "guild-tracker";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "start":
				event.embed(GuildTrackerCommand.startGuildTracker(event.player, event.getGuild()));
				break;
			case "stop":
				event.embed(GuildTrackerCommand.stopGuildTracker(event.player, event.getGuild()));
				break;
			case "get":
				event.paginate(GuildTrackerCommand.getGuildTracker(event.player, new PaginatorEvent(event)));
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}

	@Override
	public CommandData getCommandData() {
		return new CommandData(name, "Main guild tracker command")
			.addSubcommands(
				new SubcommandData(
					"start",
					"Start tracking the change in a player's guild's mining and farming collections for each member"
				)
					.addOption(OptionType.STRING, "player", "Player username or mention"),
				new SubcommandData("stop", "Stop tracking a player's guild")
					.addOption(OptionType.STRING, "player", "Player username or mention"),
				new SubcommandData("get", "Stop tracking a player's guild")
					.addOption(OptionType.STRING, "player", "Get the tracker for the past 3 days. Updates every midnight")
			);
	}
}