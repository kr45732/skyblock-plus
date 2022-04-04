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

import static com.skyblockplus.utils.Constants.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class Weight {

	private final SlayerWeight slayerWeight;
	private final SkillsWeight skillsWeight;
	private final DungeonsWeight dungeonsWeight;

	public Weight(Player player) {
		this(player, false);
	}

	public Weight(Player player, boolean calculateWeight) {
		this.slayerWeight = new SlayerWeight(player);
		this.skillsWeight = new SkillsWeight(player);
		this.dungeonsWeight = new DungeonsWeight(player);

		if (calculateWeight) {
			calculateWeight("");
		}
	}

	public SkillsWeight getSkillsWeight() {
		return skillsWeight;
	}

	public SlayerWeight getSlayerWeight() {
		return slayerWeight;
	}

	public DungeonsWeight getDungeonsWeight() {
		return dungeonsWeight;
	}

	public WeightStruct getTotalWeight() {
		WeightStruct w = new WeightStruct();
		w.add(slayerWeight.getWeightStruct());
		w.add(skillsWeight.getWeightStruct());
		w.add(dungeonsWeight.getWeightStruct());

		return w;
	}

	public String getStage() {
		double weight = getTotalWeight().getRaw();
		if (weight >= 30000) {
			return "No Life";
		} else if (weight >= 15000) {
			return "End Game";
		} else if (weight >= 10000) {
			return "Early End Game";
		} else if (weight >= 7000) {
			return "Late Game";
		} else if (weight >= 2000) {
			return "Mid Game";
		} else {
			return "Early Game";
		}
	}

	public Weight calculateWeight(String exclude) {
		slayerWeight.getWeightStruct().reset();
		skillsWeight.getWeightStruct().reset();
		dungeonsWeight.getWeightStruct().reset();

		exclude = exclude.toLowerCase();
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
			dungeonsWeight.getDungeonWeight("catacombs");
		}
		for (String dungeonClassName : DUNGEON_CLASS_NAMES) {
			if (!exclude.equals(dungeonClassName)) {
				dungeonsWeight.getClassWeight(dungeonClassName);
			}
		}
		return this;
	}
}
