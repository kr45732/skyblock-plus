package com.skyblockplus.weight;

import com.skyblockplus.utils.Player;

public class SkillsWeight {
    private Player player;
    private double totalSkillWeight;

    public SkillsWeight(Player player) {
        this.player = player;
    }

    public double getSkillsWeight() {
        return totalSkillWeight;
    }

    public void addSkillWeight(String skillName, double exponent, double divider) {
        double currentSkillXp = player.getSkillXp(skillName);

        if (currentSkillXp == -1) {
        } else {
            int maxLevel = player.getSkillMaxLevel(skillName);
            double level = player.getSkill(skillName).skillLevel + player.getSkill(skillName).progressToNext;
            double maxLevelExp = maxLevel == 50 ? 55172425 : 111672425;
            double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;

            if (currentSkillXp <= maxLevelExp) {
                this.totalSkillWeight += base;
            } else {
                this.totalSkillWeight += (Math.round(base) + Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968));
            }
        }
    }
}
