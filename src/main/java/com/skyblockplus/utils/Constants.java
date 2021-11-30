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

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.Main;
import java.lang.reflect.Type;
import java.util.*;

public class Constants {

	/* Constants JSON */
	public static double CATACOMBS_LEVEL_50_XP;
	public static double SKILLS_LEVEL_50_XP;
	public static double SKILLS_LEVEL_60_XP;
	public static Map<String, String> RARITY_TO_NUMBER_MAP;
	public static List<Integer> CRAFTED_MINIONS_TO_SLOTS;
	public static List<String> COSMETIC_SKILL_NAMES;
	public static Map<String, String> SKILLS_EMOJI_MAP;
	public static List<String> DUNGEON_CLASS_NAMES;
	public static List<String> SLAYER_NAMES;
	public static List<Integer> GUILD_EXP_TO_LEVEL;
	public static Map<String, Double[]> SLAYER_WEIGHTS;
	public static Map<String, Double[]> SKILL_WEIGHTS;
	public static Map<String, Double> DUNGEON_CLASS_WEIGHTS;
	public static Map<String, Double> DUNGEON_WEIGHTS;
	public static Map<String, String> DUNGEON_EMOJI_MAP;
	public static List<String> FETCHUR_ITEMS;
	public static List<String> DUNGEON_META_ITEMS;
	public static Map<String, String> HARP_SONG_ID_TO_NAME;
	public static JsonElement SLAYER_DEPRECATION_SCALING;
	public static JsonElement SKILL_RATIO_WEIGHT;
	public static JsonElement SKILL_FACTORS;
	public static JsonElement SKILL_OVERFLOW_MULTIPLIERS;
	public static JsonObject DUNGEON_COMPLETION_WORTH;
	public static JsonObject DUNGEON_COMPLETION_BUFFS;
	public static Map<String, String> HOTM_PERK_ID_TO_NAME;
	public static Map<String, Integer> HOTM_PERK_MAX_LEVEL;
	public static Map<String, String> SLAYER_EMOJI_MAP;
	public static Map<String, String> ESSENCE_EMOJI_MAP;

	/* Fetched from other sources */
	public static List<String> ENCHANT_NAMES;
	public static List<String> ALL_SKILL_NAMES;
	public static List<String> SKILL_NAMES;
	public static List<String> PET_NAMES;
	public static List<String> REFORGE_STONE_NAMES;
	public static List<String> ESSENCE_ITEM_NAMES;
	public static List<String> BITS_ITEM_NAMES;
	public static List<String> PET_ITEM_NAMES;
	public static Set<String> ALL_TALISMANS;
	public static Map<String, Long> COLLECTION_ID_TO_MAX_AMOUNT;

