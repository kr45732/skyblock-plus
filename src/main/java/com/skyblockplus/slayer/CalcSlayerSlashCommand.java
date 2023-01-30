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

package com.skyblockplus.slayer;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.skyblockplus.miscellaneous.weight.weight.Weight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.WeightStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class CalcSlayerSlashCommand extends SlashCommand {

	public CalcSlayerSlashCommand() {
		this.name = "calcslayer";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(
			getCalcSlayer(
				event.player,
				event.getOptionStr("profile"),
				event.getOptionStr("type"),
				event.getOptionInt("level", -1),
				event.getOptionInt("xp", -1),
				Player.WeightType.of(event.getOptionStr("system", "senither"))
			)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Calculate the number of slayer bosses needed to reach a certain level or xp amount")
			.addOptions(
				new OptionData(OptionType.STRING, "type", "Slayer type", true)
					.addChoice("Sven Packmaster", "sven")
					.addChoice("Revenant Horror", "rev")
					.addChoice("Tarantula Broodfather", "tara")
					.addChoice("Voidgloom Seraph", "enderman")
					.addChoice("Inferno Demonlord", "blaze"),
				new OptionData(OptionType.INTEGER, "level", "Target slayer level").setRequiredRange(1, 9),
				new OptionData(OptionType.INTEGER, "xp", "Target slayer xp").setMinValue(1),
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

	public static EmbedBuilder getCalcSlayer(
		String username,
		String profileName,
		String slayerType,
		int targetLevel,
		long targetXp,
		Player.WeightType weightType
	) {
		slayerType = slayerType.toLowerCase();
		if (!SLAYER_NAMES.contains(slayerType)) {
			return invalidEmbed("Invalid slayer type");
		}

		if (targetXp <= 0 && targetLevel <= 0) {
			return invalidEmbed("Target xp or target level must be provided and at least 1");
		}
		if (targetLevel != -1 && (targetLevel <= 0 || targetLevel > 9)) {
			return invalidEmbed("Target level must be between 1 and 9");
		}

		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			int curXp = player.getSlayer(slayerType);
			targetXp =
				targetLevel != -1
					? higherDepth(getLevelingJson(), "slayer_xp." + SLAYER_NAMES_MAP.get(slayerType) + ".[" + (targetLevel - 1) + "]")
						.getAsLong()
					: targetXp;

			if (curXp >= targetXp) {
				return invalidEmbed("You already have " + roundAndFormat(targetXp) + " xp");
			}

			long xpNeeded = targetXp - curXp;
			JsonArray bossXpArr = higherDepth(getLevelingJson(), "slayer_boss_xp").getAsJsonArray();
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < (slayerType.equals("rev") ? 5 : 4); i++) {
				double xpPerBoss = bossXpArr.get(i).getAsInt();
				int killsNeeded = (int) Math.ceil(xpNeeded / xpPerBoss);
				long cost =
					killsNeeded *
					switch (i) {
						case 0 -> 2000L;
						case 1 -> 7500L;
						case 2 -> 20000L;
						case 3 -> 50000L;
						default -> 100000L;
					};

				out
					.append("\n")
					.append(SLAYER_EMOJI_MAP.get(slayerType))
					.append(" Tier ")
					.append(toRomanNumerals(i + 1).toUpperCase())
					.append(" ")
					.append(capitalizeString(slayerType))
					.append(": ")
					.append(formatNumber(killsNeeded))
					.append(" ($")
					.append(formatNumber(cost))
					.append(")");
			}

			Weight weight = Weight.of(weightType, player).calculateWeight(slayerType);
			Weight predictedWeight = Weight.of(weightType, player).calculateWeight(slayerType);
			WeightStruct pre = weight.getSlayerWeight().getSlayerWeight(slayerType);
			WeightStruct post = predictedWeight.getSlayerWeight().getSlayerWeight(slayerType, (int) targetXp);

			return player
				.defaultPlayerEmbed()
				.setDescription(
					"**Current XP:** " +
					roundAndFormat(curXp) +
					"\n**Target XP:** " +
					roundAndFormat(targetXp) +
					"\n**XP Needed:** " +
					formatNumber(xpNeeded)
				)
				.addField("Bosses Needed", out.toString(), false)
				.addField(
					"Weight Change",
					"Total: " +
					weight.getTotalWeight().getFormatted(false) +
					" ➜ " +
					predictedWeight.getTotalWeight().getFormatted(false) +
					"\n" +
					capitalizeString(slayerType) +
					": " +
					pre.getFormatted(false) +
					" ➜ " +
					post.getFormatted(false),
					false
				);
		}

		return player.getFailEmbed();
	}
}
