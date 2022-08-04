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

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class GuildLeaderboardCommand extends Command {

	public GuildLeaderboardCommand() {
		this.name = "guild-leaderboard";
		this.cooldown = globalCooldown + 2;
		this.aliases = new String[] { "g-lb" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getLeaderboard(
		String lbType,
		String username,
		String guildName,
		Player.Gamemode gamemode,
		boolean useKey,
		PaginatorEvent event
	) {
		String hypixelKey = null;
		if (useKey) {
			hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}
		}

		lbType = getType(lbType, false);

		UsernameUuidStruct usernameUuidStruct = null;
		HypixelResponse guildResponse;
		if (username != null) {
			usernameUuidStruct = usernameToUuid(username);
			if (!usernameUuidStruct.isValid()) {
				return invalidEmbed(usernameUuidStruct.failCause());
			}
			guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
		} else {
			guildResponse = getGuildFromName(guildName);
		}
		if (!guildResponse.isValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonElement guildJson = guildResponse.response();
		guildName = higherDepth(guildJson, "name").getAsString();
		String guildId = higherDepth(guildJson, "_id").getAsString();

		if (hypixelGuildQueue.contains(guildId)) {
			return invalidEmbed("This guild is currently updating, please try again in a couple of seconds");
		}
		hypixelGuildQueue.add(guildId);
		List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
			lbType,
			gamemode,
			streamJsonArray(higherDepth(guildJson, "members")).map(u -> higherDepth(u, "uuid", "")).collect(Collectors.toList()),
			hypixelKey,
			event
		);
		hypixelGuildQueue.remove(guildId);

		String finalLbType = lbType;
		playerList.sort(Comparator.comparingDouble(cache -> -cache.getDouble(finalLbType)));

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);

		long total = 0;
		int guildRank = -1;
		String amt = "?";
		for (int i = 0, guildMemberPlayersListSize = playerList.size(); i < guildMemberPlayersListSize; i++) {
			DataObject player = playerList.get(i);
			double amount = player.getDouble(lbType);
			amount = lbType.equals("networth") ? (long) amount : amount;
			String formattedAmt = amount == -1 ? "?" : roundAndFormat(amount);
			String playerUsername = player.getString("username");

			paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(playerUsername) + ": " + formattedAmt);
			total += Math.max(0, amount);

			if (username != null && playerUsername.equals(usernameUuidStruct.username())) {
				guildRank = i;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Total " +
			capitalizeString(lbType.replace("_", " ")) +
			":** " +
			formatNumber(total) +
			(
				username != null
					? "\n**Player:** " +
					usernameUuidStruct.username() +
					"\n**Guild Rank:** #" +
					(guildRank + 1) +
					"\n**" +
					capitalizeString(lbType.replace("_", " ")) +
					":** " +
					amt
					: ""
			);

		paginateBuilder.getExtras().setEveryPageTitle(guildName).setEveryPageText(ebStr);
		event.paginate(paginateBuilder, (guildRank / 20) + 1);

		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				Player.Gamemode gamemode = getGamemodeOption("mode", Player.Gamemode.ALL);
				boolean useKey = getBooleanOption("--key");

				setArgs(3);
				if (args.length >= 2) {
					if (args.length >= 3 && args[2].startsWith("g:")) {
						paginate(getLeaderboard(args[1], null, args[2].split("g:")[1], gamemode, useKey, getPaginatorEvent()));
					} else {
						if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
							return;
						}

						paginate(getLeaderboard(args[1], player, null, gamemode, useKey, getPaginatorEvent()));
					}
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
