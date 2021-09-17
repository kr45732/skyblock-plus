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

package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.SKILL_WEIGHTS;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;

public class SkillsWeight {

	private final JsonElement profile;
	private final Player player;
	private final WeightStruct weightStruct;

	public SkillsWeight(JsonElement profile, Player player) {
		this.profile = profile;
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public static double of(double skillAverage, double exponent) {
		return (8 * (Math.pow(skillAverage * 10, 0.5 + exponent + (skillAverage / 100)) / 1250));
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getSkillsWeight(String skillName) {
		Double[] curWeights = SKILL_WEIGHTS.get(skillName);
		double exponent = curWeights[0];
		double divider = curWeights[1];
		double currentSkillXp = player.getSkillXp(profile, skillName);

		if (currentSkillXp != -1) {
			int maxLevel = player.getSkillMaxLevel(skillName, true);
			SkillsStruct skillsStruct = player.getSkill(profile, skillName, true);
			double level = skillsStruct.skillLevel + skillsStruct.progressToNext;
			double maxLevelExp = maxLevel == 50 ? Constants.SKILLS_LEVEL_50_XP : Constants.SKILLS_LEVEL_60_XP;
			double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;
			if (currentSkillXp <= maxLevelExp) {
				return weightStruct.add(new WeightStruct(base));
			}

			return weightStruct.add(new WeightStruct(Math.round(base), Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968)));
		}

		return weightStruct.add(new WeightStruct());
	}
}
