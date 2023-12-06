/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.Constants.gamemodeCommandOption;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;
import static com.skyblockplus.utils.database.LeaderboardDatabase.typeToNameSubMap;
import static com.skyblockplus.utils.utils.Utils.GLOBAL_COOLDOWN;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardSlashCommand extends SlashCommand {

	public LeaderboardSlashCommand() {
		this.name = "leaderboard";
	}

	public static EmbedBuilder getLeaderboard(
		String lbType,
		String username,
		Player.Gamemode gamemode,
		int page,
		int rank,
		double amount,
		SlashCommandEvent event
	) {
		lbType = getType(lbType);

		Player.Profile player = null;
		if (username != null) {
			player = new Player(username, gamemode).getSelectedProfile();
			if (!player.isValid()) {
				return player.getErrorEmbed();
			}
		}

		new LeaderboardPaginator(lbType, gamemode, player, page, rank, amount, event);
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.getOptionStr("player") != null) {
			event.invalidPlayerOption(true);
		}

		event.paginate(
			getLeaderboard(
				event.getOptionStr("type"),
				event.player,
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event.getOptionInt("page", -1),
				event.getOptionInt("rank", -1),
				event.getOptionDouble("amount", -1),
				event
			)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the global leaderboard")
			.addOptions(new OptionData(OptionType.STRING, "type", "Leaderboard type", true, true))
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(
				gamemodeCommandOption,
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
			event.replyClosestMatch(event.getFocusedOption().getValue(), typeToNameSubMap.values());
		}
	}
}
