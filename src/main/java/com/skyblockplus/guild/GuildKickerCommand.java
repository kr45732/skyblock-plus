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
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class GuildKickerCommand extends Command {

	public GuildKickerCommand() {
		this.name = "guild-kicker";
		this.cooldown = globalCooldown + 2;
		this.aliases = new String[] { "g-kicker" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getGuildKicker(String username, String reqs, boolean useKey, PaginatorEvent event) {
		String[] reqsArr = reqs.split("] \\[");
		if (reqsArr.length > 3) {
			return invalidEmbed("You can only enter a maximum of 3 sets of requirements");
		}
		for (int i = 0; i < reqsArr.length; i++) {
			String[] indvReqs = reqsArr[i].replace("[", "").replace("]", "").split("\\s+");
			for (String indvReq : indvReqs) {
				String[] reqDashSplit = indvReq.split(":");
				if (reqDashSplit.length != 2) {
					return invalidEmbed(indvReq + " is an invalid requirement format");
				}

				if (
					!reqDashSplit[0].equals("slayer") &&
					!reqDashSplit[0].equals("skills") &&
					!reqDashSplit[0].equals("catacombs") &&
					!reqDashSplit[0].equals("weight")
				) {
					return invalidEmbed(indvReq + " is an invalid requirement type");
				}

				try {
					Double.parseDouble(reqDashSplit[1]);
				} catch (Exception e) {
					return invalidEmbed(indvReq + " is an invalid requirement value");
				}
			}

			reqsArr[i] = reqsArr[i].replace("[", "").replace("]", "");
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}
		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.failCause());
		}
		JsonElement guildJson = guildResponse.response();

		String guildId = higherDepth(guildJson, "_id").getAsString();
		JsonElement guildLbJson = getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId);

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);
		if (!useKey) {
			if (higherDepth(guildLbJson, "data") == null) {
				return invalidEmbed(
					"This guild is not on the senither leaderboard so you must set the Hypixel API key for this server and rerun the command with `--usekey` flag"
				);
			}

			JsonArray guildMembers = higherDepth(guildLbJson, "data").getAsJsonArray();

			int missingReqsCount = 0;
			for (JsonElement guildMember : guildMembers) {
				double slayer = higherDepth(guildMember, "total_slayer").getAsDouble();
				double skills = higherDepth(guildMember, "average_skill_progress").getAsDouble();
				double catacombs = higherDepth(guildMember, "catacomb").getAsDouble();
				double weight = higherDepth(guildMember, "raw_weight.total").getAsDouble();

				boolean meetsReqs = false;

				for (String req : reqsArr) {
					String[] reqSplit = req.split("\\s+");
					double slayerReq = 0;
					double skillsReq = 0;
					double catacombsReq = 0;
					double weightReq = 0;
					for (String reqIndividual : reqSplit) {
						switch (reqIndividual.split(":")[0]) {
							case "slayer" -> slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "skills" -> skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "catacombs" -> catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "weight" -> weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
						}
					}

					if (slayer >= slayerReq && Math.max(0, skills) >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
						meetsReqs = true;
						break;
					}
				}

				if (!meetsReqs) {
					paginateBuilder.addItems(
						"• **" +
						higherDepth(guildMember, "username").getAsString() +
						"** | Slayer: " +
						formatNumber(slayer) +
						" | Skills: " +
						roundAndFormat(skills) +
						" | Cata: " +
						roundAndFormat(catacombs) +
						" | Weight: " +
						roundAndFormat(weight)
					);
					missingReqsCount++;
				}
			}

			paginateBuilder
				.getPaginatorExtras()
				.setEveryPageTitle("Guild Kick Helper")
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
				.setEveryPageText(
					"**Total missing requirements:** " +
					missingReqsCount +
					"\n**Updated:** <t:" +
					Instant
						.parse(
							higherDepth(
								streamJsonArray(
									higherDepth(getJson("https://hypixel-app-api.senither.com/leaderboard"), "data").getAsJsonArray()
								)
									.filter(g -> higherDepth(g, "id").getAsString().equals(guildId))
									.findFirst()
									.get(),
								"last_updated_at"
							)
								.getAsString()
						)
						.getEpochSecond() +
					":R>\n"
				);
		} else {
			String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}

			HypixelGuildCache guildCache = hypixelGuildsCacheMap.getIfPresent(guildId);
			List<String> guildMemberPlayersList;
			Instant lastUpdated = null;

			if (guildCache != null) {
				guildMemberPlayersList = guildCache.getCache();
				lastUpdated = guildCache.getLastUpdated();
			} else {
				HypixelGuildCache newGuildCache = new HypixelGuildCache();
				JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
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

				guildMemberPlayersList = newGuildCache.getCache();
				hypixelGuildsCacheMap.put(guildId, newGuildCache.setLastUpdated());
			}

			for (String guildMember : guildMemberPlayersList) {
				double slayer = getDoubleFromCache(guildMember, "slayer");
				double skills = getDoubleFromCache(guildMember, "skills");
				double catacombs = getLevelFromCache(guildMember, "catacombs");
				double weight = getDoubleFromCache(guildMember, "weight");

				boolean meetsReqs = false;

				for (String req : reqsArr) {
					String[] reqSplit = req.split("\\s+");
					double slayerReq = 0;
					double skillsReq = 0;
					double catacombsReq = 0;
					double weightReq = 0;
					for (String reqIndividual : reqSplit) {
						switch (reqIndividual.split(":")[0]) {
							case "slayer" -> slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "skills" -> skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "catacombs" -> catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
							case "weight" -> weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
						}
					}

					if (slayer >= slayerReq && Math.max(0, skills) >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
						meetsReqs = true;
						break;
					}
				}

				if (!meetsReqs) {
					paginateBuilder.addItems(
						"• **" +
						getStringFromCache(guildMember, "username") +
						"** | Slayer: " +
						formatNumber(slayer) +
						" | Skills: " +
						roundAndFormat(skills) +
						" | Cata: " +
						roundAndFormat(catacombs) +
						" | Weight: " +
						roundAndFormat(weight)
					);
				}
			}

			paginateBuilder
				.getPaginatorExtras()
				.setEveryPageTitle("Guild Kick Helper")
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
				.setEveryPageText(
					"**Total missing requirements:** " +
					paginateBuilder.size() +
					(lastUpdated != null ? "\n**Last Updated:** <t:" + lastUpdated.getEpochSecond() + ":R>" : "") +
					"\n"
				);
		}
		event.paginate(paginateBuilder);
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				String content = event.getMessage().getContentRaw();
				boolean useKey = false;
				if (content.contains("--usekey")) {
					useKey = true;
					content = content.replace("--usekey", "").trim();
				}
				args = content.split("\\s+", 3);

				if (args.length == 3 && args[1].toLowerCase().startsWith("u:")) {
					paginate(getGuildKicker(args[1].split(":")[1], args[2], useKey, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
