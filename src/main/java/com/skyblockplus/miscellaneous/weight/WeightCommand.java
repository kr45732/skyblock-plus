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

package com.skyblockplus.miscellaneous.weight;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.miscellaneous.weight.senither.Weight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;
import net.dv8tion.jda.api.EmbedBuilder;

public class WeightCommand extends Command {

	public WeightCommand() {
		this.name = "weight";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "we" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder calculateWeight(String username, String profileName, String type, int amount) {
		if ((SLAYER_NAMES.contains(type) && amount > 500000000) || (amount > 100)) {
			return invalidEmbed("Invalid amount");
		}

		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			if (type.equals("catacombs")) {
				SkillsStruct current = player.getCatacombs();
				SkillsStruct target = player.skillInfoFromLevel(amount, type);
				eb.addField(
					"Current",
					"Level: " + roundAndFormat(current.getProgressLevel()) + "\nXP: " + formatNumber(current.totalExp()),
					false
				);
				eb.addField(
					"Target",
					"Level: " +
					amount +
					"\nXP: " +
					formatNumber(target.totalExp()) +
					" (+" +
					formatNumber(target.totalExp() - current.totalExp()) +
					")",
					false
				);
				Weight weight = new Weight(player).calculateWeight(type);
				Weight predictedWeight = new Weight(player).calculateWeight(type);
				WeightStruct pre = weight.getDungeonsWeight().getDungeonWeight(type);
				WeightStruct post = predictedWeight.getDungeonsWeight().getDungeonWeight(type, target);
				eb.addField(
					"Weight Change",
					"Total: " +
					weight.getTotalWeight().getFormatted(false) +
					" ➜ " +
					predictedWeight.getTotalWeight().getFormatted(false) +
					"\n" +
					capitalizeString(type) +
					": " +
					pre.getFormatted(false) +
					" ➜ " +
					post.getFormatted(false),
					false
				);
				return eb;
			} else if (ALL_SKILL_NAMES.contains(type)) {
				SkillsStruct current = player.getSkill(type);
				SkillsStruct target = player.skillInfoFromLevel(amount, type);
				eb.addField(
					"Current",
					"Level: " + roundAndFormat(current.getProgressLevel()) + "\nXP: " + formatNumber(current.totalExp()),
					false
				);
				eb.addField(
					"Target",
					"Level: " +
					amount +
					"\nXP: " +
					formatNumber(target.totalExp()) +
					" (+" +
					formatNumber(target.totalExp() - current.totalExp()) +
					")",
					false
				);
				Weight weight = new Weight(player).calculateWeight(type);
				Weight predictedWeight = new Weight(player).calculateWeight(type);
				WeightStruct pre = weight.getSkillsWeight().getSkillsWeight(type);
				WeightStruct post = predictedWeight.getSkillsWeight().getSkillsWeight(type, target);
				eb.addField(
					"Skill Average Change",
					roundAndFormat(player.getSkillAverage()) + " ➜ " + roundAndFormat(player.getSkillAverage(type, amount)),
					false
				);
				eb.addField(
					"Weight Change",
					"Total: " +
					weight.getTotalWeight().getFormatted(false) +
					" ➜ " +
					predictedWeight.getTotalWeight().getFormatted(false) +
					"\n" +
					capitalizeString(type) +
					": " +
					pre.getFormatted(false) +
					" ➜ " +
					post.getFormatted(false),
					false
				);
				return eb;
			} else if (SLAYER_NAMES.contains(type)) {
				int curXp = player.getSlayer(type);
				eb.addField("Current", "Level: " + player.getSlayerLevel(type) + "\nXP: " + formatNumber(curXp), false);
				eb.addField(
					"Target",
					"Level: " +
					player.getSlayerLevel(type, amount) +
					"\nXP: " +
					formatNumber(amount) +
					" (+" +
					formatNumber(amount - curXp) +
					")",
					false
				);
				Weight weight = new Weight(player).calculateWeight(type);
				Weight predictedWeight = new Weight(player).calculateWeight(type);
				WeightStruct pre = weight.getSlayerWeight().getSlayerWeight(type);
				WeightStruct post = predictedWeight.getSlayerWeight().getSlayerWeight(type, amount);
				eb.addField(
					"Slayer Change",
					roundAndFormat(player.getTotalSlayer()) + " ➜ " + roundAndFormat(player.getTotalSlayer(type, amount)),
					false
				);
				eb.addField(
					"Weight Change",
					"Total: " +
					weight.getTotalWeight().getFormatted(false) +
					" ➜ " +
					predictedWeight.getTotalWeight().getFormatted(false) +
					"\n" +
					capitalizeString(type) +
					": " +
					pre.getFormatted(false) +
					" ➜ " +
					post.getFormatted(false),
					false
				);
				return eb;
			} else {
				return invalidEmbed("Invalid type");
			}
		}
		return player.getFailEmbed();
	}

