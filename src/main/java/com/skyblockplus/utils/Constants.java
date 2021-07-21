package com.skyblockplus.utils;

import static com.skyblockplus.utils.Utils.*;

import java.util.*;

public class Constants {

	public static final double catacombsLevel50Xp = 569809640;
	public static final double skillsLevel50Xp = 55172425;
	public static final double skillsLevel60Xp = 111672425;
	public static final Map<String, String> rarityToNumberMap = new HashMap<>();
	public static final List<String> enchantNames = new ArrayList<>();
	public static final List<Integer> craftedMinionsToSlots = Arrays.asList(
		0,
		5,
		15,
		30,
		50,
		75,
		100,
		125,
		150,
		175,
		200,
		225,
		250,
		275,
		300,
		350,
		400,
		450,
		500,
		550,
		600
	);
	public static final List<String> skillNames = new ArrayList<>();
	public static final List<String> allSkillNames = new ArrayList<>();
	public static final List<String> cosmeticSkillNames = Arrays.asList("runecrafting", "carpentry");
	public static final Map<String, String> skillsEmojiMap = new HashMap<>();
	public static final List<String> petNames = new ArrayList<>();
	public static final List<String> dungeonClassNames = Arrays.asList("healer", "mage", "berserk", "archer", "tank");
	public static final List<String> slayerNames = Arrays.asList("sven", "tara", "rev", "enderman");
	public static final List<String> reforgeStoneNames = new ArrayList<>();
	public static final List<String> essenceItemNames = new ArrayList<>();
	public static final List<String> bitsItemNames = new ArrayList<>();
	public static final List<String> petItemNames = new ArrayList<>();
	public static final List<Integer> guildExpToLevel = Arrays.asList(
		100000,
		150000,
		250000,
		500000,
		750000,
		1000000,
		1250000,
		1500000,
		2000000,
		2500000,
		2500000,
		2500000,
		2500000,
		2500000,
		3000000
	);
	public static final Map<String, Double[]> slayerWeights = new HashMap<>();
	public static final Map<String, Double[]> skillWeights = new HashMap<>();
	public static final Map<String, Double> dungeonClassWeights = new HashMap<>();
	public static final Map<String, Double> dungeonWeights = new HashMap<>();
	public static final List<String> vanillaItems = new ArrayList<>();

	public static void initialize() {
		/* rarityToNumberMap */
		rarityToNumberMap.put("MYTHIC", ";5");
		rarityToNumberMap.put("LEGENDARY", ";4");
		rarityToNumberMap.put("EPIC", ";3");
		rarityToNumberMap.put("RARE", ";2");
		rarityToNumberMap.put("UNCOMMON", ";1");
		rarityToNumberMap.put("COMMON", ";0");

		/* enchantNames */
		for (String enchantName : getEnchantsJson().getAsJsonObject().keySet()) {
			enchantNames.add(enchantName.toUpperCase());
		}
		if (!enchantNames.contains("ULTIMATE_JERRY")) {
			enchantNames.add("ULTIMATE_JERRY");
		}

		/* allSkillNames */
		allSkillNames.addAll(higherDepth(getLevelingJson(), "leveling_caps").getAsJsonObject().keySet());
		allSkillNames.remove("catacombs");

		/* skillNames */
		skillNames.addAll(allSkillNames);
		skillNames.removeIf(cosmeticSkillNames::contains);

		/* skillsEmojiMap */
		skillsEmojiMap.put("taming", "<:taming:800462115365716018>");
		skillsEmojiMap.put("farming", "<:farming:800462115055992832>");
		skillsEmojiMap.put("foraging", "<:foraging:800462114829500477>");
		skillsEmojiMap.put("combat", "<:combat:800462115009855548>");
		skillsEmojiMap.put("alchemy", "<:alchemy:800462114589376564>");
		skillsEmojiMap.put("fishing", "<:fishing:800462114853617705>");
		skillsEmojiMap.put("enchanting", "<:enchanting:800462115193225256>");
		skillsEmojiMap.put("mining", "<:mining:800462115009069076>");
		skillsEmojiMap.put("carpentry", "<:carpentry:800462115156131880>");
		skillsEmojiMap.put("runecrafting", "<:runecrafting:800462115172909086>");

		/* petNames */
		petNames.addAll(getPetNumsJson().getAsJsonObject().keySet());

		/* reforgeStoneNames */
		reforgeStoneNames.addAll(getReforgeStonesJson().getAsJsonObject().keySet());

		/* essenceItemNames */
		essenceItemNames.addAll(getEssenceCostsJson().getAsJsonObject().keySet());

		/* bitsItemNames */
		bitsItemNames.addAll(getBitsJson().getAsJsonObject().keySet());

		/* petItemNames */
		petItemNames.addAll(higherDepth(getSkyCryptPetJson(), "pet_items").getAsJsonObject().keySet());

		/* slayerWeights */
		slayerWeights.put("rev", new Double[] { 2208D, 0.15D });
		slayerWeights.put("tara", new Double[] { 2118D, 0.08D });
		slayerWeights.put("sven", new Double[] { 1962D, 0.015D });
		slayerWeights.put("enderman", new Double[] { 1430D, 0.017D });

		/* skillWeights */
		skillWeights.put("mining", new Double[] { 1.18207448, 259634D });
		skillWeights.put("foraging", new Double[] { 1.232826, 259634D });
		skillWeights.put("enchanting", new Double[] { 0.96976583, 882758D });
		skillWeights.put("farming", new Double[] { 1.217848139, 220689D });
		skillWeights.put("combat", new Double[] { 1.15797687265, 275862D });
		skillWeights.put("fishing", new Double[] { 1.406418, 88274D });
		skillWeights.put("alchemy", new Double[] { 1.0, 1103448D });
		skillWeights.put("taming", new Double[] { 1.14744, 441379D });

		/* dungeonClassWeights */
		dungeonClassWeights.put("healer", 0.0000045254834D);
		dungeonClassWeights.put("mage", 0.0000045254834D);
		dungeonClassWeights.put("berserk", 0.0000045254834D);
		dungeonClassWeights.put("archer", 0.0000045254834D);
		dungeonClassWeights.put("tank", 0.0000045254834D);

		/* dungeonWeights */
		dungeonWeights.put("catacombs", 0.0002149604615D);
		/* vanillaItems */
		// JsonArray vanillaItemsJson = getVanillaItemsJson().getAsJsonArray();
		// for(JsonElement vanillaItem: vanillaItemsJson){

		// }
	}
}
