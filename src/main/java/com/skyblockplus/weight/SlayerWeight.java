package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.SLAYER_WEIGHTS;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class SlayerWeight {

	private final JsonElement profile;
	private final Player player;
	private final WeightStruct weightStruct;

	public SlayerWeight(JsonElement profile, Player player) {
		this.profile = profile;
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

		int currentSlayerXp = player.getSlayer(profile, slayerName);

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
