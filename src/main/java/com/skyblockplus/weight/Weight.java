package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.WeightStruct;

public class Weight {

	private final SlayerWeight slayerWeight;
	private final SkillsWeight skillsWeight;
	private final DungeonsWeight dungeonsWeight;

	public Weight(Player player) {
		this(player.profileJson(), player);
	}

	public Weight(JsonElement profile, Player player) {
		this.slayerWeight = new SlayerWeight(profile, player);
		this.skillsWeight = new SkillsWeight(profile, player);
		this.dungeonsWeight = new DungeonsWeight(profile, player);
	}

	public static double of(double skillAverage, double slayer, double catacombs, double averageDungeonClass) {
		double totalWeight = 0;
		totalWeight += calculateSlayerWeight(slayer);
		totalWeight += calculateSkillsWeight(skillAverage);
		totalWeight += calculateDungeonsWeight(averageDungeonClass, catacombs);
		return totalWeight;
	}

	public static double calculateSlayerWeight(double slayer) {
		return SlayerWeight.of(
			slayer,
			(
				SLAYER_WEIGHTS.get("rev")[0] +
				SLAYER_WEIGHTS.get("sven")[0] +
				SLAYER_WEIGHTS.get("tara")[0] +
				SLAYER_WEIGHTS.get("enderman")[0]
			) /
			4,
			(
				SLAYER_WEIGHTS.get("rev")[1] +
				SLAYER_WEIGHTS.get("sven")[1] +
				SLAYER_WEIGHTS.get("tara")[1] +
				SLAYER_WEIGHTS.get("enderman")[1]
			) /
			4
		);
	}

	public static double calculateSkillsWeight(double skillAverage) {
		return SkillsWeight.of(
			skillAverage,
			(
				SKILL_WEIGHTS.get("mining")[0] +
				SKILL_WEIGHTS.get("foraging")[0] +
				SKILL_WEIGHTS.get("enchanting")[0] +
				SKILL_WEIGHTS.get("farming")[0] +
				SKILL_WEIGHTS.get("combat")[0] +
				SKILL_WEIGHTS.get("fishing")[0] +
				SKILL_WEIGHTS.get("alchemy")[0] +
				SKILL_WEIGHTS.get("taming")[0]
			) /
			8
		);
	}

	public static double calculateDungeonsWeight(double averageDungeonClass, double catacombs) {
		return DungeonsWeight.of(
			averageDungeonClass,
			(
				DUNGEON_CLASS_WEIGHTS.get("healer") +
				DUNGEON_CLASS_WEIGHTS.get("mage") +
				DUNGEON_CLASS_WEIGHTS.get("berserk") +
				DUNGEON_CLASS_WEIGHTS.get("archer") +
				DUNGEON_CLASS_WEIGHTS.get("tank")
			) /
			5,
			catacombs,
			DUNGEON_WEIGHTS.get("catacombs")
		);
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

	public WeightStruct getTotalWeight(boolean needToCalc) {
		if (needToCalc) {
			for (String slayerName : SLAYER_NAMES) {
				slayerWeight.getSlayerWeight(slayerName);
			}
			for (String skillName : SKILL_NAMES) {
				skillsWeight.getSkillsWeight(skillName);
			}
			dungeonsWeight.getDungeonWeight("catacombs");
			for (String dungeonClassName : DUNGEON_CLASS_NAMES) {
				dungeonsWeight.getClassWeight(dungeonClassName);
			}
		}

		WeightStruct w = new WeightStruct();
		w.add(slayerWeight.getWeightStruct());
		w.add(skillsWeight.getWeightStruct());
		w.add(dungeonsWeight.getWeightStruct());

		return w;
	}
}