	public static void initialize() {
		try {
			JsonObject constantsJson = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/Constants.json")
				.getAsJsonObject();

			Type mapStringString = new TypeToken<Map<String, String>>() {}.getType();
			Type listInteger = new TypeToken<List<Integer>>() {}.getType();
			Type listString = new TypeToken<List<String>>() {}.getType();
			Type mapStringDoubleArray = new TypeToken<Map<String, Double[]>>() {}.getType();
			Type mapStringDouble = new TypeToken<Map<String, Double>>() {}.getType();
			Type mapStringInteger = new TypeToken<Map<String, Integer>>() {}.getType();

			/* CATACOMBS_LEVEL_50_XP */
			CATACOMBS_LEVEL_50_XP = higherDepth(constantsJson, "CATACOMBS_LEVEL_50_XP").getAsDouble();

			/* SKILLS_LEVEL_50_XP */
			SKILLS_LEVEL_50_XP = higherDepth(constantsJson, "SKILLS_LEVEL_50_XP").getAsDouble();

			/* SKILLS_LEVEL_60_XP */
			SKILLS_LEVEL_60_XP = higherDepth(constantsJson, "SKILLS_LEVEL_60_XP").getAsDouble();

			/* RARITY_TO_NUMBER_MAP */
			RARITY_TO_NUMBER_MAP = gson.fromJson(higherDepth(constantsJson, "RARITY_TO_NUMBER_MAP"), mapStringString);

			/* CRAFTED_MINIONS_TO_SLOTS */
			CRAFTED_MINIONS_TO_SLOTS = gson.fromJson(higherDepth(constantsJson, "CRAFTED_MINIONS_TO_SLOTS"), listInteger);

			/* COSMETIC_SKILL_NAMES */
			COSMETIC_SKILL_NAMES = gson.fromJson(higherDepth(constantsJson, "COSMETIC_SKILL_NAMES"), listString);

			/* SKILLS_EMOJI_MAP */
			SKILLS_EMOJI_MAP = gson.fromJson(higherDepth(constantsJson, "SKILLS_EMOJI_MAP"), mapStringString);

			/* DUNGEON_CLASS_NAMES */
			DUNGEON_CLASS_NAMES = gson.fromJson(higherDepth(constantsJson, "DUNGEON_CLASS_NAMES"), listString);

			/* SLAYER_NAMES */
			SLAYER_NAMES = gson.fromJson(higherDepth(constantsJson, "SLAYER_NAMES"), listString);

			/* GUILD_EXP_TO_LEVEL */
			GUILD_EXP_TO_LEVEL = gson.fromJson(higherDepth(constantsJson, "GUILD_EXP_TO_LEVEL"), listInteger);

			/* SLAYER_WEIGHTS */
			SLAYER_WEIGHTS = gson.fromJson(higherDepth(constantsJson, "SLAYER_WEIGHTS"), mapStringDoubleArray);

			/* SKILL_WEIGHTS */
			SKILL_WEIGHTS = gson.fromJson(higherDepth(constantsJson, "SKILL_WEIGHTS"), mapStringDoubleArray);

			/* DUNGEON_CLASS_WEIGHTS */
			DUNGEON_CLASS_WEIGHTS = gson.fromJson(higherDepth(constantsJson, "DUNGEON_CLASS_WEIGHTS"), mapStringDouble);

			/* DUNGEON_WEIGHTS */
			DUNGEON_WEIGHTS = gson.fromJson(higherDepth(constantsJson, "DUNGEON_WEIGHTS"), mapStringDouble);

			/* DUNGEON_EMOJI_MAP */
			DUNGEON_EMOJI_MAP = gson.fromJson(higherDepth(constantsJson, "DUNGEON_EMOJI_MAP"), mapStringString);

			/* FETCHUR_ITEMS */
			FETCHUR_ITEMS = gson.fromJson(higherDepth(constantsJson, "FETCHUR_ITEMS"), listString);

			/* DUNGEON_META_ITEMS */
			DUNGEON_META_ITEMS = gson.fromJson(higherDepth(constantsJson, "DUNGEON_META_ITEMS"), listString);

			/* HARP_SONG_ID_TO_NAME */
			HARP_SONG_ID_TO_NAME = gson.fromJson(higherDepth(constantsJson, "HARP_SONG_ID_TO_NAME"), mapStringString);

			/* SLAYER_DEPRECATION_SCALING */
			SLAYER_DEPRECATION_SCALING = higherDepth(constantsJson, "SLAYER_DEPRECATION_SCALING");

			/* SKILL_RATIO_WEIGHT */
			SKILL_RATIO_WEIGHT = higherDepth(constantsJson, "SKILL_RATIO_WEIGHT");

			/* SKILL_FACTORS */
			SKILL_FACTORS = higherDepth(constantsJson, "SKILL_FACTORS");

			/* SKILL_OVERFLOW_MULTIPLIERS */
			SKILL_OVERFLOW_MULTIPLIERS = higherDepth(constantsJson, "SKILL_OVERFLOW_MULTIPLIERS");

			/* DUNGEON_COMPLETION_WORTH */
			DUNGEON_COMPLETION_WORTH = higherDepth(constantsJson, "DUNGEON_COMPLETION_WORTH").getAsJsonObject();

			/* DUNGEON_COMPLETION_BUFFS */
			DUNGEON_COMPLETION_BUFFS = higherDepth(constantsJson, "DUNGEON_COMPLETION_BUFFS").getAsJsonObject();

			/* HOTM_PERK_ID_TO_NAME */
			HOTM_PERK_ID_TO_NAME = gson.fromJson(higherDepth(constantsJson, "HOTM_PERK_ID_TO_NAME"), mapStringString);

			/* HOTM_PERK_MAX_LEVEL */
			HOTM_PERK_MAX_LEVEL = gson.fromJson(higherDepth(constantsJson, "HOTM_PERK_MAX_LEVEL"), mapStringInteger);

			/* SLAYER_EMOJI_MAP */
			SLAYER_EMOJI_MAP = gson.fromJson(higherDepth(constantsJson, "SLAYER_EMOJI_MAP"), mapStringString);

			/* ESSENCE_EMOJI_MAP */
			ESSENCE_EMOJI_MAP = gson.fromJson(higherDepth(constantsJson, "ESSENCE_EMOJI_MAP"), mapStringString);

			/* ENCHANT_NAMES */
			ENCHANT_NAMES = new ArrayList<>();
			for (Map.Entry<String, JsonElement> enchantArr : higherDepth(getEnchantsJson(), "enchants").getAsJsonObject().entrySet()) {
				for (JsonElement enchantName : enchantArr.getValue().getAsJsonArray()) {
					ENCHANT_NAMES.add(enchantName.getAsString().toUpperCase());
				}
			}
			if (!ENCHANT_NAMES.contains("ULTIMATE_JERRY")) {
				ENCHANT_NAMES.add("ULTIMATE_JERRY");
			}

			/* ALL_SKILL_NAMES */
			ALL_SKILL_NAMES = new ArrayList<>(higherDepth(getLevelingJson(), "leveling_caps").getAsJsonObject().keySet());
			ALL_SKILL_NAMES.remove("HOTM");
			ALL_SKILL_NAMES.remove("catacombs");

			/* SKILL_NAMES */
			SKILL_NAMES = new ArrayList<>(ALL_SKILL_NAMES);
			SKILL_NAMES.removeIf(COSMETIC_SKILL_NAMES::contains);

			/* PET_NAMES */
			PET_NAMES = new ArrayList<>(getPetNumsJson().getAsJsonObject().keySet());

			/* REFORGE_STONE_NAMES */
			REFORGE_STONE_NAMES = new ArrayList<>(getReforgeStonesJson().getAsJsonObject().keySet());

			/* ESSENCE_ITEM_NAMES */
			ESSENCE_ITEM_NAMES = new ArrayList<>(getEssenceCostsJson().getAsJsonObject().keySet());

			/* BITS_ITEM_NAMES */
			BITS_ITEM_NAMES = new ArrayList<>(getBitsJson().getAsJsonObject().keySet());

			/* PET_ITEM_NAMES */
			PET_ITEM_NAMES = new ArrayList<>(higherDepth(getSkyCryptPetJson(), "pet_items").getAsJsonObject().keySet());

			/* ALL_TALISMANS */
			ALL_TALISMANS = new HashSet<>();
			for (Map.Entry<String, JsonElement> talismanUpgrade : higherDepth(getTalismanJson(), "talismans")
				.getAsJsonObject()
				.entrySet()) {
				ALL_TALISMANS.add(talismanUpgrade.getKey());
				if (higherDepth(getTalismanJson(), "talisman_duplicates." + talismanUpgrade.getKey()) != null) {
					for (JsonElement duplicate : higherDepth(getTalismanJson(), "talisman_duplicates." + talismanUpgrade.getKey())
						.getAsJsonArray()) {
						ALL_TALISMANS.add(duplicate.getAsString());
					}
				}
			}

			/* COLLECTION_ID_TO_MAX_AMOUNT */
			COLLECTION_ID_TO_MAX_AMOUNT = new HashMap<>();
			for (Map.Entry<String, JsonElement> collectionCategories : higherDepth(
				getJson("https://api.hypixel.net/resources/skyblock/collections"),
				"collections"
			)
				.getAsJsonObject()
				.entrySet()) {
				for (Map.Entry<String, JsonElement> collection : higherDepth(collectionCategories.getValue(), "items")
					.getAsJsonObject()
					.entrySet()) {
					JsonArray collectionTiers = higherDepth(collection.getValue(), "tiers").getAsJsonArray();
					COLLECTION_ID_TO_MAX_AMOUNT.put(
						collection.getKey(),
						higherDepth(collectionTiers.get(collectionTiers.size() - 1), "amountRequired").getAsLong()
					);
				}
			}
		} catch (Exception e) {
			Main.log.error("Exception while initializing constants", e);
		}
	}
}
