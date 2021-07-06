package com.skyblockplus.weight;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;

public class SkillsWeight {

	private JsonElement profile;
	private Player player;
	private double totalSkillWeight;

	public SkillsWeight(JsonElement profile, Player player) {
		this.profile = profile;
		this.player = player;
	}

	public SkillsWeight() {}

	public double getSkillsWeight() {
		return totalSkillWeight;
	}

	public void addSkillWeight(String skillName, double exponent, double divider) {
		double currentSkillXp = player.getSkillXp(profile, skillName);

		if (currentSkillXp != -1) {
			int maxLevel = player.getSkillMaxLevel(skillName, true);
			double level = player.getSkill(profile, skillName).skillLevel + player.getSkill(profile, skillName).progressToNext;
			double maxLevelExp = maxLevel == 50 ? 55172425 : 111672425;
			double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;

			if (currentSkillXp <= maxLevelExp) {
				this.totalSkillWeight += base;
			} else {
				this.totalSkillWeight += (Math.round(base) + Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968));
			}
		}
	}

	public void addSkillWeight(double skillAverage, double exponent) {
		this.totalSkillWeight += (8 * (Math.pow(skillAverage * 10, 0.5 + exponent + (skillAverage / 100)) / 1250));
	}
}
