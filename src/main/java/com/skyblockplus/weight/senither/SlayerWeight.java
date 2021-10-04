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

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

import static com.skyblockplus.utils.Constants.SLAYER_WEIGHTS;

public class SlayerWeight {

	private final Player player;
	private final WeightStruct weightStruct;

	public SlayerWeight(Player player) {
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public static double of(double slayerXp, double divider, double modifier) {
		slayerXp = slayerXp / 3;

		if (slayerXp <= 1000000) {
			return 3 * (slayerXp == 0 ? 0 : slayerXp / divider);
		} else {
			double base = 1000000 / divider;
			double remaining = slayerXp - 1000000;
			double overflow = 0;

			while (remaining > 0) {
				double left = Math.min(remaining, 1000000);

				overflow += Math.pow(left / (divider * (1.5 + modifier)), 0.942);
				modifier += modifier;
				remaining -= left;
			}

			return 3 * (base + overflow);
		}
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getSlayerWeight(String slayerName) {
		Double[] curWeights = SLAYER_WEIGHTS.get(slayerName);
		double divider = curWeights[0];
		double modifier = curWeights[1];

		int currentSlayerXp = player.getSlayer(slayerName);

		if (currentSlayerXp <= 1000000) {
			return weightStruct.add(new WeightStruct(currentSlayerXp == 0 ? 0 : currentSlayerXp / divider));
		}

		double base = 1000000 / divider;
		double remaining = currentSlayerXp - 1000000;
		double overflow = 0;
		double initialModifier = modifier;

		while (remaining > 0) {
			double left = Math.min(remaining, 1000000);

			overflow += Math.pow(left / (divider * (1.5 + modifier)), 0.942);
			modifier += initialModifier;
			remaining -= left;
		}

		return weightStruct.add(new WeightStruct(base, overflow));
	}
}
