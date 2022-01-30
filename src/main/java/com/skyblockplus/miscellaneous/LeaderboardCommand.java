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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.structs.HypixelGuildCache.*;

public class LeaderboardCommand extends Command {

	public LeaderboardCommand() {
		this.name = "leaderboard";
		this.cooldown = globalCooldown + 6;
		this.aliases = new String[] { "lb" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getLeaderboard(String lbType, String username, Player.Gamemode gamemode, PaginatorEvent event) {
		if (!isValidType(lbType)) {
			return invalidEmbed(lbType + " is an invalid leaderboard type. Use `/help leaderboard` to see valid types");
		}
		lbType = lbType.toLowerCase();

		Player player = new Player(username);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(2).setItemsPerPage(20);
		String finalLbType = lbType;
		List<String> cacheList = globalLeaderboardCache.getCache(gamemode)
				.stream()
				.filter(cache -> getDoubleFromCache(cache, finalLbType) >= 0)
				.sorted(Comparator.comparingDouble(cache -> -getDoubleFromCache(cache, finalLbType)))
				.collect(Collectors.toList());

		int guildRank = -1;
		String amt = "Not on leaderboard";
		for (int i = 0, guildMemberPlayersListSize = cacheList.size(); i < guildMemberPlayersListSize; i++) {
			String guildPlayer = cacheList.get(i);
			String formattedAmt = roundAndFormat(getDoubleFromCache(guildPlayer, lbType));
			String guildPlayerUsername = getStringFromCache(guildPlayer, "username");
			paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(guildPlayerUsername) + ": " + formattedAmt);

			if (guildPlayerUsername.equals(player.getUsername())) {
				guildRank = i;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Player:** " +
			player.getUsername() +
			"\n**Rank:** " +
			(guildRank == -1 ?"Not on leaderboard" : "#" + (guildRank + 1)) +
			"\n**" +
			capitalizeString(lbType.replace("_", " ")) +
			":** " +
			amt;

		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageText(ebStr)
					.setEveryPageTitle("Global Leaderboard | " + capitalizeString(gamemode.toString()))
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/players")
		);

		event.paginate(paginateBuilder);
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				Player.Gamemode gamemode = Player.Gamemode.of(getStringOption("mode", "all"));
				if (args.length == 3 || args.length == 2) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					paginate(getLeaderboard(args[1], player, gamemode, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
