/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.miscellaneous.weight.weight;

import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public abstract class Weight {

	protected final SlayerWeight slayerWeight;
	protected final SkillsWeight skillsWeight;
	protected final DungeonsWeight dungeonsWeight;

	public Weight(boolean calculateWeight, SlayerWeight slayerWeight, SkillsWeight skillsWeight, DungeonsWeight dungeonsWeight) {
		this.slayerWeight = slayerWeight;
		this.skillsWeight = skillsWeight;
		this.dungeonsWeight = dungeonsWeight;

		if (calculateWeight) {
			calculateWeight("");
		}
	}

	public static Weight of(Player.WeightType weightType, Player.Profile player) {
		return weightType == Player.WeightType.LILY ? new LilyWeight(player) : new SenitherWeight(player);
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

	public abstract String getStage();

	public abstract Weight calculateWeight(String exclude);
}
