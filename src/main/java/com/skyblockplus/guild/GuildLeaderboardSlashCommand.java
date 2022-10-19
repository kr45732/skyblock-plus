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
import static com.skyblockplus.utils.database.LeaderboardDatabase.formattedTypesSubList;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class GuildLeaderboardSlashCommand extends SlashCommand {

	public GuildLeaderboardSlashCommand() {
		this.name = "guild-leaderboard";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		String guild = event.getOptionStr("guild");
		if (guild != null) {
			event.paginate(
				getLeaderboard(
					event.getOptionStr("type"),
					null,
					guild,
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					event.getOptionBoolean("key", false),
					event
				)
			);
			return;
		}

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			getLeaderboard(
				event.getOptionStr("type"),
				event.player,
				null,
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event.getOptionBoolean("key", false),
				event
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
			)
			.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more updated results");
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
		String guildName,
		Player.Gamemode gamemode,
		boolean useKey,
		SlashCommandEvent event
	) {
		String hypixelKey = null;
		if (useKey) {
			hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}
		}

		lbType = getType(lbType);

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
}
