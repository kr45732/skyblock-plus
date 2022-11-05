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

import com.skyblockplus.miscellaneous.weight.weight.Weight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class CalcWeightSlashCommand extends SlashCommand {

	public CalcWeightSlashCommand() {
		this.name = "calcweight";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(
			calculateWeight(
				event.player,
				event.getOptionStr("profile"),
				event.getOptionStr("type"),
				event.getOptionInt("amount", 0),
				Player.WeightType.of(event.getOptionStr("system", "senither"))
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Calculate predicted weight change for a reaching certain skill/slayer/catacombs amount")
			.addOptions(
				new OptionData(OptionType.STRING, "type", "Skill, slayer, or dungeon type to see change of", true)
					.addChoice("Sven", "sven")
					.addChoice("Revenant", "rev")
					.addChoice("Tarantula", "tara")
					.addChoice("Enderman", "enderman")
					.addChoice("Blaze", "blaze")
					.addChoice("Alchemy", "alchemy")
					.addChoice("Combat", "combat")
					.addChoice("Farming", "farming")
					.addChoice("Mining", "mining")
					.addChoice("Fishing", "fishing")
					.addChoice("Taming", "taming")
					.addChoice("Enchanting", "enchanting")
					.addChoice("Foraging", "foraging")
					.addChoice("Catacombs", "catacombs"),
				new OptionData(OptionType.INTEGER, "amount", "Target xp (slayers) or level", true).setRequiredRange(0, 500000000),
				new OptionData(OptionType.STRING, "system", "Weight system that should be used")
					.addChoice("Senither", "senither")
					.addChoice("Lily", "lily")
			)
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder calculateWeight(String username, String profileName, String type, int amount, Player.WeightType weightType) {
		if ((SLAYER_NAMES.contains(type) && amount > 500000000) || (!SLAYER_NAMES.contains(type) && amount > 100)) {
			return invalidEmbed("Invalid amount");
		}

		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Weight weight = Weight.of(weightType, player).calculateWeight(type);
			Weight predictedWeight = Weight.of(weightType, player).calculateWeight(type);
			WeightStruct pre;
			WeightStruct post;
			EmbedBuilder eb = player.defaultPlayerEmbed();
			if (type.equals("catacombs")) {
				SkillsStruct current = player.getCatacombs();
				SkillsStruct target = player.skillInfoFromLevel(amount, type);
				pre = weight.getDungeonsWeight().getDungeonWeight();
				post = predictedWeight.getDungeonsWeight().getDungeonWeight(target);

				eb
					.addField(
						"Current",
						"Level: " + roundAndFormat(current.getProgressLevel()) + "\nXP: " + formatNumber(current.totalExp()),
						false
					)
					.addField(
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
			} else if (ALL_SKILL_NAMES.contains(type)) {
				SkillsStruct current = player.getSkill(type);
				if (current == null) {
					return invalidEmbed("Skills API disabled");
				}
				SkillsStruct target = player.skillInfoFromLevel(amount, type);
				pre = weight.getSkillsWeight().getSkillsWeight(type);
				post = predictedWeight.getSkillsWeight().getSkillsWeight(type, target);

				eb
					.addField(
						"Current",
						"Level: " + roundAndFormat(current.getProgressLevel()) + "\nXP: " + formatNumber(current.totalExp()),
						false
					)
					.addField(
						"Target",
						"Level: " +
						amount +
						"\nXP: " +
						formatNumber(target.totalExp()) +
						" (+" +
						formatNumber(target.totalExp() - current.totalExp()) +
						")",
						false
					)
					.addField(
						"Skill Average Change",
						roundAndFormat(player.getSkillAverage()) + " ➜ " + roundAndFormat(player.getSkillAverage(type, amount)),
						false
					);
			} else if (SLAYER_NAMES.contains(type)) {
				int curXp = player.getSlayer(type);
				pre = weight.getSlayerWeight().getSlayerWeight(type);
				post = predictedWeight.getSlayerWeight().getSlayerWeight(type, amount);

				eb
					.addField("Current", "Level: " + player.getSlayerLevel(type) + "\nXP: " + formatNumber(curXp), false)
					.addField(
						"Target",
						"Level: " +
						player.getSlayerLevel(type, amount) +
						"\nXP: " +
						formatNumber(amount) +
						" (+" +
						formatNumber(amount - curXp) +
						")",
						false
					)
					.addField(
						"Total Slayer XP Change",
						roundAndFormat(player.getTotalSlayer()) + " ➜ " + roundAndFormat(player.getTotalSlayer(type, amount)),
						false
					);
			} else {
				return invalidEmbed("Invalid type");
			}

			if (post.getRaw() <= pre.getRaw()) {
				return invalidEmbed("You cannot choose a lower level or xp than your current amount");
			}

			return eb.addField(
				capitalizeString(weightType.name()) + " Weight Change",
				"Total: " +
				weight.getTotalWeight().getFormatted(false) +
				" ➜ " +
				predictedWeight.getTotalWeight().getFormatted(false) +
				"\n" +
				capitalizeString(type) +
				": " +
				pre.getFormatted(false) +
				" ➜ " +
				post.getFormatted(false) +
				" (+" +
				roundAndFormat(post.getRaw() - pre.getRaw()) +
				")",
				false
			);
		}
		return player.getFailEmbed();
	}
}
