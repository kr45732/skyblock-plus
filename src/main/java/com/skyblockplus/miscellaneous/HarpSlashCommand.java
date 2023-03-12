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

import static com.skyblockplus.utils.Constants.HARP_SONG_ID_TO_NAME;
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.roundAndFormat;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class HarpSlashCommand extends SlashCommand {

	public HarpSlashCommand() {
		this.name = "harp";
	}

	public static EmbedBuilder getHarp(String username, String profileName) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			JsonElement harpJson = higherDepth(player.profileJson(), "harp_quest");
			if (harpJson == null) {
				return defaultEmbed("Player has not used the harp");
			}

			EmbedBuilder eb = player
				.defaultPlayerEmbed()
				.setDescription(
					"**Last Played Song:** " +
					HARP_SONG_ID_TO_NAME.get(higherDepth(harpJson, "selected_song", "None")) +
					"\n**Claimed Melody's Hair:** " +
					higherDepth(harpJson, "claimed_talisman", false)
				);

			for (Map.Entry<String, JsonElement> song : harpJson.getAsJsonObject().entrySet()) {
				if (song.getKey().startsWith("song_") && song.getKey().endsWith("_completions")) {
					String songId = song.getKey().split("song_")[1].split("_completions")[0];
					if (HARP_SONG_ID_TO_NAME.containsKey(songId)) {
						eb.addField(
							HARP_SONG_ID_TO_NAME.get(songId),
							"Completions: " +
							song.getValue().getAsInt() +
							"\nBest Score: " +
							roundAndFormat(higherDepth(harpJson, "song_" + songId + "_best_completion", 0.0) * 100) +
							"%\nPerfect Completions: " +
							higherDepth(harpJson, "song_" + songId + "_perfect_completions", 0),
							true
						);
					}
				}
			}
			for (int i = 0; i < 3 - eb.getFields().size() % 3; i++) {
				eb.addBlankField(true);
			}

			return eb;
		}
		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getHarp(event.player, event.getOptionStr("profile")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's harp statistics")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
