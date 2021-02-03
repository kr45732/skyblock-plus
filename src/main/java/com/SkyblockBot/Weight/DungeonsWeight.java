package com.SkyblockBot.Weight;

import com.SkyblockBot.Utils.Player;

public class DungeonsWeight {

    private final double level50Xp = 569809640;
    private final Player player;
    private double totalDungeonsWeight;

    public DungeonsWeight(Player player) {
        this.player = player;
    }

    public double getDungeonsWeight() {
        return totalDungeonsWeight;
    }

    public void addDungeonClassWeight(String className, double maxPoints) {
        double currentClassLevel = player.getDungeonClassLevel(className);
        double currentClassXp = player.getDungeonClassXp(className);
        double base = Math.pow(currentClassLevel * 10, 3) * (maxPoints / 100000) / 1250;

        if (currentClassXp <= level50Xp) {
            totalDungeonsWeight += base;
        } else {
            totalDungeonsWeight += (base + Math.pow((currentClassXp - level50Xp) / (4 * level50Xp / maxPoints), 0.968));
        }
    }

    public void addCatacombsWeight(double maxPoints) {
        double catacombsSkillXp = player.getSkillXp("catacombs");
        double level = player.getCatacombsLevel();
        double base = Math.pow(level * 10, 3) * (maxPoints / 100000) / 1250;

        if (catacombsSkillXp <= level50Xp) {
            totalDungeonsWeight += base;
        } else {
            totalDungeonsWeight += (base + Math.pow((catacombsSkillXp - level50Xp) / (4 * level50Xp / maxPoints), 0.968));
        }
    }
}
