package com.skyblockplus.utils;

import static com.skyblockplus.utils.Constants.craftedMinionsToSlots;
import static com.skyblockplus.utils.Constants.skillNames;
import static com.skyblockplus.utils.Hypixel.playerFromUuid;
import static com.skyblockplus.utils.Hypixel.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.structs.*;
import com.skyblockplus.weight.Weight;
import java.time.Instant;
import java.util.*;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Player {

	private static final Logger log = LoggerFactory.getLogger(Player.class);

	public String invMissing = "";
	private JsonArray profilesArray;
	private int profileIndex;
	private JsonElement hypixelProfileJson;
	private boolean validPlayer = false;
	private String playerUuid;
	private String playerUsername;
	private String profileName;
	private String failCause = "Unknown fail cause";

	/* Constructors */
	public Player(String username) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(playerUuid);
			if (response.isNotValid()) {
				failCause = response.failCause;
				return;
			}

			this.profilesArray = response.response.getAsJsonArray();

			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.validPlayer = true;
	}

	public Player(String username, String profileName) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(playerUuid);
			if (response.isNotValid()) {
				failCause = response.failCause;
				return;
			}

			this.profilesArray = response.response.getAsJsonArray();

			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : failCause;
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.validPlayer = true;
	}

	public Player(String playerUuid, String playerUsername, JsonElement outerProfileJson) {
		this.playerUuid = playerUuid;
		this.playerUsername = playerUsername;

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

		this.validPlayer = true;
	}

	public Player(String playerUuid, String playerUsername, String profileName, JsonElement outerProfileJson) {
		this.playerUuid = playerUuid;
		this.playerUsername = playerUsername;

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

		this.validPlayer = true;
	}

	/* Constructor helper methods */
	public boolean usernameToUuid(String username) {
		UsernameUuidStruct response = Hypixel.usernameToUuid(username);
		if (response.isNotValid()) {
			failCause = response.failCause;
			return true;
		}

		this.playerUsername = response.playerUsername;
		this.playerUuid = response.playerUuid;
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
						Instant.ofEpochMilli(higherDepth(profilesArray.get(i), "members." + this.playerUuid + ".last_save").getAsLong());
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
		return higherDepth(profilesArray.get(profileIndex), "members." + this.playerUuid);
	}

	public String getUsername() {
		return this.playerUsername;
	}

	public String getProfileName() {
		return this.profileName;
	}

	public String getUuid() {
		return this.playerUuid;
	}

	public JsonElement getOuterProfileJson() {
		return profilesArray.get(profileIndex);
	}

	public boolean isValid() {
		return validPlayer;
	}

	public String getFailCause() {
		return failCause;
	}

	/* Links */
	public String skyblockStatsLink() {
		return Utils.skyblockStatsLink(playerUsername, profileName);
	}

	public String getThumbnailUrl() {
		return "https://cravatar.eu/helmavatar/" + playerUuid + "/64.png";
	}

	/* Bank and purse */
	public double getBankBalance() {
		try {
			return higherDepth(getOuterProfileJson(), "banking.balance").getAsDouble();
		} catch (Exception e) {
			return -1;
		}
	}

	public double getPurseCoins() {
		return higherDepth(profileJson(), "coin_purse", -1L);
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
		return getTotalSkillsXp(profileJson());
	}

	public int getTotalSkillsXp(JsonElement profile) {
		int totalSkillXp = 0;
		for (String skill : skillNames) {
			SkillsStruct skillInfo = getSkill(profile, skill);
			if (skillInfo == null) {
				return -1;
			} else {
				totalSkillXp += skillInfo.totalSkillExp;
			}
		}
		return totalSkillXp;
	}

	public int getFarmingCapUpgrade() {
		return higherDepth(profileJson(), "jacob2.perks.farming_level_cap", 0);
	}

	public int getSkillMaxLevel(String skillName, boolean isWeight) {
		int maxLevel = higherDepth(getLevelingJson(), "leveling_caps." + skillName, 0);

		if (skillName.equals("farming")) {
			maxLevel = isWeight ? 60 : maxLevel + getFarmingCapUpgrade();
		}

		return maxLevel;
	}

	public double getSkillXp(String skillName) {
		return getSkillXp(profileJson(), skillName);
	}

	public double getSkillXp(JsonElement profile, String skillName) {
		try {
			if (skillName.equals("catacombs")) {
				return higherDepth(profile, "dungeons.dungeon_types.catacombs.experience").getAsDouble();
			}
			return higherDepth(profile, "experience_skill_" + skillName).getAsDouble();
		} catch (Exception ignored) {}
		return -1;
	}

	public SkillsStruct getSkill(String skillName) {
		return getSkill(profileJson(), skillName);
	}

	public SkillsStruct getSkill(JsonElement profile, String skillName) {
		return getSkill(profile, skillName, false);
	}

	public SkillsStruct getSkill(JsonElement profile, String skillName, boolean isWeight) {
		try {
			double skillExp = higherDepth(profile, "experience_skill_" + skillName).getAsDouble();
			return skillInfoFromExp(skillExp, skillName, isWeight);
		} catch (Exception ignored) {}
		return null;
	}

	public double getSkillAverage() {
		return getSkillAverage(profileJson());
	}

	public double getSkillAverage(JsonElement profile) {
		double progressSA = 0;
		for (String skill : skillNames) {
			try {
				double skillExp = higherDepth(profile, "experience_skill_" + skill).getAsDouble();
				SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
				progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
			} catch (Exception e) {
				return -1;
			}
		}
		progressSA /= skillNames.size();
		return progressSA;
	}

	public SkillsStruct skillInfoFromExp(double skillExp, String skill) {
		return skillInfoFromExp(skillExp, skill, false);
	}

	public SkillsStruct skillInfoFromExp(double skillExp, String skill, boolean isWeight) {
		JsonArray skillsTable;
		if (skill.equals("catacombs")) {
			skillsTable = higherDepth(getLevelingJson(), "catacombs").getAsJsonArray();
		} else if (skill.equals("runecrafting")) {
			skillsTable = higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
		} else {
			skillsTable = higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
		}

		int maxLevel = getSkillMaxLevel(skill, isWeight);

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
		if (level < maxLevel) xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());

		double progress = xpForNext > 0 ? Math.max(0, Math.min(((double) xpCurrent) / xpForNext, 1)) : 0;

		return new SkillsStruct(skill, level, maxLevel, (long) skillExp, xpCurrent, xpForNext, progress);
	}

	/* Slayer */
	public int getTotalSlayer() {
		return getTotalSlayer(profileJson());
	}

	public int getTotalSlayer(JsonElement profile) {
		return getSlayer(profile, "sven") + getSlayer(profile, "rev") + getSlayer(profile, "tara") + getSlayer(profile, "enderman");
	}

	public int getSlayerBossKills(String slayerName, int tier) {
		return higherDepth(profileJson(), "slayer_bosses." + slayerName + ".boss_kills_tier_" + tier, 0);
	}

	public int getSlayer(String slayerName) {
		return getSlayer(profileJson(), slayerName);
	}

	public int getSlayer(JsonElement profile, String slayerName) {
		JsonElement profileSlayer = higherDepth(profile, "slayer_bosses");
		switch (slayerName) {
			case "sven":
				return higherDepth(profileSlayer, "wolf.xp", 0);
			case "rev":
				return higherDepth(profileSlayer, "zombie.xp", 0);
			case "tara":
				return higherDepth(profileSlayer, "spider.xp", 0);
			case "enderman":
				return higherDepth(profileSlayer, "enderman.xp", 0);
		}
		return -1;
	}

	public int getSlayerLevel(String slayerName) {
		switch (slayerName) {
			case "sven":
				JsonArray wolfLevelArray = higherDepth(getLevelingJson(), "slayer_xp.wolf").getAsJsonArray();
				int wolfXp = getSlayer("sven");
				int prevWolfLevel = 0;
				for (int i = 0; i < wolfLevelArray.size(); i++) {
					if (wolfXp >= wolfLevelArray.get(i).getAsInt()) {
						prevWolfLevel = i + 1;
					} else {
						break;
					}
				}
				return prevWolfLevel;
			case "rev":
				JsonArray zombieLevelArray = higherDepth(getLevelingJson(), "slayer_xp.zombie").getAsJsonArray();
				int zombieXp = getSlayer("rev");
				int prevZombieMax = 0;
				for (int i = 0; i < zombieLevelArray.size(); i++) {
					if (zombieXp >= zombieLevelArray.get(i).getAsInt()) {
						prevZombieMax = i + 1;
					} else {
						break;
					}
				}
				return prevZombieMax;
			case "tara":
				JsonArray spiderLevelArray = higherDepth(getLevelingJson(), "slayer_xp.spider").getAsJsonArray();
				int spiderXp = getSlayer("tara");
				int prevSpiderMax = 0;
				for (int i = 0; i < spiderLevelArray.size(); i++) {
					if (spiderXp >= spiderLevelArray.get(i).getAsInt()) {
						prevSpiderMax = i + 1;
					} else {
						break;
					}
				}
				return prevSpiderMax;
			case "enderman":
				JsonArray endermanLevelArray = higherDepth(getLevelingJson(), "slayer_xp.enderman").getAsJsonArray();
				int endermanXp = getSlayer("enderman");
				int prevEndermanMax = 0;
				for (int i = 0; i < endermanLevelArray.size(); i++) {
					if (endermanXp >= endermanLevelArray.get(i).getAsInt()) {
						prevEndermanMax = i + 1;
					} else {
						break;
					}
				}
				return prevEndermanMax;
		}
		return 0;
	}

	/* Dungeons */
	public String getSelectedDungeonClass() {
		try {
			return higherDepth(profileJson(), "dungeons.selected_dungeon_class").getAsString();
		} catch (Exception e) {
			return "none";
		}
	}

	public Set<String> getItemsPlayerHas(List<String> items) {
		Map<Integer, InvItem> invItemMap = getInventoryMap();
		if (invItemMap == null) {
			return null;
		}

		Collection<InvItem> itemsMap = new ArrayList<>(invItemMap.values());
		itemsMap.addAll(new ArrayList<>(getEnderChestMap().values()));
		itemsMap.addAll(new ArrayList<>(getStorageMap().values()));
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
		if (hypixelProfileJson == null) {
			this.hypixelProfileJson = playerFromUuid(playerUuid).response;
		}

		return higherDepth(hypixelProfileJson, "achievements.skyblock_treasure_hunter", 0);
	}

	public double getDungeonClassLevel(String className) {
		return getDungeonClassLevel(profileJson(), className);
	}

	public double getDungeonClassLevel(JsonElement profile, String className) {
		SkillsStruct dungeonClassLevel = skillInfoFromExp(getDungeonClassXp(profile, className), "catacombs");
		return dungeonClassLevel.skillLevel + dungeonClassLevel.progressToNext;
	}

	public SkillsStruct getDungeonClass(String className) {
		return skillInfoFromExp(getDungeonClassXp(className), "catacombs");
	}

	public double getCatacombsLevel() {
		return getCatacombsLevel(profileJson());
	}

	public double getCatacombsLevel(JsonElement profile) {
		SkillsStruct catacombsInfo = getCatacombsSkill(profile);
		return catacombsInfo.skillLevel + catacombsInfo.progressToNext;
	}

	public SkillsStruct getCatacombsSkill() {
		return getCatacombsSkill(profileJson());
	}

	public SkillsStruct getCatacombsSkill(JsonElement profile) {
		double skillExp = higherDepth(profile, "dungeons.dungeon_types.catacombs.experience") != null
			? higherDepth(profile, "dungeons.dungeon_types.catacombs.experience").getAsDouble()
			: 0;
		return skillInfoFromExp(skillExp, "catacombs");
	}

	public double getDungeonClassXp(String className) {
		return getDungeonClassXp(profileJson(), className);
	}

	public double getDungeonClassXp(JsonElement profile, String className) {
		try {
			return higherDepth(profile, "dungeons.player_classes." + className + ".experience").getAsDouble();
		} catch (Exception e) {
			return 0;
		}
	}

	/* -- Start inventory -- */
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

	public Map<Integer, InvItem> getInventoryArmorMap() {
		try {
			String contents = higherDepth(profileJson(), "inv_armor.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
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
					petLevelFromXp(higherDepth(pet, "exp", 0L), higherDepth(pet, "tier").getAsString().toLowerCase()) +
					"] " +
					capitalizeString(higherDepth(pet, "type").getAsString().toUpperCase().replace("_", " "))
				);
				invItemStruct.setId("PET");
				if (higherDepth(pet, "skin") != null && !higherDepth(pet, "skin").isJsonNull()) {
					invItemStruct.addExtraValue("PET_SKIN_" + higherDepth(pet, "skin").getAsString());
				}
				invItemStruct.setRarity(higherDepth(pet, "tier").getAsString());
				if (higherDepth(pet, "heldItem") != null && !higherDepth(pet, "heldItem").isJsonNull()) {
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

	/* Emoji viewer arrays / other inventory */
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
				armorStructMap.replace((equippedSlot - 1), getInventoryArmor().makeBold());
			}

			return armorStructMap;
		} catch (Exception e) {
			return null;
		}
	}

	public List<String[]> getWardrobe() {
		try {
			int equippedWardrobeSlot = higherDepth(profileJson(), "wardrobe_equipped_slot").getAsInt();
			Map<Integer, InvItem> equippedArmor = equippedWardrobeSlot != -1 ? getInventoryArmorMap() : null;

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
					(equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) + 1)) != null
				) {
					invFramesMap.put(i + 1, equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) + 1).getId().toLowerCase());
				} else if (
					(equippedArmor != null) &&
					(equippedWardrobeSlot > 9) &&
					((((i + 1) - equippedWardrobeSlot) % 9) == 0) &&
					((i + 1) > 36) &&
					(equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) + 1 - 3)) != null
				) {
					invFramesMap.put(i + 1, equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) + 1 - 3).getId().toLowerCase());
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

	public ArmorStruct getInventoryArmor() {
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

	public Map<String, Integer> getPlayerSacks() {
		JsonObject sacksJson = higherDepth(profileJson(), "sacks_counts").getAsJsonObject();
		Map<String, Integer> sacksMap = new HashMap<>();
		for (Map.Entry<String, JsonElement> sacksEntry : sacksJson.entrySet()) {
			sacksMap.put(sacksEntry.getKey(), sacksEntry.getValue().getAsInt());
		}

		return sacksMap;
	}

	/* -- End inventory -- */

	/* Miscellaneous */
	public String[] getAllProfileNames(boolean isIronman) {
		List<String> profileNameList = new ArrayList<>();
		if (this.profilesArray == null) {
			this.profilesArray = skyblockProfilesFromUuid(playerUuid).response.getAsJsonArray();
		}

		for (JsonElement profile : profilesArray) {
			try {
				if (isIronman && higherDepth(profile, "game_mode") == null) {
					continue;
				}

				profileNameList.add(higherDepth(profile, "cute_name").getAsString().toLowerCase());
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

	public double getWeight(JsonElement profile) {
		return new Weight(profile, this).getTotalWeight(true).getRaw();
	}

	public double getWeight() {
		return getWeight(profileJson());
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultEmbed(
			fixUsername(getUsername()) + (higherDepth(getOuterProfileJson(), "game_mode") != null ? " ♻️" : ""),
			Utils.skyblockStatsLink(getUsername(), getProfileName())
		)
			.setThumbnail(getThumbnailUrl());
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
			for (int i = 0; i < craftedMinionsToSlots.size(); i++) {
				if (uniqueCraftedMinions.size() >= craftedMinionsToSlots.get(i)) {
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
			int rarity = 0;
			switch (higherDepth(pet, "tier").getAsString().toLowerCase()) {
				case "common":
					rarity = 1;
					break;
				case "uncommon":
					rarity = 2;
					break;
				case "rare":
					rarity = 3;
					break;
				case "epic":
					rarity = 4;
					break;
				case "legendary":
					rarity = 5;
					break;
			}
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

	public double getHighestAmount(String type) {
		double highestAmount = -1.0;
		for (JsonElement profile : profilesArray) {
			profile = higherDepth(profile, "members." + this.playerUuid);
			switch (type) {
				case "slayer":
					highestAmount = Math.max(highestAmount, getTotalSlayer(profile));
				case "skills":
					highestAmount = Math.max(highestAmount, getSkillAverage(profile));
					break;
				case "catacombs":
					highestAmount = Math.max(highestAmount, getCatacombsLevel(profile));
					break;
				case "weight":
					highestAmount = Math.max(highestAmount, getWeight(profile));
					break;
				case "svenXp":
				case "revXp":
				case "taraXp":
				case "endermanXp":
					highestAmount = Math.max(highestAmount, getSlayer(profile, type.replace("Xp", "")));
					break;
				default:
					return -1;
			}
		}

		return highestAmount;
	}

	@Override
	public String toString() {
		return (
			"Player{" +
			"validPlayer=" +
			validPlayer +
			", playerUuid='" +
			playerUuid +
			'\'' +
			", playerUsername='" +
			playerUsername +
			'\'' +
			", profileName='" +
			profileName +
			'\'' +
			'}'
		);
	}
}
