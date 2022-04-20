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

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class GuildLeaderboardSlashCommand extends SlashCommand {

	public GuildLeaderboardSlashCommand() {
		this.name = "guild-leaderboard";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		String guild = event.getOptionStr("guild");
		if (guild != null) {
			event.paginate(
				GuildLeaderboardCommand.getLeaderboard(
					event.getOptionStr("type"),
					null,
					guild,
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					new PaginatorEvent(event)
				)
			);
			return;
		}

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			GuildLeaderboardCommand.getLeaderboard(
				event.getOptionStr("type"),
				event.player,
				null,
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a leaderboard for a guild. The API key must be set for this server.")
			.addOptions(new OptionData(OptionType.STRING, "type", "Leaderboard type", true, true))
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "guild", "Guild name", false)
			.addOptions(
				new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
					.addChoice("All", "all")
					.addChoice("Ironman", "ironman")
					.addChoice("Stranded", "stranded")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		} else if (event.getFocusedOption().getName().equals("type")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), HypixelGuildCache.getTypes());
		}
	}
}
