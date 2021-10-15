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
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildStatsCommand extends Command {

	public GuildStatsCommand() {
		this.name = "guild-statistics";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "guild-stats", "g-stats" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 4 || args.length == 3) && args[2].toLowerCase().startsWith("u:")) {
					paginate(getStatistics(args[2].split(":")[1], new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public static EmbedBuilder getStatistics(String username, PaginatorEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
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

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(2).setItemsPerPage(20);
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

		return null;
	}
}
