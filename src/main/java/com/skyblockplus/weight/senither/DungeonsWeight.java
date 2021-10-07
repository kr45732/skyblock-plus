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

package com.skyblockplus.weight.senither;

import static com.skyblockplus.utils.Constants.DUNGEON_CLASS_WEIGHTS;
import static com.skyblockplus.utils.Constants.DUNGEON_WEIGHTS;

import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;

public class DungeonsWeight {

	private final Player player;
	private final WeightStruct weightStruct;

	public DungeonsWeight(Player player) {
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public static double of(double averageDungeonClass, double maxClassPoints, double catacombs, double maxDungeonPoints) {
		return (5 * Math.pow(averageDungeonClass, 4.5) * maxClassPoints) + (Math.pow(catacombs, 4.5) * maxDungeonPoints);
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getClassWeight(String className) {
		SkillsStruct dungeonSkill = player.getDungeonClass(className);
		double currentClassLevel = dungeonSkill.getProgressLevel();
		double currentClassXp = dungeonSkill.getTotalExp();
		double base = Math.pow(currentClassLevel, 4.5) * DUNGEON_CLASS_WEIGHTS.get(className);

		if (currentClassXp <= Constants.CATACOMBS_LEVEL_50_XP) {
			return weightStruct.add(new WeightStruct(base));
		}

		double remaining = currentClassXp - Constants.CATACOMBS_LEVEL_50_XP;
		double splitter = (4 * Constants.CATACOMBS_LEVEL_50_XP) / base;
		return weightStruct.add(new WeightStruct(Math.floor(base), Math.pow(remaining / splitter, 0.968)));
	}

	public WeightStruct getDungeonWeight(String dungeonName) {
		double catacombsSkillXp = player.getSkillXp(dungeonName);
		double level = player.getCatacombs().getProgressLevel();
		double base = Math.pow(level, 4.5) * DUNGEON_WEIGHTS.get(dungeonName);

		if (catacombsSkillXp <= Constants.CATACOMBS_LEVEL_50_XP) {
			return weightStruct.add(new WeightStruct(base));
		}

		double remaining = catacombsSkillXp - Constants.CATACOMBS_LEVEL_50_XP;
		double splitter = (4 * Constants.CATACOMBS_LEVEL_50_XP) / base;
		return weightStruct.add(new WeightStruct(Math.floor(base), Math.pow(remaining / splitter, 0.968)));
	}
}
