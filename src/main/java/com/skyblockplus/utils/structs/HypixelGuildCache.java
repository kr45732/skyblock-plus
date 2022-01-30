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

import com.skyblockplus.utils.Player;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HypixelGuildCache {

	private static final String repeat = "=:=-1.0".repeat(17);

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
		return switch (type) {
			case "username" -> 0;
			case "uuid" -> 1;
			case "slayer" -> 2;
			case "skills" -> 3;
			case "catacombs" -> 4;
			case "weight" -> 5;
			case "sven" -> 6;
			case "rev" -> 7;
			case "tara" -> 8;
			case "enderman" -> 9;
			case "alchemy" -> 10;
			case "combat" -> 11;
			case "fishing" -> 12;
			case "farming" -> 13;
			case "foraging" -> 14;
			case "carpentry" -> 15;
			case "mining" -> 16;
			case "taming" -> 17;
			case "enchanting" -> 18;
			default -> -1;
		};
	}

	public static String getStringFromCache(String cache, String type) {
		return cache.split("=:=")[typeToIndex(type)];
	}

	public static double getDoubleFromCache(String cache, String type) {
		return Double.parseDouble(getStringFromCache(cache, type));
	}

	public void addPlayer(Player player) {
		normalCache.add(playerToCache(player, Player.Gamemode.ALL));
		ironmanCache.add(playerToCache(player, Player.Gamemode.IRONMAN));
		strandedCache.add(playerToCache(player, Player.Gamemode.STRANDED));
	}

	public synchronized void addPlayerLeaderboard(Player player) {
		String cache;
		if((cache = playerToCache(player, Player.Gamemode.ALL)) != null) {
			normalCache.removeIf(c -> getStringFromCache(c, "uuid").equals(player.getUuid()));
			normalCache.add(cache);
		}
		if((cache = playerToCache(player, Player.Gamemode.IRONMAN)) != null) {
			ironmanCache.removeIf(c -> getStringFromCache(c, "uuid").equals(player.getUuid()));
			ironmanCache.add(cache);
		}
		if((cache = playerToCache(player, Player.Gamemode.STRANDED)) != null) {
			strandedCache.removeIf(c -> getStringFromCache(c, "uuid").equals(player.getUuid()));
			strandedCache.add(cache);
		}
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
		String cache = (
			player.getUsername() +
			"=:=" +
			player.getUuid() +
			"=:=" +
			player.getHighestAmount("slayer", gamemode) +
			"=:=" +
			player.getHighestAmount("skills", gamemode) +
			"=:=" +
			player.getHighestAmount("catacombs", gamemode) +
			"=:=" +
			player.getHighestAmount("weight", gamemode) +
			"=:=" +
			player.getHighestAmount("sven", gamemode) +
			"=:=" +
			player.getHighestAmount("rev", gamemode) +
			"=:=" +
			player.getHighestAmount("tara", gamemode) +
			"=:=" +
			player.getHighestAmount("enderman", gamemode) +
			"=:=" +
			player.getHighestAmount("alchemy", gamemode) +
			"=:=" +
			player.getHighestAmount("combat", gamemode) +
			"=:=" +
			player.getHighestAmount("fishing", gamemode) +
			"=:=" +
			player.getHighestAmount("farming", gamemode) +
			"=:=" +
			player.getHighestAmount("foraging", gamemode) +
			"=:=" +
			player.getHighestAmount("carpentry", gamemode) +
			"=:=" +
			player.getHighestAmount("mining", gamemode) +
			"=:=" +
			player.getHighestAmount("taming", gamemode) +
			"=:=" +
			player.getHighestAmount("enchanting", gamemode)
		);

		return cache.endsWith(repeat) ? null : cache;
	}
}
