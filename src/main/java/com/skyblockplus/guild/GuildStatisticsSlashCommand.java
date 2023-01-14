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

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
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
public class GuildStatisticsSlashCommand extends SlashCommand {

	public GuildStatisticsSlashCommand() {
		this.name = "guild-statistics";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		String guild = event.getOptionStr("guild");
		if (guild != null) {
			event.embed(
				getStatistics(
					null,
					guild,
					event.getOptionBoolean("key", false),
					Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
					event
				)
			);
			return;
		}

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(
			getStatistics(
				event.player,
				null,
				event.getOptionBoolean("key", false),
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a guild's Skyblock statistics of slayer, skills, catacombs, and weight")
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
		}
	}

	public static EmbedBuilder getStatistics(
		String username,
		String guildName,
		boolean useKey,
		Player.Gamemode gamemode,
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

		HypixelResponse guildResponse;
		UsernameUuidStruct usernameUuidStruct = null;
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
			return invalidEmbed("This guild is currently updating, please try again in a few seconds");
		}

		hypixelGuildQueue.add(guildId);
		List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
			List.of("networth", "level", "slayer", "skills", "catacombs", "weight"),
			gamemode,
			streamJsonArray(higherDepth(guildJson, "members")).map(u -> higherDepth(u, "uuid", "")).collect(Collectors.toList()),
			hypixelKey,
			event
		);
		hypixelGuildQueue.remove(guildId);

		String levelStr = getLeaderboardTop(playerList, "level", usernameUuidStruct);
		String networthStr = getLeaderboardTop(playerList, "networth", usernameUuidStruct);
		String slayerStr = getLeaderboardTop(playerList, "slayer", usernameUuidStruct);
		String skillsStr = getLeaderboardTop(playerList, "skills", usernameUuidStruct);
		String cataStr = getLeaderboardTop(playerList, "catacombs", usernameUuidStruct);
		String weightStr = getLeaderboardTop(playerList, "weight", usernameUuidStruct);

		double averageLevel = playerList.stream().mapToDouble(m -> m.getDouble("level")).average().orElse(0);
		double averageNetworth = (long) playerList.stream().mapToDouble(m -> m.getDouble("networth")).average().orElse(0);
		double averageSlayer = playerList.stream().mapToDouble(m -> m.getDouble("slayer")).average().orElse(0);
		double averageSkills = playerList.stream().mapToDouble(m -> m.getDouble("skills")).average().orElse(0);
		double averageCata = playerList.stream().mapToDouble(m -> m.getDouble("catacombs")).average().orElse(0);
		double averageWeight = playerList.stream().mapToDouble(m -> m.getDouble("weight")).average().orElse(0);

		return defaultEmbed(guildName)
			.setDescription(
				"**Average Skyblock Level:** " +
				roundAndFormat(averageLevel) +
				"\n**Average Networth** " +
				roundAndFormat(averageNetworth) +
				"\n**Average Slayer XP:** " +
				roundAndFormat(averageSlayer) +
				"\n**Average Skills Level:** " +
				roundAndFormat(averageSkills) +
				"\n**Average Catacombs XP:** " +
				roundAndFormat(averageCata) +
				"\n**Average Weight:** " +
				roundAndFormat(averageWeight)
			)
			.addField("Top 5 Skyblock Level", levelStr, true)
			.addField("Top 5 Networth", networthStr, true)
			.addBlankField(true)
			.addField("Top 5 Slayer", slayerStr, true)
			.addField("Top 5 Skills", skillsStr, true)
			.addBlankField(true)
			.addField("Top 5 Catacombs", cataStr, true)
			.addField("Top 5 Weight", weightStr, true)
			.addBlankField(true);
	}

	public static String getLeaderboardTop(List<DataObject> playerList, String lbType, UsernameUuidStruct usernameUuidStruct) {
		List<DataObject> lb = playerList.stream().sorted(Comparator.comparingDouble(m -> -m.getDouble(lbType))).toList();

		int pos = -1;
		if (usernameUuidStruct != null) {
			for (int i = 0; i < lb.size(); i++) {
				if (lb.get(i).getString("uuid").equals(usernameUuidStruct.uuid())) {
					pos = i;
				}
			}
		}

		StringBuilder str = new StringBuilder();
		for (int i = 0; i < Math.min(5, lb.size()); i++) {
			if (pos > 5 && i == 3) {
				DataObject cur = lb.get(pos);
				str
					.append("...\n`")
					.append(pos + 1)
					.append(")` ")
					.append(fixUsername(cur.getString("username")))
					.append(": ")
					.append(roundAndFormat(cur.getDouble(lbType)))
					.append("\n");
				break;
			}
			DataObject cur = lb.get(i);
			str
				.append("`")
				.append(i + 1)
				.append(")` ")
				.append(fixUsername(cur.getString("username")))
				.append(": ")
				.append(roundAndFormat(cur.getDouble(lbType)))
				.append("\n");
		}
		return str.toString();
	}
}
