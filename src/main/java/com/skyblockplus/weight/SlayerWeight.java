package com.skyblockplus.weight;

import com.skyblockplus.utils.Player;

public class SlayerWeight {
    private Player player;
    private double totalSlayerWeight;

    public SlayerWeight(Player player) {
        this.player = player;
    }

    public SlayerWeight() {
    }

    public double getSlayerWeight() {
        return totalSlayerWeight;
    }

    public void addSlayerWeight(String slayerName, double divider) {
        int currentSlayerXp = player.getSlayer(slayerName);

        if (currentSlayerXp == 0) {
            totalSlayerWeight += 0;
        } else if (currentSlayerXp <= 1000000) {
            totalSlayerWeight += (currentSlayerXp / divider);
        } else {
            double base = 1000000 / divider;
            double remaining = currentSlayerXp - 1000000;
            double overflow = Math.pow(remaining / (divider * 1.5), 0.942);
            totalSlayerWeight += (base + overflow);
        }
    }

    public void addSlayerWeight(double slayer, double divider) {
        totalSlayerWeight += 3 * ((slayer / 3) / divider);
    }
}
