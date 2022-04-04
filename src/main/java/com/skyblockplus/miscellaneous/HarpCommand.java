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

import static com.skyblockplus.utils.Constants.HARP_SONG_ID_TO_NAME;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class HarpCommand extends Command {

	public HarpCommand() {
		this.name = "harp";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getHarp(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
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
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getHarp(player, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
