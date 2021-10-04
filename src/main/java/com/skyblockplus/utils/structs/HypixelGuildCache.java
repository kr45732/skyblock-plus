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
import java.util.List;

public class HypixelGuildCache {

	public final Instant lastUpdated;
	public final List<String> membersCache;

	public HypixelGuildCache(Instant lastUpdated, List<String> membersCache) {
		this.lastUpdated = lastUpdated;
		this.membersCache = membersCache;
	}

	public static String memberCacheFromPlayer(Player player) {
		return memberCacheFromPlayer(player, false);
	}

	public static String memberCacheFromPlayer(Player player, boolean ironmanOnly) {
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
