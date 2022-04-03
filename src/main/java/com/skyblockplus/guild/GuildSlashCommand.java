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

package com.skyblockplus.guild;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class GuildSlashCommand extends SlashCommand {

	public GuildSlashCommand() {
		this.name = "guild";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "information" -> {
				String guild = event.getOptionStr("guild");
				if (guild != null) {
					event.embed(GuildCommand.getGuildInfoFromName(guild));
					return;
				}

				if (event.invalidPlayerOption()) {
					return;
				}

				event.embed(GuildCommand.getGuildInfoFromPlayer(event.player));
			}
			case "members" -> {
				String guild = event.getOptionStr("guild");
				if (guild != null) {
					event.embed(GuildCommand.getGuildMembersFromName(guild, new PaginatorEvent(event)));
					return;
				}

				if (event.invalidPlayerOption()) {
					return;
				}

				event.paginate(GuildCommand.getGuildMembersFromPlayer(event.player, new PaginatorEvent(event)));
			}
			case "experience" -> {
				int numDays = event.getOptionInt("days", 7);

				String guild = event.getOptionStr("guild");
				if (guild != null) {
					event.embed(GuildCommand.getGuildExpFromPlayer(guild, numDays, new PaginatorEvent(event)));
					return;
				}

				if (event.invalidPlayerOption()) {
					return;
				}
				event.paginate(GuildCommand.getGuildExpFromPlayer(event.player, numDays, new PaginatorEvent(event)));
			}
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main guild command")
			.addSubcommands(
				new SubcommandData("information", "Get information and statistics about a guild")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "guild", "Guild name", false),
				new SubcommandData("members", "Get a list of all members in a player's guild")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "guild", "Guild name", false),
				new SubcommandData("experience", "Get the experience leaderboard for a player's guild")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "guild", "Guild name", false)
					.addOptions(new OptionData(OptionType.INTEGER, "days", "Number of days").setRequiredRange(1, 7))
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
