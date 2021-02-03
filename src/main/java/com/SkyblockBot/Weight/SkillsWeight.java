package com.SkyblockBot.Weight;

import com.SkyblockBot.Utils.Player;

public class SkillsWeight {
    private final Player player;
    private double totalSkillWeight;
    private boolean apiDisabled = false;

    public SkillsWeight(Player player) {
        this.player = player;
    }

    public double getSkillsWeight() {
        return totalSkillWeight;
    }

    public boolean isApiDisabled() {
        return apiDisabled;
    }

    public void addSkillWeight(String skillName, double exponent, double divider) {
        double currentSkillXp = player.getSkillXp(skillName);
        if (currentSkillXp == -1) {
            apiDisabled = true;
        } else {
            int maxLevel = player.getSkillMaxLevel(skillName);
            double level = player.getSkill(skillName).skillLevel + player.getSkill(skillName).progressToNext;
            double maxLevelExp = maxLevel == 50 ? 55172425 : 111672425;
            double base = Math.pow(level * 10, 0.5 + exponent + (level / 100)) / 1250;

            if (currentSkillXp <= maxLevelExp) {
                this.totalSkillWeight += base;
            } else {
                this.totalSkillWeight += (base + Math.pow((currentSkillXp - maxLevelExp) / divider, 0.968));
            }
        }
    }
}
