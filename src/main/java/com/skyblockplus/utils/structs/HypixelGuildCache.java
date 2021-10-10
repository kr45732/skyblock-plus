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

	private Instant lastUpdated;
	private final List<String> normalCache;
	private final List<String> ironmanCache;

	public HypixelGuildCache() {
		this.normalCache = new ArrayList<>();
		this.ironmanCache = new ArrayList<>();
	}

	public void addPlayer(Player player) {
		normalCache.add(memberCacheFromPlayer(player, false));
		ironmanCache.add(memberCacheFromPlayer(player, true));
	}

	public Instant getLastUpdated(){
		return lastUpdated;
	}

	public HypixelGuildCache setLastUpdated() {
		lastUpdated = Instant.now();
		return this;
	}

	public List<String> getCache(){
		return getCache(false);
	}

	public List<String> getCache(boolean ironmanOnly){
		return ironmanOnly ? ironmanCache : normalCache;
	}

	private String memberCacheFromPlayer(Player player, boolean ironmanOnly) {
		return (
				player.getUsername() +
						"=:=" +
						player.getHighestAmount("slayer", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("skills", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("catacombs", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("weight", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("svenXp", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("revXp", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("taraXp", ironmanOnly) +
						"=:=" +
						player.getHighestAmount("endermanXp", ironmanOnly) +
						"=:=" +
						player.getUuid()
		);
	}

	// [0] - username
	// [1] - slayer
	// [2] - skills
	// [3] - catacombs
	// [4] - weight
	// [5] - sven XP
	// [6] - rev XP
	// [7] - tara XP
	// [8] - enderman XP
	// [9] - uuid
}
