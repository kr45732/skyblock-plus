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

package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class SkyblockEventSlashCommand extends SlashCommand {

	public SkyblockEventSlashCommand() {
		this.name = "event";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		String subcommandName = event.getSubcommandName();
		if (
			(subcommandName.equals("create") || subcommandName.equals("cancel") || subcommandName.equals("end")) &&
			!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())
		) {
			event.string(client.getError() + " You must have the administrator permission in this guild to use that!");
			return;
		}

		switch (subcommandName) {
			case "create":
				event.paginate(SkyblockEventCommand.createSkyblockEvent(new PaginatorEvent(event)));
				break;
			case "current":
				event.embed(SkyblockEventCommand.getCurrentSkyblockEvent(event.getGuild().getId()));
				break;
			case "cancel":
				event.embed(SkyblockEventCommand.cancelSkyblockEvent(event.getGuild()));
				break;
			case "join":
				event.embed(SkyblockEventCommand.joinSkyblockEvent(event.getOptionStr("profile"), event.getMember()));
				break;
			case "leave":
				event.embed(SkyblockEventCommand.leaveSkyblockEvent(event.getGuild().getId(), event.getUser().getId()));
				break;
			case "leaderboard":
			case "lb":
				event.paginate(SkyblockEventCommand.getEventLeaderboard(new PaginatorEvent(event)));
				break;
			case "end":
				if (database.getSkyblockEventActive(event.getGuild().getId())) {
					event.embed(SkyblockEventCommand.endSkyblockEvent(event.getGuild().getId()));
				} else {
					event.embed(defaultEmbed("No event running"));
				}
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main event command")
			.addSubcommands(
				new SubcommandData("create", "Interactive message to create a Skyblock event"),
				new SubcommandData("end", "Force end the event"),
				new SubcommandData("current", "Get information about the current event"),
				new SubcommandData("join", "Join the current event").addOption(OptionType.STRING, "profile", "Profile name"),
				new SubcommandData("leave", "Leave the current event"),
				new SubcommandData("cancel", "Cancel the event"),
				new SubcommandData("leaderboard", "Get the leaderboard for current event")
			);
	}
}
