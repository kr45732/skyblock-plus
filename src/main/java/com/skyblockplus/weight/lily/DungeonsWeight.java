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

package com.skyblockplus.weight.lily;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;
import java.util.Map;

public class DungeonsWeight {

	private final Player player;
	private final WeightStruct weightStruct;

	public DungeonsWeight(Player player) {
		this.player = player;
		this.weightStruct = new WeightStruct();
	}

	public WeightStruct getWeightStruct() {
		return weightStruct;
	}

	public WeightStruct getDungeonWeight() {
		SkillsStruct cataSkill = player.getCatacombs();
		double level = cataSkill.getProgressLevel();
		long cataXP = cataSkill.totalExp();

		double n = cataXP < 569809640
			? 0.2 * Math.pow(level / 50, 1.538679118869934)
			: Math.pow(1 + (cataXP - CATACOMBS_LEVEL_50_XP) / 142452410 / 50, 2.967355422);

		if (level != 0) {
			if (cataXP < 569809640) {
				return weightStruct.add(
					new WeightStruct(0.5046647430979266 * ((Math.pow(1.18340401286164044, (level + 1)) - 1.05994990217254) * (1 + n)))
				);
			} else {
				return weightStruct.add(new WeightStruct(3250 * n));
			}
		} else {
			return new WeightStruct();
		}
	}

	public WeightStruct getDungeonCompletionWeight(String cataMode) {
		JsonObject dcw = DUNGEON_COMPLETION_WORTH;

		double max1000 = 0;
		double mMax1000 = 0;
		for (Map.Entry<String, JsonElement> dcwEntry : dcw.entrySet()) {
			if (dcwEntry.getKey().startsWith("catacombs_")) {
				max1000 += dcwEntry.getValue().getAsDouble();
			} else {
				mMax1000 += dcwEntry.getValue().getAsDouble();
			}
		}
		max1000 *= 1000;
		mMax1000 *= 1000;
		double upperBound = 1500;
		if (cataMode.equals("normal")) {
			if (higherDepth(player.profileJson(), "dungeons.dungeon_types.catacombs.tier_completions") == null) {
				return new WeightStruct();
			}

			double score = 0;
			for (Map.Entry<String, JsonElement> normalFloor : higherDepth(
				player.profileJson(),
				"dungeons.dungeon_types.catacombs.tier_completions"
			)
				.getAsJsonObject()
				.entrySet()) {
				int amount = normalFloor.getValue().getAsInt();
				double excess = 0;
				if (amount > 1000) {
					excess = amount - 1000;
					amount = 1000;
				}

				double floorScore = amount * dcw.get("catacombs_" + normalFloor.getKey()).getAsDouble();
				if (excess > 0) floorScore *= Math.log(excess / 1000 + 1) / Math.log(7.5) + 1;
				score += floorScore;
			}

			return weightStruct.add(new WeightStruct(score / max1000 * upperBound));
		} else {
			if (higherDepth(player.profileJson(), "dungeons.dungeon_types.master_catacombs.tier_completions") == null) {
				return new WeightStruct();
			}

			JsonObject dcb = DUNGEON_COMPLETION_BUFFS;
			for (Map.Entry<String, JsonElement> masterFloor : higherDepth(
				player.profileJson(),
				"dungeons.dungeon_types.master_catacombs.tier_completions"
			)
				.getAsJsonObject()
				.entrySet()) {
				if (higherDepth(dcb, masterFloor.getKey()) != null) {
					int amount = masterFloor.getValue().getAsInt();
					double threshold = 20;
					if (amount >= threshold) {
						upperBound += higherDepth(dcb, masterFloor.getKey()).getAsInt();
					} else {
						upperBound += higherDepth(dcb, masterFloor.getKey()).getAsInt() * Math.pow((amount / threshold), 1.840896416);
					}
				}
			}

			double masterScore = 0;
			for (Map.Entry<String, JsonElement> masterFloor : higherDepth(
				player.profileJson(),
				"dungeons.dungeon_types.master_catacombs.tier_completions"
			)
				.getAsJsonObject()
				.entrySet()) {
				int amount = masterFloor.getValue().getAsInt();
				double excess = 0;
				if (amount > 1000) {
					excess = amount - 1000;
					amount = 1000;
				}

				double floorScore = amount * dcw.get("master_catacombs_" + masterFloor.getKey()).getAsDouble();
				if (excess > 0) floorScore *= (Math.log((excess / 1000) + 1) / Math.log(5)) + 1;
				masterScore += floorScore;
			}

			return weightStruct.add(new WeightStruct((masterScore / mMax1000) * upperBound));
		}
	}
}
