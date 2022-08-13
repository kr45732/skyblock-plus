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

import static com.skyblockplus.utils.Constants.SLAYER_NAMES_MAP;
import static com.skyblockplus.utils.Utils.getWeightJson;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonArray;
import com.skyblockplus.miscellaneous.weight.weight.SlayerWeight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class SenitherSlayerWeight extends SlayerWeight {

	public SenitherSlayerWeight(Player player) {
		super(player);
	}

	public WeightStruct getSlayerWeight(String slayerName) {
		return getSlayerWeight(slayerName, player.getSlayer(slayerName));
	}

	public WeightStruct getSlayerWeight(String slayerName, int currentSlayerXp) {
		if (slayerName.equals("blaze")) {
			return new WeightStruct();
		}

		JsonArray curWeights = higherDepth(getWeightJson(), "senither.slayer." + SLAYER_NAMES_MAP.get(slayerName)).getAsJsonArray();
		double divider = curWeights.get(0).getAsDouble();
		double modifier = curWeights.get(1).getAsDouble();

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
