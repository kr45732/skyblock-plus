package com.skyblockplus.weight;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;

public class SlayerWeight {

	private JsonElement profile;
	private Player player;
	private double totalSlayerWeight;

	public SlayerWeight(JsonElement profile, Player player) {
		this.profile = profile;
		this.player = player;
	}

	public SlayerWeight() {}

	public double getSlayerWeight() {
		return totalSlayerWeight;
	}

	public void addSlayerWeight(String slayerName, double divider, double modifier) {
		int currentSlayerXp = player.getSlayer(profile, slayerName);

		if (currentSlayerXp <= 1000000) {
			totalSlayerWeight += currentSlayerXp == 0 ? 0 : currentSlayerXp / divider;
		} else {
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

			totalSlayerWeight += (base + overflow);
		}
	}

	public void addSlayerWeight(double slayer, double divider, double modifier) {
		slayer = slayer / 3;

		if (slayer <= 1000000) {
			totalSlayerWeight += 3 * (slayer == 0 ? 0 : slayer / divider);
		} else {
			double base = 1000000 / divider;
			double remaining = slayer - 1000000;
			double overflow = 0;

			while (remaining > 0) {
				double left = Math.min(remaining, 1000000);

				overflow += Math.pow(left / (divider * (1.5 + modifier)), 0.942);
				modifier += modifier;
				remaining -= left;
			}

			totalSlayerWeight += 3 * (base + overflow);
		}
	}
}
