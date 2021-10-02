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
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.structs.HypixelGuildCache.memberCacheFromPlayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildLeaderboardsCommand extends Command {

	public GuildLeaderboardsCommand() {
		this.name = "guild-leaderboard";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-lb" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 && args[2].toLowerCase().startsWith("u:")) {
					paginate(getLeaderboard(args[1], args[2].split(":")[1], event));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getLeaderboard(String lbType, String username, CommandEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		int lbTypeNum;
		switch (lbType) {
			case "slayer":
				lbTypeNum = 1;
				break;
			case "skills":
				lbTypeNum = 2;
				break;
			case "catacombs":
				lbTypeNum = 3;
				break;
			case "weight":
				lbTypeNum = 4;
				break;
			case "sven_xp":
				lbTypeNum = 5;
				break;
			case "rev_xp":
				lbTypeNum = 6;
				break;
			case "tara_xp":
				lbTypeNum = 7;
				break;
			case "enderman_xp":
				lbTypeNum = 8;
				break;
			default:
				return defaultEmbed(
					lbType +
					" is an invalid leaderboard type. Valid types are: `slayer`, `skills`, `catacombs`, `weight`, `sven_xp`, `rev_xp`, `tara_xp`, and `enderman_xp`"
				);
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.getFailCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.getUuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		JsonElement guildJson = guildResponse.getResponse();
		String guildName = higherDepth(guildJson, "name").getAsString();
		String guildId = higherDepth(guildJson, "_id").getAsString();

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(2).setItemsPerPage(20);
		HypixelGuildCache guildCache = hypixelGuildsCacheMap.getOrDefault(guildId, null);
		List<String> guildMemberPlayersList = new ArrayList<>();
		Instant lastUpdated = null;

		if (guildCache != null && Duration.between(guildCache.lastUpdated, Instant.now()).toMinutes() < 15) {
			guildMemberPlayersList = guildCache.membersCache;
			lastUpdated = guildCache.lastUpdated;
		} else {
			JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
			List<CompletableFuture<CompletableFuture<String>>> futuresList = new ArrayList<>();

			for (JsonElement guildMember : guildMembers) {
				String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

				CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);
				futuresList.add(
					guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
						try {
							if (keyCooldownMap.get(hypixelKey).getRemainingLimit().get() < 5) {
								System.out.println("Sleeping for " + keyCooldownMap.get(hypixelKey).getTimeTillReset().get() + " seconds");
								TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).getTimeTillReset().get());
							}
						} catch (Exception ignored) {}

						CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey);

						return guildMemberProfileJson.thenApply(guildMemberProfileJsonResponse -> {
							Player guildMemberPlayer = new Player(
								guildMemberUuid,
								guildMemberUsernameResponse,
								guildMemberProfileJsonResponse
							);

							if (guildMemberPlayer.isValid()) {
								return memberCacheFromPlayer(guildMemberPlayer);
							}

							return null;
						});
					})
				);
			}

			for (CompletableFuture<CompletableFuture<String>> future : futuresList) {
				try {
					String playerFutureResponse = future.get().get();
					if (playerFutureResponse != null) {
						guildMemberPlayersList.add(playerFutureResponse);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			hypixelGuildsCacheMap.put(guildId, new HypixelGuildCache(Instant.now(), guildMemberPlayersList));
		}

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.split("=:=")[lbTypeNum])));

		int guildRank = -1;
		String amt = "-1";
		for (int i = 0, guildMemberPlayersListSize = guildMemberPlayersList.size(); i < guildMemberPlayersListSize; i++) {
			String[] guildPlayer = guildMemberPlayersList.get(i).split("=:=");
			String formattedAmt = roundAndFormat(Double.parseDouble(guildPlayer[lbTypeNum]));
			paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(guildPlayer[0]) + ": " + formattedAmt);

			if (guildPlayer[0].equals(usernameUuidStruct.getUsername())) {
				guildRank = i;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Player:** " +
			usernameUuidStruct.getUsername() +
			"\n**Guild Rank:** #" +
			(guildRank + 1) +
			"\n**" +
			capitalizeString(lbType.replace("_", " ")) +
			":** " +
			amt +
			(lastUpdated != null ? "\n**Last updated:** " + instantToDHM(Duration.between(lastUpdated, Instant.now())) + " ago" : "");

		paginateBuilder
			.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(guildName)
					.setEveryPageText(ebStr)
					.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
			)
			.build()
			.paginate(event.getChannel(), 0);

		return null;
	}
}
