/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.gamemodeCommandOption;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;
import static com.skyblockplus.utils.database.LeaderboardDatabase.typeToNameSubMap;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class ServerLbSlashCommand extends SlashCommand {

	public ServerLbSlashCommand() {
		this.name = "serverlb";
	}

	public static EmbedBuilder getServerLb(String lbTypeParam, String username, Player.Gamemode gamemode, SlashCommandEvent event) {
		UsernameUuidStruct usernameUuidStruct = null;
		if (username != null) {
			usernameUuidStruct = usernameToUuid(username);
			if (!usernameUuidStruct.isValid()) {
				return errorEmbed(usernameUuidStruct.failCause());
			}
		}

		Map<String, String> discordToUuid = database
			.getAllLinkedAccounts()
			.stream()
			.collect(Collectors.toMap(LinkedAccount::discord, LinkedAccount::uuid));
		List<String> uuids = new ArrayList<>();

		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean failed = new AtomicBoolean(false);

		event
			.getGuild()
			.loadMembers(m -> {
				if (discordToUuid.containsKey(m.getId())) {
					uuids.add(discordToUuid.get(m.getId()));
				}
			})
			.onSuccess(ignored -> latch.countDown())
			.onError(ignored -> {
				failed.set(true);
				latch.countDown();
			});

		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			failed.set(true);
		}

		if (failed.get()) {
			return errorEmbed("An error occurred retrieving this server's members, please try again in a few seconds");
		}

		String lbType = getType(lbTypeParam);

		List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(List.of(lbType), gamemode, uuids);
		playerList.sort(Comparator.comparingDouble(cache -> -cache.getDouble(lbType, 0)));

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);

		double total = 0;
		int playerRank = -1;
		String amt = "None";
		for (int i = 0, playersListSize = playerList.size(); i < playersListSize; i++) {
			DataObject player = playerList.get(i);
			double amount = player.getDouble(lbType, -1);
			if (amount <= 0) {
				continue;
			}

			String formattedAmt = formatOrSimplify(amount);
			paginateBuilder.addStrings("`" + (i + 1) + ")` " + escapeText(player.getString("username")) + ": " + formattedAmt);
			total += amount;

			if (usernameUuidStruct != null && player.getString("uuid").equals(usernameUuidStruct.uuid())) {
				playerRank = i + 1;
				amt = formattedAmt;
			}
		}

		String lbTypeFormatted = capitalizeString(lbType.replace("_", " "));

		String ebStr =
			"**Total " +
			lbTypeFormatted +
			":** " +
			formatOrSimplify(total) +
			"\n**Average " +
			lbTypeFormatted +
			":** " +
			formatOrSimplify(total / paginateBuilder.size());
		if (usernameUuidStruct != null) {
			ebStr +=
			"\n**Player:** " +
			usernameUuidStruct.username() +
			"\n**Rank:** " +
			(playerRank != -1 ? "#" + playerRank + " (" + amt + ")" : "Not on leaderboard");
		}
		paginateBuilder.getExtras().setEveryPageTitle("Server Leaderboard").setEveryPageText(ebStr);
		event.paginate(paginateBuilder, playerRank != -1 ? (playerRank - 1) / paginateBuilder.getItemsPerPage() + 1 : 1);

		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			getServerLb(event.getOptionStr("type"), event.player, Player.Gamemode.of(event.getOptionStr("gamemode", "all")), event)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a leaderboard for this server")
			.addOptions(new OptionData(OptionType.STRING, "type", "Leaderboard type", true, true))
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(gamemodeCommandOption);
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
