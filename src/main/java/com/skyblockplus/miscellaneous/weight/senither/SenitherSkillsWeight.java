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

package com.skyblockplus.miscellaneous.weight.senither;

import static com.skyblockplus.utils.Utils.getWeightJson;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonArray;
import com.skyblockplus.miscellaneous.weight.weight.SkillsWeight;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;

public class SenitherSkillsWeight extends SkillsWeight {

	public SenitherSkillsWeight(Player player) {
		super(player);
	}

	@Override
	public WeightStruct getSkillsWeight(String skillName) {
		return getSkillsWeight(skillName, player.getSkill(skillName, Player.WeightType.SENITHER));
	}

	@Override
	public WeightStruct getSkillsWeight(String skillName, SkillsStruct skillsStruct) {
		JsonArray curWeights = higherDepth(getWeightJson(), "senither.skills." + skillName).getAsJsonArray();
		double exponent = curWeights.get(0).getAsDouble();
		double divider = curWeights.get(1).getAsDouble();

		if (skillsStruct != null) {
			double currentSkillXp = skillsStruct.totalExp();
			int maxLevel = skillsStruct.maxLevel();
			double level = skillsStruct.getProgressLevel();
			double maxLevelExp = maxLevel == 50 ? Constants.SKILLS_LEVEL_50_XP : Constants.SKILLS_LEVEL_60_XP;
			double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;
			if (currentSkillXp <= maxLevelExp) {
				return weightStruct.add(new WeightStruct(base));
			}

			return weightStruct.add(new WeightStruct(Math.round(base), Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968)));
		}

		return new WeightStruct();
	}
}
