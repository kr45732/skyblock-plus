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

package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.Utils.getLevelingJson;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonArray;
import com.skyblockplus.utils.Player;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HypixelGuildCache {

	public static final List<String> types = List.of("username","uuid","slayer","skills","catacombs","weight","sven","rev","tara","enderman","alchemy","combat","fishing","farming","foraging","carpentry","mining","taming","enchanting","networth","blaze");//, "sand","pumpkin","blaze_rod","netherrack","mushroom","raw_rabbit","string","prismarine_shard","potato","cactus","coal","pufferfish","gunpowder","clay","iron_ingot","jungle_wood","end_stone","ghast_tear","obsidian","acacia_wood","redstone","spruce_wood","raw_salmon","prismarine_crystals","nether_wart","raw_porkchop","gravel","wheat","nether_quartz","clownfish","raw_fish","carrot","gemstone","seeds","sugar_cane","raw_chicken","leather","magma_cream","raw_mutton","gold_ingot","spider_eye","ender_pearl","ink_sack","emerald","cocoa_beans","feather","cobblestone","hard_stone","mithril","oak_wood","diamond","ice","lapis_lazuli","birch_wood","dark_oak_wood","glowstone_dust","lily_pad","sponge","bone","rotten_flesh","slimeball","melon");
	private final List<String> normalCache;
	private final List<String> ironmanCache;
	private final List<String> strandedCache;
	private transient Instant lastUpdated;

	public HypixelGuildCache() {
		this.normalCache = new ArrayList<>();
		this.ironmanCache = new ArrayList<>();
		this.strandedCache = new ArrayList<>();
		this.lastUpdated = Instant.now();
	}

	public static boolean isValidType(String type) {
		return typeToIndex(type.toLowerCase()) >= 2;
	}

	public static int typeToIndex(String type) {
		return IntStream.range(0, types.size()).filter(i -> types.get(i).equals(type)).findFirst().orElse(-1);
	}

	public static String getStringFromCache(String cache, String type) {
		return cache.split("=:=")[typeToIndex(type)];
	}

	public static double getDoubleFromCache(String cache, String type) {
		return Double.parseDouble(getStringFromCache(cache, type));
	}

	public static int getLevelFromCache(String cache, String xpType) {
		return skillInfoFromExp((long) Double.parseDouble(getStringFromCache(cache, xpType)), xpType).currentLevel();
	}

	public void addPlayer(Player player) {
		normalCache.add(playerToCache(player, Player.Gamemode.ALL));
		ironmanCache.add(playerToCache(player, Player.Gamemode.IRONMAN));
		strandedCache.add(playerToCache(player, Player.Gamemode.STRANDED));
	}

	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public HypixelGuildCache setLastUpdated() {
		lastUpdated = Instant.now();
		return this;
	}

	public List<String> getCache() {
		return getCache(Player.Gamemode.ALL);
	}

	public List<String> getCache(Player.Gamemode gamemode) {
		return switch (gamemode) {
			case IRONMAN -> ironmanCache;
			case STRANDED -> strandedCache;
			default -> normalCache;
		};
	}

	private String playerToCache(Player player, Player.Gamemode gamemode) {
		return (
			player.getUsername() +
			"=:=" +
			player.getUuid() +
			"=:=" + getTypes().stream().map(type -> "" + player.getHighestAmount(
					type + switch (type) {
						case "catacombs", "alchemy", "combat", "fishing", "farming", "foraging", "carpentry", "mining", "taming", "enchanting" -> "_xp";
						default -> "";
					}
					, gamemode)).collect(Collectors.joining("=:="))
		);
	}

	public static SkillsStruct skillInfoFromExp(long skillExp, String skill) {
		JsonArray skillsTable =
			switch (skill) {
				case "catacombs" -> higherDepth(getLevelingJson(), "catacombs").getAsJsonArray();
				case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
				case "HOTM" -> higherDepth(getLevelingJson(), "HOTM").getAsJsonArray();
				default -> higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
			};

		int maxLevel = skill.equals("farming") ? 60 : higherDepth(getLevelingJson(), "leveling_caps." + skill, 0);

		long xpTotal = 0L;
		int level = 1;
		for (int i = 0; i < maxLevel; i++) {
			xpTotal += skillsTable.get(i).getAsLong();

			if (xpTotal > skillExp) {
				xpTotal -= skillsTable.get(i).getAsLong();
				break;
			} else {
				level = (i + 1);
			}
		}

		long xpCurrent = (long) Math.floor(skillExp - xpTotal);
		long xpForNext = 0;
		if (level < maxLevel) {
			xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());
		}

		if (skillExp == 0) {
			level = 0;
			xpForNext = 0;
		}

		double progress = xpForNext > 0 ? Math.max(0, Math.min(((double) xpCurrent) / xpForNext, 1)) : 0;

		return new SkillsStruct(skill, level, maxLevel, skillExp, xpCurrent, xpForNext, progress);
	}

	public static List<String> getTypes() {
		return types.subList(2, types.size());
	}
}
