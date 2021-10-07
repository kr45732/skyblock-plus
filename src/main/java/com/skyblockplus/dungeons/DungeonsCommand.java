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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.DUNGEON_CLASS_NAMES;
import static com.skyblockplus.utils.Constants.DUNGEON_EMOJI_MAP;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.EmbedBuilder;

public class DungeonsCommand extends Command {

	public DungeonsCommand() {
		this.name = "dungeons";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "cata", "catacombs" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerDungeons(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			try {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(3).setItemsPerPage(9);
				PaginatorExtras extras = new PaginatorExtras();
				extras
					.setEveryPageTitle(player.getUsername())
					.setEveryPageTitleUrl(player.skyblockStatsLink())
					.setEveryPageText(
						"**Secrets:** " +
						formatNumber(player.getDungeonSecrets()) +
						"\n**Selected class:** " +
						player.getSelectedDungeonClass()
					);

				SkillsStruct skillInfo = player.getCatacombs();
				extras.addEmbedField(
					DUNGEON_EMOJI_MAP.get("catacombs") +
					" " +
					capitalizeString(skillInfo.getName()) +
					" (" +
					skillInfo.getCurrentLevel() +
					")",
					simplifyNumber(skillInfo.getExpCurrent()) +
					" / " +
					simplifyNumber(skillInfo.getExpForNext()) +
					"\nTotal XP: " +
					simplifyNumber(skillInfo.getTotalExp()) +
					"\nProgress: " +
					(skillInfo.isMaxed() ? "MAX" : roundProgress(skillInfo.getProgressToNext())),
					true
				);

				extras.addBlankField(true).addBlankField(true);

				for (String className : DUNGEON_CLASS_NAMES) {
					skillInfo = player.getDungeonClass(className);
					extras.addEmbedField(
						DUNGEON_EMOJI_MAP.get(className) + " " + capitalizeString(className) + " (" + skillInfo.getCurrentLevel() + ")",
						simplifyNumber(skillInfo.getExpCurrent()) +
						" / " +
						simplifyNumber(skillInfo.getExpForNext()) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.getTotalExp()) +
						"\nProgress: " +
						(skillInfo.isMaxed() ? "MAX" : roundProgress(skillInfo.getProgressToNext())),
						true
					);
				}

				extras.addBlankField(true);

				for (String dungeonType : getJsonKeys(higherDepth(player.profileJson(), "dungeons.dungeon_types"))) {
					JsonElement curDungeonType = higherDepth(player.profileJson(), "dungeons.dungeon_types." + dungeonType);
					int min = (dungeonType.equals("catacombs") ? 0 : 1);
					int max = (dungeonType.equals("catacombs") ? 8 : 7);
					for (int i = min; i < max; i++) {
						int fastestSPlusInt = higherDepth(curDungeonType, "fastest_time_s_plus." + i, -1);
						int minutes = fastestSPlusInt / 1000 / 60;
						int seconds = fastestSPlusInt / 1000 % 60;
						String name = i == 0 ? "Entrance" : ((dungeonType.equals("catacombs") ? "Floor " : "Master ") + i);

						String ebStr = "Completions: " + higherDepth(curDungeonType, "tier_completions." + i, 0);
						ebStr += "\nBest Score: " + higherDepth(curDungeonType, "best_score." + i, 0);
						ebStr +=
							"\nFastest S+: " + (fastestSPlusInt != -1 ? minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds) : "None");

						extras.addEmbedField(DUNGEON_EMOJI_MAP.get(dungeonType + "_" + i) + " " + capitalizeString(name), ebStr, true);
					}

					if (dungeonType.equals("catacombs")) {
						extras.addBlankField(true);
					}
				}

				event.paginate(paginateBuilder.setPaginatorExtras(extras));
				return null;
			} catch (Exception e) {
				return invalidEmbed("Player has not played dungeons");
			}
		}

		return invalidEmbed(player.getFailCause());
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

					paginate(getPlayerDungeons(username, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
