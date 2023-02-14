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

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.miscellaneous.LevelSlashCommand;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.skills.CrimsonSlashCommand;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.structs.*;
import java.util.*;
import java.util.stream.Collectors;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.apache.groovy.util.Maps;

public class Player {

	private JsonArray profilesArray;
	private int profileIndex;
	private JsonElement hypixelPlayerJson;
	private boolean valid = false;
	private String uuid;
	private String username;
	private String profileName;
	private String failCause = "Unknown fail cause";
	public final Map<Integer, Double> profileToNetworth = new HashMap<>();

	/* Constructors */
	// Empty player, always invalid
	public Player() {
		failCause = "No Args Constructor";
	}

	public Player(String username) {
		this(username, true);
	}

	/**
	 * @param updateLb insert into database if true
	 */
	public Player(String username, boolean updateLb) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (!response.isValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(this);
		}
	}

	/**
	 * Ignores profile cache and updates provided gamemode synchronously
	 */
	public Player(String username, Gamemode gamemode) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY, false, true);
			if (!response.isValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		leaderboardDatabase.insertIntoLeaderboardSync(this, gamemode);
	}

	public Player(String username, String profileName) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (!response.isValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : failCause;
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		leaderboardDatabase.insertIntoLeaderboard(this);
	}

	public Player(String uuid, String username, JsonElement profileArray) {
		this(uuid, username, profileArray, true);
	}

	/**
	 * @param updateLb insert into database if true
	 */
	public Player(String uuid, String username, JsonElement profileArray, boolean updateLb) {
		this.uuid = uuid;
		this.username = username;
		if (uuid == null || username == null) {
			return;
		}

		try {
			if (profileArray == null) {
				return;
			}

			this.profilesArray = profileArray.getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(this);
		}
	}

	public Player(String uuid, String username, String profileName, JsonElement profileArray) {
		this(uuid, username, profileName, profileArray, true);
	}

	/**
	 * @param updateLb insert into database if true
	 */
	public Player(String uuid, String username, String profileName, JsonElement profileArray, boolean updateLb) {
		this.uuid = uuid;
		this.username = username;
		if (uuid == null || username == null) {
			return;
		}

		try {
			if (profileArray == null) {
				return;
			}

			this.profilesArray = profileArray.getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : "";
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(this);
		}
	}

	public Player copy() {
		return new Player(getUuid(), getUsername(), getProfileName(), getProfileArray(), false);
	}

	/* Constructor helper methods */
	public boolean usernameToUuid(String username) {
		UsernameUuidStruct response = ApiHandler.usernameToUuid(username);
		if (!response.isValid()) {
			failCause = response.failCause();
			return true;
		}

		this.username = response.username();
		this.uuid = response.uuid();
		return false;
	}

	public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
		try {
			for (int i = 0; i < profilesArray.size(); i++) {
				String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
				if (currentProfileName.equalsIgnoreCase(profileName)) {
					this.profileName = currentProfileName;
					this.profileIndex = i;
					return false;
				}
			}
		} catch (Exception ignored) {}
		return true;
	}

	public boolean getLatestProfile(JsonArray profilesArray) {
		try {
			for (int i = 0; i < profilesArray.size(); i++) {
				if (higherDepth(profilesArray.get(i), "selected", false)) {
					this.profileIndex = i;
					this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
					break;
				}
			}

			return false;
		} catch (Exception ignored) {}
		return true;
	}

	/* Getters */
	public JsonElement profileJson() {
		return higherDepth(profilesArray.get(profileIndex), "members." + this.uuid);
	}

	public String getUsername() {
		return username;
	}

	public String getUsernameFixed() {
		return fixUsername(username);
	}

	public String getProfileName() {
		return profileName;
	}

	public int getProfileIndex() {
		return profileIndex;
	}

	public String getUuid() {
		return uuid;
	}

	public JsonElement getOuterProfileJson() {
		return profilesArray.get(profileIndex);
	}

	public JsonArray getProfileArray() {
		return profilesArray;
	}

	public boolean isValid() {
		return valid;
	}

	public String getFailCause() {
		return failCause;
	}

	public JsonElement getHypixelPlayerJson() {
		if (hypixelPlayerJson == null) {
			hypixelPlayerJson = playerFromUuid(uuid).response();
		}

		return hypixelPlayerJson;
	}

	/* Links */
	public String skyblockStatsLink() {
		return Utils.skyblockStatsLink(uuid, profileName);
	}

	/* Bank and purse */
	/**
	 * @return Bank balance or -1 if bank API disabled
	 */
	public double getBankBalance() {
		return higherDepth(getOuterProfileJson(), "banking.balance", -1.0);
	}

	public double getPurseCoins() {
		return higherDepth(profileJson(), "coin_purse", 0.0);
	}

	public JsonArray getBankHistory() {
		try {
			return higherDepth(getOuterProfileJson(), "banking.transactions").getAsJsonArray();
		} catch (Exception e) {
			return null;
		}
	}

	/* Skills */
	public int getTotalSkillsXp() {
		int totalSkillXp = 0;
		for (String skill : SKILL_NAMES) {
			SkillsStruct skillInfo = getSkill(skill);
			if (skillInfo != null) {
				totalSkillXp += skillInfo.totalExp();
			} else {
				return -1;
			}
		}
		return totalSkillXp;
	}

	public int getFarmingCapUpgrade() {
		return higherDepth(profileJson(), "jacob2.perks.farming_level_cap", 0);
	}

	public int getSkillMaxLevel(String skillName, WeightType weightType) {
		if (weightType == WeightType.LILY) {
			return 60;
		}

		int maxLevel = higherDepth(getLevelingJson(), "leveling_caps." + skillName, 0);

		if (skillName.equals("farming")) {
			maxLevel = weightType == WeightType.SENITHER ? 60 : maxLevel + getFarmingCapUpgrade();
		}

		return maxLevel;
	}

	public double getSkillXp(String skillName) {
		try {
			return skillName.equals("catacombs") ? getCatacombs().totalExp() : getSkill(skillName).totalExp();
		} catch (Exception e) {
			return -1;
		}
	}

	public SkillsStruct getSkill(String skillName) {
		return getSkill(skillName, WeightType.NONE);
	}

	public SkillsStruct getSkill(String skillName, WeightType weightType) {
		try {
			return skillInfoFromExp(
				higherDepth(profileJson(), "experience_skill_" + (skillName.equals("social") ? "social2" : skillName)).getAsLong(),
				skillName,
				weightType
			);
		} catch (Exception e) {
			return null;
		}
	}

	public double getSkillAverage() {
		return getSkillAverage("", 0);
	}

	public double getSkillAverage(String skillName, int overrideAmount) {
		double skillAverage = 0;
		for (String skill : SKILL_NAMES) {
			try {
				if (skill.equals(skillName)) {
					skillAverage += overrideAmount;
				} else {
					SkillsStruct skillsStruct = getSkill(skill);
					skillAverage += skillsStruct.getProgressLevel();
				}
			} catch (Exception e) {
				return -1;
			}
		}
		return skillAverage / SKILL_NAMES.size();
	}

	public SkillsStruct skillInfoFromExp(long skillExp, String skill) {
		return skillInfoFromExp(skillExp, skill, WeightType.NONE);
	}

	public SkillsStruct skillInfoFromExp(long skillExp, String skill, WeightType weightType) {
		return levelingInfoFromExp(skillExp, skill, getSkillMaxLevel(skill, weightType));
	}

	public SkillsStruct skillInfoFromLevel(int targetLevel, String skill) {
		return skillInfoFromLevel(targetLevel, skill, WeightType.NONE);
	}

	public SkillsStruct skillInfoFromLevel(int targetLevel, String skill, WeightType weightType) {
		return levelingInfoFromLevel(targetLevel, skill, getSkillMaxLevel(skill, weightType));
	}

	public SkillsStruct getHOTM() {
		long xp = higherDepth(profileJson(), "mining_core.experience", -1L);
		return xp == -1 ? null : skillInfoFromExp(xp, "HOTM");
	}

	/* Slayer */
	public int getTotalSlayer() {
		return getTotalSlayer("", 0);
	}

	public int getTotalSlayer(String type, int overrideAmount) {
		int totalSlayer = 0;
		for (String slayerName : SLAYER_NAMES) {
			if (slayerName.equals(type)) {
				totalSlayer += overrideAmount;
			} else {
				totalSlayer += getSlayer(slayerName);
			}
		}
		return totalSlayer;
	}

	public int getSlayerBossKills(String slayerName, int tier) {
		return higherDepth(
			profileJson(),
			"slayer_bosses." + SLAYER_NAMES_MAP.getOrDefault(slayerName, slayerName) + ".boss_kills_tier_" + tier,
			0
		);
	}

	/**
	 * @param slayerName sven, rev, tara, enderman
	 */
	public int getSlayer(String slayerName) {
		return higherDepth(profileJson(), "slayer_bosses." + SLAYER_NAMES_MAP.getOrDefault(slayerName, slayerName) + ".xp", 0);
	}

	public int getSlayerLevel(String slayerName) {
		return getSlayerLevel(slayerName, getSlayer(slayerName));
	}

	public int getSlayerLevel(String slayerName, int xp) {
		JsonArray levelArray = higherDepth(getLevelingJson(), "slayer_xp." + SLAYER_NAMES_MAP.getOrDefault(slayerName, slayerName))
			.getAsJsonArray();
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

	/* Dungeons */
	public String getSelectedDungeonClass() {
		try {
			return higherDepth(profileJson(), "dungeons.selected_dungeon_class").getAsString();
		} catch (Exception e) {
			return "none";
		}
	}

	public int getHighestPlayedDungeonFloor() {
		int master = higherDepth(profileJson(), "dungeons.dungeon_types.master_catacombs.highest_tier_completed", -1);
		if (master != -1) {
			return master + 7;
		}

		return higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.highest_tier_completed", -1);
	}

	public Set<String> getItemsPlayerHas(List<String> items, InvItem... extras) {
		Map<Integer, InvItem> invItemMap = getInventoryMap();
		if (invItemMap == null) {
			return null;
		}

		Collection<InvItem> itemsMap = new ArrayList<>(invItemMap.values());
		itemsMap.addAll(new ArrayList<>(getEnderChestMap().values()));
		itemsMap.addAll(new ArrayList<>(getStorageMap().values()));
		Collections.addAll(itemsMap, extras);
		Set<String> itemsPlayerHas = new HashSet<>();

		for (InvItem item : itemsMap) {
			if (item != null) {
				if (!item.getBackpackItems().isEmpty()) {
					for (InvItem backpackItem : item.getBackpackItems()) {
						if (backpackItem != null && items.contains(backpackItem.getId())) {
							itemsPlayerHas.add(backpackItem.getId());
						}
					}
				} else {
					if (items.contains(item.getId())) {
						itemsPlayerHas.add(item.getId());
					}
				}
			}
		}

		return itemsPlayerHas;
	}

	public int getDungeonSecrets() {
		return higherDepth(getHypixelPlayerJson(), "achievements.skyblock_treasure_hunter", 0);
	}

	public SkillsStruct getDungeonClass(String className) {
		return skillInfoFromExp(higherDepth(profileJson(), "dungeons.player_classes." + className + ".experience", 0L), "catacombs");
	}

	public SkillsStruct getCatacombs() {
		return skillInfoFromExp(higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.experience", 0L), "catacombs");
	}

	/* InvItem maps */
	public Map<Integer, InvItem> getInventoryMap() {
		return getInventoryMap(false);
	}

	public Map<Integer, InvItem> getInventoryMap(boolean sort) {
		try {
			String contents = higherDepth(profileJson(), "inv_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			Map<Integer, InvItem> invMap = getGenericInventoryMap(parsedContents);
			if (sort) {
				Map<Integer, InvItem> sortedMap = new TreeMap<>();
				for (Map.Entry<Integer, InvItem> entry : invMap.entrySet()) {
					if (entry.getKey() >= 9 && entry.getKey() <= 17) {
						sortedMap.put(entry.getKey() + 18, entry.getValue());
					} else if (entry.getKey() >= 27) {
						sortedMap.put(entry.getKey() - 18, entry.getValue());
					} else {
						sortedMap.put(entry.getKey(), entry.getValue());
					}
				}
				return sortedMap;
			} else {
				return invMap;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getPersonalVaultMap() {
		try {
			String contents = higherDepth(profileJson(), "personal_vault_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getStorageMap() {
		try {
			JsonElement backpackContents = higherDepth(profileJson(), "backpack_contents");
			List<String> backpackCount = getJsonKeys(backpackContents);
			Map<Integer, InvItem> storageMap = new HashMap<>();
			int counter = 1;
			for (String bp : backpackCount) {
				Collection<InvItem> curBpMap = getGenericInventoryMap(
					NBTReader.readBase64(higherDepth(backpackContents, bp + ".data").getAsString())
				)
					.values();
				for (InvItem itemSlot : curBpMap) {
					storageMap.put(counter, itemSlot);
					counter++;
				}
			}

			return storageMap;
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getTalismanBagMap() {
		try {
			String contents = higherDepth(profileJson(), "talisman_bag.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getEquipmentMap() {
		try {
			String contents = higherDepth(profileJson(), "equippment_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getArmorMap() {
		try {
			String contents = higherDepth(profileJson(), "inv_armor.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			Map<Integer, InvItem> oldMap = getGenericInventoryMap(parsedContents);
			Map<Integer, InvItem> fixedMap = new HashMap<>();
			fixedMap.put(0, oldMap.getOrDefault(3, null));
			fixedMap.put(1, oldMap.getOrDefault(2, null));
			fixedMap.put(2, oldMap.getOrDefault(1, null));
			fixedMap.put(3, oldMap.getOrDefault(0, null));
			return fixedMap;
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getWardrobeMap() {
		try {
			String contents = higherDepth(profileJson(), "wardrobe_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getPetsMap() {
		JsonArray petsArr;
		try {
			petsArr = getPets();
		} catch (Exception e) {
			return new HashMap<>();
		}

		Map<Integer, InvItem> petsNameFormatted = new HashMap<>();

		for (int i = 0; i < petsArr.size(); i++) {
			try {
				JsonElement pet = petsArr.get(i);
				String tier = higherDepth(pet, "tier").getAsString();
				String type = higherDepth(pet, "type").getAsString();
				InvItem invItemStruct = new InvItem();
				invItemStruct.setName(
					"[Lvl " +
					petLevelFromXp(higherDepth(pet, "exp", 0L), tier.toLowerCase(), type) +
					"] " +
					capitalizeString(type.toUpperCase().replace("_", " "))
				);
				invItemStruct.setId("PET");
				if (higherDepth(pet, "skin", null) != null) {
					invItemStruct.setSkin("PET_SKIN_" + higherDepth(pet, "skin").getAsString());
				}
				invItemStruct.setRarity(tier);
				if (higherDepth(pet, "heldItem", null) != null) {
					invItemStruct.addExtraValue(higherDepth(pet, "heldItem").getAsString());
				}
				petsNameFormatted.put(i, invItemStruct);
			} catch (Exception ignored) {}
		}

		return petsNameFormatted;
	}

	public Map<Integer, InvItem> getEnderChestMap() {
		try {
			String contents = higherDepth(profileJson(), "ender_chest_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<String, Integer> getPlayerSacks() {
		try {
			JsonObject sacksJson = higherDepth(profileJson(), "sacks_counts").getAsJsonObject();
			Map<String, Integer> sacksMap = new HashMap<>();
			for (Map.Entry<String, JsonElement> sacksEntry : sacksJson.entrySet()) {
				sacksMap.put(sacksEntry.getKey(), sacksEntry.getValue().getAsInt());
			}

			return sacksMap;
		} catch (Exception e) {
			return null;
		}
	}

	/* Emoji viewer arrays & other inventory */
	public List<String[]> getTalismanBag() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "talisman_bag.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");

			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
				if (displayName != null) {
					invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
				} else {
					invFramesMap.put(i + 1, "empty");
				}
			}

			if (invFramesMap.size() % 45 != 0) {
				int toAdd = 45 - (invFramesMap.size() % 45);
				int initialSize = invFramesMap.size();
				for (int i = 0; i < toAdd; i++) {
					invFramesMap.put(initialSize + 1 + i, "blank");
				}
			}

			StringBuilder outputStringPart1 = new StringBuilder();
			StringBuilder outputStringPart2 = new StringBuilder();
			List<String[]> enderChestPages = new ArrayList<>();
			StringBuilder curNine = new StringBuilder();
			int page = 0;
			for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
				if ((i.getKey() - page) <= 27) {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart1.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				} else {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart2.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				}

				if (i.getKey() != 0 && i.getKey() % 45 == 0) {
					enderChestPages.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
					outputStringPart1 = new StringBuilder();
					outputStringPart2 = new StringBuilder();
					page += 45;
				}
			}
			return enderChestPages;
		} catch (Exception ignored) {}
		return null;
	}

	public List<String[]> getEnderChest() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "ender_chest_contents.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");

			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
				if (displayName != null) {
					String id = displayName.getString("id", "empty").toLowerCase();
					if (id.equals("pet")) {
						JsonElement petInfo = JsonParser.parseString(
							invFrames.getCompound(i).getCompound("tag.ExtraAttributes").getString("petInfo", "{}")
						);
						String newId =
							higherDepth(petInfo, "type", "") + RARITY_TO_NUMBER_MAP.getOrDefault(higherDepth(petInfo, "tier", ""), "");
						if (!newId.isEmpty()) {
							id = newId.toLowerCase();
						}
					} else if (id.equals("enchanted_book")) {
						NBTCompound enchantedBooks = invFrames.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
						if (enchantedBooks.size() == 1) {
							Map.Entry<String, Object> enchant = enchantedBooks.entrySet().stream().findFirst().get();
							id = enchant.getKey() + ";" + enchant.getValue();
						}
					}
					invFramesMap.put(i + 1, id);
				} else {
					invFramesMap.put(i + 1, "empty");
				}
			}

			StringBuilder outputStringPart1 = new StringBuilder();
			StringBuilder outputStringPart2 = new StringBuilder();
			List<String[]> enderChestPages = new ArrayList<>();
			StringBuilder curNine = new StringBuilder();
			int page = 0;
			for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
				if ((i.getKey() - page) <= 27) {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart1.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				} else {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart2.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				}

				if (i.getKey() != 0 && i.getKey() % 45 == 0) {
					enderChestPages.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
					outputStringPart1 = new StringBuilder();
					outputStringPart2 = new StringBuilder();
					page += 45;
				}
			}
			return enderChestPages;
		} catch (Exception ignored) {}
		return null;
	}

	public List<String[]> getStorage() {
		try {
			List<String[]> out = new ArrayList<>();
			for (JsonElement page : higherDepth(profileJson(), "backpack_contents")
				.getAsJsonObject()
				.entrySet()
				.stream()
				.sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())))
				.map(Map.Entry::getValue)
				.collect(Collectors.toCollection(ArrayList::new))) {
				NBTCompound decodedInventoryContents = NBTReader.readBase64(higherDepth(page, "data").getAsString());

				NBTList invFrames = decodedInventoryContents.getList("i");
				Map<Integer, String> invFramesMap = new TreeMap<>();
				for (int i = 0; i < invFrames.size(); i++) {
					NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
					if (displayName != null) {
						String id = displayName.getString("id", "empty").toLowerCase();
						if (id.equals("pet")) {
							JsonElement petInfo = JsonParser.parseString(
								invFrames.getCompound(i).getCompound("tag.ExtraAttributes").getString("petInfo", "{}")
							);
							String newId =
								higherDepth(petInfo, "type", "") + RARITY_TO_NUMBER_MAP.getOrDefault(higherDepth(petInfo, "tier", ""), "");
							if (!newId.isEmpty()) {
								id = newId.toLowerCase();
							}
						} else if (id.equals("enchanted_book")) {
							NBTCompound enchantedBooks = invFrames.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
							if (enchantedBooks.size() == 1) {
								Map.Entry<String, Object> enchant = enchantedBooks.entrySet().stream().findFirst().get();
								id = enchant.getKey() + ";" + enchant.getValue();
							}
						}
						invFramesMap.put(i + 1, id);
					} else {
						invFramesMap.put(i + 1, "empty");
					}
				}
				if (invFrames.size() < 27) {
					int curSize = invFrames.size();
					for (int i = 0; i < 27 - curSize; i++) {
						invFramesMap.put(i + 1 + curSize, "blank");
					}
				}

				StringBuilder outputStringPart1 = new StringBuilder();
				StringBuilder outputStringPart2 = new StringBuilder();
				StringBuilder curNine = new StringBuilder();
				for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
					if (i.getKey() <= 18) {
						curNine.append(itemToEmoji(i.getValue()));
						if (i.getKey() % 9 == 0) {
							outputStringPart1.append(curNine).append("\n");
							curNine = new StringBuilder();
						}
					} else {
						curNine.append(itemToEmoji(i.getValue()));
						if (i.getKey() % 9 == 0) {
							outputStringPart2.append(curNine).append("\n");
							curNine = new StringBuilder();
						}
					}
				}

				out.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
			}
			return out;
		} catch (Exception ignored) {}
		return null;
	}

	public String[] getInventory() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "inv_contents.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");
			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
				if (displayName != null) {
					String id = displayName.getString("id", "empty").toLowerCase();
					if (id.equals("pet")) {
						JsonElement petInfo = JsonParser.parseString(
							invFrames.getCompound(i).getCompound("tag.ExtraAttributes").getString("petInfo", "{}")
						);
						String newId =
							higherDepth(petInfo, "type", "") + RARITY_TO_NUMBER_MAP.getOrDefault(higherDepth(petInfo, "tier", ""), "");
						if (!newId.isEmpty()) {
							id = newId.toLowerCase();
						}
					} else if (id.equals("enchanted_book")) {
						NBTCompound enchantedBooks = invFrames.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
						if (enchantedBooks.size() == 1) {
							Map.Entry<String, Object> enchant = enchantedBooks.entrySet().stream().findFirst().get();
							id = enchant.getKey() + ";" + enchant.getValue();
						}
					}
					invFramesMap.put(i + 1, id);
				} else {
					invFramesMap.put(i + 1, "empty");
				}
			}

			StringBuilder outputStringPart1 = new StringBuilder();
			StringBuilder outputStringPart2 = new StringBuilder();
			StringBuilder curNine = new StringBuilder();
			for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
				if (i.getKey() <= 9 || i.getKey() >= 28) {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart1.insert(0, curNine + "\n");
						curNine = new StringBuilder();
					}
				} else {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart2.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				}
			}
			return new String[] { outputStringPart2.toString(), outputStringPart1.toString() };
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, ArmorStruct> getWardrobeList() {
		try {
			String encodedWardrobeContents = higherDepth(profileJson(), "wardrobe_contents.data").getAsString();
			int equippedSlot = higherDepth(profileJson(), "wardrobe_equipped_slot").getAsInt();
			NBTCompound decodedWardrobeContents = NBTReader.readBase64(encodedWardrobeContents);

			NBTList wardrobeFrames = decodedWardrobeContents.getList("i");
			Map<Integer, String> wardrobeFramesMap = new HashMap<>();
			for (int i = 0; i < wardrobeFrames.size(); i++) {
				NBTCompound displayName = wardrobeFrames.getCompound(i).getCompound("tag.display");
				if (displayName != null) {
					wardrobeFramesMap.put(i, parseMcCodes(displayName.getString("Name", "Empty")));
				} else {
					wardrobeFramesMap.put(i, "Empty");
				}
			}

			Map<Integer, ArmorStruct> armorStructMap = new HashMap<>(18);
			for (int i = 0; i < 9; i++) {
				ArmorStruct pageOneStruct = new ArmorStruct();
				for (int j = i; j < wardrobeFramesMap.size() / 2; j += 9) {
					String currentArmorPiece = wardrobeFramesMap.get(j);
					if ((j - i) / 9 == 0) {
						pageOneStruct.setHelmet(currentArmorPiece);
					} else if ((j - i) / 9 == 1) {
						pageOneStruct.setChestplate(currentArmorPiece);
					} else if ((j - i) / 9 == 2) {
						pageOneStruct.setLeggings(currentArmorPiece);
					} else if ((j - i) / 9 == 3) {
						pageOneStruct.setBoots(currentArmorPiece);
					}
				}
				armorStructMap.put(i, pageOneStruct);

				ArmorStruct pageTwoStruct = new ArmorStruct();
				for (int j = (wardrobeFramesMap.size() / 2) + i; j < wardrobeFramesMap.size(); j += 9) {
					String currentArmorPiece = wardrobeFramesMap.get(j);
					if ((j - i) / 9 == 4) {
						pageTwoStruct.setHelmet(currentArmorPiece);
					} else if ((j - i) / 9 == 5) {
						pageTwoStruct.setChestplate(currentArmorPiece);
					} else if ((j - i) / 9 == 6) {
						pageTwoStruct.setLeggings(currentArmorPiece);
					} else if ((j - i) / 9 == 7) {
						pageTwoStruct.setBoots(currentArmorPiece);
					}
				}
				armorStructMap.put(i + 9, pageTwoStruct);
			}
			if (equippedSlot > 0) {
				armorStructMap.replace((equippedSlot - 1), getArmor().makeBold());
			}

			return armorStructMap;
		} catch (Exception e) {
			return null;
		}
	}

	public List<String[]> getWardrobe() {
		try {
			int equippedWardrobeSlot = higherDepth(profileJson(), "wardrobe_equipped_slot").getAsInt();
			Map<Integer, InvItem> equippedArmor = equippedWardrobeSlot != -1 ? getArmorMap() : null;

			String encodedInventoryContents = higherDepth(profileJson(), "wardrobe_contents.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");
			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");

				if (displayName != null) {
					invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
				} else if (
					(equippedArmor != null) &&
					(equippedWardrobeSlot <= 9) &&
					((((i + 1) - equippedWardrobeSlot) % 9) == 0) &&
					((i + 1) <= 36) &&
					(equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9))) != null
				) {
					invFramesMap.put(i + 1, equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9)).getId().toLowerCase());
				} else if (
					(equippedArmor != null) &&
					(equippedWardrobeSlot > 9) &&
					((((i + 1) - equippedWardrobeSlot) % 9) == 0) &&
					((i + 1) > 36) &&
					(equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) - 3)) != null
				) {
					invFramesMap.put(i + 1, equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) - 3).getId().toLowerCase());
				} else {
					invFramesMap.put(i + 1, "empty");
				}
			}

			if (invFramesMap.size() % 36 != 0) {
				int toAdd = 36 - (invFramesMap.size() % 36);
				int initialSize = invFramesMap.size();
				for (int i = 0; i < toAdd; i++) {
					invFramesMap.put(initialSize + 1 + i, "blank");
				}
			}

			StringBuilder outputStringPart1 = new StringBuilder();
			StringBuilder outputStringPart2 = new StringBuilder();
			List<String[]> enderChestPages = new ArrayList<>();
			StringBuilder curNine = new StringBuilder();
			int page = 0;
			for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
				if ((i.getKey() - page) <= 18) {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart1.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				} else {
					curNine.append(itemToEmoji(i.getValue()));
					if (i.getKey() % 9 == 0) {
						outputStringPart2.append(curNine).append("\n");
						curNine = new StringBuilder();
					}
				}

				if (i.getKey() != 0 && i.getKey() % 36 == 0) {
					enderChestPages.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
					outputStringPart1 = new StringBuilder();
					outputStringPart2 = new StringBuilder();
					page += 36;
				}
			}
			return enderChestPages;
		} catch (Exception ignored) {}
		return null;
	}

	public ArmorStruct getArmor() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "inv_armor.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList talismanFrames = decodedInventoryContents.getList("i");

			Map<Integer, String> armorFramesMap = new HashMap<>();
			for (int i = 0; i < talismanFrames.size(); i++) {
				NBTCompound displayName = talismanFrames.getCompound(i).getCompound("tag.display");
				if (displayName != null) {
					armorFramesMap.put(i, parseMcCodes(displayName.getString("Name", "Empty")));
				} else {
					armorFramesMap.put(i, "Empty");
				}
			}
			return new ArmorStruct(armorFramesMap.get(3), armorFramesMap.get(2), armorFramesMap.get(1), armorFramesMap.get(0));
		} catch (Exception e) {
			return null;
		}
	}

	public JsonArray getPets() {
		return higherDepth(profileJson(), "pets").getAsJsonArray();
	}

	/* Miscellaneous */
	public String[] getAllProfileNames(Gamemode gamemode) {
		List<String> profileNameList = new ArrayList<>();
		if (this.profilesArray == null) {
			this.profilesArray = skyblockProfilesFromUuid(uuid).response().getAsJsonArray();
		}

		for (JsonElement profile : profilesArray) {
			try {
				if (gamemode.isGamemode(higherDepth(profile, "game_mode", "regular"))) {
					profileNameList.add(higherDepth(profile, "cute_name").getAsString().toLowerCase());
				}
			} catch (Exception ignored) {}
		}

		return profileNameList.toArray(new String[0]);
	}

	public int getFairySouls() {
		return higherDepth(profileJson(), "fairy_souls_collected", 0);
	}

	public String itemToEmoji(String itemName) {
		itemName = itemName.toUpperCase();

		try {
			return getEmojiMap().get(itemName).getAsString();
		} catch (Exception ignored) {}

		return "❓";
	}

	public double getExactLevel() {
		return higherDepth(profileJson(), "leveling.experience", 0) / 100.0;
	}

	public double getEstimatedLevel() {
		return (
			(
				LevelSlashCommand.getCoreTasksEmbed(this).total() +
				LevelSlashCommand.getEventTasksEmbed(this).total() +
				LevelSlashCommand.getDungeonTasks(this).total() +
				LevelSlashCommand.getEssenceShopTasks(this).total() +
				LevelSlashCommand.getSlayingTasks(this).total() +
				LevelSlashCommand.getSkillRelatedTasks(this).total() +
				LevelSlashCommand.getMiscellaneousTasks(this).total() +
				LevelSlashCommand.getStoryTasks(this).total()
			) /
			100.0
		);
	}

	public double getLevel() {
		return higherDepth(profileJson(), "leveling.experience") != null ? getExactLevel() : getEstimatedLevel();
	}

	public String getLevelColor(int level) {
		String color = "None";
		for (Map.Entry<Integer, String> colorReq : Maps
			.of(
				1,
				"Default",
				40,
				"Common",
				80,
				"Uncommon",
				120,
				"Rare",
				160,
				"Epic",
				200,
				"Legendary",
				240,
				"Mythic",
				280,
				"Device",
				320,
				"Special",
				360,
				"Very Special"
			)
			.entrySet()) {
			if (level >= colorReq.getKey()) {
				color = colorReq.getValue();
			} else {
				break;
			}
		}
		return color;
	}

	public int getMagicPower() {
		Map<Integer, InvItem> accessoryBagMap = getTalismanBagMap();
		if (accessoryBagMap == null) {
			return 0;
		}

		List<InvItem> accessoryBag = accessoryBagMap
			.values()
			.stream()
			.filter(o -> o != null && o.getRarity() != null)
			.sorted(Comparator.comparingInt(o -> Integer.parseInt(RARITY_TO_NUMBER_MAP.get(o.getRarity()).replace(";", ""))))
			.collect(Collectors.toCollection(ArrayList::new));
		// Don't reverse the rarity because we are iterating reverse order
		Set<String> ignoredTalismans = new HashSet<>();
		for (int i = accessoryBag.size() - 1; i >= 0; i--) {
			String accessoryId = accessoryBag.get(i).getId();

			if (ignoredTalismans.contains(accessoryId)) {
				accessoryBag.remove(i);
			}

			ignoredTalismans.add(accessoryId);
			JsonElement children = higherDepth(getParentsJson(), accessoryId);
			if (children != null) {
				for (JsonElement child : children.getAsJsonArray()) {
					ignoredTalismans.add(child.getAsString());
				}
			}
		}

		int magicPower = 0;
		for (Map.Entry<String, Integer> entry : rarityToMagicPower.entrySet()) {
			long count = accessoryBag.stream().filter(i -> Objects.equals(i.getRarity(), entry.getKey())).count();
			long power = count * entry.getValue();
			magicPower += power;
		}

		int hegemony = rarityToMagicPower.getOrDefault(
			accessoryBag.stream().filter(a -> a.getId().equals("HEGEMONY_ARTIFACT")).map(InvItem::getRarity).findFirst().orElse(""),
			0
		);
		if (hegemony != 0) {
			magicPower += hegemony;
		}

		return magicPower;
	}

	public double getLilyWeight() {
		return new LilyWeight(this, true).getTotalWeight().getRaw();
	}

	public double getWeight() {
		return new SenitherWeight(this, true).getTotalWeight().getRaw();
	}

	public double getWeight(String... weightTypes) {
		SenitherWeight weight = new SenitherWeight(this);
		for (String weightType : weightTypes) {
			if (SLAYER_NAMES.contains(weightType)) {
				weight.getSlayerWeight().getSlayerWeight(weightType);
			} else if (SKILL_NAMES.contains(weightType)) {
				weight.getSkillsWeight().getSkillsWeight(weightType);
			} else if (DUNGEON_CLASS_NAMES.contains(weightType)) {
				weight.getDungeonsWeight().getClassWeight(weightType);
			} else if (weightType.equals("catacombs")) {
				weight.getDungeonsWeight().getDungeonWeight();
			} else {
				throw new IllegalArgumentException("Invalid weight type: " + weightType);
			}
		}

		return weight.getTotalWeight().getRaw();
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultPlayerEmbed("");
	}

	public EmbedBuilder defaultPlayerEmbed(String extra) {
		return defaultEmbed(fixUsername(getUsername()) + getSymbol(" ") + extra, skyblockStatsLink()).setThumbnail(getAvatarUrl());
	}

	public CustomPaginator.Builder defaultPlayerPaginator(User... users) {
		return defaultPlayerPaginator(PaginatorExtras.PaginatorType.DEFAULT, users);
	}

	public CustomPaginator.Builder defaultPlayerPaginator(PaginatorExtras.PaginatorType type, User... users) {
		return defaultPaginator(users)
			.setColumns(1)
			.setItemsPerPage(1)
			.setPaginatorExtras(
				new PaginatorExtras(type)
					.setEveryPageTitle(fixUsername(getUsername()) + getSymbol(" "))
					.setEveryPageThumbnail(getAvatarUrl())
					.setEveryPageTitleUrl(skyblockStatsLink())
			);
	}

	public EmbedBuilder getFailEmbed() {
		return invalidEmbed(failCause);
	}

	public int getNumberMinionSlots() {
		try {
			Set<String> uniqueCraftedMinions = new HashSet<>();

			for (Map.Entry<String, JsonElement> member : higherDepth(getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
				try {
					JsonArray craftedMinions = higherDepth(member.getValue(), "crafted_generators").getAsJsonArray();
					for (JsonElement minion : craftedMinions) {
						uniqueCraftedMinions.add(minion.getAsString());
					}
				} catch (Exception ignored) {}
			}

			int prevMax = 0;
			for (int i = 0; i < CRAFTED_MINIONS_TO_SLOTS.size(); i++) {
				if (uniqueCraftedMinions.size() >= CRAFTED_MINIONS_TO_SLOTS.get(i)) {
					prevMax = i;
				} else {
					break;
				}
			}

			return (prevMax + 5);
		} catch (Exception e) {
			return 0;
		}
	}

	public int getPetScore() {
		JsonArray playerPets;
		try {
			playerPets = getPets();
		} catch (Exception e) {
			return 0;
		}
		Map<String, Integer> petsMap = new HashMap<>();
		for (JsonElement pet : playerPets) {
			String petName = higherDepth(pet, "type").getAsString();
			int rarity =
				switch (higherDepth(pet, "tier").getAsString().toLowerCase()) {
					case "common" -> 1;
					case "uncommon" -> 2;
					case "rare" -> 3;
					case "epic" -> 4;
					case "legendary" -> 5;
					default -> 0;
				};
			if (petsMap.containsKey(petName)) {
				if (petsMap.get(petName) < rarity) {
					petsMap.replace(petName, rarity);
				}
			} else {
				petsMap.put(petName, rarity);
			}
		}

		int petScore = 0;
		for (int i : petsMap.values()) {
			petScore += i;
		}

		return petScore;
	}

	public String getSymbol(String... prefix) {
		return getGamemode().getSymbol(prefix);
	}

	public boolean isGamemode(Gamemode gamemode) {
		return gamemode.isGamemode(getGamemode());
	}

	public Gamemode getGamemode() {
		return Gamemode.of(higherDepth(getOuterProfileJson(), "game_mode", "regular"));
	}

	public double getHighestAmount(String type) {
		return getHighestAmount(type, Gamemode.ALL);
	}

	public double getHighestAmount(String type, Gamemode gamemode) {
		double highestAmount = -1.0;
		int beforeProfileIndex = this.profileIndex;
		this.profileIndex = 0;
		for (JsonElement ignored : profilesArray) {
			if (isGamemode(gamemode)) {
				switch (type) {
					case "slayer":
						highestAmount = Math.max(highestAmount, getTotalSlayer());
					case "skills":
						highestAmount = Math.max(highestAmount, getSkillAverage());
						break;
					case "catacombs":
						highestAmount = Math.max(highestAmount, getCatacombs().getProgressLevel());
						break;
					case "catacombs_xp":
						highestAmount = Math.max(highestAmount, getCatacombs().totalExp());
						break;
					case "healer", "mage", "berserk", "archer", "tank":
						highestAmount = Math.max(highestAmount, getDungeonClass(type).getProgressLevel());
						break;
					case "weight":
						highestAmount = Math.max(highestAmount, getWeight());
						break;
					case "sven":
					case "rev":
					case "tara":
					case "enderman":
					case "blaze":
						highestAmount = Math.max(highestAmount, getSlayer(type));
						break;
					case "alchemy":
					case "combat":
					case "fishing":
					case "farming":
					case "foraging":
					case "carpentry":
					case "mining":
					case "taming":
					case "enchanting":
					case "social":
						highestAmount = Math.max(highestAmount, getSkill(type) != null ? getSkill(type).getProgressLevel() : -1);
						break;
					case "alchemy_xp":
					case "combat_xp":
					case "fishing_xp":
					case "farming_xp":
					case "foraging_xp":
					case "carpentry_xp":
					case "mining_xp":
					case "taming_xp":
					case "enchanting_xp":
					case "social_xp":
						highestAmount =
							Math.max(
								highestAmount,
								getSkill(type.split("_xp")[0]) != null ? getSkill(type.split("_xp")[0]).totalExp() : -1
							);
						break;
					case "hotm":
						highestAmount = Math.max(highestAmount, getHOTM() != null ? getHOTM().totalExp() : -1);
						break;
					case "bank":
						highestAmount = Math.max(highestAmount, getBankBalance());
						break;
					case "purse":
						highestAmount = Math.max(highestAmount, getPurseCoins());
						break;
					case "coins":
						highestAmount = Math.max(highestAmount, Math.max(0, getBankBalance()) + getPurseCoins());
						break;
					case "pet_score":
						highestAmount = Math.max(highestAmount, getPetScore());
						break;
					case "networth":
						highestAmount = Math.max(highestAmount, getNetworth());
						break;
					case "fairy_souls":
						highestAmount = Math.max(highestAmount, getFairySouls());
						break;
					case "slot_collector":
						highestAmount = Math.max(highestAmount, getNumberMinionSlots());
						break;
					case "dungeon_secrets":
						highestAmount = Math.max(highestAmount, getDungeonSecrets());
						break;
					case "accessory_count":
						highestAmount = Math.max(highestAmount, getAccessoryCount());
						break;
					case "total_slayer":
						highestAmount = Math.max(highestAmount, getTotalSlayer());
						break;
					case "slayer_nine":
						highestAmount = Math.max(highestAmount, getNumLvlNineSlayers());
						break;
					case "maxed_collections":
						highestAmount = Math.max(highestAmount, getNumMaxedCollections());
						break;
					case "mage_rep":
						highestAmount = Math.max(highestAmount, getMageRep());
						break;
					case "barbarian_rep":
						highestAmount = Math.max(highestAmount, getBarbarianRep());
						break;
					case "ironman":
					case "stranded":
						highestAmount = Math.max(highestAmount, isGamemode(Gamemode.of(type)) ? 1 : -1);
						break;
					case "lily_weight":
						highestAmount = Math.max(highestAmount, getLilyWeight());
						break;
					case "lily_slayer_weight":
						highestAmount = Math.max(highestAmount, new LilyWeight(this, true).getSlayerWeight().getWeightStruct().getRaw());
						break;
					case "level":
						highestAmount = Math.max(highestAmount, getLevel());
						break;
					default:
						if (collectionNameToId.containsKey(type)) {
							highestAmount = Math.max(highestAmount, getCollection(collectionNameToId.get(type)));
							break;
						} else if (skyblockStats.contains(type)) {
							highestAmount = Math.max(highestAmount, getStat(type));
							break;
						} else {
							this.profileIndex = beforeProfileIndex;
							return -1;
						}
				}
			}

			this.profileIndex++;
		}

		this.profileIndex = beforeProfileIndex;
		return highestAmount;
	}

	public int getNumLvlNineSlayers() {
		int lvlNineSlayers = getSlayer("sven") >= 1000000 ? 1 : 0;
		lvlNineSlayers = getSlayer("rev") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		lvlNineSlayers = getSlayer("tara") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		lvlNineSlayers = getSlayer("enderman") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		lvlNineSlayers = getSlayer("blaze") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		return lvlNineSlayers;
	}

	public long getCollection(String id) {
		return higherDepth(profileJson(), "collection." + id, -1);
	}

	public long getCombinedCollection(String id) {
		long amount = 0;
		for (Map.Entry<String, JsonElement> member : higherDepth(getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			amount += higherDepth(member.getValue(), "collection." + id, 0L);
		}
		return amount;
	}

	public double getStat(String stat) {
		return higherDepth(profileJson(), "stats." + skyblockStatToCase.getOrDefault(stat, stat), -1.0);
	}

	public int getNumMaxedCollections() {
		Map<String, Long> collections = new HashMap<>();
		for (Map.Entry<String, JsonElement> member : higherDepth(getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			try {
				for (Map.Entry<String, JsonElement> collection : higherDepth(member.getValue(), "collection")
					.getAsJsonObject()
					.entrySet()) {
					collections.compute(collection.getKey(), (k, v) -> (v == null ? 0 : v) + collection.getValue().getAsLong());
				}
			} catch (Exception ignored) {}
		}
		int numMaxedColl = 0;
		for (Map.Entry<String, Long> collection : collections.entrySet()) {
			long maxAmount = higherDepth(getCollectionsJson(), collection.getKey() + ".tiers.[-1]", -1L);
			if (maxAmount != -1 && collection.getValue() >= maxAmount) {
				numMaxedColl++;
			}
		}

		return numMaxedColl;
	}

	public int getAccessoryCount() {
		return getItemsPlayerHas(new ArrayList<>(ALL_TALISMANS), getTalismanBagMap().values().toArray(new InvItem[0])).size();
	}

	public double getNetworth() {
		return profileToNetworth.computeIfAbsent(profileIndex, k -> NetworthExecute.getNetworth(this));
	}

	public int getMageRep() {
		return higherDepth(profileJson(), "nether_island_player_data.mages_reputation", 0);
	}

	public int getBarbarianRep() {
		return higherDepth(profileJson(), "nether_island_player_data.barbarians_reputation", 0);
	}

	@Override
	public String toString() {
		return (
			"Player{" +
			"validPlayer=" +
			valid +
			", playerUuid='" +
			uuid +
			'\'' +
			", playerUsername='" +
			username +
			'\'' +
			", profileName='" +
			profileName +
			'\'' +
			'}'
		);
	}

	public int getBestiaryTier() {
		int total = 0;
		for (Map.Entry<String, List<String>> location : bestiaryLocationToFamilies.entrySet()) {
			for (String mob : location.getValue()) {
				int kills = higherDepth(profileJson(), "bestiary.kills_" + mob, 0);
				String type = "MOB";
				if (location.getKey().equals("Private Island")) {
					type = "ISLAND";
				} else if (bestiaryBosses.contains(mob)) {
					type = "BOSS";
				}
				total +=
					levelingInfoFromExp(kills, "bestiary." + type, higherDepth(getLevelingJson(), "bestiary.caps." + type).getAsInt())
						.currentLevel();
			}
		}
		return total;
	}

	public double getBestiaryLevel() {
		return getBestiaryTier() / 10.0;
	}

	public String getAuctionUrl() {
		return Utils.getAuctionUrl(uuid);
	}

	public String getAvatarUrl() {
		return Utils.getAvatarlUrl(uuid);
	}

	public enum WeightType {
		NONE,
		SENITHER,
		LILY;

		public static WeightType of(String name) {
			return valueOf(name.toUpperCase());
		}
	}

	public enum Gamemode {
		ALL,
		REGULAR,
		STRANDED,
		IRONMAN,
		IRONMAN_STRANDED;

		public static Gamemode of(String gamemode) {
			return valueOf(
				switch (gamemode = gamemode.toUpperCase()) {
					case "ISLAND" -> "STRANDED";
					case "BINGO" -> "REGULAR";
					case "" -> "ALL";
					default -> gamemode;
				}
			);
		}

		public String toCacheType() {
			return (
				switch (this) {
					case IRONMAN, STRANDED -> name().toLowerCase();
					default -> "all";
				} +
				"_lb"
			);
		}

		public boolean isGamemode(Object gamemode) {
			if (gamemode instanceof Gamemode mode) {
				return (this == IRONMAN_STRANDED) ? ((gamemode == IRONMAN) || (gamemode == STRANDED)) : ((this == ALL) || (this == mode));
			}

			return isGamemode(of((String) gamemode));
		}

		public String getSymbol(String... prefix) {
			return (
				(prefix.length >= 1 ? prefix[0] : "") +
				switch (this) {
					case IRONMAN -> "\u267B️";
					case STRANDED -> "\uD83C\uDFDD";
					default -> "";
				}
			).stripTrailing();
		}
	}

	public boolean isInventoryApiEnabled() {
		return higherDepth(profileJson(), "inv_contents.data", null) != null;
	}

	public boolean isBankApiEnabled() {
		return getBankBalance() != -1;
	}

	public boolean isCollectionsApiEnabled() {
		try {
			return higherDepth(profileJson(), "collection").getAsJsonObject() != null;
		} catch (Exception ignored) {}
		return false;
	}

	public boolean isVaultApiEnabled() {
		return higherDepth(profileJson(), "personal_vault_contents.data", null) != null;
	}

	public boolean isSkillsApiEnabled() {
		return getSkill("combat") != null;
	}

	public int getDojoPoints() {
		int totalPoints = 0;
		for (Map.Entry<String, String> dojoQuest : CrimsonSlashCommand.dojoQuests.entrySet()) {
			int points = higherDepth(profileJson(), "nether_island_player_data.dojo.dojo_points_" + dojoQuest.getKey(), 0);
			totalPoints += points;
		}
		return totalPoints;
	}

	public String getDojoBelt() {
		int totalPoints = getDojoPoints();
		String belt;

		if (totalPoints >= 7000) {
			belt = "Black";
		} else if (totalPoints >= 6000) {
			belt = "Brown";
		} else if (totalPoints >= 4000) {
			belt = "Blue";
		} else if (totalPoints >= 2000) {
			belt = "Green";
		} else if (totalPoints >= 1000) {
			belt = "Yellow";
		} else {
			belt = "White";
		}

		return belt;
	}
}
