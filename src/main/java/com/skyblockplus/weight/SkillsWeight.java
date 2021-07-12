package com.skyblockplus.weight;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;

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
			SkillsStruct skillsStruct = player.getSkill(profile, skillName, true);
			double level = skillsStruct.skillLevel + skillsStruct.progressToNext;
			double maxLevelExp = maxLevel == 50 ? Constants.skillsLevel50Xp : Constants.skillsLevel60Xp;
			double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;

			if (currentSkillXp <= maxLevelExp) {
				this.totalSkillWeight += base;
			} else {
				this.totalSkillWeight += Math.round(base) + Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968);
			}
		}
	}

	public void addSkillWeight(double skillAverage, double exponent) {
		this.totalSkillWeight += (8 * (Math.pow(skillAverage * 10, 0.5 + exponent + (skillAverage / 100)) / 1250));
	}
}
