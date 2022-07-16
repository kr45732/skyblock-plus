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
import static com.skyblockplus.utils.structs.HypixelGuildCache.getDoubleFromCache;
import static com.skyblockplus.utils.structs.HypixelGuildCache.getStringFromCache;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class GuildStatisticsCommand extends Command {

	public GuildStatisticsCommand() {
		this.name = "guild-statistics";
		this.cooldown = globalCooldown + 2;
		this.aliases = new String[] { "guild-stats", "g-stats" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getStatistics(String username, String guildName, String serverId) {
		String hypixelKey = database.getServerHypixelApiKey(serverId);

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		HypixelResponse guildResponse;
		if (username != null) {
			UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
			if (usernameUuidStruct.isNotValid()) {
				return invalidEmbed(usernameUuidStruct.failCause());
			}

			guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
		} else {
			guildResponse = getGuildFromName(guildName);
		}
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonElement guildJson = guildResponse.response();
		guildName = higherDepth(guildJson, "name").getAsString();
		String guildId = higherDepth(guildJson, "_id").getAsString();

		HypixelGuildCache guildCache = hypixelGuildsCacheMap.getIfPresent(guildId);
		List<String> guildMemberPlayersList;
		Instant lastUpdated = null;

		if (guildCache != null) {
			guildMemberPlayersList = guildCache.getCache();
			lastUpdated = guildCache.getLastUpdated();
		} else {
			if (hypixelGuildQueue.contains(guildId)) {
				return invalidEmbed("This guild is currently updating, please try again in a couple of seconds");
			}

			hypixelGuildQueue.add(guildId);

			HypixelGuildCache newGuildCache = new HypixelGuildCache();
			JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
			List<CompletableFuture<String>> futuresList = new ArrayList<>();

			for (JsonElement guildMember : guildMembers) {
				String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

				try {
					if (keyCooldownMap.get(hypixelKey).isRateLimited()) {
						System.out.println("Sleeping for " + keyCooldownMap.get(hypixelKey).getTimeTillReset() + " seconds");
						TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).getTimeTillReset());
					}
				} catch (Exception ignored) {}

				futuresList.add(
					asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey)
						.thenApply(guildMemberProfileJsonResponse -> {
							Player guildMemberPlayer = new Player(
								guildMemberUuid,
								usernameToUuid(guildMemberUuid).username(),
								guildMemberProfileJsonResponse
							);

							if (guildMemberPlayer.isValid()) {
								newGuildCache.addPlayer(guildMemberPlayer);
							}
							return null;
						})
				);
			}

			for (CompletableFuture<String> future : futuresList) {
				try {
					future.get();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			guildMemberPlayersList = newGuildCache.getCache();
			hypixelGuildsCacheMap.put(guildId, newGuildCache.setLastUpdated());

			hypixelGuildQueue.remove(guildId);
		}

		List<String> slayerLb = guildMemberPlayersList
                .stream()
                .sorted(Comparator.comparingDouble(m -> -getDoubleFromCache(m, "slayer"))).toList();
		List<String> skillsLb = guildMemberPlayersList
                .stream()
                .sorted(Comparator.comparingDouble(m -> -getDoubleFromCache(m, "skills"))).toList();
		List<String> cataLb = guildMemberPlayersList
                .stream()
                .sorted(Comparator.comparingDouble(m -> -getDoubleFromCache(m, "catacombs"))).toList();
		List<String> weightLb = guildMemberPlayersList
                .stream()
                .sorted(Comparator.comparingDouble(m -> -getDoubleFromCache(m, "weight"))).toList();

		double averageSlayer = slayerLb.stream().mapToDouble(m -> getDoubleFromCache(m, "slayer")).sum() / slayerLb.size();
		double averageSkills = skillsLb.stream().mapToDouble(m -> getDoubleFromCache(m, "skills")).sum() / skillsLb.size();
		double averageCata = cataLb.stream().mapToDouble(m -> getDoubleFromCache(m, "catacombs")).sum() / cataLb.size();
		double averageWeight = weightLb.stream().mapToDouble(m -> getDoubleFromCache(m, "weight")).sum() / weightLb.size();

		eb = defaultEmbed(guildName, "https://hypixel-leaderboard.senither.com/guilds/" + guildId);
		eb.setDescription(
			"**Average Slayer XP:** " +
			roundAndFormat(averageSlayer) +
			"\n**Average Skills Level:** " +
			roundAndFormat(averageSkills) +
			"\n**Average Catacombs Level:** " +
			roundAndFormat(averageCata) +
			"\n**Average Weight:** " +
			roundAndFormat(averageWeight) +
			(lastUpdated != null ? "\n**Last Updated:** <t:" + lastUpdated.getEpochSecond() + ":R>" : "")
		);
		StringBuilder slayerStr = new StringBuilder();
		for (int i = 0; i < Math.min(5, slayerLb.size()); i++) {
			String cur = slayerLb.get(i);
			slayerStr
				.append("`")
				.append(i + 1)
				.append(")` ")
				.append(fixUsername(getStringFromCache(cur, "username")))
				.append(": ")
				.append(roundAndFormat(getDoubleFromCache(cur, "slayer")))
				.append("\n");
		}
		StringBuilder skillsStr = new StringBuilder();
		for (int i = 0; i < Math.min(5, skillsLb.size()); i++) {
			String cur = skillsLb.get(i);
			skillsStr
				.append("`")
				.append(i + 1)
				.append(")` ")
				.append(fixUsername(getStringFromCache(cur, "username")))
				.append(": ")
				.append(roundAndFormat(getDoubleFromCache(cur, "skills")))
				.append("\n");
		}
		StringBuilder cataStr = new StringBuilder();
		for (int i = 0; i < Math.min(5, cataLb.size()); i++) {
			String cur = cataLb.get(i);
			cataStr
				.append("`")
				.append(i + 1)
				.append(")` ")
				.append(fixUsername(getStringFromCache(cur, "username")))
				.append(": ")
				.append(roundAndFormat(getDoubleFromCache(cur, "catacombs")))
				.append("\n");
		}
		StringBuilder weightStr = new StringBuilder();
		for (int i = 0; i < Math.min(5, weightLb.size()); i++) {
			String cur = weightLb.get(i);
			weightStr
				.append("`")
				.append(i + 1)
				.append(")` ")
				.append(fixUsername(getStringFromCache(cur, "username")))
				.append(": ")
				.append(roundAndFormat(getDoubleFromCache(cur, "weight")))
				.append("\n");
		}
		eb.addField("Top 5 Slayer", slayerStr.toString(), true);
		eb.addField("Top 5 Skills", skillsStr.toString(), true);
		eb.addBlankField(true);
		eb.addField("Top 5 Catacombs", cataStr.toString(), true);
		eb.addField("Top 5 Weight", weightStr.toString(), true);
		eb.addBlankField(true);

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				setArgs(2);
				if (args.length == 2 && args[1].startsWith("g:")) {
					embed(getStatistics(null, args[1].split("g:")[1], event.getGuild().getId()));
				} else {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getStatistics(player, null, event.getGuild().getId()));
				}
			}
		}
			.queue();
	}
}
