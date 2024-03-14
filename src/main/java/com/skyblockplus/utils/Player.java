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

package com.skyblockplus.utils;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.HypixelUtils.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.miscellaneous.LevelSlashCommand;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.miscellaneous.weight.cole.ColeWeight;
import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.skills.CrimsonSlashCommand;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.structs.*;
import com.skyblockplus.utils.utils.StringUtils;
import com.skyblockplus.utils.utils.Utils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.apache.groovy.util.Maps;

public class Player {

	public final Map<Integer, Double> profileToNetworth = new ConcurrentHashMap<>();
	public final Map<Integer, Double> profileToMuseum = new ConcurrentHashMap<>();
	private final List<Profile> profiles = new ArrayList<>();
	private String uuid;
	private String username;
	private int selectedProfileIndex;

	@Getter
	private boolean valid = false;

	private String failCause = "Unknown fail cause";
	private int crystalNucleusAchievement = -2;

	/* Constructors */
	public Player(String username, boolean updateLb) {
		if (checkUsername(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (!response.isValid()) {
				failCause = response.failCause();
				return;
			}

			populateProfiles(response.response());
			findProfileBySelected();
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(getSelectedProfile());
		}
	}

	public Player(String username, String profileName, boolean updateLb) {
		if (checkUsername(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (!response.isValid()) {
				failCause = response.failCause();
				return;
			}

			populateProfiles(response.response());
			if (findProfileByName(profileName)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(getSelectedProfile());
		}
	}

	public Player(String username, String uuid, JsonElement profileArray, boolean updateLb) {
		if (uuid == null || username == null || profileArray == null) {
			return;
		}

		this.uuid = uuid;
		this.username = username;

		try {
			populateProfiles(profileArray);
			findProfileBySelected();
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(getSelectedProfile());
		}
	}

	public Player(String username, String uuid, String profileName, JsonElement profileArray, boolean updateLb) {
		if (uuid == null || username == null || profileArray == null) {
			return;
		}

		this.uuid = uuid;
		this.username = username;

		try {
			populateProfiles(profileArray);
			if (findProfileByName(profileName)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (updateLb) {
			leaderboardDatabase.insertIntoLeaderboard(getSelectedProfile());
		}
	}

	/**
	 * Ignores profile cache and updates provided gamemode synchronously. Used only for leaderboard
	 * command
	 */
	public Player(String username, Gamemode gamemode) {
		if (checkUsername(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid, false, true);
			if (!response.isValid()) {
				failCause = response.failCause();
				return;
			}

			populateProfiles(response.response());
			findProfileBySelected();
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		leaderboardDatabase.insertIntoLeaderboardSync(getSelectedProfile(), gamemode);
	}

	public static Profile create(String username) {
		return create(username, null);
	}

	public static Profile create(String username, String profileName) {
		return (profileName != null ? new Player(username, profileName, true) : new Player(username, true)).getSelectedProfile();
	}

	/**
	 * @return true if invalid
	 */
	private boolean checkUsername(String username) {
		UsernameUuidStruct response = ApiHandler.usernameToUuid(username);
		if (!response.isValid()) {
			failCause = response.failCause();
			return true;
		}

		this.username = response.username();
		this.uuid = response.uuid();
		return false;
	}

	/**
	 * @return true if invalid
	 */
	private boolean findProfileByName(String profileName) {
		for (Profile profile : profiles) {
			if (profile.getProfileName().equalsIgnoreCase(profileName)) {
				this.selectedProfileIndex = profile.getProfileIndex();
				return false;
			}
		}

		if (failCause.equals("Unknown fail cause")) {
			failCause =
				"Invalid profile. Did you mean " +
				getClosestMatch(profileName, profiles.stream().map(Profile::getProfileName).toList()) +
				"?";
		}
		return true;
	}

	private void findProfileBySelected() {
		for (Profile profile : profiles) {
			if (higherDepth(profile.getOuterProfileJson(), "selected", false)) {
				this.selectedProfileIndex = profile.getProfileIndex();
				break;
			}
		}
	}

	private void populateProfiles(JsonElement profileElement) {
		JsonArray profileArray = profileElement.getAsJsonArray();
		for (int i = 0; i < profileArray.size(); i++) {
			this.profiles.add(new Profile(i, profileArray.get(i)));
		}
	}

	/**
	 * @return selected profile if valid else invalid profile
	 */
	public Profile getSelectedProfile() {
		if (valid) {
			return profiles.get(selectedProfileIndex);
		} else {
			return new Profile(-1, null);
		}
	}

	@Override
	public String toString() {
		return (
			"Player{" +
			"valid=" +
			valid +
			", uuid='" +
			uuid +
			'\'' +
			", username='" +
			username +
			'\'' +
			", profile='" +
			(valid ? getSelectedProfile().getProfileName() : null) +
			'\'' +
			'}'
		);
	}

	public enum Gamemode {
		ALL,
		REGULAR,
		STRANDED,
		IRONMAN,
		IRONMAN_STRANDED,
		SELECTED;

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

		public String toLeaderboardName() {
			return (
				switch (this) {
					case IRONMAN, STRANDED, SELECTED -> name().toLowerCase();
					default -> "all";
				} +
				"_lb"
			);
		}

		public boolean isGamemode(Gamemode mode) {
			if (this == IRONMAN_STRANDED) {
				return mode == IRONMAN || mode == STRANDED;
			}

			return this == ALL || this == mode;
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

		public String getName() {
			return switch (this) {
				case IRONMAN, STRANDED -> capitalizeString(name());
				default -> "Regular";
			};
		}
	}

	public enum WeightType {
		NONE,
		SENITHER,
		LILY;

		public static WeightType of(String name) {
			return valueOf(name.toUpperCase());
		}
	}

	public class Profile {

		@Getter
		private final int profileIndex;

		private final JsonElement profileJson;

		@Getter
		private final String profileName;

		private HypixelResponse museumResponse;

		public Profile(int profileIndex, JsonElement profileJson) {
			this.profileJson = profileJson;
			this.profileIndex = profileIndex;
			this.profileName = higherDepth(profileJson, "cute_name", null);
		}

		/* Getters */
		public Player getOuter() {
			return Player.this;
		}

		public Map<Integer, Double> getProfileToNetworth() {
			return profileToNetworth;
		}

		public String getUsername() {
			return username;
		}

		public String getEscapedUsername() {
			return escapeText(username);
		}

		public String getUuid() {
			return uuid;
		}

		public List<Profile> getProfiles() {
			return profiles;
		}

		public boolean isValid() {
			return valid;
		}

		public String getFailCause() {
			return failCause;
		}

		public EmbedBuilder getErrorEmbed() {
			return Utils.errorEmbed(failCause);
		}

		public JsonElement profileJson() {
			return higherDepth(profileJson, "members." + uuid);
		}

		public boolean isSelected() {
			return profileIndex == selectedProfileIndex;
		}

		public JsonElement getOuterProfileJson() {
			return profileJson;
		}

		public List<String> getMatchingProfileNames(Gamemode gamemode) {
			List<String> profileNames = new ArrayList<>();

			for (Profile profile : profiles) {
				try {
					if (gamemode.isGamemode(profile.getGamemode())) {
						profileNames.add(profile.getProfileName());
					}
				} catch (Exception ignored) {}
			}

			return profileNames;
		}

		public String itemToEmoji(String itemName) {
			return Utils.getEmoji(itemName.toUpperCase(), "❓");
		}

		public int getCrystalNucleusAchievements() {
			if (crystalNucleusAchievement == -2) {
				crystalNucleusAchievement = higherDepth(playerFromUuid(uuid).response(), "achievements.skyblock_crystal_nucleus", -1);
			}

			return crystalNucleusAchievement;
		}

		public double getAmount(String type, boolean useHighest) {
			return useHighest ? getHighestAmount(type, Gamemode.ALL) : getAmount(getSelectedProfile(), type);
		}

		public static double getAmount(Profile profile, String type) {
			return switch (type) {
				case "selected_class" -> DUNGEON_CLASS_NAMES.indexOf(profile.getSelectedDungeonClass());
				case "gamemode" -> profile.getGamemode().ordinal();
				case "emblem" -> new ArrayList<>(EMBLEM_NAME_TO_ICON.keySet()).indexOf(profile.getEmblem());
				case "farming_cap" -> profile.getFarmingCapUpgrade();
				case "slayer", "total_slayer" -> profile.getTotalSlayerXp();
				case "skills" -> profile.getSkillAverage();
				case "skills_xp" -> profile.getTotalSkillsXp();
				case "catacombs" -> profile.getCatacombs().getProgressLevel();
				case "catacombs_xp" -> profile.getCatacombsXp();
				case "healer", "mage", "berserk", "archer", "tank" -> profile.getDungeonClass(type).getProgressLevel();
				case "weight" -> profile.getWeight();
				case "wolf", "zombie", "spider", "enderman", "blaze", "vampire" -> profile.getSlayerXp(type);
				case "alchemy", "combat", "fishing", "farming", "foraging", "carpentry", "mining", "taming", "enchanting", "social" -> {
					SkillsStruct skillsStruct = profile.getSkill(type);
					yield skillsStruct != null ? skillsStruct.getProgressLevel() : -1;
				}
				case "alchemy_xp",
					"combat_xp",
					"fishing_xp",
					"farming_xp",
					"foraging_xp",
					"carpentry_xp",
					"mining_xp",
					"taming_xp",
					"enchanting_xp",
					"social_xp" -> profile.getSkillXp(type.split("_xp")[0]);
				case "healer_xp", "mage_xp", "berserk_xp", "archer_xp", "tank_xp" -> profile.getDungeonClassXp(type.split("_xp")[0]);
				case "hotm" -> profile.getHOTM() != null ? profile.getHOTM().totalExp() : -1;
				case "bank" -> profile.getBankBalance();
				case "purse" -> profile.getPurseCoins();
				case "coins" -> Math.max(0, profile.getBankBalance()) + profile.getPurseCoins();
				case "pet_score" -> profile.getPetScore();
				case "networth" -> profile.getNetworth();
				case "museum" -> profile.getMuseumWorth();
				case "museum_hypixel" -> profile.getHypixelMuseumWorth();
				case "fairy_souls" -> profile.getFairySouls();
				case "minion_slots" -> profile.getNumberMinionSlots();
				case "dungeon_secrets" -> profile.getDungeonSecrets();
				case "maxed_slayers" -> profile.getNumMaxedSlayers();
				case "maxed_collections" -> profile.getNumMaxedCollections();
				case "class_average" -> profile.getDungeonClassAverage();
				case "mage_reputation" -> profile.getMageRep();
				case "barbarian_reputation" -> profile.getBarbarianRep();
				case "lily_weight" -> profile.getLilyWeight();
				case "lily_slayer_weight" -> new LilyWeight(profile, true).getSlayerWeight().getWeightStruct().getRaw();
				case "cole_weight" -> profile.getColeWeight();
				case "bestiary" -> profile.getBestiaryLevel();
				case "level" -> profile.getLevel();
				default -> {
					if (collectionNameToId.containsKey(type)) {
						yield profile.getCollection(collectionNameToId.get(type));
					} else if (skyblockStats.contains(type)) {
						yield profile.getStat(type);
					} else {
						yield -1;
					}
				}
			};
		}

		public double getHighestAmount(String type) {
			return getHighestAmount(type, Gamemode.ALL);
		}

		public double getHighestAmount(String type, Gamemode gamemode) {
			double highestAmount = -1.0;

			for (Profile profile : profiles) {
				if (gamemode == Gamemode.SELECTED ? profile.isSelected() : gamemode.isGamemode(profile.getGamemode())) {
					highestAmount = Math.max(getAmount(profile, type), highestAmount);
				}
			}

			return highestAmount;
		}

		/* Links */
		public String skyblockStatsLink() {
			return StringUtils.skyblockStatsLink(uuid, getProfileName());
		}

		public String getAuctionUrl() {
			return StringUtils.getAuctionUrl(uuid);
		}

		public String getAvatarUrl() {
			return StringUtils.getAvatarUrl(uuid);
		}

		/* Bank and purse */

		/**
		 * @return Bank balance or -1 if bank API disabled
		 */
		public double getBankBalance() {
			return higherDepth(getOuterProfileJson(), "banking.balance", -1.0);
		}

		public double getPurseCoins() {
			double purseCoins = higherDepth(profileJson(), "currencies.coin_purse", 0.0);
			// How are people able to have 9E-70 coins??
			return purseCoins < 0.01 ? 0 : purseCoins;
		}

		public JsonArray getBankHistory() {
			try {
				return higherDepth(getOuterProfileJson(), "banking.transactions").getAsJsonArray();
			} catch (Exception e) {
				return null;
			}
		}

		/* Skills */
		public int getFarmingCapUpgrade() {
			return higherDepth(profileJson(), "jacobs_contest.perks.farming_level_cap", 0);
		}

		public int getSkillMaxLevel(String skillName, WeightType weightType) {
			if (weightType == WeightType.LILY) {
				return 60;
			}

			int maxLevel = higherDepth(getLevelingJson(), "leveling_caps." + skillName, 50);

			if (skillName.equals("farming")) {
				maxLevel = weightType == WeightType.SENITHER ? 60 : maxLevel + getFarmingCapUpgrade();
			}

			return maxLevel;
		}

		public long getSkillXp(String skillName) {
			return higherDepth(profileJson(), "player_data.experience.SKILL_" + skillName.toUpperCase(), -1L);
		}

		public SkillsStruct getSkill(String skillName) {
			return getSkill(skillName, WeightType.NONE);
		}

		public SkillsStruct getSkill(String skillName, WeightType weightType) {
			long skillXp = getSkillXp(skillName);
			if (skillXp == -1) {
				return null;
			}

			return skillInfoFromExp(skillXp, skillName, weightType);
		}

		public double getSkillAverage() {
			return getSkillAverage("", 0);
		}

		public long getTotalSkillsXp() {
			long totalXp = 0;
			for (String skill : SKILL_NAMES) {
				long skillXp = getSkillXp(skill);
				if (skillXp == -1) {
					return -1;
				}
			}
			return totalXp;
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
			return levelingInfoFromLevel(targetLevel, skill, getSkillMaxLevel(skill, WeightType.NONE));
		}

		public SkillsStruct getHOTM() {
			long xp = higherDepth(profileJson(), "mining_core.experience", -1L);
			return xp == -1 ? null : skillInfoFromExp(xp, "HOTM");
		}

		/* Slayer */
		public int getTotalSlayerXp() {
			return getTotalSlayerXp("", 0);
		}

		public int getTotalSlayerXp(String type, int overrideAmount) {
			int totalSlayer = 0;
			for (String slayerName : SLAYER_NAMES) {
				if (slayerName.equals(type)) {
					totalSlayer += overrideAmount;
				} else {
					totalSlayer += getSlayerXp(slayerName);
				}
			}
			return totalSlayer;
		}

		public int getSlayerBossKills(String slayerName, int tier) {
			return higherDepth(profileJson(), "slayer.slayer_bosses." + slayerName + ".boss_kills_tier_" + tier, 0);
		}

		public int getSlayerXp(String slayerName) {
			return higherDepth(profileJson(), "slayer.slayer_bosses." + slayerName + ".xp", 0);
		}

		public int getSlayerLevel(String slayerName) {
			return slayerLevelFromXp(slayerName, getSlayerXp(slayerName));
		}

		/* Dungeons */
		public int getDungeonSecrets() {
			return higherDepth(profileJson(), "dungeons.secrets", 0);
		}

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

		public Set<String> getItemsPlayerHas(List<String> items) {
			Map<Integer, InvItem> invItemMap = getInventoryMap();
			if (invItemMap == null) {
				return null;
			}

			Collection<InvItem> itemsMap = new ArrayList<>(invItemMap.values());
			itemsMap.addAll(new ArrayList<>(getEnderChestMap().values()));
			Map<Integer, InvItem> storageMap = getStorageMap();
			if (storageMap != null) {
				itemsMap.addAll(new ArrayList<>(getStorageMap().values()));
			}

			Set<String> itemsPlayerHas = new HashSet<>();
			for (InvItem item : itemsMap) {
				if (item != null) {
					if (!item.getBackpackItems().isEmpty()) {
						for (InvItem backpackItem : item.getBackpackItems()) {
							if (backpackItem != null && items.contains(backpackItem.getId())) {
								itemsPlayerHas.add(backpackItem.getId());
							}
						}
					} else if (items.contains(item.getId())) {
						itemsPlayerHas.add(item.getId());
					}
				}
			}

			return itemsPlayerHas;
		}

		public long getDungeonClassXp(String className) {
			return higherDepth(profileJson(), "dungeons.player_classes." + className + ".experience", 0L);
		}

		public SkillsStruct getDungeonClass(String className) {
			return skillInfoFromExp(getDungeonClassXp(className), className);
		}

		public double getDungeonClassAverage() {
			double dungeonClassAverage = 0;
			for (String className : DUNGEON_CLASS_NAMES) {
				try {
					dungeonClassAverage += getDungeonClass(className).getProgressLevel();
				} catch (Exception e) {
					return -1;
				}
			}
			return dungeonClassAverage / DUNGEON_CLASS_NAMES.size();
		}

		public long getCatacombsXp() {
			return higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.experience", 0L);
		}

		public SkillsStruct getCatacombs() {
			return skillInfoFromExp(getCatacombsXp(), "catacombs");
		}

		/* InvItem maps */
		public Map<Integer, InvItem> getInventoryMap() {
			return getInventoryMap(false);
		}

		public Map<Integer, InvItem> getInventoryMap(boolean sort) {
			try {
				String contents = higherDepth(profileJson(), "inventory.inv_contents.data").getAsString();
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
				String contents = higherDepth(profileJson(), "inventory.personal_vault_contents.data").getAsString();
				NBTCompound parsedContents = NBTReader.readBase64(contents);
				return getGenericInventoryMap(parsedContents);
			} catch (Exception ignored) {}
			return null;
		}

		public Map<Integer, InvItem> getStorageMap() {
			try {
				Map<Integer, InvItem> storageMap = new HashMap<>();
				int counter = 1;
				for (Map.Entry<String, JsonElement> bp : higherDepth(profileJson(), "inventory.backpack_contents")
					.getAsJsonObject()
					.entrySet()) {
					Collection<InvItem> curBpMap = getGenericInventoryMap(
						NBTReader.readBase64(higherDepth(bp.getValue(), "data").getAsString())
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

		public Map<Integer, InvItem> getMuseumMap() {
			try {
				Map<Integer, InvItem> museumMap = new HashMap<>();
				int counter = 0;

				HypixelResponse museumResponse = getMuseum();
				JsonElement museumJson = museumResponse.get(uuid);

				JsonElement items = higherDepth(museumJson, "items");
				if (items != null) {
					for (Map.Entry<String, JsonElement> entry : items.getAsJsonObject().entrySet()) {
						if (!higherDepth(entry.getValue(), "borrowing", false)) {
							String contents = higherDepth(entry.getValue(), "items.data").getAsString();
							NBTCompound parsedContents = NBTReader.readBase64(contents);
							for (Map.Entry<Integer, InvItem> parsedItem : getGenericInventoryMap(parsedContents).entrySet()) {
								museumMap.put(counter, parsedItem.getValue());
								counter++;
							}
						}
					}
				}

				JsonElement special = higherDepth(museumJson, "special");
				if (special != null) {
					for (JsonElement item : special.getAsJsonArray()) {
						String contents = higherDepth(item, "items.data").getAsString();
						NBTCompound parsedContents = NBTReader.readBase64(contents);
						for (Map.Entry<Integer, InvItem> parsedItem : getGenericInventoryMap(parsedContents).entrySet()) {
							museumMap.put(counter, parsedItem.getValue());
							counter++;
						}
					}
				}

				return museumMap;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public Map<Integer, InvItem> getTalismanBagMap() {
			try {
				String contents = higherDepth(profileJson(), "inventory.bag_contents.talisman_bag.data").getAsString();
				NBTCompound parsedContents = NBTReader.readBase64(contents);
				return getGenericInventoryMap(parsedContents);
			} catch (Exception ignored) {}
			return null;
		}

		public Map<Integer, InvItem> getEquipmentMap() {
			try {
				String contents = higherDepth(profileJson(), "inventory.equipment_contents.data").getAsString();
				NBTCompound parsedContents = NBTReader.readBase64(contents);
				return getGenericInventoryMap(parsedContents);
			} catch (Exception ignored) {}
			return null;
		}

		public Map<Integer, InvItem> getArmorMap() {
			try {
				String contents = higherDepth(profileJson(), "inventory.inv_armor.data").getAsString();
				NBTCompound parsedContents = NBTReader.readBase64(contents);
				Map<Integer, InvItem> oldMap = getGenericInventoryMap(parsedContents);
				Map<Integer, InvItem> orderedMap = new HashMap<>();
				orderedMap.put(0, oldMap.getOrDefault(3, null));
				orderedMap.put(1, oldMap.getOrDefault(2, null));
				orderedMap.put(2, oldMap.getOrDefault(1, null));
				orderedMap.put(3, oldMap.getOrDefault(0, null));
				return orderedMap;
			} catch (Exception ignored) {}
			return null;
		}

		public Map<Integer, InvItem> getWardrobeMap() {
			try {
				String contents = higherDepth(profileJson(), "inventory.wardrobe_contents.data").getAsString();
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
				String contents = higherDepth(profileJson(), "inventory.ender_chest_contents.data").getAsString();
				NBTCompound parsedContents = NBTReader.readBase64(contents);
				return getGenericInventoryMap(parsedContents);
			} catch (Exception ignored) {}
			return null;
		}

		public Map<String, Integer> getPlayerSacks() {
			try {
				JsonObject sacksJson = higherDepth(profileJson(), "inventory.sacks_counts").getAsJsonObject();
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
				NBTList items = NBTReader
					.readBase64(higherDepth(profileJson(), "inventory.bag_contents.talisman_bag.data").getAsString())
					.getList("i");
				Map<Integer, String> itemsMap = new TreeMap<>();

				for (int i = 0; i < items.size(); i++) {
					itemsMap.put(i + 1, items.getCompound(i).getString("tag.ExtraAttributes.id", "EMPTY"));
				}

				if (itemsMap.size() % 45 != 0) {
					int toAdd = 45 - (itemsMap.size() % 45);
					int initialSize = itemsMap.size();
					for (int i = 0; i < toAdd; i++) {
						itemsMap.put(initialSize + 1 + i, "BLANK");
					}
				}

				List<String[]> pages = new ArrayList<>();
				int page = 0;
				StringBuilder pageTop = new StringBuilder();
				StringBuilder pageBottom = new StringBuilder();
				StringBuilder row = new StringBuilder();
				for (Map.Entry<Integer, String> i : itemsMap.entrySet()) {
					row.append(itemToEmoji(i.getValue()));

					if (i.getKey() % 9 == 0) {
						if (i.getKey() - page <= 27) {
							pageTop.append(row).append("\n");
						} else {
							pageBottom.append(row).append("\n");
						}
						row = new StringBuilder();
					}

					if (i.getKey() != 0 && i.getKey() % 45 == 0) {
						pages.add(new String[] { pageTop.toString(), pageBottom.toString() });
						pageTop = new StringBuilder();
						pageBottom = new StringBuilder();
						page += 45;
					}
				}

				return pages;
			} catch (Exception ignored) {}
			return null;
		}

		public List<String[]> getEnderChest() {
			try {
				NBTList items = NBTReader
					.readBase64(higherDepth(profileJson(), "inventory.ender_chest_contents.data").getAsString())
					.getList("i");
				Map<Integer, String> itemsMap = new TreeMap<>();

				for (int i = 0; i < items.size(); i++) {
					String id = items.getCompound(i).getString("tag.ExtraAttributes.id", "EMPTY");

					if (id.equals("PET")) {
						String petInfoStr = items.getCompound(i).getString("tag.ExtraAttributes.petInfo");
						if (petInfoStr != null) {
							JsonElement petInfo = JsonParser.parseString(petInfoStr);
							id = higherDepth(petInfo, "type", null) + ";" + RARITY_TO_NUMBER_MAP.get(higherDepth(petInfo, "tier", null));
						}
					} else if (id.equals("ENCHANTED_BOOK")) {
						NBTCompound enchants = items.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
						if (enchants.size() == 1) {
							Map.Entry<String, Object> enchant = enchants.entrySet().iterator().next();
							id = enchant.getKey() + ";" + enchant.getValue();
						}
					}

					itemsMap.put(i + 1, id.toUpperCase());
				}

				List<String[]> pages = new ArrayList<>();
				int page = 0;
				StringBuilder pageTop = new StringBuilder();
				StringBuilder pageBottom = new StringBuilder();
				StringBuilder row = new StringBuilder();
				for (Map.Entry<Integer, String> i : itemsMap.entrySet()) {
					row.append(itemToEmoji(i.getValue()));

					if (i.getKey() % 9 == 0) {
						if (i.getKey() - page <= 27) {
							pageTop.append(row).append("\n");
						} else {
							pageBottom.append(row).append("\n");
						}
						row = new StringBuilder();
					}

					if (i.getKey() != 0 && i.getKey() % 45 == 0) {
						pages.add(new String[] { pageTop.toString(), pageBottom.toString() });
						pageTop = new StringBuilder();
						pageBottom = new StringBuilder();
						page += 45;
					}
				}

				return pages;
			} catch (Exception ignored) {}
			return null;
		}

		public List<String[]> getStorage() {
			try {
				List<String[]> pages = new ArrayList<>();

				for (JsonElement page : higherDepth(profileJson(), "inventory.backpack_contents")
					.getAsJsonObject()
					.entrySet()
					.stream()
					.sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())))
					.map(Map.Entry::getValue)
					.collect(Collectors.toCollection(ArrayList::new))) {
					NBTList items = NBTReader.readBase64(higherDepth(page, "data").getAsString()).getList("i");
					Map<Integer, String> itemsMap = new TreeMap<>();

					for (int i = 0; i < items.size(); i++) {
						String id = items.getCompound(i).getString("tag.ExtraAttributes.id", "EMPTY");

						if (id.equals("PET")) {
							String petInfoStr = items.getCompound(i).getString("tag.ExtraAttributes.petInfo");
							if (petInfoStr != null) {
								JsonElement petInfo = JsonParser.parseString(petInfoStr);
								id =
									higherDepth(petInfo, "type", null) + ";" + RARITY_TO_NUMBER_MAP.get(higherDepth(petInfo, "tier", null));
							}
						} else if (id.equals("ENCHANTED_BOOK")) {
							NBTCompound enchants = items.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
							if (enchants.size() == 1) {
								Map.Entry<String, Object> enchant = enchants.entrySet().iterator().next();
								id = enchant.getKey() + ";" + enchant.getValue();
							}
						}

						itemsMap.put(i + 1, id.toUpperCase());
					}

					if (items.size() < 27) {
						int curSize = items.size();
						for (int i = 0; i < 27 - curSize; i++) {
							itemsMap.put(i + 1 + curSize, "BLANK");
						}
					}

					StringBuilder pageTop = new StringBuilder();
					StringBuilder pageBottom = new StringBuilder();
					StringBuilder row = new StringBuilder();
					for (Map.Entry<Integer, String> i : itemsMap.entrySet()) {
						row.append(itemToEmoji(i.getValue()));

						if (i.getKey() % 9 == 0) {
							if (i.getKey() <= 18) {
								pageTop.append(row).append("\n");
							} else {
								pageBottom.append(row).append("\n");
							}
							row = new StringBuilder();
						}
					}

					pages.add(new String[] { pageTop.toString(), pageBottom.toString() });
				}

				return pages;
			} catch (Exception ignored) {}
			return null;
		}

		public String[] getInventory() {
			try {
				NBTList items = NBTReader.readBase64(higherDepth(profileJson(), "inventory.inv_contents.data").getAsString()).getList("i");
				Map<Integer, String> itemsMap = new TreeMap<>();

				for (int i = 0; i < items.size(); i++) {
					String id = items.getCompound(i).getString("tag.ExtraAttributes.id", "EMPTY");

					if (id.equals("PET")) {
						String petInfoStr = items.getCompound(i).getString("tag.ExtraAttributes.petInfo");
						if (petInfoStr != null) {
							JsonElement petInfo = JsonParser.parseString(petInfoStr);
							id = higherDepth(petInfo, "type", null) + ";" + RARITY_TO_NUMBER_MAP.get(higherDepth(petInfo, "tier", null));
						}
					} else if (id.equals("ENCHANTED_BOOK")) {
						NBTCompound enchants = items.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
						if (enchants.size() == 1) {
							Map.Entry<String, Object> enchant = enchants.entrySet().iterator().next();
							id = enchant.getKey() + ";" + enchant.getValue();
						}
					}

					itemsMap.put(i + 1, id.toUpperCase());
				}

				StringBuilder pageTop = new StringBuilder();
				StringBuilder pageBottom = new StringBuilder();
				StringBuilder row = new StringBuilder();
				for (Map.Entry<Integer, String> i : itemsMap.entrySet()) {
					row.append(itemToEmoji(i.getValue()));

					if (i.getKey() % 9 == 0) {
						if (i.getKey() <= 9 || i.getKey() >= 28) {
							pageTop.insert(0, row + "\n");
						} else {
							pageBottom.append(row).append("\n");
						}
						row = new StringBuilder();
					}
				}

				return new String[] { pageBottom.toString(), pageTop.toString() };
			} catch (Exception ignored) {}
			return null;
		}

		public Map<Integer, ArmorStruct> getWardrobeList() {
			try {
				int equippedSlot = higherDepth(profileJson(), "inventory.wardrobe_equipped_slot", -1);
				NBTList items = NBTReader
					.readBase64(higherDepth(profileJson(), "inventory.wardrobe_contents.data").getAsString())
					.getList("i");
				Map<Integer, String> itemsMap = new HashMap<>();

				for (int i = 0; i < items.size(); i++) {
					itemsMap.put(i, cleanMcCodes(items.getCompound(i).getString("tag.display.Name", "Empty")));
				}

				Map<Integer, ArmorStruct> armorStructMap = new HashMap<>();
				for (int i = 0; i < 9; i++) {
					ArmorStruct pageOneStruct = new ArmorStruct();
					for (int j = i; j < itemsMap.size() / 2; j += 9) {
						String currentArmorPiece = itemsMap.get(j);
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
					for (int j = itemsMap.size() / 2 + i; j < itemsMap.size(); j += 9) {
						String currentArmorPiece = itemsMap.get(j);
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

				if (equippedSlot != -1) {
					armorStructMap.replace(equippedSlot - 1, getArmor().makeBold());
				}

				return armorStructMap;
			} catch (Exception e) {
				return null;
			}
		}

		public List<String[]> getWardrobe() {
			try {
				int equippedWardrobeSlot = higherDepth(profileJson(), "inventory.wardrobe_equipped_slot", -1);
				Map<Integer, InvItem> equippedArmor = equippedWardrobeSlot != -1 ? getArmorMap() : null;
				NBTList items = NBTReader
					.readBase64(higherDepth(profileJson(), "inventory.wardrobe_contents.data").getAsString())
					.getList("i");
				Map<Integer, String> itemsMap = new TreeMap<>();

				for (int i = 0; i < items.size(); i++) {
					String id = items.getCompound(i).getString("tag.ExtraAttributes.id", "EMPTY");

					if (equippedArmor != null && (i + 1 - equippedWardrobeSlot) % 9 == 0) {
						if (equippedWardrobeSlot <= 9 && (i + 1 <= 36) && equippedArmor.get((i + 1 - equippedWardrobeSlot) / 9) != null) {
							id = equippedArmor.get((i + 1 - equippedWardrobeSlot) / 9).getId();
						} else if (
							equippedWardrobeSlot > 9 && (i + 1 > 36) && equippedArmor.get((i + 1 - equippedWardrobeSlot) / 9 - 3) != null
						) {
							id = equippedArmor.get((i + 1 - equippedWardrobeSlot) / 9 - 3).getId();
						}
					}

					itemsMap.put(i + 1, id);
				}

				if (itemsMap.size() % 36 != 0) {
					int toAdd = 36 - (itemsMap.size() % 36);
					int initialSize = itemsMap.size();
					for (int i = 0; i < toAdd; i++) {
						itemsMap.put(initialSize + 1 + i, "BLANK");
					}
				}

				List<String[]> pages = new ArrayList<>();
				int page = 0;
				StringBuilder pageTop = new StringBuilder();
				StringBuilder pageBottom = new StringBuilder();
				StringBuilder row = new StringBuilder();
				for (Map.Entry<Integer, String> i : itemsMap.entrySet()) {
					row.append(itemToEmoji(i.getValue()));

					if (i.getKey() % 9 == 0) {
						if (i.getKey() - page <= 18) {
							pageTop.append(row).append("\n");
						} else {
							pageBottom.append(row).append("\n");
						}
						row = new StringBuilder();
					}

					if (i.getKey() != 0 && i.getKey() % 36 == 0) {
						pages.add(new String[] { pageTop.toString(), pageBottom.toString() });
						pageTop = new StringBuilder();
						pageBottom = new StringBuilder();
						page += 36;
					}
				}

				return pages;
			} catch (Exception ignored) {}
			return null;
		}

		public ArmorStruct getArmor() {
			try {
				NBTList items = NBTReader.readBase64(higherDepth(profileJson(), "inventory.inv_armor.data").getAsString()).getList("i");
				Map<Integer, String> itemsMap = new HashMap<>();

				for (int i = 0; i < items.size(); i++) {
					itemsMap.put(i, cleanMcCodes(items.getCompound(i).getString("tag.display.Name", "Empty")));
				}

				return new ArmorStruct(itemsMap.get(3), itemsMap.get(2), itemsMap.get(1), itemsMap.get(0));
			} catch (Exception ignored) {}
			return null;
		}

		public JsonArray getPets() {
			return higherDepth(profileJson(), "pets_data.pets").getAsJsonArray();
		}

		/* Miscellaneous */
		public String getEmblem() {
			return higherDepth(profileJson(), "leveling.selected_symbol", "none");
		}

		public int getFairySouls() {
			return higherDepth(profileJson(), "fairy_soul.total_collected", 0);
		}

		public double getExactLevel() {
			return higherDepth(profileJson(), "leveling.experience", 0) / 100.0;
		}

		public double getEstimatedLevel() {
			return (
				(LevelSlashCommand.getCoreTasksEmbed(this).total() +
					LevelSlashCommand.getEventTasksEmbed(this).total() +
					LevelSlashCommand.getDungeonTasks(this).total() +
					LevelSlashCommand.getEssenceShopTasks(this).total() +
					LevelSlashCommand.getSlayingTasks(this).total() +
					LevelSlashCommand.getSkillRelatedTasks(this).total() +
					LevelSlashCommand.getMiscellaneousTasks(this).total() +
					LevelSlashCommand.getStoryTasks(this).total()) /
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
				.filter(o -> o != null && RARITY_TO_NUMBER_MAP.containsKey(o.getRarity()))
				.sorted(Comparator.comparingInt(o -> -RARITY_TO_NUMBER_MAP.get(o.getRarity())))
				.collect(Collectors.toCollection(ArrayList::new));

			Set<String> ignoredAccessories = new HashSet<>();
			boolean countedPartyHat = false;
			int magicPower = 0;

			// Accessories are sorted from highest to lowest rarity
			// in case they have children accessories with lower rarities
			for (InvItem accessory : accessoryBag) {
				String accessoryId = accessory.getId();
				if (ignoredAccessories.contains(accessoryId)) {
					continue;
				}

				ignoredAccessories.add(accessoryId);
				JsonElement children = higherDepth(getParentsJson(), accessoryId);
				if (children != null) {
					for (JsonElement child : children.getAsJsonArray()) {
						ignoredAccessories.add(child.getAsString());
					}
				}

				if (accessoryId.equals("HEGEMONY_ARTIFACT")) {
					magicPower += rarityToMagicPower.get(accessory.getRarity());
				} else if (accessoryId.equals("ABICASE")) {
					JsonElement activeContacts = higherDepth(profileJson(), "nether_island_player_data.abiphone.active_contacts");
					if (activeContacts != null) {
						magicPower += activeContacts.getAsJsonArray().size() / 2;
					}
				} else if (accessoryId.startsWith("PARTY_HAT")) {
					if (countedPartyHat) {
						// Only one party hat counts towards magic power
						continue;
					} else {
						countedPartyHat = true;
					}
				}

				magicPower += rarityToMagicPower.get(accessory.getRarity());
			}

			if (higherDepth(profileJson(), "rift.access.consumed_prism", false)) {
				magicPower += 11;
			}

			return magicPower;
		}

		public double getColeWeight() {
			return new ColeWeight(this).getTotalWeight();
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
			return defaultEmbed(escapeText(getUsername()) + getSymbol(" ") + extra, skyblockStatsLink()).setThumbnail(getAvatarUrl());
		}

		public CustomPaginator.Builder defaultPlayerPaginator(User... users) {
			return defaultPlayerPaginator(PaginatorExtras.PaginatorType.DEFAULT, users);
		}

		public CustomPaginator.Builder defaultPlayerPaginator(PaginatorExtras.PaginatorType type, User... users) {
			return defaultPaginator(users)
				.updateExtras(extra ->
					extra
						.setType(type)
						.setEveryPageTitle(escapeText(getUsername()) + getSymbol(" "))
						.setEveryPageThumbnail(getAvatarUrl())
						.setEveryPageTitleUrl(skyblockStatsLink())
				);
		}

		public int getNumberMinionSlots() {
			try {
				Set<String> uniqueCraftedMinions = new HashSet<>();

				for (Map.Entry<String, JsonElement> member : higherDepth(getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
					try {
						JsonArray craftedMinions = higherDepth(member.getValue(), "player_data.crafted_generators").getAsJsonArray();
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

			Map<String, Integer> highestRarity = new HashMap<>();
			List<String> highestLevel = new ArrayList<>();
			for (JsonElement pet : playerPets) {
				String petName = higherDepth(pet, "type").getAsString();
				String rarity = higherDepth(pet, "tier").getAsString();

				int rarityInt =
					switch (rarity) {
						case "COMMON" -> 1;
						case "UNCOMMON" -> 2;
						case "RARE" -> 3;
						case "EPIC" -> 4;
						case "LEGENDARY" -> 5;
						case "MYTHIC" -> 6;
						default -> 0;
					};
				highestRarity.compute(petName, (k, v) -> v == null || v < rarityInt ? rarityInt : v);

				// Each unique maxed pet adds a bonus pet score
				if (
					!highestLevel.contains(petName) &&
					petLevelFromXp(higherDepth(pet, "exp", 0L), rarity, petName) >=
						higherDepth(getPetJson(), "custom_pet_leveling." + petName + ".max_level", 100)
				) {
					highestLevel.add(petName);
				}
			}

			return highestRarity.values().stream().mapToInt(i -> i).sum() + highestLevel.size();
		}

		public String getSymbol(String... prefix) {
			return getGamemode().getSymbol(prefix);
		}

		public Gamemode getGamemode() {
			return Gamemode.of(higherDepth(getOuterProfileJson(), "game_mode", "regular"));
		}

		public int getNumMaxedSlayers() {
			int numMaxedSlayers = getSlayerXp("wolf") >= 1000000 ? 1 : 0;
			numMaxedSlayers += getSlayerXp("zombie") >= 1000000 ? 1 : 0;
			numMaxedSlayers += getSlayerXp("spider") >= 1000000 ? 1 : 0;
			numMaxedSlayers += getSlayerXp("enderman") >= 1000000 ? 1 : 0;
			numMaxedSlayers += getSlayerXp("blaze") >= 1000000 ? 1 : 0;
			numMaxedSlayers += getSlayerXp("vampire") >= 2400 ? 1 : 0;
			return numMaxedSlayers;
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
			return higherDepth(profileJson(), "player_stats." + V2_STATS_MAP.getOrDefault(stat, stat), -1.0);
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

		public double getNetworth() {
			if (!profileToNetworth.containsKey(profileIndex)) {
				NetworthExecute calc = new NetworthExecute();
				calc.getPlayerNetworth(this, null);
				profileToNetworth.put(profileIndex, calc.getNetworth());
				profileToMuseum.put(profileIndex, museumResponse != null && museumResponse.isValid() ? calc.getTotal("museum", false) : -1);
			}
			return profileToNetworth.get(profileIndex);
		}

		public double getMuseumWorth() {
			return profileToMuseum.getOrDefault(profileIndex, -1.0);
		}

		public double getHypixelMuseumWorth() {
			return museumResponse != null && museumResponse.isValid() ? higherDepth(museumResponse.response(), uuid + ".value", -1.0) : -1;
		}

		public int getMageRep() {
			return higherDepth(profileJson(), "nether_island_player_data.mages_reputation", 0);
		}

		public int getBarbarianRep() {
			return higherDepth(profileJson(), "nether_island_player_data.barbarians_reputation", 0);
		}

		public int getBestiaryTier() {
			int tier = 0;

			for (Map.Entry<String, JsonElement> entry : getBestiaryJson().entrySet()) {
				if (entry.getKey().equals("brackets")) {
					continue;
				}

				for (JsonElement mob : higherDepth(entry.getValue(), "mobs").getAsJsonArray()) {
					int kills = streamJsonArray(higherDepth(mob, "mobs"))
						.mapToInt(e -> higherDepth(profileJson(), "bestiary.kills." + e.getAsString(), 0))
						.sum();

					tier += bestiaryTierFromKills(kills, higherDepth(mob, "bracket").getAsInt(), higherDepth(mob, "cap").getAsInt());
				}
			}

			return tier;
		}

		public double getBestiaryLevel() {
			return getBestiaryTier() / 10.0;
		}

		public boolean isInventoryApiEnabled() {
			return higherDepth(profileJson(), "inventory.inv_contents.data", null) != null;
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
			return higherDepth(profileJson(), "inventory.personal_vault_contents.data", null) != null;
		}

		public boolean isSkillsApiEnabled() {
			return higherDepth(profileJson(), "player_data.experience") != null;
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

		public HypixelResponse getMuseum() {
			if (museumResponse == null) {
				museumResponse = skyblockMuseumFromProfileId(higherDepth(getOuterProfileJson(), "profile_id").getAsString(), uuid);
			}
			return museumResponse;
		}

		@Override
		public String toString() {
			return (
				"Player.Profile{" +
				"valid=" +
				valid +
				", uuid='" +
				uuid +
				'\'' +
				", username='" +
				username +
				'\'' +
				", profile='" +
				profileName +
				'\'' +
				'}'
			);
		}
	}
}
