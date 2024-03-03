/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.miscellaneous.weight.cole;

import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;

import com.skyblockplus.utils.Player;
import lombok.Getter;

@Getter
public class ColeWeight {

	private double experienceWeight;
	private double powderWeight;
	private double collectionsWeight;
	private double miscellaneousWeight;

	public ColeWeight(Player.Profile player) {
		// Experience
		experienceWeight += Math.max(player.getSkillXp("mining"), 0) / 1000000.0;

		// Powder
		powderWeight +=
		(higherDepth(player.profileJson(), "mining_core.powder_mithril_total", 0) +
			higherDepth(player.profileJson(), "mining_core.powder_spent_mithril", 0)) /
		60000.0;
		powderWeight +=
		(higherDepth(player.profileJson(), "mining_core.powder_gemstone_total", 0) +
			higherDepth(player.profileJson(), "mining_core.powder_spent_gemstone", 0)) /
		60000.0;

		// Collections
		collectionsWeight += getCollection(player, "MITHRIL_ORE") / 500000.0;
		collectionsWeight += getCollection(player, "GEMSTONE_COLLECTION") / 1400000.0;
		collectionsWeight += getCollection(player, "GOLD_INGOT") / 500000.0;
		collectionsWeight += getCollection(player, "NETHERRACK") / 45000.0;
		collectionsWeight += getCollection(player, "DIAMOND") / 1000000.0;
		collectionsWeight += getCollection(player, "ICE") / 1000000.0;
		collectionsWeight += getCollection(player, "REDSTONE") / 2000000.0;
		collectionsWeight += getCollection(player, "INK_SACK:4") / 4000000.0;
		collectionsWeight += getCollection(player, "SULPHUR") / 9999999999.0;
		collectionsWeight += getCollection(player, "COAL") / 500000.0;
		collectionsWeight += getCollection(player, "EMERALD") / 400000.0;
		collectionsWeight += getCollection(player, "ENDER_STONE") / 400000.0;
		collectionsWeight += getCollection(player, "GLOWSTONE_DUST") / 350000.0;
		collectionsWeight += getCollection(player, "GRAVEL") / 333333.0;
		collectionsWeight += getCollection(player, "IRON_INGOT") / 1000000.0;
		collectionsWeight += getCollection(player, "MYCEL") / 300000.0;
		collectionsWeight += getCollection(player, "QUARTZ") / 400000.0;
		collectionsWeight += getCollection(player, "OBSIDIAN") / 200000.0;
		collectionsWeight += getCollection(player, "SAND:1") / 150000.0;
		collectionsWeight += getCollection(player, "SAND") / 500000.0;
		collectionsWeight += getCollection(player, "COBBLESTONE") / 1000000.0;
		collectionsWeight += getCollection(player, "HARD_STONE") / 200000.0;
		collectionsWeight += getCollection(player, "METAL_HEART") / 40.0;

		// Misc
		miscellaneousWeight += higherDepth(player.profileJson(), "bestiary.kills.scatha_10", 0) / 4.0;
		miscellaneousWeight += higherDepth(player.profileJson(), "bestiary.kills.worm_5", 0) / 16.0;
		miscellaneousWeight += higherDepth(player.profileJson(), "mining_core.crystals.jade_crystal.total_placed", 0) / 2.0;
	}

	public double getTotalWeight() {
		return experienceWeight + powderWeight + collectionsWeight + miscellaneousWeight;
	}

	private long getCollection(Player.Profile player, String name) {
		return Math.max(player.getCollection(name), 0);
	}
}
