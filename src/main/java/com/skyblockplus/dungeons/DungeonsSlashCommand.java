/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class DungeonsSlashCommand extends SlashCommand {

	public DungeonsSlashCommand() {
		this.name = "dungeons";
	}

	public static EmbedBuilder getPlayerDungeons(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			try {
				CustomPaginator.Builder paginateBuilder = player
					.defaultPlayerPaginator(PaginatorExtras.PaginatorType.EMBED_FIELDS, event.getUser())
					.setColumns(3)
					.setItemsPerPage(9);
				PaginatorExtras extras = paginateBuilder
					.getExtras()
					.setEveryPageText(
						"**Secrets:** " +
						formatNumber(player.getDungeonSecrets()) +
						"\n**Selected Class:** " +
						capitalizeString(player.getSelectedDungeonClass())
					);

				SkillsStruct skillInfo = player.getCatacombs();
				extras.addEmbedField(
					DUNGEON_EMOJI_MAP.get("catacombs") + " " + capitalizeString(skillInfo.name()) + " (" + skillInfo.currentLevel() + ")",
					simplifyNumber(skillInfo.expCurrent()) +
					" / " +
					simplifyNumber(skillInfo.expForNext()) +
					"\nTotal XP: " +
					simplifyNumber(skillInfo.totalExp()) +
					"\nProgress: " +
					(skillInfo.isMaxed() ? "MAX" : roundProgress(skillInfo.progressToNext())),
					true
				);

				extras.addBlankField(true).addBlankField(true);

				for (String className : DUNGEON_CLASS_NAMES) {
					skillInfo = player.getDungeonClass(className);
					extras.addEmbedField(
						DUNGEON_EMOJI_MAP.get(className) + " " + capitalizeString(className) + " (" + skillInfo.currentLevel() + ")",
						simplifyNumber(skillInfo.expCurrent()) +
						" / " +
						simplifyNumber(skillInfo.expForNext()) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.totalExp()) +
						"\nProgress: " +
						(skillInfo.isMaxed() ? "MAX" : roundProgress(skillInfo.progressToNext())),
						true
					);
				}

				extras.addBlankField(true);

				for (Map.Entry<String, JsonElement> dungeon : higherDepth(player.profileJson(), "dungeons.dungeon_types")
					.getAsJsonObject()
					.entrySet()) {
					boolean isRegular = dungeon.getKey().equals("catacombs");

					int min = (isRegular ? 0 : 1);
					for (int i = min; i <= 7; i++) {
						int fastestSPlusInt = higherDepth(dungeon.getValue(), "fastest_time_s_plus." + i, -1);
						int minutes = fastestSPlusInt / 1000 / 60;
						int seconds = (fastestSPlusInt / 1000) % 60;
						String name = i == 0 ? "Entrance" : ((isRegular ? "Floor " : "Master ") + i);

						String ebStr = "Completions: " + higherDepth(dungeon.getValue(), "tier_completions." + i, 0);
						ebStr += "\nBest Score: " + higherDepth(dungeon.getValue(), "best_score." + i, 0);
						ebStr +=
						"\nFastest S+: " + (fastestSPlusInt != -1 ? minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds) : "None");

						extras.addEmbedField(DUNGEON_EMOJI_MAP.get(dungeon.getKey() + "_" + i) + " " + capitalizeString(name), ebStr, true);
					}

					extras.addBlankField(true);
					if (!isRegular) {
						extras.addBlankField(true);
					}
				}

				event.paginate(paginateBuilder);
				return null;
			} catch (Exception e) {
				return errorEmbed(player.getEscapedUsername() + " has not played dungeons");
			}
		}

		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerDungeons(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the dungeons data of a player")
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
