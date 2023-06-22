/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.escapeUsername;
import static com.skyblockplus.utils.utils.StringUtils.skyblockStatsLink;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class ProfilesSlashCommand extends SlashCommand {

	public ProfilesSlashCommand() {
		this.name = "profiles";
	}

	public static EmbedBuilder getPlayerProfiles(String username, SlashCommandEvent event) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (!usernameUuid.isValid()) {
			return errorEmbed(usernameUuid.failCause());
		}

		HypixelResponse profilesJson = skyblockProfilesFromUuid(usernameUuid.uuid());
		if (!profilesJson.isValid()) {
			return profilesJson.getErrorEmbed();
		}

		Map<String, CompletableFuture<String>> uuidToUsername = new HashMap<>();
		for (JsonElement profile : profilesJson.response().getAsJsonArray()) {
			for (String uuid : higherDepth(profile, "members").getAsJsonObject().keySet()) {
				if (!uuidToUsername.containsKey(uuid)) {
					uuidToUsername.put(uuid, asyncUuidToUsername(uuid));
				}
			}
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator();

		List<String> pageTitlesUrls = new ArrayList<>();
		for (JsonElement profile : profilesJson.response().getAsJsonArray()) {
			pageTitlesUrls.add(skyblockStatsLink(usernameUuid.uuid(), higherDepth(profile, "cute_name").getAsString()));
			JsonObject members = higherDepth(profile, "members").getAsJsonObject();

			StringBuilder profileStr = new StringBuilder(
				"• **Profile Name:** " +
				higherDepth(profile, "cute_name").getAsString() +
				Player.Gamemode.of(higherDepth(profile, "game_mode", "regular")).getSymbol(" ")
			);

			profileStr.append("\n• **Member Count:** ").append(members.size());
			profileStr.append("\n\n**Members:** ");

			for (String uuid : members.keySet()) {
				try {
					profileStr.append("\n• ").append(escapeUsername(uuidToUsername.get(uuid).get()));
				} catch (Exception ignored) {}
			}
			paginateBuilder.addStrings(profileStr.toString());
		}

		paginateBuilder.getExtras().setEveryPageTitle(usernameUuid.username()).setTitleUrls(pageTitlesUrls);
		event.paginate(paginateBuilder);
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerProfiles(event.player, event));
	}

	@Override
	public SlashCommandData getCommandData() {
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
}
