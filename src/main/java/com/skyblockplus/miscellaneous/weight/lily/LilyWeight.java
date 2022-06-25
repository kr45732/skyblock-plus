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

package com.skyblockplus.miscellaneous.weight.lily;

import static com.skyblockplus.utils.Constants.SKILL_NAMES;
import static com.skyblockplus.utils.Constants.SLAYER_NAMES;

import com.skyblockplus.miscellaneous.weight.weight.Weight;
import com.skyblockplus.utils.Player;

public class LilyWeight extends Weight {

	public LilyWeight(Player player) {
		this(player, false);
	}

	public LilyWeight(Player player, boolean calculateWeight) {
		super(calculateWeight, new LilySlayerWeight(player), new LilySkillsWeight(player),new LilyDungeonsWeight(player));
	}

	@Override
	public LilySkillsWeight getSkillsWeight() {
		return (LilySkillsWeight) skillsWeight;
	}

	@Override
	public LilySlayerWeight getSlayerWeight() {
		return (LilySlayerWeight) slayerWeight;
	}

	@Override
	public LilyDungeonsWeight getDungeonsWeight() {
		return (LilyDungeonsWeight) dungeonsWeight;
	}

	@Override
	public String getStage() {
		double weight = getTotalWeight().getRaw();
		if (weight >= 43900) {
			return "Prestigious";
		} else if (weight >= 38325) {
			return "Far End Game";
		} else if (weight >= 32150) {
			return "End Game";
		} else if (weight >= 22560) {
			return "Early End Game";
		} else if (weight >= 16920) {
			return "Late Game";
		} else if (weight >= 10152) {
			return "Early-Late Game";
		} else if (weight >= 5922) {
			return "Mid Game";
		} else if (weight >= 2961) {
			return "Early-Mid Game";
		} else if (weight >= 1269) {
			return "Early Game";
		} else {
			return "Fresh";
		}
	}

	@Override
	public LilyWeight calculateWeight(String exclude) {
		exclude = exclude.toLowerCase();

		slayerWeight.getWeightStruct().reset();
		skillsWeight.getWeightStruct().reset();
		dungeonsWeight.getWeightStruct().reset();

		for (String slayerName : SLAYER_NAMES) {
			if (!exclude.equals(slayerName)) {
				slayerWeight.getSlayerWeight(slayerName);
			}
		}
		for (String skillName : SKILL_NAMES) {
			if (!exclude.equals(skillName)) {
				skillsWeight.getSkillsWeight(skillName);
			}
		}
		if (!exclude.equals("catacombs")) {
			dungeonsWeight.getDungeonWeight();
		}
		getDungeonsWeight().getDungeonCompletionWeight("normal");
		getDungeonsWeight().getDungeonCompletionWeight("master");

		return this;
	}
}
