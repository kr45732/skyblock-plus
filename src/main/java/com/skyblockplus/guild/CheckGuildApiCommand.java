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
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class CheckGuildApiCommand extends Command {

	public CheckGuildApiCommand() {
		this.name = "check-guild-api";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getGuildCheckApi(String username, PaginatorEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonArray guildMembers = guildResponse.get("members").getAsJsonArray();
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
						Player player = new Player(guildMemberUuid, guildMemberUsernameResponse, guildMemberProfileJsonResponse);

						if (player.isValid()) {
							boolean invEnabled = player.isInventoryApiEnabled();
							boolean bankEnabled = player.isBankApiEnabled();
							boolean collectionsEnabled = player.isCollectionsApiEnabled();
							boolean vaultEnabled = player.isVaultApiEnabled();
							boolean skillsEnabled = player.isSkillsApiEnabled();

							if (invEnabled && bankEnabled && collectionsEnabled && vaultEnabled && skillsEnabled) {
								return client.getSuccess() + " **" + player.getUsername() + ":** all APIs enabled";
							} else {
								String out =
									(invEnabled ? "" : "Inventory API, ") +
									(bankEnabled ? "" : "Bank API, ") +
									(collectionsEnabled ? "" : "Collections API, ") +
									(vaultEnabled ? "" : "Vault API, ") +
									(skillsEnabled ? "" : "Skills API, ");

								return client.getError() + " **" + player.getUsername() + ":** " + out.substring(0, out.length() - 2);
							}
						}
						return client.getError() + " **" + player.getUsername() + ":** unable to get data";
					});
				})
			);
		}

		List<String> out = new ArrayList<>();
		for (CompletableFuture<CompletableFuture<String>> future : futuresList) {
			try {
				out.add(future.get().get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		out.sort(Comparator.comparing(o -> !o.contains(client.getError())));
		CustomPaginator.Builder paginator = event.getPaginator().setItemsPerPage(20);
		paginator.addItems(out);
		event.paginate(
			paginator.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(guildResponse.get("name").getAsString())
					.setEveryPageText("**API Disabled Count:** " + out.stream().filter(o -> o.contains(client.getError())).count() + "\n")
			)
		);
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getGuildCheckApi(player, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
