/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils;

import com.google.gson.JsonElement;
import com.skyblockplus.Main;

import java.util.*;

import static com.skyblockplus.utils.Utils.*;

public class Constants {

	public static final double CATACOMBS_LEVEL_50_XP = 569809640;
	public static final double SKILLS_LEVEL_50_XP = 55172425;
	public static final double SKILLS_LEVEL_60_XP = 111672425;
	public static final Map<String, String> RARITY_TO_NUMBER_MAP = new HashMap<>();
	public static final List<String> ENCHANT_NAMES = new ArrayList<>();
	public static final List<Integer> CRAFTED_MINIONS_TO_SLOTS = Arrays.asList(
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
	public static final List<String> SKILL_NAMES = new ArrayList<>();
	public static final List<String> ALL_SKILL_NAMES = new ArrayList<>();
	public static final List<String> COSMETIC_SKILL_NAMES = Arrays.asList("runecrafting", "carpentry");
	public static final Map<String, String> SKILLS_EMOJI_MAP = new HashMap<>();
	public static final List<String> PET_NAMES = new ArrayList<>();
	public static final List<String> DUNGEON_CLASS_NAMES = Arrays.asList("healer", "mage", "berserk", "archer", "tank");
	public static final List<String> SLAYER_NAMES = Arrays.asList("sven", "tara", "rev", "enderman");
	public static final List<String> REFORGE_STONE_NAMES = new ArrayList<>();
	public static final List<String> ESSENCE_ITEM_NAMES = new ArrayList<>();
	public static final List<String> BITS_ITEM_NAMES = new ArrayList<>();
	public static final List<String> PET_ITEM_NAMES = new ArrayList<>();
	public static final List<Integer> GUILD_EXP_TO_LEVEL = Arrays.asList(
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
	public static final Map<String, Double[]> SLAYER_WEIGHTS = new HashMap<>();
	public static final Map<String, Double[]> SKILL_WEIGHTS = new HashMap<>();
	public static final Map<String, Double> DUNGEON_CLASS_WEIGHTS = new HashMap<>();
	public static final Map<String, Double> DUNGEON_WEIGHTS = new HashMap<>();
	public static final Map<String, String> DUNGEON_EMOJI_MAP = new HashMap<>();
	public static final List<String> FETCHUR_ITEMS = Arrays.asList(
		"**Item:** Red Wool\n**Quantity:** 50|WOOL:14",
		"**Item:** Yellow Stained Glass\n**Quantity:** 20|STAINED_GLASS:4",
		"**Item:** Compass\n**Quantity:** 1|COMPASS",
		"**Item:** Mithril\n**Quantity:** 20|MITHRIL_ORE",
		"**Item:** Firework Rocket\n**Quantity:** 1|FIREWORK",
		"**Item:** Cheap Coffee or Decent Coffee\n**Quantity:** 1|CHEAP_COFFEE",
		"**Item:** Iron Door or Any Wooden Door\n**Quantity:** 20|WOOD_DOOR",
		"**Item:** Rabbit's Feet\n**Quantity:** 3|RABBIT_FOOT",
		"**Item:** Superboom TNT\n**Quantity:** 1|SUPERBOOM_TNT",
		"**Item:** Pumpkin\n**Quantity:** 1|PUMPKIN",
		"**Item:** Flint and Steel\n**Quantity:** 1|FLINT_AND_STEEL",
		"**Item:** Nether Quartz Ore\n**Quantity:** 50|QUARTZ",
		"**Item:** Ender Pearl\n**Quantity:** 16|ENDER_PEARL"
	);
	public static final List<String> DUNGEON_META_ITEMS = Arrays.asList(
		"HYPERION",
		"VALKYRIE",
		"SCYLLA",
		"AXE_OF_THE_SHREDDED",
		"JUJU_SHORTBOW",
		"TERMINATOR"
	);
	public static final Map<String, String> HARP_SONG_ID_TO_NAME = new HashMap<>();

	public static void initialize() {
		try {/* rarityToNumberMap */
			RARITY_TO_NUMBER_MAP.put("MYTHIC", ";5");
			RARITY_TO_NUMBER_MAP.put("LEGENDARY", ";4");
			RARITY_TO_NUMBER_MAP.put("EPIC", ";3");
			RARITY_TO_NUMBER_MAP.put("RARE", ";2");
			RARITY_TO_NUMBER_MAP.put("UNCOMMON", ";1");
			RARITY_TO_NUMBER_MAP.put("COMMON", ";0");

			/* enchantNames */
			for (Map.Entry<String, JsonElement> enchantArr : higherDepth(getEnchantsJson(), "enchants").getAsJsonObject().entrySet()) {
				for (JsonElement enchantName : enchantArr.getValue().getAsJsonArray()) {
					ENCHANT_NAMES.add(enchantName.getAsString().toUpperCase());
				}
			}
			if (!ENCHANT_NAMES.contains("ULTIMATE_JERRY")) {
				ENCHANT_NAMES.add("ULTIMATE_JERRY");
			}

			/* allSkillNames */
			ALL_SKILL_NAMES.addAll(higherDepth(getLevelingJson(), "leveling_caps").getAsJsonObject().keySet());
			ALL_SKILL_NAMES.remove("catacombs");

			/* skillNames */
			SKILL_NAMES.addAll(ALL_SKILL_NAMES);
			SKILL_NAMES.removeIf(COSMETIC_SKILL_NAMES::contains);

			/* skillsEmojiMap */
			SKILLS_EMOJI_MAP.put("taming", "<:taming:800462115365716018>");
			SKILLS_EMOJI_MAP.put("farming", "<:farming:800462115055992832>");
			SKILLS_EMOJI_MAP.put("foraging", "<:foraging:800462114829500477>");
			SKILLS_EMOJI_MAP.put("combat", "<:combat:800462115009855548>");
			SKILLS_EMOJI_MAP.put("alchemy", "<:alchemy:800462114589376564>");
			SKILLS_EMOJI_MAP.put("fishing", "<:fishing:800462114853617705>");
			SKILLS_EMOJI_MAP.put("enchanting", "<:enchanting:800462115193225256>");
			SKILLS_EMOJI_MAP.put("mining", "<:mining:800462115009069076>");
			SKILLS_EMOJI_MAP.put("carpentry", "<:carpentry:800462115156131880>");
			SKILLS_EMOJI_MAP.put("runecrafting", "<:runecrafting:800462115172909086>");

			/* petNames */
			PET_NAMES.addAll(getPetNumsJson().getAsJsonObject().keySet());

			/* reforgeStoneNames */
			REFORGE_STONE_NAMES.addAll(getReforgeStonesJson().getAsJsonObject().keySet());

			/* essenceItemNames */
			ESSENCE_ITEM_NAMES.addAll(getEssenceCostsJson().getAsJsonObject().keySet());

			/* bitsItemNames */
			BITS_ITEM_NAMES.addAll(getBitsJson().getAsJsonObject().keySet());

			/* petItemNames */
			PET_ITEM_NAMES.addAll(higherDepth(getSkyCryptPetJson(), "pet_items").getAsJsonObject().keySet());

			/* slayerWeights */
			SLAYER_WEIGHTS.put("rev", new Double[] { 2208D, 0.15D });
			SLAYER_WEIGHTS.put("tara", new Double[] { 2118D, 0.08D });
			SLAYER_WEIGHTS.put("sven", new Double[] { 1962D, 0.015D });
			SLAYER_WEIGHTS.put("enderman", new Double[] { 1430D, 0.017D });

			/* skillWeights */
			SKILL_WEIGHTS.put("mining", new Double[] { 1.18207448, 259634D });
			SKILL_WEIGHTS.put("foraging", new Double[] { 1.232826, 259634D });
			SKILL_WEIGHTS.put("enchanting", new Double[] { 0.96976583, 882758D });
			SKILL_WEIGHTS.put("farming", new Double[] { 1.217848139, 220689D });
			SKILL_WEIGHTS.put("combat", new Double[] { 1.15797687265, 275862D });
			SKILL_WEIGHTS.put("fishing", new Double[] { 1.406418, 88274D });
			SKILL_WEIGHTS.put("alchemy", new Double[] { 1.0, 1103448D });
			SKILL_WEIGHTS.put("taming", new Double[] { 1.14744, 441379D });

			/* dungeonClassWeights */
			DUNGEON_CLASS_WEIGHTS.put("healer", 0.0000045254834D);
			DUNGEON_CLASS_WEIGHTS.put("mage", 0.0000045254834D);
			DUNGEON_CLASS_WEIGHTS.put("berserk", 0.0000045254834D);
			DUNGEON_CLASS_WEIGHTS.put("archer", 0.0000045254834D);
			DUNGEON_CLASS_WEIGHTS.put("tank", 0.0000045254834D);

			/* dungeonWeights */
			DUNGEON_WEIGHTS.put("catacombs", 0.0002149604615D);

			/* dungeonEmojiMap */
			DUNGEON_EMOJI_MAP.put("healer", "<:healer:867508034914091058>");
			DUNGEON_EMOJI_MAP.put("mage", "<:mage:867508034602663947>");
			DUNGEON_EMOJI_MAP.put("berserk", "<:berserk:867508034870968350>");
			DUNGEON_EMOJI_MAP.put("archer", "<:archer:867508034561507329>");
			DUNGEON_EMOJI_MAP.put("tank", "<:tank:867508034888663050>");
			DUNGEON_EMOJI_MAP.put("catacombs_0", "<:entrance:867511391438503946>");
			DUNGEON_EMOJI_MAP.put("catacombs_1", "<:floor_1:867511391400230942>");
			DUNGEON_EMOJI_MAP.put("catacombs_2", "<:floor_2:867511391287640074>");
			DUNGEON_EMOJI_MAP.put("catacombs_3", "<:floor_3:867511391333253180>");
			DUNGEON_EMOJI_MAP.put("catacombs_4", "<:floor_4:867511391615057950>");
			DUNGEON_EMOJI_MAP.put("catacombs_5", "<:floor_5:867511391434178581>");
			DUNGEON_EMOJI_MAP.put("catacombs_6", "<:floor_6:867511391284101210>");
			DUNGEON_EMOJI_MAP.put("catacombs_7", "<:floor_7:867511391635111936>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_1", "<:master_floor_1:867511391488835645>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_2", "<:master_floor_2:867511391477170207>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_3", "<:master_floor_3:867511391282921522>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_4", "<:master_floor_4:867511391535759440>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_5", "<:master_floor_5:867511391518457877>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_6", "<:master_floor_6:867511391228657685>");
			DUNGEON_EMOJI_MAP.put("master_catacombs_7", "<:master_floor_7:867511391644549120>");
			DUNGEON_EMOJI_MAP.put("catacombs", "<:catacombs:867535694103707679>");

			/* HARP_SONG_ID_TO_NAME */
			HARP_SONG_ID_TO_NAME.put("hymn_joy", "Hymn to the Joy");
			HARP_SONG_ID_TO_NAME.put("frere_jacques", "Frère Jacques");
			HARP_SONG_ID_TO_NAME.put("amazing_grace", "Amazing Grace");
			HARP_SONG_ID_TO_NAME.put("brahms", "Brahm's Lullaby");
			HARP_SONG_ID_TO_NAME.put("happy_birthday", "Happy Birthday to You");
			HARP_SONG_ID_TO_NAME.put("greensleeves", "Greensleeves");
			HARP_SONG_ID_TO_NAME.put("jeopardy", "Geothermy?");
			HARP_SONG_ID_TO_NAME.put("minuet", "Minuet");
			HARP_SONG_ID_TO_NAME.put("joy_world", "Joy to the World");
			HARP_SONG_ID_TO_NAME.put("pure_imagination", "Godly Imagination");
			HARP_SONG_ID_TO_NAME.put("vie_en_rose", "La Vie en Rose");
			HARP_SONG_ID_TO_NAME.put("fire_and_flames", "Through the Campfire");
			HARP_SONG_ID_TO_NAME.put("pachelbel", "Pachelbel");
		} catch (Exception e) {
			Main.log.error("Exception while initializing constants", e);
		}
	}
}
