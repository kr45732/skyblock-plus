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

import static com.skyblockplus.utils.ApiHandler.cacheDatabase;
import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.Constants.gamemodeCommandOption;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;
import static com.skyblockplus.utils.database.LeaderboardDatabase.typeToNameSubMap;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.GLOBAL_COOLDOWN;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class GuildLbSlashCommand extends SlashCommand {

	public GuildLbSlashCommand() {
		this.name = "guildlb";
		this.cooldown = GLOBAL_COOLDOWN;
	}

	public static EmbedBuilder getGuildLb(String lbTypeParam, Player.Gamemode gamemode, String comparisonMethod, SlashCommandEvent event) {
		String lbType = getType(lbTypeParam);

		Map<String, List<String>> guildCaches = cacheDatabase.getGuildCaches();
		List<String> allGuildMembers = guildCaches.values().stream().flatMap(Collection::stream).toList();

		Map<String, Double> cachedPlayers = leaderboardDatabase
			.getCachedPlayers(List.of(lbType), gamemode, allGuildMembers)
			.stream()
			.collect(Collectors.toMap(e -> e.getString("uuid"), e -> e.getDouble(lbType), (e1, e2) -> e1));

		Map<String, Double> guildLbCaches = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : guildCaches.entrySet()) {
			DoubleStream stream = entry.getValue().stream().filter(cachedPlayers::containsKey).mapToDouble(cachedPlayers::get);
			double value;
			if (comparisonMethod.equals("sum")) {
				value = stream.sum();
			} else {
				value = stream.average().orElse(0);
			}
			guildLbCaches.put(entry.getKey(), value);
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);
		paginateBuilder
			.getExtras()
			.setEveryPageTitle(
				"Global" + (gamemode == Player.Gamemode.ALL ? "" : " " + capitalizeString(gamemode.toString())) + " Guild Leaderboard"
			)
			.setEveryPageText(
				"**Type:** " +
				capitalizeString(lbType.replace("_", " ")) +
				" (" +
				capitalizeString(comparisonMethod) +
				")" +
				"\n**Sum:** " +
				formatOrSimplify(guildLbCaches.values().stream().mapToDouble(e -> e).sum()) +
				"\n**Average:** " +
				formatOrSimplify(guildLbCaches.values().stream().mapToDouble(e -> e).average().orElse(0))
			);

		List<Map.Entry<String, Double>> guildLbEntries = guildLbCaches
			.entrySet()
			.stream()
			.sorted(Comparator.comparingDouble(e -> -e.getValue()))
			.toList();
		for (int i = 0; i < guildLbEntries.size(); i++) {
			Map.Entry<String, Double> entry = guildLbEntries.get(i);
			paginateBuilder.addStrings("`" + (i + 1) + ")` " + escapeText(entry.getKey()) + ": " + formatOrSimplify(entry.getValue()));
		}

		event.paginate(paginateBuilder);
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(
			getGuildLb(
				event.getOptionStr("type"),
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event.getOptionStr("comparison", "average"),
				event
			)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the global guild leaderboard")
			.addOption(OptionType.STRING, "type", "Leaderboard type", true, true)
			.addOptions(
				gamemodeCommandOption,
				new OptionData(OptionType.STRING, "comparison", "Comparison method").addChoice("Average", "average").addChoice("Sum", "sum")
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
