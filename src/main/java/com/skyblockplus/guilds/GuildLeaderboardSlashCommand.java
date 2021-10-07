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
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class GuildLeaderboardSlashCommand extends SlashCommand {

	public GuildLeaderboardSlashCommand() {
		this.name = "guild-leaderboard";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			GuildLeaderboardCommand.getLeaderboard(
				event.getOptionStr("type"),
				event.player,
				event.getOptionBoolean("ironman", false),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return new CommandData("guild-leaderboard", "Get a leaderboard for a guild. The API key must be set for this server.")
			.addOptions(
				new OptionData(OptionType.STRING, "type", "The leaderboard type", true)
					.addChoice("Slayer", "slayer")
					.addChoice("Skills", "skills")
					.addChoice("Catacombs", "catacombs")
					.addChoice("Sven Xp", "sven_xp")
					.addChoice("Revenant Xp", "rev_xp")
					.addChoice("Tarantula Xp", "tara_xp")
					.addChoice("Enderman Xp", "enderman_xp")
			)
			.addOption(OptionType.STRING, "player", "Player username or mention")
			.addOption(OptionType.BOOLEAN, "ironman", "If the leaderboard should be ironman only");
	}
}
