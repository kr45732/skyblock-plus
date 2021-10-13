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

		if(calculateWeight){
			calculateWeight();
		}
	}

	public static double of(double skillAverage, double slayer, double catacombs, double averageDungeonClass) {
		double totalWeight = 0;
		totalWeight += calculateSlayerWeight(slayer);
		totalWeight += calculateSkillsWeight(skillAverage);
		totalWeight += calculateDungeonsWeight(averageDungeonClass, catacombs);
		return totalWeight;
	}

	public static double calculateSlayerWeight(double slayer) {
		return SlayerWeight.of(
			slayer,
			(
				SLAYER_WEIGHTS.get("rev")[0] +
				SLAYER_WEIGHTS.get("sven")[0] +
				SLAYER_WEIGHTS.get("tara")[0] +
				SLAYER_WEIGHTS.get("enderman")[0]
			) /
			4,
			(
				SLAYER_WEIGHTS.get("rev")[1] +
				SLAYER_WEIGHTS.get("sven")[1] +
				SLAYER_WEIGHTS.get("tara")[1] +
				SLAYER_WEIGHTS.get("enderman")[1]
			) /
			4
		);
	}

	public static double calculateSkillsWeight(double skillAverage) {
		return SkillsWeight.of(
			skillAverage,
			(
				SKILL_WEIGHTS.get("mining")[0] +
				SKILL_WEIGHTS.get("foraging")[0] +
				SKILL_WEIGHTS.get("enchanting")[0] +
				SKILL_WEIGHTS.get("farming")[0] +
				SKILL_WEIGHTS.get("combat")[0] +
				SKILL_WEIGHTS.get("fishing")[0] +
				SKILL_WEIGHTS.get("alchemy")[0] +
				SKILL_WEIGHTS.get("taming")[0]
			) /
			8
		);
	}

	public static double calculateDungeonsWeight(double averageDungeonClass, double catacombs) {
		return DungeonsWeight.of(
			averageDungeonClass,
			(
				DUNGEON_CLASS_WEIGHTS.get("healer") +
				DUNGEON_CLASS_WEIGHTS.get("mage") +
				DUNGEON_CLASS_WEIGHTS.get("berserk") +
				DUNGEON_CLASS_WEIGHTS.get("archer") +
				DUNGEON_CLASS_WEIGHTS.get("tank")
			) /
			5,
			catacombs,
			DUNGEON_WEIGHTS.get("catacombs")
		);
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

	private void calculateWeight(){
		for (String slayerName : SLAYER_NAMES) {
			slayerWeight.getSlayerWeight(slayerName);
		}
		for (String skillName : SKILL_NAMES) {
			skillsWeight.getSkillsWeight(skillName);
		}
		dungeonsWeight.getDungeonWeight("catacombs");
		for (String dungeonClassName : DUNGEON_CLASS_NAMES) {
			dungeonsWeight.getClassWeight(dungeonClassName);
		}
	}
}
