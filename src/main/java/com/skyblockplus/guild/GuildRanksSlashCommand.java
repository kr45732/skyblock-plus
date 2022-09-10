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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
public class GuildRanksSlashCommand extends SlashCommand {

	public GuildRanksSlashCommand() {
		this.name = "guild-ranks";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			getRanks(event.player, Player.Gamemode.of(event.getOptionStr("gamemode", "all")), event.getOptionBoolean("key", false), event)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get helper which shows who to promote or demote in your guild")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(
				new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
					.addChoice("All", "all")
					.addChoice("Ironman", "ironman")
					.addChoice("Stranded", "stranded")
			)
			.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more accurate results");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getRanks(String username, Player.Gamemode gamemode, boolean useKey, SlashCommandEvent event) {
		String hypixelKey = null;
		if (useKey) {
			hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}
		}

		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (!usernameUuid.isValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (!guildResponse.isValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonElement guildJson = guildResponse.response();
		String guildId = higherDepth(guildJson, "_id").getAsString();
		String guildName = higherDepth(guildJson, "name").getAsString();
		JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();

		JsonElement lbSettings;
		try {
			lbSettings =
				higherDepth(
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json")),
					guildId + ".guild_leaderboard"
				)
					.getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed(
				guildName +
				"'s rank settings are not setup. Please join the [Skyblock Plus Discord](" +
				DISCORD_SERVER_INVITE_LINK +
				") and mention CrypticPlasma to setup this for your guild."
			);
		}

		String lbType = higherDepth(lbSettings, "lb_type").getAsString();
		List<String> ignoredRanks = streamJsonArray(higherDepth(lbSettings, "ignored_ranks"))
			.map(e -> e.getAsString().toLowerCase())
			.toList();
		List<String> rankTypes = new ArrayList<>();
		if (lbType.equals("position")) {
			for (JsonElement i : higherDepth(lbSettings, "types").getAsJsonArray()) {
				rankTypes.add(i.getAsString().toLowerCase());
			}
		}

		if (hypixelGuildQueue.contains(guildId)) {
			return invalidEmbed("This guild is currently updating, please try again in a couple of seconds");
		}
		hypixelGuildQueue.add(guildId);
		List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
			List.of("slayer", "skills", "catacombs", "weight", "networth"),
			gamemode,
			streamJsonArray(guildMembers).map(u -> higherDepth(u, "uuid", "")).collect(Collectors.toList()),
			hypixelKey,
			event
		);
		hypixelGuildQueue.remove(guildId);

		List<String> uniqueGuildName = new ArrayList<>();
		for (int i = playerList.size() - 1; i >= 0; i--) {
			DataObject lbM = playerList.get(i);
			String gMemUuid = lbM.getString("uuid");
			String gMemUsername = lbM.getString("username");
			JsonElement gMemJson = streamJsonArray(guildMembers)
				.filter(m -> higherDepth(m, "uuid", "").equals(gMemUuid))
				.findFirst()
				.orElse(null);
			String curRank = higherDepth(gMemJson, "rank", null);

			if (curRank == null || ignoredRanks.contains(curRank.toLowerCase())) {
				playerList.remove(i);
			} else {
				playerList
					.get(i)
					.put("rank", curRank.toLowerCase())
					.put(
						"gxp",
						higherDepth(gMemJson, "expHistory")
							.getAsJsonObject()
							.entrySet()
							.stream()
							.mapToDouble(g -> g.getValue().getAsDouble())
							.sum()
					)
					.put(
						"duration",
						Duration.between(Instant.now(), Instant.ofEpochMilli(higherDepth(gMemJson, "joined").getAsLong())).abs().toSeconds()
					);
				uniqueGuildName.add(gMemUsername);
			}
		}

		if (lbType.equals("position")) {
			List<DataObject> guildSlayer = playerList
				.stream()
				.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("slayer")))
				.collect(Collectors.toCollection(ArrayList::new));
			List<DataObject> guildSkills = playerList
				.stream()
				.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("skills")))
				.collect(Collectors.toCollection(ArrayList::new));
			List<DataObject> guildCatacombs = playerList
				.stream()
				.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("catacombs")))
				.collect(Collectors.toCollection(ArrayList::new));
			List<DataObject> guildWeight = playerList
				.stream()
				.sorted(Comparator.comparingDouble(o1 -> -o1.getDouble("weight")))
				.collect(Collectors.toCollection(ArrayList::new));

			for (String s : uniqueGuildName) {
				int slayerRank = -1;
				int skillsRank = -1;
				int catacombsRank = -1;
				int weightRank = -1;

				if (rankTypes.contains("slayer")) {
					for (int j = 0; j < guildSlayer.size(); j++) {
						try {
							if (s.equals(guildSlayer.get(j).getString("username"))) {
								slayerRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (rankTypes.contains("skills")) {
					for (int j = 0; j < guildSkills.size(); j++) {
						try {
							if (s.equals(guildSkills.get(j).getString("username"))) {
								skillsRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (rankTypes.contains("catacombs")) {
					for (int j = 0; j < guildCatacombs.size(); j++) {
						try {
							if (s.equals(guildCatacombs.get(j).getString("username"))) {
								catacombsRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (rankTypes.contains("weight")) {
					for (int j = 0; j < guildWeight.size(); j++) {
						try {
							if (s.equals(guildWeight.get(j).getString("username"))) {
								weightRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (guildName.equals("Skyblock Forceful")) {
					if (slayerRank < skillsRank) {
						guildSkills.set(skillsRank, null);
						if (slayerRank < catacombsRank) {
							guildCatacombs.set(catacombsRank, null);
						} else {
							guildSlayer.set(slayerRank, null);
						}
					} else {
						guildSlayer.set(slayerRank, null);
						if (skillsRank < catacombsRank) {
							guildCatacombs.set(catacombsRank, null);
						} else {
							guildSkills.set(skillsRank, null);
						}
					}
				}
			}

			List<List<DataObject>> guildLeaderboards = new ArrayList<>();

			if (rankTypes.contains("slayer")) {
				guildLeaderboards.add(guildSlayer);
			}
			if (rankTypes.contains("skills")) {
				guildLeaderboards.add(guildSkills);
			}
			if (rankTypes.contains("catacombs")) {
				guildLeaderboards.add(guildCatacombs);
			}
			if (rankTypes.contains("weight")) {
				guildLeaderboards.add(guildWeight);
			}

			JsonArray ranksArr = higherDepth(lbSettings, "ranks").getAsJsonArray();

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);
			int totalChange = 0;
			for (List<DataObject> currentLeaderboard : guildLeaderboards) {
				for (int i = 0; i < currentLeaderboard.size(); i++) {
					DataObject currentPlayer = currentLeaderboard.get(i);
					if (currentPlayer == null) {
						continue;
					}

					if (ignoredRanks.contains(currentPlayer.getString("rank"))) {
						continue;
					}

					String playerRank = currentPlayer.getString("rank");
					String playerUsername = currentPlayer.getString("username");

					for (JsonElement rank : ranksArr) {
						if (i <= higherDepth(rank, "range", 0) - 1) {
							JsonArray rankNames = higherDepth(rank, "names").getAsJsonArray();
							List<String> rankNamesList = new ArrayList<>();
							for (JsonElement rankName : rankNames) {
								rankNamesList.add(rankName.getAsString());
							}

							if (!rankNamesList.contains(playerRank.toLowerCase())) {
								paginateBuilder.addItems(("- /g setrank " + fixUsername(playerUsername) + " " + rankNamesList.get(0)));
								totalChange++;
							}
							break;
						}
					}
				}
			}

			if (paginateBuilder.size() == 0) {
				return defaultEmbed("No rank changes");
			}

			paginateBuilder
				.getExtras()
				.setEveryPageTitle("Rank changes for " + guildName)
				.setEveryPageText("**Total rank changes:** " + totalChange);
			event.paginate(paginateBuilder);
		} else {
			List<String> pbItems = new ArrayList<>();
			int totalChange = 0;
			JsonObject defaultRankObj = higherDepth(lbSettings, "default_rank").getAsJsonObject();
			List<String> defaultRank = streamJsonArray(higherDepth(defaultRankObj, "names")).map(JsonElement::getAsString).toList();
			JsonArray defaultRanksArr = higherDepth(defaultRankObj, "requirements").getAsJsonArray();

			for (DataObject gMember : playerList) {
				if (!defaultRanksArr.isEmpty()) {
					boolean meetsReqOr = false;
					for (JsonElement reqOr : defaultRanksArr) {
						boolean meetsReqAnd = true;
						for (JsonElement reqAnd : reqOr.getAsJsonArray()) {
							double amount = gMember.getDouble(higherDepth(reqAnd, "type").getAsString());

							if (amount < higherDepth(reqAnd, "amount").getAsDouble()) {
								meetsReqAnd = false;
								break;
							}
						}
						meetsReqOr = meetsReqAnd;
						if (meetsReqAnd) {
							break;
						}
					}

					if (!meetsReqOr) {
						pbItems.add("- /g kick " + fixUsername(gMember.getString("username")) + " doesn't meet reqs");
						totalChange++;
						continue;
					}
				}

				int highestRankMet = -1;
				JsonArray gRanks = higherDepth(lbSettings, "ranks").getAsJsonArray();
				for (int i = 0; i < gRanks.size(); i++) {
					JsonElement rank = gRanks.get(i);

					boolean meetsReqOr = false;
					for (JsonElement reqOr : higherDepth(rank, "requirements").getAsJsonArray()) {
						boolean meetsReqAnd = true;
						for (JsonElement reqAnd : reqOr.getAsJsonArray()) {
							double amount = gMember.getDouble(higherDepth(reqAnd, "type").getAsString());

							if (amount < higherDepth(reqAnd, "amount").getAsDouble()) {
								meetsReqAnd = false;
								break;
							}
						}
						meetsReqOr = meetsReqAnd;
						if (meetsReqAnd) {
							break;
						}
					}

					if (meetsReqOr) {
						highestRankMet = Math.max(i, highestRankMet);
					}
				}

				if (highestRankMet != -1) {
					List<String> rankNamesList = streamJsonArray(higherDepth(gRanks.get(highestRankMet), "names").getAsJsonArray())
						.map(JsonElement::getAsString)
						.toList();
					if (!rankNamesList.contains(gMember.getString("username").toLowerCase())) {
						pbItems.add(("- /g setrank " + fixUsername(gMember.getString("username")) + " " + rankNamesList.get(0)));
						totalChange++;
					}
				} else {
					if (!defaultRank.contains(gMember.getString("rank"))) {
						pbItems.add(("- /g setrank " + fixUsername(gMember.getString("username")) + " " + defaultRank.get(0)));
						totalChange++;
					}
				}
			}

			if (pbItems.isEmpty()) {
				return defaultEmbed("No rank changes");
			}

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20).addItems(pbItems.stream().sorted().toList());
			paginateBuilder
				.getExtras()
				.setEveryPageTitle("Rank changes for " + guildName)
				.setEveryPageText("**Total rank changes:** " + totalChange);

			event.paginate(paginateBuilder);
		}

		return null;
	}
}
