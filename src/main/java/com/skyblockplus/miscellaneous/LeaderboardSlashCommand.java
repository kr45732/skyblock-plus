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

import static com.skyblockplus.utils.database.LeaderboardDatabase.formattedTypesSubList;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardSlashCommand extends SlashCommand {

	public LeaderboardSlashCommand() {
		this.name = "leaderboard";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			getLeaderboard(
				event.getOptionStr("type"),
				event.player,
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event.getOptionInt("page", -1),
				event.getOptionInt("rank", -1),
				event.getOptionDouble("amount", -1),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get the global leaderboard. Player's on leaderboard are only added or updated when commands are run")
			.addOptions(new OptionData(OptionType.STRING, "type", "Leaderboard type", true, true))
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(
				new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
					.addChoice("All", "all")
					.addChoice("Ironman", "ironman")
					.addChoice("Stranded", "stranded"),
				new OptionData(OptionType.INTEGER, "page", "Page number").setMinValue(1),
				new OptionData(OptionType.INTEGER, "rank", "Rank number").setMinValue(1),
				new OptionData(OptionType.NUMBER, "amount", "Amount value").setMinValue(0)
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		} else if (event.getFocusedOption().getName().equals("type")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), formattedTypesSubList);
		}
	}

	public static EmbedBuilder getLeaderboard(
		String lbType,
		String username,
		Player.Gamemode gamemode,
		int page,
		int rank,
		double amount,
		PaginatorEvent event
	) {
		lbType = getType(lbType, true);

		Player player = null;
		if (username != null) {
			player = new Player(username, gamemode);
			if (!player.isValid()) {
				return player.getFailEmbed();
			}
		}

		new LeaderboardPaginator(lbType, gamemode, player, page, rank, amount, event);
		return null;
	}
}
