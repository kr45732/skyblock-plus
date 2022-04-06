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

import static com.skyblockplus.utils.Constants.ALL_SKILL_NAMES;
import static com.skyblockplus.utils.Constants.SLAYER_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.miscellaneous.weight.senither.Weight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;
import net.dv8tion.jda.api.EmbedBuilder;

public class CalcWeightCommand extends Command {

	public CalcWeightCommand() {
		this.name = "calcweight";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder calculateWeight(String username, String profileName, String type, int amount) {
		if ((SLAYER_NAMES.contains(type) && amount > 500000000) || (!SLAYER_NAMES.contains(type) && amount > 100)) {
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

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

				if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
					return;
				}

				embed(calculateWeight(player, args.length == 3 ? args[2] : null, type, amount));
			}
		}
			.queue();
	}
}
