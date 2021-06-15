package com.skyblockplus.utils;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.structs.ArmorStruct;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.skyblockplus.weight.Weight;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;

public class Player {

	public String invMissing = "";
	private JsonElement profileJson;
	private JsonElement outerProfileJson;
	private JsonElement hypixelProfileJson;
	private JsonArray profilesArray;
	private boolean validPlayer = false;
	private String playerUuid;
	private String playerUsername;
	private String profileName;
	private Map<String, String> emojiMap;

	/* Constructors */
	public Player(String username) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			this.profilesArray =
				higherDepth(getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid), "profiles")
					.getAsJsonArray();

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
			this.profilesArray =
				higherDepth(getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid), "profiles")
					.getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
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
			this.profilesArray = higherDepth(outerProfileJson, "profiles").getAsJsonArray();
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
			this.profilesArray = higherDepth(outerProfileJson, "profiles").getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				return;
			}
		} catch (Exception e) {
			return;
		}

		this.validPlayer = true;
	}

	/* Constructor helper methods */
	public boolean usernameToUuid(String username) {
		try {
			UsernameUuidStruct usernameJson = Utils.usernameToUuid(username);
			this.playerUsername = usernameJson.playerUsername;
			this.playerUuid = usernameJson.playerUuid;
			return false;
		} catch (Exception ignored) {}
		return true;
	}

	public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
		try {
			for (int i = 0; i < profilesArray.size(); i++) {
				String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
				if (currentProfileName.equalsIgnoreCase(profileName)) {
					this.profileName = currentProfileName;
					this.outerProfileJson = profilesArray.get(i);
					this.profileJson = higherDepth(profilesArray.get(i), "members." + this.playerUuid);
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
					this.profileJson = higherDepth(profilesArray.get(i), "members." + this.playerUuid);
					this.outerProfileJson = profilesArray.get(i);
					lastProfileSave = lastSaveLoop;
					this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/* Getters */
	public JsonElement getProfileJson() {
		return profileJson;
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
		return outerProfileJson;
	}

	public boolean isValid() {
		return validPlayer;
	}

	/* Links */
	public static String skyblockStatsLink(String username, String profileName) {
		if (username == null) {
			return null;
		}
		return ("https://sky.shiiyu.moe/stats/" + username + (profileName != null ? "/" + profileName : ""));
	}

	public String skyblockStatsLink() {
		return skyblockStatsLink(playerUsername, profileName);
	}

	public String getThumbnailUrl() {
		return "https://cravatar.eu/helmavatar/" + playerUuid + "/64.png";
	}

	/* Bank and purse */
	public double getBankBalance() {
		try {
			return higherDepth(outerProfileJson, "banking.balance").getAsDouble();
		} catch (Exception e) {
			return -1;
		}
	}

	public double getPurseCoins() {
		try {
			return higherDepth(profileJson, "coin_purse").getAsLong();
		} catch (Exception e) {
			return -1;
		}
	}

	public JsonArray getBankHistory() {
		try {
			return higherDepth(outerProfileJson, "banking.transactions").getAsJsonArray();
		} catch (Exception e) {
			return null;
		}
	}

	/* Skills */
	public int getTotalSkillsXp() {
		return getTotalSkillsXp(profileJson);
	}

	public int getTotalSkillsXp(JsonElement profile) {
		JsonElement skillsCap = higherDepth(getLevelingJson(), "leveling_caps");

		List<String> skills = getJsonKeys(skillsCap);
		skills.remove("catacombs");
		skills.remove("runecrafting");
		skills.remove("carpentry");

		int totalSkillXp = 0;
		for (String skill : skills) {
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
		try {
			return higherDepth(profileJson, "jacob2.perks.farming_level_cap").getAsInt();
		} catch (Exception e) {
			return 0;
		}
	}

	public int getSkillMaxLevel(String skillName, boolean isWeight) {
		if (skillName.equals("farming")) {
			return isWeight ? 60 : (higherDepth(getLevelingJson(), "leveling_caps." + skillName).getAsInt() + getFarmingCapUpgrade());
		}

		return higherDepth(getLevelingJson(), "leveling_caps." + skillName).getAsInt();
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
		return getSkill(profileJson, skillName);
	}

	public SkillsStruct getSkill(JsonElement profile, String skillName) {
		try {
			double skillExp = higherDepth(profile, "experience_skill_" + skillName).getAsDouble();
			return skillInfoFromExp(skillExp, skillName);
		} catch (Exception ignored) {}
		return null;
	}

	public double getSkillAverage() {
		JsonElement skillsCap = higherDepth(getLevelingJson(), "leveling_caps");

		List<String> skills = getJsonKeys(skillsCap);
		skills.remove("catacombs");
		skills.remove("runecrafting");
		skills.remove("carpentry");

		double progressSA = 0;
		for (String skill : skills) {
			try {
				double skillExp = higherDepth(profileJson, "experience_skill_" + skill).getAsDouble();
				SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
				progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
			} catch (Exception e) {
				return -1;
			}
		}
		progressSA /= skills.size();
		return progressSA;
	}

	public double getSkillAverage(JsonElement profile) {
		JsonElement skillsCap = higherDepth(getLevelingJson(), "leveling_caps");

		List<String> skills = getJsonKeys(skillsCap);
		skills.remove("catacombs");
		skills.remove("runecrafting");
		skills.remove("carpentry");

		double progressSA = 0;
		for (String skill : skills) {
			try {
				double skillExp = higherDepth(profile, "experience_skill_" + skill).getAsDouble();
				SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
				progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
			} catch (Exception e) {
				return -1;
			}
		}
		progressSA /= skills.size();
		return progressSA;
	}

	public SkillsStruct skillInfoFromExp(double skillExp, String skill) {
		JsonElement skillsCap = higherDepth(getLevelingJson(), "leveling_caps");

		JsonArray skillsTable;
		if (skill.equals("catacombs")) {
			skillsTable = higherDepth(getLevelingJson(), "catacombs").getAsJsonArray();
		} else if (skill.equals("runecrafting")) {
			skillsTable = higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
		} else {
			skillsTable = higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
		}
		int maxLevel;
		try {
			maxLevel = higherDepth(skillsCap, skill).getAsInt();
		} catch (Exception e) {
			maxLevel = 50;
		}

		if (skill.equals("farming")) {
			maxLevel += getFarmingCapUpgrade();
		}

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
		return getTotalSlayer(profileJson);
	}

	public int getTotalSlayer(JsonElement profile) {
		return getSlayer(profile, "sven") + getSlayer(profile, "rev") + getSlayer(profile, "tara") + getSlayer(profile, "enderman");
	}

	public int getSlayer(String slayerName) {
		return getSlayer(profileJson, slayerName);
	}

	public int getSlayer(JsonElement profile, String slayerName) {
		JsonElement profileSlayer = higherDepth(profile, "slayer_bosses");
		switch (slayerName) {
			case "sven":
				return higherDepth(profileSlayer, "wolf.xp") != null ? higherDepth(profileSlayer, "wolf.xp").getAsInt() : 0;
			case "rev":
				return higherDepth(profileSlayer, "zombie.xp") != null ? higherDepth(profileSlayer, "zombie.xp").getAsInt() : 0;
			case "tara":
				return higherDepth(profileSlayer, "spider.xp") != null ? higherDepth(profileSlayer, "spider.xp").getAsInt() : 0;
			case "enderman":
				return higherDepth(profileSlayer, "enderman.xp") != null ? higherDepth(profileSlayer, "enderman.xp").getAsInt() : 0;
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
			return higherDepth(profileJson, "dungeons.selected_dungeon_class").getAsString();
		} catch (Exception e) {
			return "none";
		}
	}

	public String getNecronBlade() {
		Map<Integer, InvItem> inventoryMap = getInventoryMap();
		if (inventoryMap == null) {
			return "\n**Inventory API Disabled**";
		}

		for (InvItem item : inventoryMap.values()) {
			if (!item.getBackpackItems().isEmpty()) {
				List<InvItem> backpackItems = item.getBackpackItems();
				for (InvItem backpackItem : backpackItems) {
					if (backpackItem.getId().equalsIgnoreCase("hyperion")) {
						return "\n**Hyperion:** yes";
					} else if (backpackItem.getId().equalsIgnoreCase("valkyrie")) {
						return "\n**Valkyrie:** yes";
					}
				}
			} else {
				if (item.getId().equalsIgnoreCase("hyperion")) {
					return "\n**Hyperion:** yes";
				} else if (item.getId().equalsIgnoreCase("valkyrie")) {
					return "\n**Valkyrie:** yes";
				}
			}
		}

		Map<Integer, InvItem> enderChestMap = getEnderChestMap();
		for (InvItem item : enderChestMap.values()) {
			if (!item.getBackpackItems().isEmpty()) {
				List<InvItem> backpackItems = item.getBackpackItems();
				for (InvItem backpackItem : backpackItems) {
					if (backpackItem.getId().equalsIgnoreCase("hyperion")) {
						return "\n**Hyperion:** yes";
					} else if (backpackItem.getId().equalsIgnoreCase("valkyrie")) {
						return "\n**Valkyrie:** yes";
					}
				}
			} else {
				if (item.getId().equalsIgnoreCase("hyperion")) {
					return "\n**Hyperion:** yes";
				} else if (item.getId().equalsIgnoreCase("valkyrie")) {
					return "\n**Valkyrie:** yes";
				}
			}
		}

		return "\n**No Hyperion or Valkyrie**";
	}

	public String getFastestF7Time() {
		try {
			int f7TimeMilliseconds = higherDepth(profileJson, "dungeons.dungeon_types.catacombs.fastest_time_s_plus.7").getAsInt();
			int minutes = f7TimeMilliseconds / 1000 / 60;
			int seconds = f7TimeMilliseconds % 1000 % 60;
			return ("\n**Fastest F7 S+:** " + minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds));
		} catch (Exception e) {
			return "\n**No F7 S+ time found**";
		}
	}

	public int getBonemerang() {
		int boneCount = 0;

		try {
			Map<Integer, InvItem> inventoryMap = getInventoryMap();
			if (inventoryMap == null) {
				return -1;
			}

			for (InvItem item : inventoryMap.values()) {
				if (!item.getBackpackItems().isEmpty()) {
					List<InvItem> backpackItems = item.getBackpackItems();
					for (InvItem backpackItem : backpackItems) {
						if (backpackItem.getId().equalsIgnoreCase("bone_boomerang")) {
							boneCount++;
						}
					}
				} else {
					if (item.getId().equalsIgnoreCase("bone_boomerang")) {
						boneCount++;
					}
				}
			}

			Map<Integer, InvItem> enderChestMap = getEnderChestMap();
			for (InvItem item : enderChestMap.values()) {
				if (!item.getBackpackItems().isEmpty()) {
					List<InvItem> backpackItems = item.getBackpackItems();
					for (InvItem backpackItem : backpackItems) {
						if (backpackItem.getId().equalsIgnoreCase("bone_boomerang")) {
							boneCount++;
						}
					}
				} else {
					if (item.getId().equalsIgnoreCase("bone_boomerang")) {
						boneCount++;
					}
				}
			}
		} catch (Exception ignored) {}

		return boneCount;
	}

	public int getDungeonSecrets() {
		if (hypixelProfileJson == null) {
			this.hypixelProfileJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid);
		}

		try {
			return higherDepth(hypixelProfileJson, "player.achievements.skyblock_treasure_hunter").getAsInt();
		} catch (Exception e) {
			return 0;
		}
	}

	public double getDungeonClassLevel(String className) {
		return getDungeonClassLevel(profileJson, className);
	}

	public double getDungeonClassLevel(JsonElement profile, String className) {
		SkillsStruct dungeonClassLevel = skillInfoFromExp(getDungeonClassXp(profile, className), "catacombs");
		return dungeonClassLevel.skillLevel + dungeonClassLevel.progressToNext;
	}

	public SkillsStruct getDungeonClass(String className) {
		return skillInfoFromExp(getDungeonClassXp(className), "catacombs");
	}

	public double getCatacombsLevel() {
		return getCatacombsLevel(profileJson);
	}

	public double getCatacombsLevel(JsonElement profile) {
		SkillsStruct catacombsInfo = getCatacombsSkill(profile);
		if (catacombsInfo != null) {
			return catacombsInfo.skillLevel + catacombsInfo.progressToNext;
		}
		return 0;
	}

	public SkillsStruct getCatacombsSkill() {
		return getCatacombsSkill(profileJson);
	}

	public SkillsStruct getCatacombsSkill(JsonElement profile) {
		try {
			double skillExp = higherDepth(profile, "dungeons.dungeon_types.catacombs.experience").getAsDouble();
			return skillInfoFromExp(skillExp, "catacombs");
		} catch (Exception e) {
			return null;
		}
	}

	public double getDungeonClassXp(String className) {
		return getDungeonClassXp(profileJson, className);
	}

	public double getDungeonClassXp(JsonElement profile, String className) {
		try {
			return higherDepth(profile, "dungeons.player_classes." + className + ".experience").getAsDouble();
		} catch (Exception e) {
			return 0;
		}
	}

	/* -- Start inventory -- */
	public static Map<Integer, InvItem> getGenericInventoryMap(NBTCompound parsedContents) {
		try {
			NBTList items = parsedContents.getList("i");
			Map<Integer, InvItem> itemsMap = new HashMap<>();

			for (int i = 0; i < items.size(); i++) {
				try {
					NBTCompound item = items.getCompound(i);
					if (!item.isEmpty()) {
						InvItem itemInfo = new InvItem();
						itemInfo.setName(parseMcCodes(item.getString("tag.display.Name", "None")));
						itemInfo.setLore(
							parseMcCodes(item.getString("tag.display.Lore", "None").replace(", ", "\n").replace("[", "").replace("]", ""))
						);
						itemInfo.setCount(Integer.parseInt(item.getString("Count", "0").replace("b", " ")));
						itemInfo.setId(item.getString("tag.ExtraAttributes.id", "None"));
						itemInfo.setCreationTimestamp(item.getString("tag.ExtraAttributes.timestamp", "None"));
						itemInfo.setHbpCount(item.getInt("tag.ExtraAttributes.hot_potato_count", 0));
						itemInfo.setRecombobulated(item.getInt("tag.ExtraAttributes.rarity_upgrades", 0) == 1);
						itemInfo.setModifier(item.getString("tag.ExtraAttributes.modifier", "None"));
						itemInfo.setDungeonFloor(Integer.parseInt(item.getString("tag.ExtraAttributes.item_tier", "-1")));
						itemInfo.setNbtTag(item.toString());

						try {
							NBTCompound enchants = item.getCompound("tag.ExtraAttributes.enchantments");
							List<String> enchantsList = new ArrayList<>();
							for (Map.Entry<String, Object> enchant : enchants.entrySet()) {
								enchantsList.add(enchant.getKey() + ";" + enchant.getValue());
							}
							itemInfo.setEnchantsFormatted(enchantsList);
						} catch (Exception ignored) {}

						String itemSkinStr = item.getString("tag.ExtraAttributes.skin", "None");
						if (!itemSkinStr.equals("None")) {
							itemInfo.addExtraValue("PET_SKIN_" + itemSkinStr);
						}

						try {
							NBTList necronBladeScrolls = item.getList("tag.ExtraAttributes.ability_scroll");
							for (Object scroll : necronBladeScrolls) {
								try {
									itemInfo.addExtraValue("" + scroll);
								} catch (Exception ignored) {}
							}
						} catch (Exception ignored) {}

						if (item.getInt("tag.ExtraAttributes.wood_singularity_count", 0) == 1) {
							itemInfo.addExtraValue("WOOD_SINGULARITY");
						}

						try {
							byte[] backpackContents = item.getByteArray("tag.ExtraAttributes." + itemInfo.getId().toLowerCase() + "_data");
							NBTCompound parsedContentsBackpack = NBTReader.read(new ByteArrayInputStream(backpackContents));
							itemInfo.setBackpackItems(getGenericInventoryMap(parsedContentsBackpack).values());
						} catch (Exception ignored) {}

						itemsMap.put(i, itemInfo);
						continue;
					}
				} catch (Exception ignored) {}
				itemsMap.put(i, null);
			}

			return itemsMap;
		} catch (Exception ignored) {}

		return null;
	}

	/* InvItem maps */
	public Map<Integer, InvItem> getInventoryMap() {
		try {
			String contents = higherDepth(profileJson, "inv_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getStorageMap() {
		try {
			JsonElement backpackContents = higherDepth(profileJson, "backpack_contents");
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
			String contents = higherDepth(profileJson, "talisman_bag.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getInventoryArmorMap() {
		try {
			String contents = higherDepth(profileJson, "inv_armor.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getWardrobeMap() {
		try {
			String contents = higherDepth(profileJson, "wardrobe_contents.data").getAsString();
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
					petLevelFromXp(higherDepth(pet, "exp").getAsLong(), higherDepth(pet, "tier").getAsString().toLowerCase()) +
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
			String contents = higherDepth(profileJson, "ender_chest_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	/* Emoji viewer arrays / other inventory */
	public List<String[]> getTalismanBag() {
		try {
			String encodedInventoryContents = higherDepth(profileJson, "talisman_bag.data").getAsString();
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
			String encodedInventoryContents = higherDepth(profileJson, "ender_chest_contents.data").getAsString();
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
			String encodedInventoryContents = higherDepth(profileJson, "inv_contents.data").getAsString();
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
			String encodedWardrobeContents = higherDepth(profileJson, "wardrobe_contents.data").getAsString();
			int equippedSlot = higherDepth(profileJson, "wardrobe_equipped_slot").getAsInt();
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
			int equippedWardrobeSlot = higherDepth(profileJson, "wardrobe_equipped_slot").getAsInt();
			Map<Integer, InvItem> equippedArmor = equippedWardrobeSlot != -1 ? getInventoryArmorMap() : null;

			String encodedInventoryContents = higherDepth(profileJson, "wardrobe_contents.data").getAsString();
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public ArmorStruct getInventoryArmor() {
		try {
			String encodedInventoryContents = higherDepth(profileJson, "inv_armor.data").getAsString();
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
		return higherDepth(profileJson, "pets").getAsJsonArray();
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, Integer> getPlayerSacks() {
		JsonElement sacksJson = higherDepth(profileJson, "sacks_counts");
		return new Gson().fromJson(sacksJson, HashMap.class);
	}

	/* -- End inventory -- */

	/* Miscellaneous */
	public String[] getAllProfileNames(boolean isIronman) {
		List<String> profileNameList = new ArrayList<>();
		if (this.profilesArray == null) {
			this.profilesArray =
				higherDepth(getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid), "profiles")
					.getAsJsonArray();
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
		try {
			return higherDepth(profileJson, "fairy_souls_collected").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	public String itemToEmoji(String itemName) {
		if (emojiMap == null) {
			emojiMap = new HashMap<>();
			emojiMap.put("empty", "<:empty:814669776201711637>");
			emojiMap.put("hyperion", "<:hyperion:814675220455096321>");
			emojiMap.put("rogue_sword", "<:rogue_sword:814675777479114803>");
			emojiMap.put("grappling_hook", "<:grappling_hook:814676007118176257>");
			emojiMap.put("runaans_bow", "<:runaans_bow:814676456897511424>");
			emojiMap.put("overflux_power_orb", "<:overflux_power_orb:814676605514678292>");
			emojiMap.put("superboom_tnt", "<:superboom_tnt:814690915318235146>");
			emojiMap.put("spirit_leap", "<:spirit_leap:814677057107263508>");
			emojiMap.put("skyblock_menu", "<:skyblock_menu:814676947602374698>");
			emojiMap.put("greater_backpack", "<:greater_backpack:814679082667081769>");
			emojiMap.put("dungeon_stone", "<:dungeon_stone:814680424994570291>");
			emojiMap.put("defuse_kit", "<:defuse_kit:814680645724012597>");
			emojiMap.put("rabbit_hat", "<:rabbit_hat:814680929117011988>");
			emojiMap.put("jerry_staff", "<:jerry_staff:814681305488818197>");
			emojiMap.put("flower_of_truth", "<:flower_of_truth:814687420413902849>");
			emojiMap.put("bone_boomerang", "<:bone_boomerang:814687704104435732>");
			emojiMap.put("snow_block", "<:snow_block:814690652569993248>");
			emojiMap.put("auger_rod", "<:auger_rod:814688099044687872>");
			emojiMap.put("death_bow", "<:death_bow:814688302707769354>");
			emojiMap.put("diver_fragment", "<:diver_fragment:814688560816324639>");
			emojiMap.put("blue_ice_hunk", "<:blue_ice_hunk:814688991769133087>");
			emojiMap.put("aspect_of_the_end", "<:aspect_of_the_end:814689179110735902>");
			emojiMap.put("enchanted_book", "<:enchanted_book:814689302960930826>");
			emojiMap.put("ice_hunk", "<:ice_hunk:814689461307703363>");
			emojiMap.put("golden_apple", "<:golden_apple:814689788359082004>");
			emojiMap.put("stonk_pickaxe", "<:stonk_pickaxe:814689918311596044>");
			emojiMap.put("white_gift", "<:white_gift:814690119591919696>");
			emojiMap.put("item_spirit_bow", "<:item_spirit_bow:815283134416551997>");
			emojiMap.put("ice_spray_wand", "<:ice_spray_wand:815283295896993813>");
			emojiMap.put("jumbo_backpack", "<:jumbo_backpack:815283480476254219>");
			emojiMap.put("kismet_feather", "<:kismet_feather:815284259517759508>");
			emojiMap.put("wither_cloak", "<:wither_cloak:815283770189021204>");
			emojiMap.put("dungeon_chest_key", "<:dungeon_chest_key:815284007767769100>");
			emojiMap.put("florid_zombie_sword", "<:florid_zombie_sword:815284420567236649>");
			emojiMap.put("medium_backpack", "<:medium_backpack:815284582353731635>");
			emojiMap.put("phantom_rod", "<:phantom_rod:815284719868182568>");
			emojiMap.put("fel_pearl", "<:fel_pearl:815286137576488980>");
			emojiMap.put("holy_fragment", "<:holy_fragment:815285660214493184>");
			emojiMap.put("unstable_fragment", "<:unstable_fragment:815285660264038400>");
			emojiMap.put("young_fragment", "<:young_fragment:815285660230352897>");
			emojiMap.put("enchanted_bone", "<:enchanted_bone:815286708764934214>");
			emojiMap.put("enchanted_rotten_flesh", "<:enchanted_rotten_flesh:815287217316560896>");
			emojiMap.put("training_weights", "<:training_weights:815287498560503820>");
			emojiMap.put("beastmaster_crest_rare", "<:beastmaster_crest_rare:815288186170245140>");
			emojiMap.put("enchanted_ice", "<:enchanted_ice:815288002649522217>");
			emojiMap.put("zombie_knight_helmet", "<:zombie_knight_helmet:815300154311049236>");
			emojiMap.put("earth_shard", "<:earth_shard:815300345345474563>");
			emojiMap.put("pumpkin_dicer", "<:pumpkin_dicer:815300807980220436>");
			emojiMap.put("infinite_superboom_tnt", "<:infinite_superboom_tnt:815305646194688000>");
			emojiMap.put("sniper_bow", "<:sniper_bow:816310296309137428>");
			emojiMap.put("thorns_boots", "<:spirit_boots:816310296217518131>");
			emojiMap.put("shadow_fury", "<:shadow_fury:816310296179245076>");
			emojiMap.put("skeleton_master_boots", "<:skeleton_master_boots:816310296096276550>");
			emojiMap.put("machine_gun_bow", "<:machine_gun_bow:816310296053678141>");
			emojiMap.put("beastmaster_crest_common", "<:beastmaster_crest_common:816310296045813770>");
			emojiMap.put("last_breath", "<:last_breath:816310296041357342>");
			emojiMap.put("beastmaster_crest_uncommon", "<:beastmaster_crest_uncommon:816310296037163068>");
			emojiMap.put("beastmaster_crest_legendary", "<:beastmaster_crest_legendary:816310296032313344>");
			emojiMap.put("crypt_bow", "<:crypt_bow:816310296011735050>");
			emojiMap.put("crypt_dreadlord_sword", "<:crypt_dreadlord_sword:816310295990370304>");
			emojiMap.put("beastmaster_crest_epic", "<:beastmaster_crest_epic:816310295907270698>");
			emojiMap.put("skeleton_master_helmet", "<:skeleton_master_helmet:816310295785242676>");
			emojiMap.put("bonzo_mask", "<:bonzo_mask:816132749982171156>");
			emojiMap.put("broken_piggy_bank", "<:broken_piggy_bank:816129549536329738>");
			emojiMap.put("cracked_piggy_bank", "<:cracked_piggy_bank:816129538601779250>");
			emojiMap.put("piggy_bank", "<:piggy_bank:816129528224546826>");
			emojiMap.put("sword_of_revelations", "<:sword_of_revelations:815310159617589288>");
			emojiMap.put("shaman_sword", "<:shaman_sword:815309622658465882>");
			emojiMap.put("adaptive_blade", "<:adaptive_blade:815309431038410833>");
			emojiMap.put("raider_axe", "<:raider_axe:815308927020564491>");
			emojiMap.put("wither_boots", "<:wither_boots:815308403219365929>");
			emojiMap.put("soul_whip", "<:soul_whip:815307925052457010>");
			emojiMap.put("rotten_leggings", "<:rotten_leggings:815307870937284699>");
			emojiMap.put("super_heavy_chestplate", "<:super_heavy_chestplate:815307825504976916>");
			emojiMap.put("large_backpack", "<:large_backpack:815299347822149632>");
			emojiMap.put("midas_sword", "<:midas_sword:815307715467018242>");
			emojiMap.put("bonzo_staff", "<:bonzo_staff:815307683883647076>");
			emojiMap.put("flaming_sword", "<:flaming_sword:815307168109953024>");
			emojiMap.put("midas_staff", "<:midas_staff:815306843751448597>");
			emojiMap.put("hunter_knife", "<:hunter_knife:815306466596356137>");
			emojiMap.put("fancy_sword", "<:fancy_sword:815306055533461544>");
			emojiMap.put("sorrow", "<:sorrow:816324126956453929>");
			emojiMap.put("plasma", "<:plasma:816324127412977694>");
			emojiMap.put("volta", "<:volta:816324127307595797>");
			emojiMap.put("bag_of_cash", "<:bag_of_cash:816324127253332038>");
			emojiMap.put("ancient_claw", "<:ancient_claw:816324552405549117>");
			emojiMap.put("summoning_ring", "<:summoning_ring:816433732331241483>");
			emojiMap.put("zombie_knight_sword", "<:zombie_knight_sword:816433732331634719>");
			emojiMap.put("enchanted_iron", "<:enchanted_iron:816433732587225158>");
			emojiMap.put("enchanted_ancient_claw", "<:enchanted_ancient_claw:816433732624842842>");
			emojiMap.put("griffin_feather", "<:griffin_feather:816433732624973884>");
			emojiMap.put("potion", "<:potion:816433732688281650>");
			emojiMap.put("enchanted_gold", "<:enchanted_gold:816433732721573938>");
			emojiMap.put("emerald_blade", "<:emerald_blade:816433732763910166>");
			emojiMap.put("ancestral_spade", "<:ancestral_spade:816433732969693214>");
			emojiMap.put("magma_bow", "<:magma_bow:816435478076850187>");
			emojiMap.put("end_stone_sword", "<:end_stone_sword:816435478223519766>");
			emojiMap.put("gold_axe", "<:gold_axe:816435478471114773>");
			emojiMap.put("aatrox_batphone", "<:aatrox_batphone:816435478580428841>");
			emojiMap.put("giants_sword", "<:giants_sword:816435478609133619>");
			emojiMap.put("rod_of_the_sea", "<:rod_of_the_sea:816438156445745203>");
			emojiMap.put("theoretical_hoe_warts_3", "<:theoretical_hoe_warts_3:816438156680626176>");
			emojiMap.put("treecapitator_axe", "<:treecapitator:816438156873302086>");
			emojiMap.put("theoretical_hoe_cane_3", "<:theoretical_hoe_cane_3:816438156953518150>");
			emojiMap.put("fel_sword", "<:fel_sword:816442343816822785>");
			emojiMap.put("jingle_bells", "<:jingle_bells:816442343900446731>");
			emojiMap.put("aspect_of_the_dragon", "<:aspect_of_the_dragon:816442343922204674>");
			emojiMap.put("gift_compass", "<:gift_compass:816442344014348300>");
			emojiMap.put("magical_bucket", "<:magical_bucket:816442344055242773>");
			emojiMap.put("mana_flux_power_orb", "<:mana_flux_power_orb:816442344060092477>");
			emojiMap.put("sea_lantern", "<:sea_lantern:816442344093646849>");
			emojiMap.put("valkyrie", "<:valkyrie:816442344260763649>");
			emojiMap.put("livid_dagger", "<:livid_dagger:816442344264957952>");
			emojiMap.put("wise_wither_chestplate", "<:storm_chestplate:816442344265744395>");
			emojiMap.put("diamond_pickaxe", "<:diamond_pickaxe:816442344269414421>");
			emojiMap.put("rune", "<:rune:816442344320532532>");
			emojiMap.put("wither_goggles", "<:wither_goggles:816442344382922783>");
			emojiMap.put("skeleton_master_leggings", "<:skeleton_master_leggings:816442344387641385>");
			emojiMap.put("wise_wither_leggings", "<:storm_leggings:816442344459075584>");
			emojiMap.put("personal_compactor_7000", "<:personal_compactor_7000:816442344462090281>");
			emojiMap.put("wise_wither_helmet", "<:storm_helmet:816442344525791253>");
			emojiMap.put("wise_wither_boots", "<:storm_boots:816442344534310963>");
			emojiMap.put("slime_hat", "<:slime_hat:816442344559083580>");
			emojiMap.put("tarantula_boots", "<:tarantula_boots:816442344572190760>");
			emojiMap.put("zombie_soldier_boots", "<:zombie_soldier_boots:816442344617934868>");
			emojiMap.put("shadow_assassin_helmet", "<:shadow_assassin_helmet:816444761559793694>");
			emojiMap.put("wither_catalyst", "<:wither_catalyst:816444761493864448>");
			emojiMap.put("zombie_soldier_chestplate", "<:zombie_soldier_chestplate:816445031946780693>");
			emojiMap.put("cake_soul", "<:cake_soul:816445265480777818>");
			emojiMap.put("power_wither_helmet", "<:necron_helmet:816450192366370866>");
			emojiMap.put("tank_wither_helmet", "<:goldor_helmet:816450192198991872>");
			emojiMap.put("mimic_fragment", "<:mimic_fragment:816452177928519709>");
			emojiMap.put("reaper_sword", "<:reaper_falchion:816452177882775552>");
			emojiMap.put("speed_wither_chestplate", "<:maxor_chestplate:816450192131227679>");
			emojiMap.put("builders_wand", "<:builders_wand:816452177487724575>");
			emojiMap.put("zombie_soldier_cutlass", "<:zombie_soldier_cutlass:816450973651304448>");
			emojiMap.put("tank_wither_chestplate", "<:goldor_chestplate:816450192139223080>");
			emojiMap.put("power_wither_leggings", "<:necron_leggings:816450192084566037>");
			emojiMap.put("speed_wither_leggings", "<:maxor_leggings:816450191904473118>");
			emojiMap.put("personal_compactor_6000", "<:personal_compactor_6000:816452177588650025>");
			emojiMap.put("speed_wither_helmet", "<:maxor_helmet:816450192487350279>");
			emojiMap.put("skeleton_soldier_helmet", "<:skeleton_soldier_helmet:816452177794826291>");
			emojiMap.put("tank_wither_leggings", "<:goldor_leggings:816450192336879669>");
			emojiMap.put("tank_wither_boots", "<:goldor_boots:816450192203055137>");
			emojiMap.put("remnant_of_the_eye", "<:remnant_of_the_eye:816438156840271953>");
			emojiMap.put("speed_wither_boots", "<:maxor_boots:816450192240672828>");
			emojiMap.put("power_wither_chestplate", "<:necron_chestplate:816450192285892608>");
			emojiMap.put("power_wither_boots", "<:necron_boots:816450192270163989>");
			emojiMap.put("wither_blood", "<:wither_blood:816450973681320006>");
			emojiMap.put("ornate_zombie_sword", "<:ornatezombiesword:816452177567678475>");
			emojiMap.put("blank", "<:blank:817050888186101761>");
			emojiMap.put("goblin_egg", "<:goblin_egg:816457683606437918>");
			emojiMap.put("leaping_sword", "<:leaping_sword:816457683237470209>");
			emojiMap.put("healing_ring", "<:healing_ring:817495522372747336>");
			emojiMap.put("speed_artifact", "<:speed_artifact:817495522213625920>");
			emojiMap.put("night_vision_charm", "<:night_vision_charm:817495522133803010>");
			emojiMap.put("cheetah_talisman", "<:cheetah_talisman:817495522201960470>");
			emojiMap.put("red_claw_artifact", "<:red_claw_artifact:817495522410627072>");
			emojiMap.put("artifact_potion_affinity", "<:potion_artifact:817495522192392224>");
			emojiMap.put("green_gift", "<:green_gift:816457683636322324>");
			emojiMap.put("healing_talisman", "<:healing_talisman:817495522096316497>");
			emojiMap.put("treasure_ring", "<:treasure_ring:817495522775793674>");
			emojiMap.put("wand_of_healing", "<:wand_of_healing:816457683862945793>");
			emojiMap.put("spider_artifact", "<:spider_artifact:817495522570272768>");
			emojiMap.put("enchanted_titanium", "<:enchanted_titanium:816457683703300168>");
			emojiMap.put("farming_talisman", "<:farming_talisman:817495522407481414>");
			emojiMap.put("skeleton_talisman", "<:skeleton_talisman:817495522390442025>");
			emojiMap.put("lava_talisman", "<:lava_talisman:817495522457812992>");
			emojiMap.put("fractured_mithril_pickaxe", "<a:fractured_mithril_pickaxe:816455519106629652>");
			emojiMap.put("auto_recombobulator", "<:auto_recombobulator:817495522369601627>");
			emojiMap.put("snow_cannon", "<:snow_cannon:816457683775258674>");
			emojiMap.put("shady_ring", "<:shady_ring:817495522427797504>");
			emojiMap.put("enchanted_slime_ball", "<:enchanted_slime_ball:816458357333557248>");
			emojiMap.put("sea_creature_talisman", "<:sea_creature_talisman:817495522520727582>");
			emojiMap.put("red_claw_talisman", "<:red_claw_talisman:817495522427797514>");
			emojiMap.put("red_gift", "<:red_gift:816457683737640980>");
			emojiMap.put("survivor_cube", "<:survivor_cube:817495522662154240>");
			emojiMap.put("eternal_hoof", "<:eternal_hoof:817495522390048768>");
			emojiMap.put("potion_ring", "<:potion_ring:817495522385461268>");
			emojiMap.put("slime_ball", "<:slime_ball:816458704768729088>");
			emojiMap.put("radiant_power_orb", "<:radiant_power_orb:816457683699630150>");
			emojiMap.put("grand_exp_bottle", "<:grand_exp_bottle:816457683695042580>");
			emojiMap.put("haste_ring", "<:haste_ring:817495522369470484>");
			emojiMap.put("voodoo_doll", "<:voodoo_doll:816457683589660694>");
			emojiMap.put("speed_talisman", "<:speed_talisman:817495522611822612>");
			emojiMap.put("treasure_artifact", "<:treasure_artifact:817495522704752671>");
			emojiMap.put("hegemony_artifact", "<:hegemony_artifact:817495522389655602>");
			emojiMap.put("mithril_ore", "<:mithril_ore:816457683615612989>");
			emojiMap.put("scavenger_talisman", "<:scavenger_talisman:817495522381529138>");
			emojiMap.put("sea_creature_ring", "<:sea_creature_ring:817495522260156428>");
			emojiMap.put("speed_ring", "<:speed_ring:817495522511945820>");
			emojiMap.put("sea_creature_artifact", "<:seacreature_artifact:817495522067480588>");
			emojiMap.put("glacite_jewel", "<:glacite_jewel:816457683707101234>");
			emojiMap.put("lucky_hoof", "<:lucky_hoof:817495522544713758>");
			emojiMap.put("enchanted_mithril", "<:enchanted_mithril:816457683942244412>");
			emojiMap.put("seal_of_the_family", "<:seal_of_the_family:817495522377203732>");
			emojiMap.put("experience_artifact", "<:experience_artifact:817495522075213865>");
			emojiMap.put("red_claw_ring", "<:red_claw_ring:817495521992245360>");
			emojiMap.put("spider_ring", "<:spider_ring:817495522227126293>");
			emojiMap.put("snow_blaster", "<:snow_blaster:816457683845513276>");
			emojiMap.put("potion_talisman", "<:potion_talisman:817495522373271559>");
			emojiMap.put("frozen_chicken", "<:frozen_chicken:817502293259059251>");
			emojiMap.put("wolf_talisman", "<:wolf_talisman:817502250045538335>");
			emojiMap.put("fish_affinity_talisman", "<:fishing_talisman:817502293229436939>");
			emojiMap.put("magnetic_talisman", "<:magnetic_talisman:817502250187620362>");
			emojiMap.put("wood_talisman", "<:wood_talisman:817502250331144272>");
			emojiMap.put("bat_talisman", "<:bat_talisman:817502293296676864>");
			emojiMap.put("spider_talisman", "<:spider_talisman:817502250536534036>");
			emojiMap.put("feather_artifact", "<:feather_artifact:817502293116977163>");
			emojiMap.put("personal_compactor_4000", "<:personal_compactor_4000:817502250313449502>");
			emojiMap.put("coin_talisman", "<:coin_talisman:817502293099806771>");
			emojiMap.put("bits_talisman", "<:bits_talisman:817502293347663922>");
			emojiMap.put("candy_artifact", "<:candy_artifact:817502293317648395>");
			emojiMap.put("bait_ring", "<:bait_ring:817502293280161869>");
			emojiMap.put("wither_artifact", "<:wither_artifact:817502250410049566>");
			emojiMap.put("vaccine_talisman", "<:vaccine_talisman:817502250283827200>");
			emojiMap.put("devour_ring", "<:devour_ring:817502293347926036>");
			emojiMap.put("bat_artifact", "<:bat_artifact:817502293086830614>");
			emojiMap.put("fire_talisman", "<:fire_talisman:817502293489745950>");
			emojiMap.put("potato_talisman", "<:potato_talisman:817502250255515668>");
			emojiMap.put("emerald_ring", "<:emerald_ring:817502293305851945>");
			emojiMap.put("hunter_ring", "<:hunter_ring:817502293095088189>");
			emojiMap.put("tarantula_talisman", "<:tarantula_talisman:817502250082762763>");
			emojiMap.put("titanium_artifact", "<:titanium_artifact:817502250334683136>");
			emojiMap.put("hunter_talisman", "<:hunter_talisman:817502250237821011>");
			emojiMap.put("zombie_talisman", "<:zombie_talisman:817502250279632937>");
			emojiMap.put("feather_ring", "<:feather_ring:817502293472575576>");
			emojiMap.put("gravity_talisman", "<:gravity_talisman:817502293544927294>");
			emojiMap.put("zombie_artifact", "<:zombie_artifact:817502250427482152>");
			emojiMap.put("personal_compactor_5000", "<:personal_compactor_5000:817502250309910539>");
			emojiMap.put("new_year_cake_bag", "<:cake_bag:817502293489877052>");
			emojiMap.put("village_talisman", "<:village_talisman:817502250439016458>");
			emojiMap.put("candy_relic", "<:candy_relic:817502292953399357>");
			emojiMap.put("crooked_ring", "<:crooked_ring:817502293410709524>");
			emojiMap.put("wolf_paw", "<:wolf_paw:817502250439016448>");
			emojiMap.put("intimidation_artifact", "<:intimidation_artifact:817502250150658098>");
			emojiMap.put("wolf_ring", "<:wolf_ring:817502250242015252>");
			emojiMap.put("catacombs_expert_ring", "<:catacombs_expert_ring:817502293501935618>");
			emojiMap.put("zombie_ring", "<:zombie_ring:817502250037411871>");
			emojiMap.put("ender_artifact", "<:ender_artifact:817502293817557012>");
			emojiMap.put("feather_talisman", "<:feather_talisman:817502293104525393>");
			emojiMap.put("intimidation_ring", "<:intimidation_ring:817502250117103637>");
			emojiMap.put("candy_ring", "<:candy_ring:817502293321580624>");
			emojiMap.put("bat_ring", "<:bat_ring:817502293435088966>");
			emojiMap.put("intimidation_talisman", "<:intimidation_talisman:817502250263511061>");
			emojiMap.put("mineral_talisman", "<:mineral_talisman:817502250279895062>");
			emojiMap.put("candy_talisman", "<:candy_talisman:817502293116977203>");
			emojiMap.put("treasure_talisman", "<:treasure_talisman:817502250347790416>");
			emojiMap.put("mine_talisman", "<:mine_talisman:817502249889955912>");
			emojiMap.put("wither_relic", "<:wither_relic:817502249986293871>");
			emojiMap.put("farmer_orb", "<:farmer_orb:817502293670756362>");
			emojiMap.put("wedding_ring_9", "<:wedding_ring_9:817868454001901668>");
			emojiMap.put("scorpion_foil", "<:scorpion_foil:817868453834522634>");
			emojiMap.put("pooch_sword", "<:pooch_sword:817868453812895844>");
			emojiMap.put("campfire_talisman_29", "<:campfire_talisman_29:817868453805424700>");
			emojiMap.put("day_crystal", "<:day_crystal:817508883664863232>");
			emojiMap.put("explosive_bow", "<:explosive_bow:817868453733597254>");
			emojiMap.put("party_hat_crab", "<a:party_hat_crab4:817868453952094278>");
			emojiMap.put("small_backpack", "<:small_backpack:817868453804769301>");
			emojiMap.put("night_crystal", "<:night_crystal:817508883719127140>");
			emojiMap.put("scarf_thesis", "<:scarf_thesis:817508883526057995>");
			emojiMap.put("trick_or_treat_bag", "<:trick_or_treat_bag:817868453972934716>");
			emojiMap.put("super_magic_mushroom_soup", "<:super_magic_mushroom_soup:817868453599772695>");
			emojiMap.put("wand_of_mending", "<:wand_of_mending:817868453678546976>");
			emojiMap.put("pigs_foot", "<:pigs_foot:817508883726598144>");
			emojiMap.put("scarf_studies", "<:scarf_studies:817508883583991859>");
			emojiMap.put("diamond_spade", "<:diamond_spade:817868453515624510>");
			emojiMap.put("bat_person_ring", "<:bat_person_ring:817508883735511091>");
			emojiMap.put("bat_person_artifact", "<:bat_person_artifact:817508883698417684>");
			emojiMap.put("scarf_grimoir", "<:scarf_grimoir:817508883755958332>");
			emojiMap.put("bat_person_talisman", "<:bat_person_talisman:817508883391447071>");
			emojiMap.put("sharp_shark_tooth_necklace", "<:sharp_shark_tooth_necklace:817869329479368745>");
			emojiMap.put("booster_cookie", "<:booster_cookie:820521703196196905>");
			emojiMap.put("french_bread", "<:french_bread:820521703217037362>");
			emojiMap.put("melody_hair", "<:melody_hair:820521703338672129>");
			emojiMap.put("beheaded_horror", "<:beheaded_horror:820521703124893716>");
			emojiMap.put("diamond_sword", "<:diamond_sword:820521703205371924>");
			emojiMap.put("personal_deletor_4000", "<:personal_deletor_4000:820521703506444289>");
			emojiMap.put("block_zapper", "<:block_zapper:820521703578140712>");
			emojiMap.put("livid_fragment", "<:livid_fragment:820521702948995074>");
			emojiMap.put("old_dragon_boots", "<:old_dragon_boots:820521703011909683>");
			emojiMap.put("old_dragon_leggings", "<:old_dragon_leggings:820521703746699275>");
			emojiMap.put("blessed_fruit", "<:blessed_fruit:820521702818971660>");
			emojiMap.put("game_breaker", "<:game_breaker:820521702852919327>");
			emojiMap.put("personal_deletor_7000", "<:personal_deletor_7000:820521703473021000>");
			emojiMap.put("jerry_talisman_golden", "<:jerry_talisman_golden:820521703418494996>");
			emojiMap.put("fishing_rod", "<:fishing_rod:820521703028424705>");
			emojiMap.put("razor_sharp_shark_tooth_necklace", "<:razor_sharp_shark_tooth_necklace:820521703536197642>");
			emojiMap.put("jerry_talisman_green", "<:jerry_talisman_green:820521703339065414>");
			emojiMap.put("leather_chestplate", "<:leather_chestplate:820521703066173461>");
			emojiMap.put("jerry_candy", "<:jerry_candy:820521703368425512>");
			emojiMap.put("king_talisman", "<:king_talisman:820521703427670046>");
			emojiMap.put("pet_skin_monkey_golden", "<:pet_skin_monkey_golden:820521703091732491>");
			emojiMap.put("arachne_fragment", "<:arachne_fragment:820521702987137044>");
			emojiMap.put("beacon", "<:beacon:820521703112966194>");
			emojiMap.put("iron_sword", "<:iron_sword:820521703280476191>");
			emojiMap.put("arachne_keeper_fragment", "<:arachne_keeper_fragment:820521703116505088>");
			emojiMap.put("obsidian_chestplate", "<:obsidian_chestplate:820521704270725160>");
			emojiMap.put("pet_skin_sheep_neon_red", "<:pet_skin_sheep_neon_red:820521703620739084>");
			emojiMap.put("healing_tissue", "<:healing_tissue:820521703309312050>");
			emojiMap.put("diamond_chestplate", "<:diamond_chestplate:820521703246397490>");
			emojiMap.put("soul_string", "<:soul_string:820536394988257321>");
			emojiMap.put("dark_orb", "<:dark_orb:820536334945878036>");
			emojiMap.put("large_agronomy_sack", "<:large_agronomy_sack:820536335030157313>");
			emojiMap.put("old_dragon_helmet", "<:old_dragon_helmet:820536394509844480>");
			emojiMap.put("echolocator", "<:echolocator:820536334993195038>");
			emojiMap.put("shadow_goggles", "<:shadow_goggles:820536394530291713>");
			emojiMap.put("new_year_cake", "<:new_year_cake:820536335185477643>");
			emojiMap.put("mender_fedora", "<:mender_fedora:820536394354393129>");
			emojiMap.put("frozen_scythe", "<:frozen_scythe:820536334971306014>");
			emojiMap.put("fungi_cutter", "<:fungi_cutter:820536334983626762>");
			emojiMap.put("plasmaflux_power_orb", "<:plasmaflux_power_orb:820536394481008670>");
			emojiMap.put("kat_flower", "<:kat_flower:820536335134621746>");
			emojiMap.put("giant_fragment_bigfoot", "<:giant_fragment_bigfoot:820536334812053525>");
			emojiMap.put("daedalus_axe", "<:daedalus_axe:820536334996996096>");
			emojiMap.put("crystal_fragment", "<:crystal_fragment:820536334895677440>");
			emojiMap.put("skeleton_hat", "<:skeleton_hat:820536394463969311>");
			emojiMap.put("prismapump", "<:prismapump:820536394912366622>");
			emojiMap.put("treasurite", "<:treasurite:820536394450599937>");
			emojiMap.put("large_slayer_sack", "<:large_slayer_sack:820536335314976779>");
			emojiMap.put("scarf_grimoire", "<:scarf_grimoire:820536394706583553>");
			emojiMap.put("venoms_touch", "<:venoms_touch:820536394673160242>");
			emojiMap.put("large_foraging_sack", "<:large_foraging_sack:820536334984544298>");
			emojiMap.put("nether_wart_pouch", "<:nether_wart_pouch:820536335252586536>");
			emojiMap.put("old_dragon_chestplate", "<:old_dragon_chestplate:820536335227027456>");
			emojiMap.put("souls_rebound", "<:souls_rebound:820536394442866739>");
			emojiMap.put("mosquito_bow", "<:mosquito_bow:820536335148122132>");
			emojiMap.put("bat_wand", "<:bat_wand:820536334920843274>");
			emojiMap.put("hoe_of_greater_tilling", "<:hoe_of_greater_tilling:820536335075901500>");
			emojiMap.put("enderman_mask", "<:enderman_mask:820536334967373824>");
			emojiMap.put("biofuel", "<:biofuel:820536335017312266>");
			emojiMap.put("reaper_scythe", "<:reaper_scythe:820536394450993162>");
			emojiMap.put("large_husbandry_sack", "<:large_husbandry_sack:820536335147335721>");
			emojiMap.put("zombie_commander_whip", "<:zombie_commander_whip:820536394790862858>");
			emojiMap.put("dirt_bottle", "<:dirt_bottle:820536334946402304>");
			emojiMap.put("weird_tuba", "<:weird_tuba:820536394770415626>");
			emojiMap.put("onyx", "<:onyx:820536394300391466>");
			emojiMap.put("aspect_of_the_jerry", "<:aspect_of_the_jerry:820536394560307200>");
			emojiMap.put("silent_death", "<:silent_death:820536394295672853>");
			emojiMap.put("hyper_cleaver", "<:hyper_cleaver:820536335030550588>");
			emojiMap.put("basket_of_seeds", "<:basket_of_seeds:820536334753988609>");
			emojiMap.put("reaper_orb", "<:reaper_orb:820536394476290060>");
			emojiMap.put("reaper_mask", "<:reaper_mask:820536394391879681>");
			emojiMap.put("wand_of_restoration", "<:wand_of_restoration:820536394854170654>");
			emojiMap.put("titanium_relic", "<:titanium_relic:820536395084333056>");
			emojiMap.put("the_shredder", "<:the_shredder:820536394891001867>");
			emojiMap.put("dungeon_decoy", "<:dungeon_decoy:820536334958723082>");
			emojiMap.put("gold_bonzo_head", "<:gold_bonzo_head:820536334711521292>");
			emojiMap.put("spooky_pie", "<:spooky_pie:820536394866753546>");
			emojiMap.put("spirit_mask", "<:spirit_mask:820536395130077194>");
			emojiMap.put("shears", "<:shears:820536394136682507>");
			emojiMap.put("dante_talisman", "<:dante_talisman:852390130525798480>");
			emojiMap.put("elegant_tuxedo_chestplate", "<:elegant_tuxedo_chestplate:852389308953264128>");
			emojiMap.put("edible_mace", "<:edible_mace:852382990931460126>");
			emojiMap.put("strong_dragon_chestplate", "<:strong_dragon_chestplate:852389868700172328>");
			emojiMap.put("resistance_chestplate", "<:resistance_chestplate:852385746853429248>");
			emojiMap.put("strong_dragon_leggings", "<:strong_dragon_leggings:852389966658797568>");
			emojiMap.put("recombobulator_3000", "<:recombobulator_3000:852647805813784597>");
			emojiMap.put("royal_pigeon", "<:royal_pigeon:852384070642040852>");
			emojiMap.put("axe_of_the_shredded", "<:axe_of_the_shredded:852382098379767839>");
			emojiMap.put("mastiff_leggings", "<:mastiff_leggings:852386568374976562>");
			emojiMap.put("bat_person_chestplate", "<:bat_person_chestplate:852388656245112872>");
			emojiMap.put("euclid_wheat_hoe_tier_2", "<:euclid_wheat_hoe_tier_2:852384865982480404>");
			emojiMap.put("pigman_sword", "<:pigman_sword:852382052899749929>");
			emojiMap.put("yeti_sword", "<:yeti_sword:852381191092568067>");
			emojiMap.put("bat_person_leggings", "<:bat_person_leggings:852388750616952842>");
			emojiMap.put("farm_suit_chestplate", "<:farm_suit_chestplate:852386221267353610>");
			emojiMap.put("melon_dicer", "<:melon_dicer:852383984918069319>");
			emojiMap.put("end_stone_bow", "<:end_stone_bow:852390188498812949>");
			emojiMap.put("angler_leggings", "<:angler_leggings:852389115328593920>");
			emojiMap.put("silk_edge_sword", "<:silk_edge_sword:852380937063890944>");
			emojiMap.put("elegant_tuxedo_leggings", "<:elegant_tuxedo_leggings:852389387047403550>");
			emojiMap.put("bat_person_boots", "<:bat_person_boots:852388872288731146>");
			emojiMap.put("lasr_eye", "<:lasr_eye:852390268753543168>");
			emojiMap.put("speedster_legs", "<:speedster_legs:852389551438561310>");
			emojiMap.put("staff_of_the_rising_sun", "<:staff_of_the_rising_sun:852384766077829180>");
			emojiMap.put("strong_dragon_boots", "<:strong_dragon_boots:852389989056905246>");
			emojiMap.put("farm_suit_leggings", "<:farm_suit_leggings:852386308449763358>");
			emojiMap.put("angler_chestplate", "<:angler_chestplate:852389047183736904>");
			emojiMap.put("resistance_leggings", "<:resistance_leggings:852385931754995762>");
			emojiMap.put("crown_of_greed", "<:crown_of_greed:852385313531494420>");
			emojiMap.put("farming_for_dummies", "<:farming_for_dummies:852385000916779038>");
			emojiMap.put("wise_dragon_boots", "<:wise_dragon_boots:852387559387430932>");
			emojiMap.put("superior_dragon_chestplate", "<:superior_dragon_chestplate:852390526950965268>");
			emojiMap.put("jacob_ticket", "<:jacob_ticket:852384499493109780>");
			emojiMap.put("resistance_boots", "<:resistance_boots:852385821914431508>");
			emojiMap.put("angler_helmet", "<:angler_helmet:852388975293628468>");
			emojiMap.put("ink_wand", "<:ink_wand:852382154905223189>");
			emojiMap.put("mastiff_chestplate", "<:mastiff_chestplate:852386485700919317>");
			emojiMap.put("mastiff_boots", "<:mastiff_boots:852386609738416188>");
			emojiMap.put("astraea", "<:astraea:852376024805277707>");
			emojiMap.put("recluse_fang", "<:recluse_fang:852385192890204210>");
			emojiMap.put("ranchers_boots", "<:rancher_boots:852386387415793725>");
			emojiMap.put("diamante_handle", "<:diamante_handle:852390352555999282>");
			emojiMap.put("young_dragon_leggings", "<:young_dragon_leggings:852387441217241108>");
			emojiMap.put("stereo_leggings", "<:stereo_leggings:852385077890383922>");
			emojiMap.put("angler_boots", "<:angler_boots:852389175536910366>");
			emojiMap.put("elegant_tuxedo_boots", "<:elegant_tuxedo_boots:852389458980372480>");
			emojiMap.put("titanium_drill_dr_x555", "<:titanium_drill_dr_x555:852384242842730498>");
			emojiMap.put("wise_dragon_chestplate", "<:wise_dragon_chestplate:852386755494936576>");
			emojiMap.put("wand_of_atonement", "<:wand_of_atonement:852383836284125188>");
			emojiMap.put("superior_dragon_leggings", "<:superior_dragon_leggings:852651076011819058>");
			emojiMap.put("sorrow_helmet", "<:sorrow_helmet:852678036360659024>");
			emojiMap.put("reaper_chestplate", "<:reaper_chestplate:852679848748646431>");
			emojiMap.put("mineral_boots", "<:mineral_boots:852681050995294238>");
			emojiMap.put("end_boots", "<:ender_boots:852681141470101574>");
			emojiMap.put("werewolf_chestplate", "<:werewolf_chestplate:852679884119736391>");
			emojiMap.put("frozen_blaze_helmet", "<:frozen_blaze_helmet:852679658209017886>");
			emojiMap.put("reaper_leggings", "<:reaper_leggings:852680410621280326>");
			emojiMap.put("snow_suit_chestplate", "<:snow_chestplate:852680092546105344>");
			emojiMap.put("diamond_sadan_head", "<:diamond_sadan_head:852677584957472789>");
			emojiMap.put("warden_helmet", "<:warden_helmet:852677301338243073>");
			emojiMap.put("shark_scale_chestplate", "<:shark_scale_chestplate:852679793891999757>");
			emojiMap.put("conjuring", "<a:conjuring:852672397383172116>");
			emojiMap.put("shark_scale_boots", "<:shark_scale_boots:852680917415886869>");
			emojiMap.put("sorrow_chestplate", "<:sorrow_chestplate:852673371748696125>");
			emojiMap.put("ember_helmet", "<:ember_helmet:852679751981989938>");
			emojiMap.put("snow_suit_boots", "<:snow_boots:852681016287166474>");
			emojiMap.put("end_leggings", "<:ender_leggings:852680801752973363>");
			emojiMap.put("tarantula_leggings", "<:tarantula_leggings:852680555333287956>");
			emojiMap.put("tarantula_helmet", "<:tarantula_helmet:852679543369105518>");
			emojiMap.put("blaze_boots", "<:blaze_boots:852681100233015336>");
			emojiMap.put("snow_suit_leggings", "<:snow_leggings:852680496948576277>");
			emojiMap.put("end_helmet", "<:end_helmet:852679703713546260>");
			emojiMap.put("young_dragon_helmet", "<:young_dragon_helmet:852678122234708028>");
			emojiMap.put("werewolf_boots", "<:werewolf_boots:852680985715408956>");
			emojiMap.put("blaze_chestplate", "<:blaze_chestplate:852680218320568330>");
			emojiMap.put("tarantula_chestplate", "<:tarantula_chestplate:852680132154097676>");
			emojiMap.put("dragon_leggings", "<:dragon_leggings:852676407082352752>");
			emojiMap.put("shark_scale_leggings", "<:shark_scale_leggings:852680379382628392>");
			emojiMap.put("reaper_boots", "<:reaper_boots:852680948339179530>");
			emojiMap.put("young_dragon_chestplate", "<:young_dragon_chestplate:852673482685022249>");
			emojiMap.put("skin_snowglobe", "<:skin_snowglobe:852679124467056641>");
			emojiMap.put("end_chestplate", "<:ender_chestplate:852680263343144960>");
			emojiMap.put("ember_leggings", "<:ember_leggings:852680872180056076>");
			emojiMap.put("sorrow_leggings", "<:sorrow_leggings:852673678021361664>");
			emojiMap.put("blaze_leggings", "<:blaze_leggings:852680665040027688>");
			emojiMap.put("ember_boots", "<:ember_boots:852681172374388737>");
			emojiMap.put("final_destination_leggings", "<:final_destination_leggings:852677963660656711>");
			emojiMap.put("werewolf_leggings", "<:werewolf_leggings:852680449742471198>");
			emojiMap.put("shark_scale_helmet", "<:shark_scale_helmet:852678601823223838>");
			emojiMap.put("superior_dragon_helmet", "<:superior_dragon_helmet:852677783967891526>");
			emojiMap.put("diamond_necron_head", "<:diamond_necron_head:852677482750279732>");
			emojiMap.put("ember_chestplate", "<:ember_chestplate:852680335664611348>");
			emojiMap.put("young_dragon_boots", "<:young_dragon_boots:852678517128167424>");
			emojiMap.put("sorrow_boots", "<:sorrow_boots:852678462099292161>");
			emojiMap.put("mineral_helmet", "<:mineral_helmet:852679592035614750>");
			emojiMap.put("superior_dragon_boots", "<:superior_dragon_boots:852678370793488436>");
			emojiMap.put("pig_mask", "<:pig_mask:852681460754284544>");
			emojiMap.put("werewolf_helmet", "<:werewolf_helmet:852679047740653578>");
			emojiMap.put("mineral_chestplate", "<:mineral_chestplate:852680167243513897>");
			emojiMap.put("mineral_leggings", "<:mineral_leggings:852680631048994857>");
		}

		itemName = itemName.replace("starred_", "");
		if (emojiMap.containsKey(itemName)) {
			return emojiMap.get(itemName);
		}

		if (!invMissing.contains(itemName)) {
			invMissing += "\n• " + itemName;
		}
		return "❓";
	}

	public double getWeight(JsonElement profile) {
		Weight playerWeight = new Weight(profile, this);
		return playerWeight.getTotalWeight();
	}

	public double getWeight() {
		return getWeight(profileJson);
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultEmbed(
			fixUsername(getUsername()) + (higherDepth(outerProfileJson, "game_mode") != null ? " ♻️" : ""),
			skyblockStatsLink(getUsername(), getProfileName())
		)
			.setThumbnail(getThumbnailUrl());
	}

	public int petLevelFromXp(long petExp, String rarity) {
		int petRarityOffset = higherDepth(getPetJson(), "pet_rarity_offset." + rarity.toUpperCase()).getAsInt();
		JsonArray petLevelsXpPer = higherDepth(getPetJson(), "pet_levels").getAsJsonArray();
		long totalExp = 0;
		for (int i = petRarityOffset; i < petLevelsXpPer.size(); i++) {
			totalExp += petLevelsXpPer.get(i).getAsLong();
			if (totalExp >= petExp) {
				return (Math.min(i - petRarityOffset + 1, 100));
			}
		}
		return 100;
	}

	public int getNumberMinionSlots() {
		try {
			List<String> profileMembers = getJsonKeys(higherDepth(outerProfileJson, "members"));
			Set<String> uniqueCraftedMinions = new HashSet<>();

			for (String member : profileMembers) {
				try {
					JsonArray craftedMinions = higherDepth(outerProfileJson, "members." + member + ".crafted_generators").getAsJsonArray();
					for (JsonElement minion : craftedMinions) {
						uniqueCraftedMinions.add(minion.getAsString());
					}
				} catch (Exception ignored) {}
			}

			int[] craftedMinionsToSlots = new int[] {
				0,
				5,
				15,
				30,
				50,
				75,
				100,
				125,
				150,
				175,
				200,
				225,
				250,
				275,
				300,
				350,
				400,
				450,
				500,
				550,
				600,
			};

			int prevMax = 0;
			for (int i = 0; i < craftedMinionsToSlots.length; i++) {
				if (uniqueCraftedMinions.size() >= craftedMinionsToSlots[i]) {
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
			switch (type) {
				case "slayer":
					highestAmount = Math.max(highestAmount, getTotalSlayer(higherDepth(profile, "members." + this.playerUuid)));
				case "skills":
					highestAmount = Math.max(highestAmount, getSkillAverage(higherDepth(profile, "members." + this.playerUuid)));
					break;
				case "catacombs":
					SkillsStruct cataSkill = getCatacombsSkill(higherDepth(profile, "members." + this.playerUuid));
					if (cataSkill != null) {
						highestAmount = Math.max(highestAmount, getCatacombsLevel(profile));
					}
					break;
				case "weight":
					highestAmount = Math.max(highestAmount, getWeight(higherDepth(profile, "members." + this.playerUuid)));
					break;
				case "svenXp":
				case "revXp":
				case "taraXp":
				case "endermanXp":
					highestAmount =
						Math.max(highestAmount, getSlayer(higherDepth(profile, "members." + this.playerUuid), type.replace("Xp", "")));
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
