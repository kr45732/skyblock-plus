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
			(slayerWeights.get("rev")[0] + slayerWeights.get("sven")[0] + slayerWeights.get("tara")[0] + slayerWeights.get("enderman")[0]) /
			4,
			(slayerWeights.get("rev")[1] + slayerWeights.get("sven")[1] + slayerWeights.get("tara")[1] + slayerWeights.get("enderman")[1]) /
			4
		);
	}

	public static double calculateSkillsWeight(double skillAverage) {
		return SkillsWeight.of(
			skillAverage,
			(
				skillWeights.get("mining")[0] +
				skillWeights.get("foraging")[0] +
				skillWeights.get("enchanting")[0] +
				skillWeights.get("farming")[0] +
				skillWeights.get("combat")[0] +
				skillWeights.get("fishing")[0] +
				skillWeights.get("alchemy")[0] +
				skillWeights.get("taming")[0]
			) /
			8
		);
	}

	public static double calculateDungeonsWeight(double averageDungeonClass, double catacombs) {
		return DungeonsWeight.of(
			averageDungeonClass,
			(
				dungeonClassWeights.get("healer") +
				dungeonClassWeights.get("mage") +
				dungeonClassWeights.get("berserk") +
				dungeonClassWeights.get("archer") +
				dungeonClassWeights.get("tank")
			) /
			5,
			catacombs,
			dungeonWeights.get("catacombs")
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
			for (String slayerName : slayerNames) {
				slayerWeight.getSlayerWeight(slayerName);
			}
			for (String skillName : skillNames) {
				skillsWeight.getSkillsWeight(skillName);
			}
			dungeonsWeight.getDungeonWeight("catacombs");
			for (String dungeonClassName : dungeonClassNames) {
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
