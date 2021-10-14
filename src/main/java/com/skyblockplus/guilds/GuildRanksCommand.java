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

package com.skyblockplus.guilds;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.structs.HypixelGuildCache.getDoubleFromCache;
import static com.skyblockplus.utils.structs.HypixelGuildCache.getStringFromCache;

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
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildRanksCommand extends Command {

	public GuildRanksCommand() {
		this.name = "guild-ranks";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-rank", "g-ranks" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 3 || args.length == 2) && args[1].toLowerCase().startsWith("u:")) {
					boolean ironmanOnly = false;
					for (int i = 0; i < args.length; i++) {
						if (args[i].startsWith("mode:")) {
							ironmanOnly = args[i].split("mode:")[1].equals("ironman");
							removeArg(i);
						}
					}

					boolean useKey = false;
					for (int i = 0; i < args.length; i++) {
						if (args[i].equals("--usekey")) {
							useKey = true;
							removeArg(i);
						}
					}

					paginate(getLeaderboard(args[1].split(":")[1], ironmanOnly, useKey, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public static EmbedBuilder getLeaderboard(String username, boolean ironmanOnly, boolean useKey, PaginatorEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		if (ironmanOnly) {
			if (hypixelKey == null) {
				return invalidEmbed("You must set a Hypixel API key to use the ironman only option");
			}
			try {
				higherDepth(getJson("https://api.hypixel.net/key?key=" + hypixelKey), "record.key").getAsString();
			} catch (Exception e) {
				return invalidEmbed("You must set a valid Hypixel API key to use the ironman only option");
			}
			if (!keyCooldownMap.containsKey(hypixelKey)) {
				keyCooldownMap.put(hypixelKey, new HypixelKeyInformation());
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
			return invalidEmbed(usernameUuid.getFailCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.getUuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		JsonElement guildJson = guildResponse.getResponse();

		String guildId = higherDepth(guildJson, "_id").getAsString();
		String guildName = higherDepth(guildJson, "name").getAsString();
		if (!guildName.equals("Skyblock Forceful") && !guildName.equals("Skyblock Gods") && !guildName.equals("Ironman Sweats")) {
			return invalidEmbed(
				guildName +
				"'s settings are not setup. Please join the [Skyblock Plus Discord](" +
				DISCORD_SERVER_INVITE_LINK +
				") to setup this for your guild."
			);
		}

		List<String> staffRankNames = new ArrayList<>();
		JsonElement lbSettings;
		List<String> rankTypes = new ArrayList<>();

		try {
			lbSettings =
				higherDepth(
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json")),
					guildId + ".guild_leaderboard"
				);

			for (JsonElement i : higherDepth(lbSettings, "staff_ranks").getAsJsonArray()) {
				staffRankNames.add(i.getAsString().toLowerCase());
			}

			for (JsonElement i : higherDepth(lbSettings, "types").getAsJsonArray()) {
				rankTypes.add(i.getAsString().toLowerCase());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return defaultEmbed("Error getting data");
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
				guildMemberPlayersList = guildCache.getCache(ironmanOnly);
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
								if (keyCooldownMap.get(hypixelKey).getRemainingLimit().get() < 5) {
									System.out.println(
										"Sleeping for " + keyCooldownMap.get(hypixelKey).getTimeTillReset().get() + " seconds"
									);
									TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).getTimeTillReset().get());
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

				guildMemberPlayersList = newGuildCache.getCache(ironmanOnly);
				hypixelGuildsCacheMap.put(guildId, newGuildCache.setLastUpdated());
			}

			for (String lbM : guildMemberPlayersList) {
				String gMemUsername = getStringFromCache(lbM, "username");
				String gMemUuid = getStringFromCache(lbM, "uuid");
				double slayer = getDoubleFromCache(lbM, "slayer");
				double skills = getDoubleFromCache(lbM, "skills");
				double catacombs = getDoubleFromCache(lbM, "catacombs");
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
			JsonArray guildLbJson = higherDepth(getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId), "data")
				.getAsJsonArray();
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
					Instant mLastUpdated = Instant.parse(higherDepth(lbM, "last_updated_at").getAsString());
					lastUpdated = lastUpdated == null || mLastUpdated.isBefore(lastUpdated) ? mLastUpdated : lastUpdated;
				}
			}
		}

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSlayer()));
		ArrayList<GuildRanksStruct> guildSlayer = new ArrayList<>(gMembers);

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSkills()));
		ArrayList<GuildRanksStruct> guildSkills = new ArrayList<>(gMembers);

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getCatacombs()));
		ArrayList<GuildRanksStruct> guildCatacombs = new ArrayList<>(gMembers);

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getWeight()));
		ArrayList<GuildRanksStruct> guildWeight = new ArrayList<>(gMembers);

		for (String s : uniqueGuildName) {
			int slayerRank = -1;
			int skillsRank = -1;
			int catacombsRank = -1;
			int weightRank = -1;

			if (rankTypes.contains("slayer")) {
				for (int j = 0; j < guildSlayer.size(); j++) {
					try {
						if (s.equals(guildSlayer.get(j).getName())) {
							slayerRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (rankTypes.contains("skills")) {
				for (int j = 0; j < guildSkills.size(); j++) {
					try {
						if (s.equals(guildSkills.get(j).getName())) {
							skillsRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (rankTypes.contains("catacombs")) {
				for (int j = 0; j < guildCatacombs.size(); j++) {
					try {
						if (s.equals(guildCatacombs.get(j).getName())) {
							catacombsRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (rankTypes.contains("weight")) {
				for (int j = 0; j < guildWeight.size(); j++) {
					try {
						if (s.equals(guildWeight.get(j).getName())) {
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

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(20);
		int totalChange = 0;
		for (ArrayList<GuildRanksStruct> currentLeaderboard : guildLeaderboards) {
			for (int i = 0; i < currentLeaderboard.size(); i++) {
				GuildRanksStruct currentPlayer = currentLeaderboard.get(i);
				if (currentPlayer == null) {
					continue;
				}

				if (staffRankNames.contains(currentPlayer.getGuildRank())) {
					continue;
				}

				String playerRank = currentPlayer.getGuildRank().toLowerCase();
				String playerUsername = currentPlayer.getName();

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

		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageTitle("Rank changes for " + guildName)
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
				.setEveryPageText(
					"**Total rank changes:** " +
					totalChange +
					(
						lastUpdated != null
							? "\n**Last updated:** " + instantToDHM(Duration.between(lastUpdated, Instant.now())) + " ago"
							: ""
					) +
					"\n"
				)
		);
		event.paginate(paginateBuilder);

		return null;
	}
}
