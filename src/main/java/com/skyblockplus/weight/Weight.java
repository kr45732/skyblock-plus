package com.skyblockplus.weight;

import com.skyblockplus.utils.Player;

import java.util.HashMap;
import java.util.Map;

public class Weight {
    private final Player player;
    private final Map<String, Double> slayerWeights;
    private final Map<String, Double[]> skillWeights;
    private final Map<String, Double> dungeonClassWeights;
    private final Map<String, Double> catacombsWeights;

    public Weight(Player player) {
        this(player, defaultSlayerWeights(), defaultSkillWeights(), defaultDungeonClassWeights(),
                defaultCatacombsWeights());
    }

    public Weight(Player player, Map<String, Double> slayerWeights, Map<String, Double[]> skillWeights,
            Map<String, Double> dungeonClassWeights, Map<String, Double> catacombsWeights) {
        this.player = player;
        this.slayerWeights = slayerWeights;
        this.skillWeights = skillWeights;
        this.dungeonClassWeights = dungeonClassWeights;
        this.catacombsWeights = catacombsWeights;
    }

    private static Map<String, Double> defaultSlayerWeights() {
        Map<String, Double> tempSlayerWeights = new HashMap<>();
        tempSlayerWeights.put("rev", 2208D);
        tempSlayerWeights.put("sven", 1962D);
        tempSlayerWeights.put("tara", 2118D);
        return tempSlayerWeights;
    }

    private static Map<String, Double[]> defaultSkillWeights() {
        Map<String, Double[]> tempSkillWeights = new HashMap<>();
        tempSkillWeights.put("mining", new Double[] { 1.18207448, 259634D });
        tempSkillWeights.put("foraging", new Double[] { 1.232826, 259634D });
        tempSkillWeights.put("enchanting", new Double[] { 0.96976583, 882758D });
        tempSkillWeights.put("farming", new Double[] { 1.217848139, 220689D });
        tempSkillWeights.put("combat", new Double[] { 1.22307, 275862D });
        tempSkillWeights.put("fishing", new Double[] { 1.406418, 88274D });
        tempSkillWeights.put("alchemy", new Double[] { 1.0, 1103448D });
        tempSkillWeights.put("taming", new Double[] { 1.14744, 441379D });
        return tempSkillWeights;
    }

    private static Map<String, Double> defaultDungeonClassWeights() {
        Map<String, Double> tempDungeonClassWeights = new HashMap<>();
        tempDungeonClassWeights.put("healer", 0.0000045254834D);
        tempDungeonClassWeights.put("mage", 0.0000045254834D);
        tempDungeonClassWeights.put("berserk", 0.0000045254834D);
        tempDungeonClassWeights.put("archer", 0.0000045254834D);
        tempDungeonClassWeights.put("tank", 0.0000045254834D);
        return tempDungeonClassWeights;
    }

    private static Map<String, Double> defaultCatacombsWeights() {
        Map<String, Double> tempCatacombsWeights = new HashMap<>();
        tempCatacombsWeights.put("catacombs", 0.0002149604615D);
        return tempCatacombsWeights;
    }

    public double getPlayerWeight() {
        double totalWeight = 0;
        totalWeight += getSlayerWeight();
        totalWeight += getSkillsWeight();
        totalWeight += getDungeonsWeight();
        return totalWeight;
    }

    public double getSlayerWeight() {
        SlayerWeight slayerWeight = new SlayerWeight(player);
        slayerWeight.addSlayerWeight("rev", slayerWeights.get("rev"));
        slayerWeight.addSlayerWeight("sven", slayerWeights.get("sven"));
        slayerWeight.addSlayerWeight("tara", slayerWeights.get("tara"));
        return slayerWeight.getSlayerWeight();
    }

    public double getSkillsWeight() {
        SkillsWeight skillsWeight = new SkillsWeight(player);
        skillsWeight.addSkillWeight("mining", skillWeights.get("mining")[0], skillWeights.get("mining")[1]);
        skillsWeight.addSkillWeight("foraging", skillWeights.get("foraging")[0], skillWeights.get("foraging")[1]);
        skillsWeight.addSkillWeight("enchanting", skillWeights.get("enchanting")[0], skillWeights.get("enchanting")[1]);
        skillsWeight.addSkillWeight("farming", skillWeights.get("farming")[0], skillWeights.get("farming")[1]);
        skillsWeight.addSkillWeight("combat", skillWeights.get("combat")[0], skillWeights.get("combat")[1]);
        skillsWeight.addSkillWeight("fishing", skillWeights.get("fishing")[0], skillWeights.get("fishing")[1]);
        skillsWeight.addSkillWeight("alchemy", skillWeights.get("alchemy")[0], skillWeights.get("alchemy")[1]);
        skillsWeight.addSkillWeight("taming", skillWeights.get("taming")[0], skillWeights.get("taming")[1]);
        return skillsWeight.getSkillsWeight();
    }

    public double getDungeonsWeight() {
        DungeonsWeight dungeonsWeight = new DungeonsWeight(player);
        dungeonsWeight.addDungeonClassWeight("healer", dungeonClassWeights.get("healer"));
        dungeonsWeight.addDungeonClassWeight("mage", dungeonClassWeights.get("mage"));
        dungeonsWeight.addDungeonClassWeight("berserk", dungeonClassWeights.get("berserk"));
        dungeonsWeight.addDungeonClassWeight("archer", dungeonClassWeights.get("archer"));
        dungeonsWeight.addDungeonClassWeight("tank", dungeonClassWeights.get("tank"));
        dungeonsWeight.addCatacombsWeight(catacombsWeights.get("catacombs"));
        return dungeonsWeight.getDungeonsWeight();
    }
}
