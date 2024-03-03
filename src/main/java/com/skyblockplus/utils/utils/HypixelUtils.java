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

package com.skyblockplus.utils.utils;

import static com.skyblockplus.utils.ApiHandler.playerFromUuid;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Constants.GUILD_EXP_TO_LEVEL;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.escapeText;
import static com.skyblockplus.utils.utils.Utils.crimsonArmorRegex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class HypixelUtils {

	public static DiscordInfoStruct getPlayerDiscordInfo(String username, boolean useCache) {
		try {
			UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
			if (!usernameUuidStruct.isValid()) {
				return new DiscordInfoStruct(usernameUuidStruct.failCause());
			}
			HypixelResponse response = playerFromUuid(usernameUuidStruct.uuid(), useCache);
			if (!response.isValid()) {
				return new DiscordInfoStruct(response.failCause());
			}

			if (response.get("socialMedia.links.DISCORD") == null) {
				return new DiscordInfoStruct(escapeText(usernameUuidStruct.username()) + " is not linked on Hypixel");
			}

			String discord = response.get("socialMedia.links.DISCORD").getAsString();
			String minecraftUsername = response.get("displayname").getAsString();
			String minecraftUuid = response.get("uuid").getAsString();

			return new DiscordInfoStruct(discord, minecraftUsername, minecraftUuid);
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
		if (vanillaItems.isEmpty()) {
			getPriceOverrideJson();
		}

		return vanillaItems.contains(id);
	}

	public static int slayerLevelFromXp(String slayerName, int xp) {
		JsonArray levelArray = higherDepth(getLevelingJson(), "slayer_xp." + slayerName).getAsJsonArray();
		int level = 0;
		for (int i = 0; i < levelArray.size(); i++) {
			if (xp >= levelArray.get(i).getAsInt()) {
				level = i + 1;
			} else {
				break;
			}
		}
		return level;
	}

	public static int bestiaryTierFromKills(int kills, int bracket, int cap) {
		kills = Math.min(kills, cap);
		int tier = 0;
		for (JsonElement requiredKills : higherDepth(getBestiaryJson(), "brackets." + bracket).getAsJsonArray()) {
			if (kills >= requiredKills.getAsInt()) {
				tier++;
			} else {
				break;
			}
		}
		return tier;
	}

	public static SkillsStruct levelingInfoFromExp(long exp, String name, int maxLevel) {
		JsonArray xpTable =
			(switch (name) {
					case "social", "HOTM" -> higherDepth(getLevelingJson(), name);
					case "catacombs", "healer", "mage", "berserk", "archer", "tank" -> higherDepth(getLevelingJson(), "catacombs");
					case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp");
					default -> higherDepth(getLevelingJson(), "leveling_xp");
				}).getAsJsonArray();
		long xpTotal = 0;

		int level = 1;
		for (int i = 0; i < xpTable.size(); i++) {
			if (i == maxLevel) {
				break;
			}

			if (xpTotal + xpTable.get(i).getAsLong() > exp) {
				break;
			}

			level = i + 1;
			xpTotal += xpTable.get(i).getAsLong();
		}
		long xpCurrent;

		if (exp <= 0) {
			level = 0;
			xpCurrent = 0;
		} else {
			xpCurrent = (long) Math.floor(exp - xpTotal);
		}
		long xpForNext = 0;

		if (level < maxLevel && level < xpTable.size()) {
			xpForNext = (long) Math.ceil(xpTable.get(level).getAsLong());
		}
		double progress = 0;

		if (xpForNext > 0) {
			progress = Math.max(0, Math.min((double) xpCurrent / xpForNext, 1));
		}
		return new SkillsStruct(name, level, maxLevel, exp, xpCurrent, xpForNext, progress);
	}

	public static SkillsStruct levelingInfoFromLevel(int targetLevel, String name, int maxLevel) {
		JsonArray skillsTable =
			switch (name) {
				case "catacombs", "social", "HOTM" -> higherDepth(getLevelingJson(), name).getAsJsonArray();
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

		return new SkillsStruct(name, targetLevel, maxLevel, xpTotal, 0, xpForNext, 0);
	}

	/** Used only for NetworthExecute Note: some npc_buy.cost can be decimals */
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

	public static double calculateWithTaxes(double price) {
		double tax = 0;

		// 1% for claiming bin over 1m (when buying)
		if (price >= 1000000) {
			tax += 0.01;
		}

		// Tax for starting new bin (when reselling)
		if (price <= 10000000) {
			tax += 0.01;
		} else if (price <= 100000000) {
			tax += 0.02;
		} else {
			tax += 0.025;
		}

		return price * (1 - tax);
	}

	public static boolean isCrimsonArmor(String itemId, boolean onlyPrestige) {
		Matcher matcher = crimsonArmorRegex.matcher(itemId);
		return matcher.matches() && (!onlyPrestige || !matcher.group(1).isEmpty());
	}
}
