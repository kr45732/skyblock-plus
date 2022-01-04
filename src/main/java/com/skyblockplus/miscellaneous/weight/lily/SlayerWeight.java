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

import static com.skyblockplus.utils.Constants.SLAYER_DEPRECATION_SCALING;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class SlayerWeight {

	private final Player player;
	private final WeightStruct weightStruct;

	public SlayerWeight(Player player) {
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getSlayerWeight(String slayerName) {
		double slayerXp = player.getSlayer(slayerName);

		double score;
		double d = slayerXp / 100000;
		if (slayerXp >= 6416) {
			double D = (d - Math.pow(3, (-5.0 / 2))) * (d + Math.pow(3, -5.0 / 2));
			double u = Math.cbrt(3 * (d + Math.sqrt(D)));
			double v = Math.cbrt(3 * (d - Math.sqrt(D)));
			score = u + v - 1;
		} else {
			score = Math.sqrt(4.0 / 3) * Math.cos(Math.acos(d * Math.pow(3, 5.0 / 2)) / 3) - 1;
		}

		double scaleFactor = higherDepth(SLAYER_DEPRECATION_SCALING, slayerName).getAsDouble();
		int intScore = (int) score;
		double distance = slayerXp - actualInt(intScore);
		double effectiveDistance = distance * Math.pow(scaleFactor, intScore);
		double effectiveScore = effectiveInt(intScore, scaleFactor) + effectiveDistance;
		double weight;
		switch (slayerName) {
			case "rev":
				weight = (effectiveScore / 7000) + (slayerXp / 900000);
				break;
			case "tara":
				weight = (effectiveScore / 4800) + ((slayerXp * 1.6) / 900000);
				break;
			case "sven":
				weight = (effectiveScore / 2200) + ((slayerXp * 3.6) / 900000);
				break;
			case "enderman":
				weight = (effectiveScore / 1000) + ((slayerXp * 10) / 900000);
				break;
			default:
				return null;
		}

		return weightStruct.add(new WeightStruct(weight));
	}

	private double actualInt(int intScore) {
		return ((Math.pow(intScore, 3) / 6) + (Math.pow(intScore, 2) / 2) + (intScore / 3.0)) * 100000;
	}

	private double effectiveInt(int intScore, double scaleFactor) {
		double total = 0;
		for (int k = 0; k < intScore; k++) {
			total += (Math.pow((k + 1), 2) + (k + 1)) * Math.pow(scaleFactor, (k + 1));
		}
		return 1000000 * total * (0.05 / scaleFactor);
	}
}
