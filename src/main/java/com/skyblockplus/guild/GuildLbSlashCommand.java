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

package com.skyblockplus.guild;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.gamemodeCommandOption;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;
import static com.skyblockplus.utils.database.LeaderboardDatabase.typeToNameSubMap;
import static com.skyblockplus.utils.utils.StringUtils.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import groovy.lang.Tuple2;
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
	}

	public static EmbedBuilder getGuildLb(
		String lbTypeParam,
		String guildName,
		Player.Gamemode gamemode,
		String comparisonMethod,
		SlashCommandEvent event
	) {
		HypixelResponse guildResponse = null;
		if (guildName != null) {
			guildResponse = getGuildFromName(guildName);
			if (!guildResponse.isValid()) {
				return guildResponse.getErrorEmbed();
			}
		}

		String lbType = getType(lbTypeParam);

		Map<Tuple2<String, String>, List<String>> guildCaches = cacheDatabase.getGuildCaches();
		List<String> allGuildMembers = guildCaches.values().stream().flatMap(Collection::stream).toList();

		Map<String, Double> cachedPlayers = leaderboardDatabase
			.getCachedPlayers(List.of(lbType), gamemode, allGuildMembers)
			.stream()
			.collect(Collectors.toMap(e -> e.getString("uuid"), e -> e.getDouble(lbType), (e1, e2) -> e1));

		Map<Tuple2<String, String>, Double> guildLbCaches = new HashMap<>();
		for (Map.Entry<Tuple2<String, String>, List<String>> entry : guildCaches.entrySet()) {
			DoubleStream stream = entry.getValue().stream().filter(cachedPlayers::containsKey).mapToDouble(cachedPlayers::get);
			guildLbCaches.put(entry.getKey(), comparisonMethod.equals("sum") ? stream.sum() : stream.average().orElse(0));
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);

		int guildRank = -1;
		String amt = "None";

		List<Map.Entry<Tuple2<String, String>, Double>> guildLbEntries = guildLbCaches
			.entrySet()
			.stream()
			.sorted(Comparator.comparingDouble(e -> -e.getValue()))
			.toList();
		for (int i = 0; i < guildLbEntries.size(); i++) {
			Map.Entry<Tuple2<String, String>, Double> entry = guildLbEntries.get(i);

			String formattedAmt = formatOrSimplify(entry.getValue());
			paginateBuilder.addStrings("`" + (i + 1) + ")` " + escapeText(entry.getKey().getV2()) + ": " + formattedAmt);

			if (guildResponse != null && entry.getKey().getV1().equals(guildResponse.get("_id").getAsString())) {
				guildRank = i + 1;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Type:** " +
			capitalizeString(lbType.replace("_", " ")) +
			" (" +
			capitalizeString(comparisonMethod) +
			")" +
			"\n**Sum:** " +
			formatOrSimplify(guildLbCaches.values().stream().mapToDouble(e -> e).sum()) +
			"\n**Average:** " +
			formatOrSimplify(guildLbCaches.values().stream().mapToDouble(e -> e).average().orElse(0));
		if (guildResponse != null) {
			ebStr +=
				"\n**Guild:** " +
				guildResponse.get("name").getAsString() +
				"\n**Rank:** " +
				(guildRank != -1 ? "#" + guildRank + " (" + amt + ")" : "Not on leaderboard");
		}

		paginateBuilder
			.getExtras()
			.setEveryPageTitle(
				"Global" + (gamemode == Player.Gamemode.ALL ? "" : " " + capitalizeString(gamemode.toString())) + " Guild Leaderboard"
			)
			.setEveryPageText(ebStr);

		event.paginate(paginateBuilder, guildRank != -1 ? (guildRank - 1) / paginateBuilder.getItemsPerPage() + 1 : 1);
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(
			getGuildLb(
				event.getOptionStr("type"),
				event.getOptionStr("guild"),
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
			.addOption(OptionType.STRING, "guild", "Guild name", false)
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
