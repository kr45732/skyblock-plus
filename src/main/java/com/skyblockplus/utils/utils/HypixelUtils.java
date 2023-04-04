/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
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

package com.skyblockplus.utils.utils;

import static com.skyblockplus.utils.ApiHandler.playerFromUuid;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Constants.GUILD_EXP_TO_LEVEL;
import static com.skyblockplus.utils.utils.JsonUtils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HypixelUtils {

	public static DiscordInfoStruct getPlayerDiscordInfo(String username) {
		try {
			UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
			if (!usernameUuidStruct.isValid()) {
				return new DiscordInfoStruct(usernameUuidStruct.failCause());
			}
			HypixelResponse response = playerFromUuid(usernameUuidStruct.uuid());
			if (!response.isValid()) {
				return new DiscordInfoStruct(response.failCause());
			}

			if (response.get("socialMedia.links.DISCORD") == null) {
				return new DiscordInfoStruct(usernameUuidStruct.username() + " is not linked on Hypixel");
			}

			String discordTag = response.get("socialMedia.links.DISCORD").getAsString();
			String minecraftUsername = response.get("displayname").getAsString();
			String minecraftUuid = response.get("uuid").getAsString();

			return new DiscordInfoStruct(discordTag, minecraftUsername, minecraftUuid);
		} catch (Exception e) {
			return new DiscordInfoStruct();
		}
	}

	public static int petLevelFromXp(long petExp, String rarity, String id) {
		int petRarityOffset = higherDepth(getPetJson(), "pet_rarity_offset." + rarity.toUpperCase()).getAsInt();
		JsonArray petLevelsXpPer = higherDepth(getPetJson(), "pet_levels").getAsJsonArray().deepCopy();
		JsonElement customLevelingJson = higherDepth(getPetJson(), "custom_pet_leveling." + id);
		if (customLevelingJson != null) {
			switch (higherDepth(customLevelingJson, "type", 0)) {
				case 1 -> petLevelsXpPer.addAll(higherDepth(customLevelingJson, "pet_levels").getAsJsonArray());
				case 2 -> petLevelsXpPer = higherDepth(customLevelingJson, "pet_levels").getAsJsonArray();
			}
		}
		int maxLevel = higherDepth(customLevelingJson, "max_level", 100);
		long totalExp = 0;
		for (int i = petRarityOffset; i < petLevelsXpPer.size(); i++) {
			totalExp += petLevelsXpPer.get(i).getAsLong();
			if (totalExp >= petExp) {
				return (Math.min(i - petRarityOffset + 1, maxLevel));
			}
		}
		return maxLevel;
	}

	public static boolean isVanillaItem(String id) {
		if (vanillaItems == null) {
			getPriceOverrideJson();
		}

		return vanillaItems.contains(id);
	}

	public static SkillsStruct levelingInfoFromExp(long skillExp, String skill, int maxLevel) {
		JsonArray skillsTable =
			switch (skill) {
				case "catacombs", "social", "HOTM", "bestiary.ISLAND", "bestiary.MOB", "bestiary.BOSS" -> higherDepth(
					getLevelingJson(),
					skill
				)
					.getAsJsonArray();
				case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
				default -> higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
			};

		long xpTotal = 0L;
		int level = 1;
		for (int i = 0; i < skillsTable.size(); i++) {
			if (i == maxLevel) {
				break;
			}

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
		if (level < maxLevel && level < skillsTable.size()) {
			xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());
		}

		if (skillExp == 0) {
			level = 0;
			xpForNext = 0;
		}

		double progress = xpForNext > 0 ? Math.max(0, Math.min(((double) xpCurrent) / xpForNext, 1)) : 0;

		return new SkillsStruct(skill, level, maxLevel, skillExp, xpCurrent, xpForNext, progress);
	}

	public static SkillsStruct levelingInfoFromLevel(int targetLevel, String skill, int maxLevel) {
		JsonArray skillsTable =
			switch (skill) {
				case "catacombs", "social", "HOTM", "bestiary.ISLAND", "bestiary.MOB", "bestiary.BOSS" -> higherDepth(
					getLevelingJson(),
					skill
				)
					.getAsJsonArray();
				case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
				default -> higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
			};

		long xpTotal = 0L;
		int level = 1;
		for (int i = 0; i < maxLevel; i++) {
			xpTotal += skillsTable.get(i).getAsLong();

			if (level >= targetLevel) {
				xpTotal -= skillsTable.get(i).getAsLong();
				break;
			} else {
				level = (i + 1);
			}
		}

		long xpForNext = 0;
		if (level < maxLevel) {
			xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());
		}

		return new SkillsStruct(skill, targetLevel, maxLevel, xpTotal, 0, xpForNext, 0);
	}

	/**
	 * Used only for NetworthExecute
	 */
	public static List<String> getRecipe(String itemId) {
		JsonElement recipe = higherDepth(getInternalJsonMappings(), itemId + ".recipe");
		if (recipe != null) {
			return recipe
				.getAsJsonObject()
				.entrySet()
				.stream()
				.filter(e -> !e.getKey().equals("count"))
				.map(e -> e.getValue().getAsString())
				.filter(e -> !e.isEmpty())
				.collect(Collectors.toCollection(ArrayList::new));
		}

		JsonElement npcBuyCost = higherDepth(getInternalJsonMappings(), itemId + ".npc_buy.cost");
		if (npcBuyCost != null) {
			return streamJsonArray(npcBuyCost).map(JsonElement::getAsString).collect(Collectors.toCollection(ArrayList::new));
		}

		return null;
	}

	public static int getRecipeCount(String itemId) {
		return higherDepth(
			getInternalJsonMappings(),
			itemId + ".recipe.count",
			higherDepth(getInternalJsonMappings(), itemId + ".npc_buy.count", 1)
		);
	}

	public static double getNpcSellPrice(String itemId) {
		return higherDepth(getSkyblockItemsJson().get(itemId), "npc_sell_price", -1.0);
	}

	public static int guildExpToLevel(int guildExp) {
		int guildLevel = 0;

		for (int i = 0;; i++) {
			int expNeeded = i >= GUILD_EXP_TO_LEVEL.size()
				? GUILD_EXP_TO_LEVEL.get(GUILD_EXP_TO_LEVEL.size() - 1)
				: GUILD_EXP_TO_LEVEL.get(i);
			guildExp -= expNeeded;
			if (guildExp < 0) {
				return guildLevel;
			} else {
				guildLevel++;
			}
		}
	}
}