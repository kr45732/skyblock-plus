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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.skills.SkillsSlashCommand;
import com.skyblockplus.slayer.SlayerSlashCommand;
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
public class SkyblockSlashCommand extends SlashCommand {

	public SkyblockSlashCommand() {
		this.name = "skyblock";
	}

	public static EmbedBuilder getSkyblock(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginator = event.getPaginator(PaginatorExtras.PaginatorType.EMBED_PAGES);
			PaginatorExtras extras = paginator.getExtras();
			SenitherWeight weight = new SenitherWeight(player, true);
			LilyWeight lilyWeight = new LilyWeight(player, true);

			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.addField(getEmoji("SAPLING") + " Skill Average", roundAndFormat(player.getSkillAverage()), true);
			eb.addField(getEmoji("OVERFLUX_CAPACITOR") + " Total Slayer XP", formatNumber(player.getTotalSlayer()), true);
			eb.addField(DUNGEON_EMOJI_MAP.get("catacombs") + " Catacombs", roundAndFormat(player.getCatacombs().getProgressLevel()), true);
			eb.addField(getEmoji("TRAINING_WEIGHTS") + " Senither Weight", weight.getTotalWeight().getFormatted(), true);
			eb.addField("\uD83C\uDFC5 Senither Stage", weight.getStage(), true);
			eb.addField(getEmoji("TRAINING_WEIGHTS") + " Lily weight", lilyWeight.getTotalWeight().getFormatted(), true);
			eb.addField("\uD83C\uDFC5 Lily Stage", lilyWeight.getStage(), true);
			double playerNetworth = player.getNetworth();
			eb.addField(
				getEmoji("ENCHANTED_GOLD") + " Networth",
				playerNetworth == -1 ? "Inventory API disabled" : roundAndFormat(playerNetworth),
				true
			);
			eb.addField(
				getEmoji("PIGGY_BANK") + " Bank & purse coins",
				(player.getBankBalance() == -1 ? "API disabled" : simplifyNumber(player.getBankBalance())) +
				" + " +
				simplifyNumber(player.getPurseCoins()),
				true
			);
			extras.addEmbedPage(eb);

			extras.addEmbedPage(SkillsSlashCommand.getPlayerSkillsFirstPage(player));

			extras.addEmbedPage(SlayerSlashCommand.getPlayerSlayer(player));

			try {
				eb =
					player
						.defaultPlayerEmbed()
						.setDescription(
							"**Secrets:** " +
							formatNumber(player.getDungeonSecrets()) +
							"\n**Selected Class:** " +
							capitalizeString(player.getSelectedDungeonClass())
						);
				SkillsStruct skillInfo = player.getCatacombs();
				eb.addField(
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
				eb.addBlankField(true).addBlankField(true);
				for (String className : DUNGEON_CLASS_NAMES) {
					skillInfo = player.getDungeonClass(className);
					eb.addField(
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
				eb.addBlankField(true);
				for (Map.Entry<String, JsonElement> dungeon : higherDepth(player.profileJson(), "dungeons.dungeon_types")
					.getAsJsonObject()
					.entrySet()) {
					boolean isRegular = dungeon.getKey().equals("catacombs");

					int min = (isRegular ? 0 : 1);
					int embedCount = 0;
					for (int i = min; i < 8; i++) {
						if (higherDepth(dungeon.getValue(), "tier_completions." + i, 0) == 0) {
							continue;
						}

						int fastestSPlusInt = higherDepth(dungeon.getValue(), "fastest_time_s_plus." + i, -1);
						int minutes = fastestSPlusInt / 1000 / 60;
						int seconds = fastestSPlusInt / 1000 % 60;
						String name = i == 0 ? "Entrance" : ((isRegular ? "Floor " : "Master ") + i);

						String ebStr = "Completions: " + higherDepth(dungeon.getValue(), "tier_completions." + i, 0);
						ebStr += "\nBest Score: " + higherDepth(dungeon.getValue(), "best_score." + i, 0);
						ebStr +=
							"\nFastest S+: " + (fastestSPlusInt != -1 ? minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds) : "None");

						eb.addField(DUNGEON_EMOJI_MAP.get(dungeon.getKey() + "_" + i) + " " + capitalizeString(name), ebStr, true);
						embedCount++;
					}

					for (int i = 0; i < Math.ceil(embedCount / 3.0) * 3 - embedCount; i++) {
						eb.addBlankField(true);
					}
				}
				extras.addEmbedPage(eb);
			} catch (Exception e) {
				extras.addEmbedPage(player.defaultPlayerEmbed().setDescription("Player has not played dungeons"));
			}

			event.paginate(paginator);
			return null;
		}

		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getSkyblock(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get an overview of a player's Skyblock statistics")
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
