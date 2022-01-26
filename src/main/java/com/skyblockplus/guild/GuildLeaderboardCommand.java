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

import static com.skyblockplus.Main.database;
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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildLeaderboardCommand extends Command {

	public GuildLeaderboardCommand() {
		this.name = "guild-leaderboard";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-lb" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getLeaderboard(String lbType, String username, Player.Gamemode gamemode, PaginatorEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		if (!isValidType(lbType)) { // Type is invalid, username, or uuid
			return invalidEmbed(lbType + " is an invalid leaderboard type. Use `/help guild-leaderboard` to see valid types");
		}
		lbType = lbType.toLowerCase();

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonElement guildJson = guildResponse.response();
		String guildName = higherDepth(guildJson, "name").getAsString();
		String guildId = higherDepth(guildJson, "_id").getAsString();

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(2).setItemsPerPage(20);
		HypixelGuildCache guildCache = hypixelGuildsCacheMap.getIfPresent(guildId);
		List<String> guildMemberPlayersList;
		Instant lastUpdated = null;

		if (guildCache != null) {
			guildMemberPlayersList = guildCache.getCache(gamemode);
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

						CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey);
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

		String finalLbType = lbType;
		guildMemberPlayersList.sort(Comparator.comparingDouble(cache -> -getDoubleFromCache(cache, finalLbType)));

		int guildRank = -1;
		String amt = "-1";
		for (int i = 0, guildMemberPlayersListSize = guildMemberPlayersList.size(); i < guildMemberPlayersListSize; i++) {
			String guildPlayer = guildMemberPlayersList.get(i);
			String formattedAmt = roundAndFormat(getDoubleFromCache(guildPlayer, lbType));
			String guildPlayerUsername = getStringFromCache(guildPlayer, "username");
			paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(guildPlayerUsername) + ": " + formattedAmt);

			if (guildPlayerUsername.equals(usernameUuidStruct.username())) {
				guildRank = i;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Player:** " +
			usernameUuidStruct.username() +
			"\n**Guild Rank:** #" +
			(guildRank + 1) +
			"\n**" +
			capitalizeString(lbType.replace("_", " ")) +
			":** " +
			amt +
			(lastUpdated != null ? "\n**Last updated:** <t:" + lastUpdated.getEpochSecond() + ":R>" : "");

		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageTitle(guildName)
				.setEveryPageText(ebStr)
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
		);
		event.paginate(paginateBuilder);

		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 4 || args.length == 3) && args[2].toLowerCase().startsWith("u:")) {
					Player.Gamemode gamemode = Player.Gamemode.of(getStringOption("mode"));

					paginate(getLeaderboard(args[1], args[2].split(":")[1], gamemode, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
