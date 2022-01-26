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

import static com.skyblockplus.utils.ApiHandler.playerFromUuid;
import static com.skyblockplus.utils.ApiHandler.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.miscellaneous.weight.senither.Weight;
import com.skyblockplus.utils.structs.*;
import java.time.Instant;
import java.util.*;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;

public class Player {

	public String invMissing = "";
	private JsonArray profilesArray;
	private int profileIndex;
	private JsonElement hypixelPlayerJson;
	private boolean valid = false;
	private String uuid;
	private String username;
	private String profileName;
	private String failCause = "Unknown fail cause";

	/* Constructors */
	// Empty player, always invalid
	public Player(){}

	public Player(String username) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (response.isNotValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.valid = true;
	}

	public Player(String username, String profileName) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (response.isNotValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : failCause;
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.valid = true;
	}

	public Player(String uuid, String username, JsonElement outerProfileJson) {
		this.uuid = uuid;
		this.username = username;

		try {
			if (outerProfileJson == null) {
				return;
			}

			this.profilesArray = outerProfileJson.getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.valid = true;
	}

	public Player(String uuid, String username, String profileName, JsonElement outerProfileJson) {
		this.uuid = uuid;
		this.username = username;

		try {
			if (outerProfileJson == null) {
				return;
			}

			this.profilesArray = outerProfileJson.getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : "";
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.valid = true;
	}

	/* Constructor helper methods */
	public boolean usernameToUuid(String username) {
		UsernameUuidStruct response = ApiHandler.usernameToUuid(username);
		if (response.isNotValid()) {
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
			Instant lastProfileSave = Instant.EPOCH;
			for (int i = 0; i < profilesArray.size(); i++) {
				Instant lastSaveLoop;
				try {
					lastSaveLoop =
						Instant.ofEpochMilli(higherDepth(profilesArray.get(i), "members." + this.uuid + ".last_save").getAsLong());
				} catch (Exception e) {
					continue;
				}

				if (lastSaveLoop.isAfter(lastProfileSave)) {
					this.profileIndex = i;
					lastProfileSave = lastSaveLoop;
					this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
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

	public String getProfileName() {
		return profileName;
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

	/* Links */
	public String skyblockStatsLink() {
		return Utils.skyblockStatsLink(username, profileName);
	}

	public String getThumbnailUrl() {
		return "https://cravatar.eu/helmavatar/" + uuid + "/64.png";
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
			return skillInfoFromExp(higherDepth(profileJson(), "experience_skill_" + skillName).getAsLong(), skillName, weightType);
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
		JsonArray skillsTable =
			switch (skill) {
				case "catacombs" -> higherDepth(getLevelingJson(), "catacombs").getAsJsonArray();
				case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
				case "HOTM" -> higherDepth(getLevelingJson(), "HOTM").getAsJsonArray();
				default -> higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
			};

		int maxLevel = getSkillMaxLevel(skill, weightType);

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

	public SkillsStruct skillInfoFromLevel(int targetLevel, String skill) {
		return skillInfoFromLevel(targetLevel, skill, WeightType.NONE);
	}

	public SkillsStruct skillInfoFromLevel(int targetLevel, String skill, WeightType weightType) {
		JsonArray skillsTable =
			switch (skill) {
				case "catacombs" -> higherDepth(getLevelingJson(), "catacombs").getAsJsonArray();
				case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
				case "HOTM" -> higherDepth(getLevelingJson(), "HOTM").getAsJsonArray();
				default -> higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
			};

		int maxLevel = getSkillMaxLevel(skill, weightType);

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

	public SkillsStruct getHOTM() {
		long xp = higherDepth(profileJson(), "mining_core.experience", -1L);
		return xp == -1 ? null : skillInfoFromExp(xp, "HOTM");
	}

	/* Slayer */
	public int getTotalSlayer() {
		return getTotalSlayer("", 0);
	}

	public int getTotalSlayer(String type, int overrideAmount) {
		return (
			(type.equals("sven") ? overrideAmount : getSlayer("sven")) +
			(type.equals("rev") ? overrideAmount : getSlayer("rev")) +
			(type.equals("tara") ? overrideAmount : getSlayer("tara")) +
			(type.equals("enderman") ? overrideAmount : getSlayer("enderman"))
		);
	}

	public int getSlayerBossKills(String slayerName, int tier) {
		return higherDepth(profileJson(), "slayer_bosses." + slayerName + ".boss_kills_tier_" + tier, 0);
	}

	/**
	 * @param slayerName sven, rev, tara, enderman
	 */
	public int getSlayer(String slayerName) {
		JsonElement profileSlayer = higherDepth(profileJson(), "slayer_bosses");
		return switch (slayerName) {
			case "sven" -> higherDepth(profileSlayer, "wolf.xp", 0);
			case "rev" -> higherDepth(profileSlayer, "zombie.xp", 0);
			case "tara" -> higherDepth(profileSlayer, "spider.xp", 0);
			case "enderman" -> higherDepth(profileSlayer, "enderman.xp", 0);
			default -> 0;
		};
	}

	public int getSlayerLevel(String slayerName) {
		return getSlayerLevel(slayerName, getSlayer(slayerName));
	}

	public int getSlayerLevel(String slayerName, int xp) {
		JsonArray levelArray = higherDepth(
			getLevelingJson(),
			"slayer_xp." +
			switch (slayerName) {
				case "sven" -> "wolf";
				case "rev" -> "zombie";
				case "tara" -> "spider";
				default -> slayerName;
			}
		)
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
			if (item == null) {
				continue;
			}

			if (!item.getBackpackItems().isEmpty()) {
				List<InvItem> backpackItems = item.getBackpackItems();
				for (InvItem backpackItem : backpackItems) {
					if (backpackItem == null) {
						continue;
					}

					if (items.contains(backpackItem.getId())) {
						itemsPlayerHas.add(capitalizeString(backpackItem.getId().toLowerCase().replace("_", " ")));
					}
				}
			} else {
				if (items.contains(item.getId())) {
					itemsPlayerHas.add(capitalizeString(item.getId().toLowerCase().replace("_", " ")));
				}
			}
		}

		return itemsPlayerHas;
	}

	public String getFastestF7Time() {
		try {
			int f7TimeMilliseconds = higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.fastest_time_s_plus.7").getAsInt();
			int minutes = f7TimeMilliseconds / 1000 / 60;
			int seconds = f7TimeMilliseconds / 1000 % 60;
			return ("\n**Fastest F7 S+:** " + minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds));
		} catch (Exception e) {
			return "\n**No F7 S+ time found**";
		}
	}

	public int getDungeonSecrets() {
		if (hypixelPlayerJson == null) {
			hypixelPlayerJson = playerFromUuid(uuid).response();
		}

		return higherDepth(hypixelPlayerJson, "achievements.skyblock_treasure_hunter", 0);
	}

	public SkillsStruct getDungeonClass(String className) {
		return skillInfoFromExp(higherDepth(profileJson(), "dungeons.player_classes." + className + ".experience", 0L), "catacombs");
	}

	public SkillsStruct getCatacombs() {
		return skillInfoFromExp(higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.experience", 0L), "catacombs");
	}

	/* InvItem maps */
	public Map<Integer, InvItem> getInventoryMap() {
		try {
			String contents = higherDepth(profileJson(), "inv_contents.data").getAsString();
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

	public List<InvItem> getPetsMapNames() {
		JsonArray petsArr = getPets();

		List<InvItem> petsNameFormatted = new ArrayList<>();

		for (JsonElement pet : petsArr) {
			try {
				InvItem invItemStruct = new InvItem();
				invItemStruct.setName(
					"[Lvl " +
					petLevelFromXp(
						higherDepth(pet, "exp", 0L),
						higherDepth(pet, "tier").getAsString().toLowerCase(),
						higherDepth(pet, "type").getAsString()
					) +
					"] " +
					capitalizeString(higherDepth(pet, "type").getAsString().toUpperCase().replace("_", " "))
				);
				invItemStruct.setId("PET");
				if (higherDepth(pet, "skin", null) != null) {
					invItemStruct.addExtraValue("PET_SKIN_" + higherDepth(pet, "skin").getAsString());
				}
				invItemStruct.setRarity(higherDepth(pet, "tier").getAsString());
				if (higherDepth(pet, "heldItem", null) != null) {
					invItemStruct.addExtraValue(higherDepth(pet, "heldItem").getAsString());
				}
				petsNameFormatted.add(invItemStruct);
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
					invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
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

	public String[] getInventory() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "inv_contents.data").getAsString();
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
				if (gamemode == Gamemode.ALL || Gamemode.of(higherDepth(profile, "game_mode", "regular")) == gamemode) {
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

		if (!invMissing.contains(itemName)) {
			invMissing += "\n• " + itemName;
		}
		return "❓";
	}

	public double getWeight() {
		return new Weight(this, true).getTotalWeight().getRaw();
	}

	public double getWeight(String... weightTypes) {
		Weight weight = new Weight(this);
		for (String weightType : weightTypes) {
			if (SLAYER_NAMES.contains(weightType)) {
				weight.getSlayerWeight().getSlayerWeight(weightType);
			} else if (SKILL_NAMES.contains(weightType)) {
				weight.getSkillsWeight().getSkillsWeight(weightType);
			} else if (DUNGEON_CLASS_NAMES.contains(weightType)) {
				weight.getDungeonsWeight().getClassWeight(weightType);
			} else if (weightType.equals("catacombs")) {
				weight.getDungeonsWeight().getDungeonWeight("catacombs");
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
		return defaultEmbed(fixUsername(getUsername()) + getSymbol(" ") + extra, skyblockStatsLink()).setThumbnail(getThumbnailUrl());
	}

	public EmbedBuilder getFailEmbed() {
		return invalidEmbed(failCause);
	}

	public int getNumberMinionSlots() {
		try {
			List<String> profileMembers = getJsonKeys(higherDepth(getOuterProfileJson(), "members"));
			Set<String> uniqueCraftedMinions = new HashSet<>();

			for (String member : profileMembers) {
				try {
					JsonArray craftedMinions = higherDepth(getOuterProfileJson(), "members." + member + ".crafted_generators")
						.getAsJsonArray();
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
		JsonArray playerPets = getPets();
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
		return (
			(prefix.length >= 1 ? prefix[0] : "") +
			switch (getGamemode()) {
				case IRONMAN -> "\u267B️";
				case STRANDED -> "\uD83C\uDFDD";
				default -> "";
			}
		).stripTrailing();
	}

	public boolean isGamemode(Gamemode gamemode) {
		return gamemode == Gamemode.ALL || getGamemode() == gamemode;
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
			if (!isGamemode(gamemode)) {
				continue;
			}

			switch (type) {
				case "slayer":
					highestAmount = Math.max(highestAmount, getTotalSlayer());
				case "skills":
					highestAmount = Math.max(highestAmount, getSkillAverage());
					break;
				case "catacombs":
					highestAmount = Math.max(highestAmount, getCatacombs().getProgressLevel());
					break;
				case "weight":
					highestAmount = Math.max(highestAmount, getWeight());
					break;
				case "sven":
				case "rev":
				case "tara":
				case "enderman":
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
					highestAmount = Math.max(highestAmount, getSkill(type) != null ? getSkill(type).getProgressLevel() : -1);
					break;
				case "bank":
					highestAmount = Math.max(highestAmount, getBankBalance());
					break;
				case "purse":
					highestAmount = Math.max(highestAmount, getPurseCoins());
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
				case "ironman":
				case "stranded":
					highestAmount = Math.max(highestAmount, isGamemode(Gamemode.of(type)) ? 1 : -1);
					break;
				default:
					this.profileIndex = beforeProfileIndex;
					return -1;
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
		return getSlayer("enderman") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
	}

	public int getNumMaxedCollections() {
		int numMaxedColl = 0;
		try {
			for (Map.Entry<String, JsonElement> collection : higherDepth(profileJson(), "collection").getAsJsonObject().entrySet()) {
				long maxAmount = COLLECTION_ID_TO_MAX_AMOUNT.getOrDefault(collection.getKey(), -1L);
				if (maxAmount != -1 && collection.getValue().getAsLong() >= maxAmount) {
					numMaxedColl++;
				}
			}
		} catch (Exception ignored) {}

		return numMaxedColl;
	}

	public int getAccessoryCount() {
		return getItemsPlayerHas(new ArrayList<>(ALL_TALISMANS), getTalismanBagMap().values().toArray(new InvItem[0])).size();
	}

	public double getNetworth() {
		return NetworthExecute.getTotalNetworth(username, profileName);
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

	public enum WeightType {
		NONE,
		SENITHER,
		LILY,
	}

	public enum Gamemode {
		ALL,
		REGULAR,
		STRANDED,
		IRONMAN;

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
	}
}
