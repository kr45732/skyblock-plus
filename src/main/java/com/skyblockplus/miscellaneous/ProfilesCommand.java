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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProfilesCommand extends Command {

	public ProfilesCommand() {
		this.name = "profiles";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerProfiles(String username, PaginatorEvent event) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse profilesJson = skyblockProfilesFromUuid(usernameUuid.uuid());
		if (profilesJson.isNotValid()) {
			return invalidEmbed(profilesJson.failCause());
		}

		List<CompletableFuture<String>> profileUsernameFutureList = new ArrayList<>();

		for (JsonElement profile : profilesJson.response().getAsJsonArray()) {
			List<String> uuids = getJsonKeys(higherDepth(profile, "members"));

			for (String uuid : uuids) {
				profileUsernameFutureList.add(
					asyncUuidToUsername(uuid)
						.thenApply(playerUsername -> {
							String lastLogin =
								"<t:" +
								Instant.ofEpochMilli(higherDepth(profile, "members." + uuid + ".last_save").getAsLong()).getEpochSecond() +
								">";

							return "\n• " + fixUsername(playerUsername) + " played on " + lastLogin;
						})
				);
			}
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator();

		List<String> pageTitlesUrls = new ArrayList<>();
		int count = 0;
		for (JsonElement profile : profilesJson.response().getAsJsonArray()) {
			pageTitlesUrls.add(skyblockStatsLink(usernameUuid.username(), higherDepth(profile, "cute_name").getAsString()));
			StringBuilder profileStr = new StringBuilder(
				"• **Profile Name:** " +
				higherDepth(profile, "cute_name").getAsString() +
				Player.Gamemode.of(higherDepth(profile, "game_mode", "regular")).getSymbol(" ")
			);
			List<String> uuids = getJsonKeys(higherDepth(profile, "members"));
			profileStr.append("\n• **Member Count:** ").append(uuids.size());
			profileStr.append("\n\n**Members:** ");

			for (String ignored1 : uuids) {
				try {
					profileStr.append(profileUsernameFutureList.get(count).get());
				} catch (Exception ignored) {}
				count++;
			}
			paginateBuilder.addItems(profileStr.toString());
		}

		paginateBuilder.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle(usernameUuid.username()).setTitleUrls(pageTitlesUrls));

		event.paginate(paginateBuilder);
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

					paginate(getPlayerProfiles(player, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
