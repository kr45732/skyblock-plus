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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.Main;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class Constants {

	public static JsonObject CONSTANTS;

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
	public static Map<String, String> DUNGEON_EMOJI_MAP;
	public static List<String> FETCHUR_ITEMS;
	public static Map<String, String> HARP_SONG_ID_TO_NAME;
	public static Map<String, String> HOTM_PERK_ID_TO_NAME;
	public static Map<String, Integer> HOTM_PERK_MAX_LEVEL;
	public static Map<String, String> SLAYER_EMOJI_MAP;
	public static Map<String, String> SLAYER_NAMES_MAP;
	public static Map<String, String> ESSENCE_EMOJI_MAP;
	public static Map<String, Integer> IGNORED_ENCHANTS;
	public static Map<String, String> MAYOR_NAME_TO_SKIN;
	public static JsonObject ARMOR_PRESTIGE_COST;
	public static JsonObject POWER_TO_BASE_STATS;

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
	public static Map<String, String> NUMBER_TO_RARITY_MAP;

	public static void initialize() {
		try {
			CONSTANTS = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/Constants.json").getAsJsonObject();

			Type mapStringString = new TypeToken<Map<String, String>>() {}.getType();
			Type listInteger = new TypeToken<List<Integer>>() {}.getType();
			Type listString = new TypeToken<List<String>>() {}.getType();
			Type mapStringInteger = new TypeToken<Map<String, Integer>>() {}.getType();

			/* CATACOMBS_LEVEL_50_XP */
			CATACOMBS_LEVEL_50_XP = higherDepth(CONSTANTS, "CATACOMBS_LEVEL_50_XP").getAsDouble();

			/* SKILLS_LEVEL_50_XP */
			SKILLS_LEVEL_50_XP = higherDepth(CONSTANTS, "SKILLS_LEVEL_50_XP").getAsDouble();

			/* SKILLS_LEVEL_60_XP */
			SKILLS_LEVEL_60_XP = higherDepth(CONSTANTS, "SKILLS_LEVEL_60_XP").getAsDouble();

			/* RARITY_TO_NUMBER_MAP */
			RARITY_TO_NUMBER_MAP = gson.fromJson(higherDepth(CONSTANTS, "RARITY_TO_NUMBER_MAP"), mapStringString);

			/* CRAFTED_MINIONS_TO_SLOTS */
			CRAFTED_MINIONS_TO_SLOTS = gson.fromJson(higherDepth(CONSTANTS, "CRAFTED_MINIONS_TO_SLOTS"), listInteger);

			/* COSMETIC_SKILL_NAMES */
			COSMETIC_SKILL_NAMES = gson.fromJson(higherDepth(CONSTANTS, "COSMETIC_SKILL_NAMES"), listString);

			/* SKILLS_EMOJI_MAP */
			SKILLS_EMOJI_MAP = gson.fromJson(higherDepth(CONSTANTS, "SKILLS_EMOJI_MAP"), mapStringString);

			/* DUNGEON_CLASS_NAMES */
			DUNGEON_CLASS_NAMES = gson.fromJson(higherDepth(CONSTANTS, "DUNGEON_CLASS_NAMES"), listString);

			/* SLAYER_NAMES */
			SLAYER_NAMES = gson.fromJson(higherDepth(CONSTANTS, "SLAYER_NAMES"), listString);

			/* GUILD_EXP_TO_LEVEL */
			GUILD_EXP_TO_LEVEL = gson.fromJson(higherDepth(CONSTANTS, "GUILD_EXP_TO_LEVEL"), listInteger);

			/* DUNGEON_EMOJI_MAP */
			DUNGEON_EMOJI_MAP = gson.fromJson(higherDepth(CONSTANTS, "DUNGEON_EMOJI_MAP"), mapStringString);

			/* FETCHUR_ITEMS */
			FETCHUR_ITEMS = gson.fromJson(higherDepth(CONSTANTS, "FETCHUR_ITEMS"), listString);

			/* HARP_SONG_ID_TO_NAME */
			HARP_SONG_ID_TO_NAME = gson.fromJson(higherDepth(CONSTANTS, "HARP_SONG_ID_TO_NAME"), mapStringString);

			/* HOTM_PERK_ID_TO_NAME */
			HOTM_PERK_ID_TO_NAME = gson.fromJson(higherDepth(CONSTANTS, "HOTM_PERK_ID_TO_NAME"), mapStringString);

			/* HOTM_PERK_MAX_LEVEL */
			HOTM_PERK_MAX_LEVEL = gson.fromJson(higherDepth(CONSTANTS, "HOTM_PERK_MAX_LEVEL"), mapStringInteger);

			/* SLAYER_EMOJI_MAP */
			SLAYER_EMOJI_MAP = gson.fromJson(higherDepth(CONSTANTS, "SLAYER_EMOJI_MAP"), mapStringString);

			/* SLAYER_NAMES_MAP */
			SLAYER_NAMES_MAP = gson.fromJson(higherDepth(CONSTANTS, "SLAYER_NAMES_MAP"), mapStringString);

			/* ESSENCE_EMOJI_MAP */
			ESSENCE_EMOJI_MAP = gson.fromJson(higherDepth(CONSTANTS, "ESSENCE_EMOJI_MAP"), mapStringString);

			/* IGNORED_ENCHANTS */
			IGNORED_ENCHANTS = gson.fromJson(higherDepth(CONSTANTS, "IGNORED_ENCHANTS"), mapStringInteger);

			/* MAYOR_NAME_TO_SKIN */
			MAYOR_NAME_TO_SKIN = gson.fromJson(higherDepth(CONSTANTS, "MAYOR_NAME_TO_SKIN"), mapStringString);

			/* ARMOR_PRESTIGE_COST */
			ARMOR_PRESTIGE_COST = higherDepth(CONSTANTS, "ARMOR_PRESTIGE_COST").getAsJsonObject();

			/* POWER_TO_BASE_STATS */
			POWER_TO_BASE_STATS = higherDepth(CONSTANTS, "POWER_TO_BASE_STATS").getAsJsonObject();

			/* ENCHANT_NAMES */
			HashSet<String> enchantNames = new HashSet<>();
			for (Map.Entry<String, JsonElement> enchantArr : higherDepth(getEnchantsJson(), "enchants").getAsJsonObject().entrySet()) {
				for (JsonElement enchantName : enchantArr.getValue().getAsJsonArray()) {
					enchantNames.add(enchantName.getAsString().toUpperCase());
				}
			}
			enchantNames.add("ULTIMATE_JERRY");
			ENCHANT_NAMES = new ArrayList<>(enchantNames);

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
			PET_ITEM_NAMES =
				getSkyblockItemsJson()
					.entrySet()
					.stream()
					.filter(e -> higherDepth(e.getValue(), "category", "").equals("PET_ITEM"))
					.map(Map.Entry::getKey)
					.toList();

			/* ALL_TALISMANS */
			ALL_TALISMANS = new HashSet<>();
			for (Map.Entry<String, JsonElement> talismanUpgrade : higherDepth(getTalismanJson(), "ACCESSORIES")
				.getAsJsonObject()
				.entrySet()) {
				ALL_TALISMANS.add(talismanUpgrade.getKey());
				if (higherDepth(getTalismanJson(), "ACCESSORY_DUPLICATES." + talismanUpgrade.getKey()) != null) {
					for (JsonElement duplicate : higherDepth(getTalismanJson(), "ACCESSORY_DUPLICATES." + talismanUpgrade.getKey())
						.getAsJsonArray()) {
						ALL_TALISMANS.add(duplicate.getAsString());
					}
				}
			}
			ALL_TALISMANS.add("CHUMMING_TALISMAN");

			/* NUMBER_TO_RARITY_MAP */
			NUMBER_TO_RARITY_MAP =
				RARITY_TO_NUMBER_MAP.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().replace(";", ""), Map.Entry::getKey));
		} catch (Exception e) {
			Main.log.error("Exception while initializing constants", e);
		}
	}

	public static JsonElement getConstant(String key) {
		return higherDepth(CONSTANTS, key);
	}
}
