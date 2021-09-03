package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.DUNGEON_CLASS_WEIGHTS;
import static com.skyblockplus.utils.Constants.DUNGEON_WEIGHTS;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class DungeonsWeight {

	private final JsonElement profile;
	private final Player player;
	private final WeightStruct weightStruct;

	public DungeonsWeight(JsonElement profile, Player player) {
		this.profile = profile;
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public static double of(double averageDungeonClass, double maxClassPoints, double catacombs, double maxDungeonPoints) {
		return (5 * Math.pow(averageDungeonClass, 4.5) * maxClassPoints) + (Math.pow(catacombs, 4.5) * maxDungeonPoints);
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getClassWeight(String className) {
		double currentClassLevel = player.getDungeonClassLevel(profile, className);
		double currentClassXp = player.getDungeonClassXp(profile, className);
		double base = Math.pow(currentClassLevel, 4.5) * DUNGEON_CLASS_WEIGHTS.get(className);

		if (currentClassXp <= Constants.CATACOMBS_LEVEL_50_XP) {
			return weightStruct.add(new WeightStruct(base));
		}

		double remaining = currentClassXp - Constants.CATACOMBS_LEVEL_50_XP;
		double splitter = (4 * Constants.CATACOMBS_LEVEL_50_XP) / base;
		return weightStruct.add(new WeightStruct(Math.floor(base), Math.pow(remaining / splitter, 0.968)));
	}

	public WeightStruct getDungeonWeight(String dungeonName) {
		double catacombsSkillXp = player.getSkillXp(profile, dungeonName);
		double level = player.getCatacombsLevel(profile);
		double base = Math.pow(level, 4.5) * DUNGEON_WEIGHTS.get(dungeonName);

		if (catacombsSkillXp <= Constants.CATACOMBS_LEVEL_50_XP) {
			return weightStruct.add(new WeightStruct(base));
		}

		double remaining = catacombsSkillXp - Constants.CATACOMBS_LEVEL_50_XP;
		double splitter = (4 * Constants.CATACOMBS_LEVEL_50_XP) / base;
		return weightStruct.add(new WeightStruct(Math.floor(base), Math.pow(remaining / splitter, 0.968)));
	}
}
