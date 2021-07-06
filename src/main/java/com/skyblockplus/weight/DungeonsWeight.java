package com.skyblockplus.weight;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;

public class DungeonsWeight {

	private final double level50Xp = 569809640;
	private JsonElement profile;
	private Player player;
	private double totalDungeonsWeight;

	public DungeonsWeight(JsonElement profile, Player player) {
		this.profile = profile;
		this.player = player;
	}

	public DungeonsWeight() {}

	public double getDungeonsWeight() {
		return totalDungeonsWeight;
	}

	public void addDungeonClassWeight(String className, double maxPoints) {
		double currentClassLevel = player.getDungeonClassLevel(profile, className);
		double currentClassXp = player.getDungeonClassXp(profile, className);
		double base = Math.pow(currentClassLevel, 4.5) * maxPoints;

		if (currentClassXp <= level50Xp) {
			totalDungeonsWeight += base;
		} else {
			double remaining = currentClassXp - level50Xp;
			double splitter = (4 * level50Xp) / base;
			totalDungeonsWeight += (Math.floor(base) + Math.pow(remaining / splitter, 0.968));
		}
	}

	public void addCatacombsWeight(double maxPoints) {
		double catacombsSkillXp = player.getSkillXp(profile, "catacombs");
		double level = player.getCatacombsLevel(profile);
		double base = Math.pow(level, 4.5) * maxPoints;

		if (catacombsSkillXp <= level50Xp) {
			totalDungeonsWeight += base;
		} else {
			double remaining = catacombsSkillXp - level50Xp;
			double splitter = (4 * level50Xp) / base;
			totalDungeonsWeight += (Math.floor(base) + Math.pow(remaining / splitter, 0.968));
		}
	}

	public void addDungeonClassWeight(double averageDungeonClass, double maxPoints) {
		totalDungeonsWeight += 5 * Math.pow(averageDungeonClass, 4.5) * maxPoints;
	}

	public void addCatacombsWeight(double catacombs, double maxPoints) {
		totalDungeonsWeight += Math.pow(catacombs, 4.5) * maxPoints;
	}
}
