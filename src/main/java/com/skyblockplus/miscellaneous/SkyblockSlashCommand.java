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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Constants.DUNGEON_EMOJI_MAP;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class SkyblockSlashCommand extends SlashCommand {

	public SkyblockSlashCommand() {
		this.name = "skyblock";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getSkyblock(event.player, event.getOptionStr("profile"), new PaginatorEvent(event)));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get an overview of a player's Skyblock statistics")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getSkyblock(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginator = event.getPaginator();
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);
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

			eb = player.defaultPlayerEmbed();
			double trueSA = 0;
			double progressSA = 0;
			for (String skillName : ALL_SKILL_NAMES) {
				SkillsStruct skillInfo = player.getSkill(skillName);
				if (skillInfo != null) {
					eb.addField(
						SKILLS_EMOJI_MAP.get(skillName) + " " + capitalizeString(skillInfo.name()) + " (" + skillInfo.currentLevel() + ")",
						simplifyNumber(skillInfo.expCurrent()) +
						" / " +
						simplifyNumber(skillInfo.expForNext()) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.totalExp()) +
						"\nProgress: " +
						(skillInfo.isMaxed() ? "MAX" : roundProgress(skillInfo.progressToNext())),
						true
					);
					if (!COSMETIC_SKILL_NAMES.contains(skillName)) {
						trueSA += skillInfo.currentLevel();
						progressSA += skillInfo.getProgressLevel();
					}
				} else {
					eb.addField(SKILLS_EMOJI_MAP.get(skillName) + " " + capitalizeString(skillName) + " (?) ", "Unable to retrieve", true);
				}
			}
			trueSA /= SKILL_NAMES.size();
			progressSA /= SKILL_NAMES.size();
			eb.setDescription(
				"**True Skill Average:** " + roundAndFormat(trueSA) + "\n**Progress Skill Average:** " + roundAndFormat(progressSA)
			);
			extras.addEmbedPage(eb);

			eb = player.defaultPlayerEmbed();
			int svenOneKills = player.getSlayerBossKills("wolf", 0);
			int svenTwoKills = player.getSlayerBossKills("wolf", 1);
			int svenThreeKills = player.getSlayerBossKills("wolf", 2);
			int svenFourKills = player.getSlayerBossKills("wolf", 3);
			int revOneKills = player.getSlayerBossKills("zombie", 0);
			int revTwoKills = player.getSlayerBossKills("zombie", 1);
			int revThreeKills = player.getSlayerBossKills("zombie", 2);
			int revFourKills = player.getSlayerBossKills("zombie", 3);
			int revFiveKills = player.getSlayerBossKills("zombie", 4);
			int taraOneKills = player.getSlayerBossKills("spider", 0);
			int taraTwoKills = player.getSlayerBossKills("spider", 1);
			int taraThreeKills = player.getSlayerBossKills("spider", 2);
			int taraFourKills = player.getSlayerBossKills("spider", 3);
			int endermanOneKills = player.getSlayerBossKills("enderman", 0);
			int endermanTwoKills = player.getSlayerBossKills("enderman", 1);
			int endermanThreeKills = player.getSlayerBossKills("enderman", 2);
			int endermanFourKills = player.getSlayerBossKills("enderman", 3);
			int blazeOneKills = player.getSlayerBossKills("blaze", 0);
			int blazeTwoKills = player.getSlayerBossKills("blaze", 1);
			int blazeThreeKills = player.getSlayerBossKills("blaze", 2);
			int blazeFourKills = player.getSlayerBossKills("blaze", 3);
			String svenKills =
				"**Tier 1:** " +
				svenOneKills +
				"\n**Tier 2:** " +
				svenTwoKills +
				"\n**Tier 3:** " +
				svenThreeKills +
				"\n**Tier 4:** " +
				svenFourKills;
			String revKills =
				"**Tier 1:** " +
				revOneKills +
				"\n**Tier 2:** " +
				revTwoKills +
				"\n**Tier 3:** " +
				revThreeKills +
				"\n**Tier 4:** " +
				revFourKills +
				"\n**Tier 5:** " +
				revFiveKills;
			String taraKills =
				"**Tier 1:** " +
				taraOneKills +
				"\n**Tier 2:** " +
				taraTwoKills +
				"\n**Tier 3:** " +
				taraThreeKills +
				"\n**Tier 4:** " +
				taraFourKills;
			String endermanKills =
				"**Tier 1:** " +
				endermanOneKills +
				"\n**Tier 2:** " +
				endermanTwoKills +
				"\n**Tier 3:** " +
				endermanThreeKills +
				"\n**Tier 4:** " +
				endermanFourKills;
			String blazeKills =
				"**Tier 1:** " +
				blazeOneKills +
				"\n**Tier 2:** " +
				blazeTwoKills +
				"\n**Tier 3:** " +
				blazeThreeKills +
				"\n**Tier 4:** " +
				blazeFourKills;
			long coinsSpentOnSlayers =
				2000L *
				(svenOneKills + revOneKills + taraOneKills + endermanOneKills) +
				7500L *
				(svenTwoKills + revTwoKills + taraTwoKills + endermanTwoKills) +
				20000L *
				(svenThreeKills + revThreeKills + taraThreeKills + endermanThreeKills) +
				50000L *
				(svenFourKills + revFourKills + taraFourKills + endermanFourKills) +
				100000L *
				revFiveKills +
				10000L *
				blazeOneKills +
				25000L *
				blazeTwoKills +
				60000L *
				blazeThreeKills +
				150000L *
				blazeFourKills;
			eb.setDescription(
				"**Total Slayer:** " +
				formatNumber(player.getTotalSlayer()) +
				" XP\n**Total Coins Spent:** " +
				simplifyNumber(coinsSpentOnSlayers)
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("sven") + " Wolf (" + player.getSlayerLevel("sven") + ")",
				simplifyNumber(player.getSlayer("sven")) + " XP",
				true
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("rev") + " Zombie (" + player.getSlayerLevel("rev") + ")",
				simplifyNumber(player.getSlayer("rev")) + " XP",
				true
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("tara") + " Spider (" + player.getSlayerLevel("tara") + ")",
				simplifyNumber(player.getSlayer("tara")) + " XP",
				true
			);
			eb.addField("Boss Kills", svenKills, true);
			eb.addField("Boss Kills", revKills, true);
			eb.addField("Boss Kills", taraKills, true);
			eb.addField(
				SLAYER_EMOJI_MAP.get("enderman") + " Enderman (" + player.getSlayerLevel("enderman") + ")",
				simplifyNumber(player.getSlayer("enderman")) + " XP",
				true
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("blaze") + " Blaze (" + player.getSlayerLevel("blaze") + ")",
				simplifyNumber(player.getSlayer("blaze")) + " XP",
				true
			);
			eb.addBlankField(true);
			eb.addField("Boss Kills", endermanKills, true);
			eb.addField("Boss Kills", blazeKills, true);
			eb.addBlankField(true);
			eb.addBlankField(true);
			extras.addEmbedPage(eb);

			try {
				eb =
					player
						.defaultPlayerEmbed()
						.setDescription(
							"**Secrets:** " +
							formatNumber(player.getDungeonSecrets()) +
							"\n**Selected Class:** " +
							player.getSelectedDungeonClass()
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
				for (String dungeonType : getJsonKeys(higherDepth(player.profileJson(), "dungeons.dungeon_types"))) {
					JsonElement curDungeonType = higherDepth(player.profileJson(), "dungeons.dungeon_types." + dungeonType);
					int min = (dungeonType.equals("catacombs") ? 0 : 1);
					int embedCount = 0;
					for (int i = min; i < 8; i++) {
						if (higherDepth(curDungeonType, "tier_completions." + i, 0) == 0) {
							continue;
						}

						int fastestSPlusInt = higherDepth(curDungeonType, "fastest_time_s_plus." + i, -1);
						int minutes = fastestSPlusInt / 1000 / 60;
						int seconds = fastestSPlusInt / 1000 % 60;
						String name = i == 0 ? "Entrance" : ((dungeonType.equals("catacombs") ? "Floor " : "Master ") + i);

						String ebStr = "Completions: " + higherDepth(curDungeonType, "tier_completions." + i, 0);
						ebStr += "\nBest Score: " + higherDepth(curDungeonType, "best_score." + i, 0);
						ebStr +=
							"\nFastest S+: " + (fastestSPlusInt != -1 ? minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds) : "None");

						eb.addField(DUNGEON_EMOJI_MAP.get(dungeonType + "_" + i) + " " + capitalizeString(name), ebStr, true);
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

			event.paginate(paginator.setPaginatorExtras(extras));
			return null;
		}

		return player.getFailEmbed();
	}
}
