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

import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.database.LeaderboardDatabase.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCommand extends Command {

	public LeaderboardCommand() {
		this.name = "leaderboard";
		this.cooldown = globalCooldown + 3;
		this.aliases = new String[] { "lb" };
		this.botPermissions = defaultPerms();
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				Player.Gamemode gamemode = getGamemodeOption("mode", Player.Gamemode.ALL);
				int page = getIntOption("page", -1);
				int rank = getIntOption("rank", -1);
				double amount = getDoubleOption("amount", -1);
				player = getStringOption("u");
				if (player == null) {
					LinkedAccount linkedUserUsername = database.getByDiscord(getAuthor().getId());
					if (linkedUserUsername != null) {
						player = linkedUserUsername.uuid();
					}
				}

				setArgs(2);
				if (args.length >= 2) {
					paginate(getLeaderboard(args[1], player, gamemode, page, rank, amount, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
