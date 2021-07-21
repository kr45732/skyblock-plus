package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;

public class SkillsWeight {

	private final JsonElement profile;
	private final Player player;
	private final WeightStruct weightStruct;

	public SkillsWeight(JsonElement profile, Player player) {
		this.profile = profile;
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getSkillsWeight(String skillName) {
		Double[] curWeights = skillWeights.get(skillName);
		double exponent = curWeights[0];
		double divider = curWeights[1];
		double currentSkillXp = player.getSkillXp(profile, skillName);

		if (currentSkillXp != -1) {
			int maxLevel = player.getSkillMaxLevel(skillName, true);
			SkillsStruct skillsStruct = player.getSkill(profile, skillName, true);
			double level = skillsStruct.skillLevel + skillsStruct.progressToNext;
			double maxLevelExp = maxLevel == 50 ? Constants.skillsLevel50Xp : Constants.skillsLevel60Xp;
			double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;
			if (currentSkillXp <= maxLevelExp) {
				return weightStruct.add(new WeightStruct(base));
			}

			return weightStruct.add(new WeightStruct(Math.round(base), Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968)));
		}

		return weightStruct.add(new WeightStruct());
	}

	public static double of(double skillAverage, double exponent) {
		return (8 * (Math.pow(skillAverage * 10, 0.5 + exponent + (skillAverage / 100)) / 1250));
	}
}