	public static EmbedBuilder getPlayerWeight(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(3);
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

			Weight weight = new Weight(player);
			EmbedBuilder eb = player.defaultPlayerEmbed(" | Senither Weight");
			StringBuilder slayerStr = new StringBuilder();
			for (String slayerName : SLAYER_NAMES) {
				slayerStr
					.append(SLAYER_EMOJI_MAP.get(slayerName))
					.append(" ")
					.append(capitalizeString(slayerName))
					.append(": ")
					.append(weight.getSlayerWeight().getSlayerWeight(slayerName).getFormatted())
					.append("\n");
			}
			StringBuilder skillsStr = new StringBuilder();
			for (String skillName : SKILL_NAMES) {
				skillsStr
					.append(SKILLS_EMOJI_MAP.get(skillName))
					.append(" ")
					.append(capitalizeString(skillName))
					.append(": ")
					.append(weight.getSkillsWeight().getSkillsWeight(skillName).getFormatted())
					.append("\n");
			}
			StringBuilder dungeonsStr = new StringBuilder();

			dungeonsStr
				.append(DUNGEON_EMOJI_MAP.get("catacombs"))
				.append(" ")
				.append(capitalizeString("catacombs"))
				.append(": ")
				.append(weight.getDungeonsWeight().getDungeonWeight("catacombs").getFormatted())
				.append("\n");
			for (String dungeonClassName : DUNGEON_CLASS_NAMES) {
				dungeonsStr
					.append(DUNGEON_EMOJI_MAP.get(dungeonClassName))
					.append(" ")
					.append(capitalizeString(dungeonClassName))
					.append(": ")
					.append(weight.getDungeonsWeight().getClassWeight(dungeonClassName).getFormatted())
					.append("\n");
			}

			eb.addField("Slayer | " + weight.getSlayerWeight().getWeightStruct().getFormatted(), slayerStr.toString(), false);
			eb.addField("Skills | " + weight.getSkillsWeight().getWeightStruct().getFormatted(), skillsStr.toString(), false);
			eb.addField("Dungeons | " + weight.getDungeonsWeight().getWeightStruct().getFormatted(), dungeonsStr.toString(), false);
			eb.setDescription("**Total Weight:** " + weight.getTotalWeight().getFormatted());
			extras.addEmbedPage(eb);

			com.skyblockplus.miscellaneous.weight.lily.Weight lilyWeight = new com.skyblockplus.miscellaneous.weight.lily.Weight(player);
			EmbedBuilder lilyEb = player.defaultPlayerEmbed(" | Lily Weight");
			StringBuilder lilySlayerStr = new StringBuilder();
			for (String slayerName : SLAYER_NAMES) {
				lilySlayerStr
					.append(SLAYER_EMOJI_MAP.get(slayerName))
					.append(" ")
					.append(capitalizeString(slayerName))
					.append(": ")
					.append(lilyWeight.getSlayerWeight().getSlayerWeight(slayerName).getFormatted())
					.append("\n");
			}
			StringBuilder lilySkillsStr = new StringBuilder();
			for (String skillName : SKILL_NAMES) {
				lilySkillsStr
					.append(SKILLS_EMOJI_MAP.get(skillName))
					.append(" ")
					.append(capitalizeString(skillName))
					.append(": ")
					.append(lilyWeight.getSkillsWeight().getSkillsWeight(skillName).getFormatted())
					.append("\n");
			}
			String lilyDungeonsStr =
				DUNGEON_EMOJI_MAP.get("catacombs") +
				" Catacombs: " +
				lilyWeight.getDungeonsWeight().getDungeonWeight().getFormatted() +
				"\n" +
				DUNGEON_EMOJI_MAP.get("catacombs_1") +
				" Normal floor completions: " +
				lilyWeight.getDungeonsWeight().getDungeonCompletionWeight("normal").getFormatted() +
				"\n" +
				DUNGEON_EMOJI_MAP.get("master_catacombs_1") +
				" Master floor completions: " +
				lilyWeight.getDungeonsWeight().getDungeonCompletionWeight("master").getFormatted() +
				"\n";

			lilyEb.addField("Slayer | " + lilyWeight.getSlayerWeight().getWeightStruct().getFormatted(), lilySlayerStr.toString(), false);
			lilyEb.addField("Skills | " + lilyWeight.getSkillsWeight().getWeightStruct().getFormatted(), lilySkillsStr.toString(), false);
			lilyEb.addField("Dungeons | " + lilyWeight.getDungeonsWeight().getWeightStruct().getFormatted(), lilyDungeonsStr, false);
			lilyEb.setDescription("**Total Weight:** " + lilyWeight.getTotalWeight().getFormatted());
			extras.addEmbedPage(lilyEb);

			event.paginate(paginateBuilder.setPaginatorExtras(extras));
			return null;
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length >= 2 && args[1].equals("calculate")) {
					String type = getStringOption("type");
					int amount = getIntOption("amount");

					if (type == null) {
						embed(invalidEmbed("Type is not provided or invalid"));
						return;
					}
					if (amount < 0) {
						embed(invalidEmbed("Amount is not provided or invalid"));
						return;
					}

					if (getMentionedUsername(args.length == 2 ? -2 : 2)) {
						return;
					}

					embed(calculateWeight(player, args.length == 4 ? args[3] : null, type, amount));
					return;
				} else if (args.length == 4 || args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerWeight(player, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
