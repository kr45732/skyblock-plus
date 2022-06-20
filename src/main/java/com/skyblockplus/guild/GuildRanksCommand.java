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
import static com.skyblockplus.utils.structs.HypixelGuildCache.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.*;
import java.io.FileReader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class GuildRanksCommand extends Command {

	public GuildRanksCommand() {
		this.name = "guild-ranks";
		this.cooldown = globalCooldown + 2;
		this.aliases = new String[] { "g-rank", "g-ranks" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getRanks(String username, Player.Gamemode gamemode, boolean useKey, PaginatorEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		if (gamemode == Player.Gamemode.IRONMAN || gamemode == Player.Gamemode.STRANDED) {
			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return invalidEmbed("You must set a valid Hypixel API key to use the ironman or stranded only gamemode");
			}
			useKey = true;
		} else if (useKey) {
			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}
		}

		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonElement guildJson = guildResponse.response();

		String guildId = higherDepth(guildJson, "_id").getAsString();
		String guildName = higherDepth(guildJson, "name").getAsString();
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
				"'s is not setup. Please join the [Skyblock Plus Discord](" +
				DISCORD_SERVER_INVITE_LINK +
				") and mention CrypticPlasma to setup this for your guild."
			);
		}

		String lbType = higherDepth(lbSettings, "lb_type").getAsString();
		List<String> staffRankNames = new ArrayList<>();
		List<String> rankTypes = new ArrayList<>();

		for (JsonElement i : higherDepth(lbSettings, "staff_ranks").getAsJsonArray()) {
			staffRankNames.add(i.getAsString().toLowerCase());
		}

		if (lbType.equals("position")) {
			for (JsonElement i : higherDepth(lbSettings, "types").getAsJsonArray()) {
				rankTypes.add(i.getAsString().toLowerCase());
			}
		}

		boolean ignoreStaff = higherDepth(lbSettings, "ignore_staff").getAsBoolean();

		JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
		List<String> uniqueGuildName = new ArrayList<>();
		List<GuildRanksStruct> gMembers = new ArrayList<>();
		Map<String, String> ranksMap = new HashMap<>();
		for (JsonElement guildM : guildMembers) {
			ranksMap.put(higherDepth(guildM, "uuid").getAsString(), higherDepth(guildM, "rank").getAsString().toLowerCase());
		}

		Instant lastUpdated = null;
		if (useKey) {
			HypixelGuildCache guildCache = hypixelGuildsCacheMap.getIfPresent(guildId);
			List<String> guildMemberPlayersList;
			if (guildCache != null) {
				guildMemberPlayersList = guildCache.getCache(gamemode);
				lastUpdated = guildCache.getLastUpdated();
			} else {
				HypixelGuildCache newGuildCache = new HypixelGuildCache();
				List<CompletableFuture<CompletableFuture<String>>> futuresList = new ArrayList<>();

				for (JsonElement guildMember : guildMembers) {
					String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

					CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);
					futuresList.add(
						guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
							try {
								if (keyCooldownMap.get(hypixelKey).isRateLimited()) {
									System.out.println("Sleeping for " + keyCooldownMap.get(hypixelKey).timeTillReset().get() + " seconds");
									TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).timeTillReset().get());
								}
							} catch (Exception ignored) {}

							CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(
								guildMemberUuid,
								hypixelKey
							);

							return guildMemberProfileJson.thenApply(guildMemberProfileJsonResponse -> {
								Player guildMemberPlayer = new Player(
									guildMemberUuid,
									guildMemberUsernameResponse,
									guildMemberProfileJsonResponse
								);

								if (guildMemberPlayer.isValid()) {
									newGuildCache.addPlayer(guildMemberPlayer);
								}

								return null;
							});
						})
					);
				}

				for (CompletableFuture<CompletableFuture<String>> future : futuresList) {
					try {
						future.get().get();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				guildMemberPlayersList = newGuildCache.getCache(gamemode);
				hypixelGuildsCacheMap.put(guildId, newGuildCache.setLastUpdated());
			}

			for (String lbM : guildMemberPlayersList) {
				String gMemUsername = getStringFromCache(lbM, "username");
				String gMemUuid = getStringFromCache(lbM, "uuid");
				double slayer = getDoubleFromCache(lbM, "slayer");
				double skills = getDoubleFromCache(lbM, "skills");
				double catacombs = getLevelFromCache(lbM, "catacombs");
				double weight = getDoubleFromCache(lbM, "weight");

				String curRank = ranksMap.get(gMemUuid);

				if (curRank != null) {
					if (ignoreStaff && staffRankNames.contains(curRank)) {
						continue;
					}

					gMembers.add(new GuildRanksStruct(gMemUsername, skills, slayer, catacombs, weight, curRank));
					uniqueGuildName.add(gMemUsername);
				}
			}
		} else {
			JsonArray guildLbJson;
			try {
				guildLbJson =
					higherDepth(getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId), "data").getAsJsonArray();
			} catch (Exception e) {
				return invalidEmbed(guildName + " is not on the senither leaderboard. You must run this command with usekey set to true.");
			}
			for (JsonElement lbM : guildLbJson) {
				String lbUuid = higherDepth(lbM, "uuid").getAsString().replace("-", "");
				String curRank = ranksMap.get(lbUuid);

				if (curRank != null) {
					if (ignoreStaff && staffRankNames.contains(curRank)) {
						continue;
					}

					gMembers.add(
						new GuildRanksStruct(
							higherDepth(lbM, "username").getAsString(),
							higherDepth(lbM, "average_skill_progress").getAsDouble(),
							higherDepth(lbM, "total_slayer").getAsDouble(),
							higherDepth(lbM, "catacomb").getAsDouble(),
							higherDepth(lbM, "weight").getAsDouble(),
							curRank
						)
					);
					uniqueGuildName.add(higherDepth(lbM, "username").getAsString());
				}
			}
			lastUpdated =
				Instant.parse(
					higherDepth(
						streamJsonArray(higherDepth(getJson("https://hypixel-app-api.senither.com/leaderboard"), "data").getAsJsonArray())
							.filter(g -> higherDepth(g, "id").getAsString().equals(guildId))
							.findFirst()
							.get(),
						"last_updated_at"
					)
						.getAsString()
				);
		}

		if (lbType.equals("position")) {
			gMembers.sort(Comparator.comparingDouble(o1 -> -o1.slayer()));
			ArrayList<GuildRanksStruct> guildSlayer = new ArrayList<>(gMembers);

			gMembers.sort(Comparator.comparingDouble(o1 -> -o1.skills()));
			ArrayList<GuildRanksStruct> guildSkills = new ArrayList<>(gMembers);

			gMembers.sort(Comparator.comparingDouble(o1 -> -o1.catacombs()));
			ArrayList<GuildRanksStruct> guildCatacombs = new ArrayList<>(gMembers);

			gMembers.sort(Comparator.comparingDouble(o1 -> -o1.weight()));
			ArrayList<GuildRanksStruct> guildWeight = new ArrayList<>(gMembers);

			for (String s : uniqueGuildName) {
				int slayerRank = -1;
				int skillsRank = -1;
				int catacombsRank = -1;
				int weightRank = -1;

				if (rankTypes.contains("slayer")) {
					for (int j = 0; j < guildSlayer.size(); j++) {
						try {
							if (s.equals(guildSlayer.get(j).name())) {
								slayerRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (rankTypes.contains("skills")) {
					for (int j = 0; j < guildSkills.size(); j++) {
						try {
							if (s.equals(guildSkills.get(j).name())) {
								skillsRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (rankTypes.contains("catacombs")) {
					for (int j = 0; j < guildCatacombs.size(); j++) {
						try {
							if (s.equals(guildCatacombs.get(j).name())) {
								catacombsRank = j;
								break;
							}
						} catch (NullPointerException ignored) {}
					}
				}

				if (rankTypes.contains("weight")) {
					for (int j = 0; j < guildWeight.size(); j++) {
						try {
							if (s.equals(guildWeight.get(j).name())) {
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

			ArrayList<ArrayList<GuildRanksStruct>> guildLeaderboards = new ArrayList<>();

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
			for (ArrayList<GuildRanksStruct> currentLeaderboard : guildLeaderboards) {
				for (int i = 0; i < currentLeaderboard.size(); i++) {
					GuildRanksStruct currentPlayer = currentLeaderboard.get(i);
					if (currentPlayer == null) {
						continue;
					}

					if (staffRankNames.contains(currentPlayer.guildRank())) {
						continue;
					}

					String playerRank = currentPlayer.guildRank().toLowerCase();
					String playerUsername = currentPlayer.name();

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

			paginateBuilder
				.getExtras()
				.setEveryPageTitle("Rank changes for " + guildName)
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
				.setEveryPageText(
					"**Total rank changes:** " +
					totalChange +
					(lastUpdated != null ? "\n**Last Updated:** <t:" + lastUpdated.getEpochSecond() + ":R>" : "") +
					"\n"
				);
			event.paginate(paginateBuilder);
		} else {
			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);
			int totalChange = 0;
			List<String> defaultRank = streamJsonArray(higherDepth(lbSettings, "default_role").getAsJsonArray())
				.map(JsonElement::getAsString)
				.collect(Collectors.toList());
			for (GuildRanksStruct gMember : gMembers) {
				int highestRankMet = -1;
				JsonArray gRanks = higherDepth(lbSettings, "ranks").getAsJsonArray();
				for (int i = 0; i < gRanks.size(); i++) {
					JsonElement rank = gRanks.get(i);

					boolean meetsReqOr = false;
					for (JsonElement reqOr : higherDepth(rank, "requirements").getAsJsonArray()) {
						boolean meetsReqAnd = true;
						for (JsonElement reqAnd : reqOr.getAsJsonArray()) {
							double amount =
								switch (higherDepth(reqAnd, "type").getAsString()) {
									case "slayer" -> gMember.slayer();
									case "skills" -> gMember.skills();
									case "catacombs" -> gMember.catacombs();
									case "weight" -> gMember.weight();
									default -> 0;
								};

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
						.collect(Collectors.toList());
					if (!rankNamesList.contains(gMember.guildRank().toLowerCase())) {
						paginateBuilder.addItems(("- /g setrank " + fixUsername(gMember.name()) + " " + rankNamesList.get(0)));
						totalChange++;
					}
				} else {
					if (!defaultRank.contains(gMember.guildRank().toLowerCase())) {
						paginateBuilder.addItems(("- /g setrank " + fixUsername(gMember.name()) + " " + defaultRank.get(0)));
						totalChange++;
					}
				}
			}

			paginateBuilder
				.getExtras()
				.setEveryPageTitle("Rank changes for " + guildName)
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
				.setEveryPageText(
					"**Total rank changes:** " +
					totalChange +
					(lastUpdated != null ? "\n**Last Updated:** <t:" + lastUpdated.getEpochSecond() + ":R>" : "") +
					"\n"
				);
			if (paginateBuilder.size() == 0) {
				return defaultEmbed("No rank changes");
			}
			event.paginate(paginateBuilder);
		}
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 3 || args.length == 2) && args[1].toLowerCase().startsWith("u:")) {
					Player.Gamemode gamemode = getGamemodeOption("mode", Player.Gamemode.ALL);
					boolean useKey = getBooleanOption("--usekey");

					paginate(getRanks(args[1].split(":")[1], gamemode, useKey, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
