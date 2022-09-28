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
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class ProfilesSlashCommand extends SlashCommand {

	public ProfilesSlashCommand() {
		this.name = "profiles";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerProfiles(event.player, event));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a information about all of a player's profiles")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getPlayerProfiles(String username, SlashCommandEvent event) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (!usernameUuid.isValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse profilesJson = skyblockProfilesFromUuid(usernameUuid.uuid());
		if (!profilesJson.isValid()) {
			return invalidEmbed(profilesJson.failCause());
		}

		List<CompletableFuture<String>> profileUsernameFutureList = new ArrayList<>();

		for (JsonElement profile : profilesJson.response().getAsJsonArray()) {
			List<String> uuids = getJsonKeys(higherDepth(profile, "members"));

			for (String uuid : uuids) {
				profileUsernameFutureList.add(
					asyncUuidToUsername(uuid).thenApplyAsync(playerUsername -> "\n• " + fixUsername(playerUsername), executor)
				);
			}
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator();

		List<String> pageTitlesUrls = new ArrayList<>();
		int count = 0;
		for (JsonElement profile : profilesJson.response().getAsJsonArray()) {
			pageTitlesUrls.add(skyblockStatsLink(usernameUuid.uuid(), higherDepth(profile, "cute_name").getAsString()));
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
}
