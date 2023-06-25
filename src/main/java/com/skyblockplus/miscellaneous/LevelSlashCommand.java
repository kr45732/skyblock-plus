/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.getEmoji;
import static com.skyblockplus.utils.utils.Utils.getEmojiObj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.springframework.stereotype.Component;

@Component
public class LevelSlashCommand extends SlashCommand {

	public LevelSlashCommand() {
		this.name = "level";
	}

	public static EmbedBuilder getLevel(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (!player.isValid()) {
			return player.getErrorEmbed();
		}

		LevelRecord coreTasks = getCoreTasksEmbed(player);
		LevelRecord eventTasks = getEventTasksEmbed(player);
		LevelRecord dungeonTasks = getDungeonTasks(player);
		LevelRecord essenceShopTasks = getEssenceShopTasks(player);
		LevelRecord slayingTasks = getSlayingTasks(player);
		LevelRecord skillRelatedTasks = getSkillRelatedTasks(player);
		LevelRecord miscellaneousTasks = getMiscellaneousTasks(player);
		LevelRecord storyTasks = getStoryTasks(player);

		boolean isEstimated = higherDepth(player.profileJson(), "leveling.experience") == null;
		double apiSbLevel = player.getExactLevel();
		double calculatedSbLevel =
			(
				coreTasks.total() +
				eventTasks.total() +
				dungeonTasks.total() +
				essenceShopTasks.total() +
				slayingTasks.total() +
				skillRelatedTasks.total() +
				miscellaneousTasks.total() +
				storyTasks.total()
			) /
			100.0;

		double displaySbLevel = apiSbLevel == 0 ? calculatedSbLevel : apiSbLevel;

		EmbedBuilder eb = player
			.defaultPlayerEmbed()
			.setDescription(
				"<:levels:1067859971221499954> **" +
				(isEstimated ? "Estimated " : "") +
				"Level:** " +
				roundAndFormat(displaySbLevel) +
				"\n<:levels:1067859971221499954> **" +
				(isEstimated ? "Estimated " : "") +
				"Level Color:** " +
				player.getLevelColor((int) displaySbLevel) +
				"\n\n" +
				getEmoji("NETHER_STAR") +
				" Core Tasks: " +
				coreTasks.getFormatted() +
				"\n" +
				getEmoji("WATCH") +
				" Event Tasks: " +
				eventTasks.getFormatted() +
				"\n" +
				DUNGEON_EMOJI_MAP.get("catacombs") +
				" Dungeon Tasks: " +
				dungeonTasks.getFormatted() +
				"\n" +
				ESSENCE_EMOJI_MAP.get("wither") +
				" Essence Shop Tasks: " +
				essenceShopTasks.getFormatted() +
				"\n" +
				getEmoji("GOLD_SWORD") +
				" Slaying Tasks: " +
				slayingTasks.getFormatted() +
				"\n" +
				getEmoji("DIAMOND_SWORD") +
				" Skill Related Tasks: " +
				skillRelatedTasks.getFormatted() +
				"\n" +
				getEmoji("EMPTY_MAP") +
				" Miscellaneous Tasks: " +
				miscellaneousTasks.getFormatted() +
				"\n" +
				getEmoji("BOOK_AND_QUILL") +
				" Story Tasks: " +
				storyTasks.getFormatted()
			);

		Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();
		pages.put(SelectOption.of("Overview", "overview").withEmoji(Emoji.fromFormatted("<:levels:1067859971221499954>")), eb);
		pages.put(SelectOption.of("Core Tasks", "core_tasks").withEmoji(getEmojiObj("NETHER_STAR")), coreTasks.eb());
		pages.put(SelectOption.of("Event Tasks", "event_tasks").withEmoji(getEmojiObj("WATCH")), eventTasks.eb());
		pages.put(
			SelectOption.of("Dungeon Tasks", "dungeon_tasks").withEmoji(Emoji.fromFormatted(DUNGEON_EMOJI_MAP.get("catacombs"))),
			dungeonTasks.eb()
		);
		pages.put(
			SelectOption.of("Essence Shop Tasks", "essence_shop_tasks").withEmoji(Emoji.fromFormatted(ESSENCE_EMOJI_MAP.get("wither"))),
			essenceShopTasks.eb()
		);
		pages.put(SelectOption.of("Slaying Tasks", "slaying_tasks").withEmoji(getEmojiObj("GOLD_SWORD")), slayingTasks.eb());
		pages.put(
			SelectOption.of("Skill Related Tasks", "skill_related_tasks").withEmoji(getEmojiObj("DIAMOND_SWORD")),
			skillRelatedTasks.eb()
		);
		pages.put(
			SelectOption.of("Miscellaneous Tasks", "miscellaneous_tasks").withEmoji(getEmojiObj("EMPTY_MAP")),
			miscellaneousTasks.eb()
		);
		pages.put(SelectOption.of("Story Tasks", "story_tasks").withEmoji(getEmojiObj("BOOK_AND_QUILL")), storyTasks.eb());

		new SelectMenuPaginator("overview", new PaginatorExtras().setSelectPages(pages), event);

		return null;
	}

	public static LevelRecord getCoreTasksEmbed(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Core Tasks");
		JsonElement taskJson = higherDepth(getSbLevelsJson(), "core_task");

		// Skills
		int skillsSbXp = 0;
		for (String skill : SKILL_NAMES) {
			SkillsStruct skillsStruct = player.getSkill(skill);
			if (skillsStruct != null) {
				for (int i = 1; i <= skillsStruct.currentLevel(); i++) {
					if (i <= 10) {
						skillsSbXp += 5;
					} else if (i <= 25) {
						skillsSbXp += 10;
					} else if (i <= 50) {
						skillsSbXp += 20;
					} else {
						skillsSbXp += 30;
					}
				}
			}
		}

		// Fairy souls
		int fairySoulSbXp = player.getFairySouls() / 5 * higherDepth(taskJson, "fairy_souls_xp").getAsInt();

		// Accessories
		int magicPowerSbXp =
			higherDepth(player.profileJson(), "accessory_bag_storage.highest_magical_power", player.getMagicPower()) *
			higherDepth(taskJson, "accessory_bag_xp").getAsInt();

		// Pets
		int petScoreSbXp =
			higherDepth(player.profileJson(), "leveling.highest_pet_score", player.getPetScore()) *
			higherDepth(taskJson, "pet_score_xp").getAsInt();

		// Collections
		Map<String, Long> collections = new HashMap<>();
		for (Map.Entry<String, JsonElement> member : higherDepth(player.getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			try {
				for (Map.Entry<String, JsonElement> collection : higherDepth(member.getValue(), "collection")
					.getAsJsonObject()
					.entrySet()) {
					collections.compute(collection.getKey(), (k, v) -> (v == null ? 0 : v) + collection.getValue().getAsLong());
				}
			} catch (Exception ignored) {}
		}
		int collectionsSbXp = 0;
		for (Map.Entry<String, Long> collection : collections.entrySet()) {
			JsonElement tiers = higherDepth(getCollectionsJson(), collection.getKey() + ".tiers");
			if (tiers != null) {
				for (JsonElement amtRequired : tiers.getAsJsonArray()) {
					if (collection.getValue() >= amtRequired.getAsLong()) {
						collectionsSbXp += higherDepth(taskJson, "collections_xp").getAsInt();
					} else {
						break;
					}
				}
			}
		}

		// Minions
		Set<String> uniqueCraftedMinions = new HashSet<>();
		for (Map.Entry<String, JsonElement> member : higherDepth(player.getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			try {
				for (JsonElement minion : higherDepth(member.getValue(), "crafted_generators").getAsJsonArray()) {
					uniqueCraftedMinions.add(minion.getAsString());
				}
			} catch (Exception ignored) {}
		}
		int minionsSbXp = 0;
		for (String uniqueCraftedMinion : uniqueCraftedMinions) {
			int idx = uniqueCraftedMinion.lastIndexOf("_");
			minionsSbXp += higherDepth(getMiscJson(), "minionXp." + uniqueCraftedMinion.substring(idx + 1)).getAsInt();
		}

		// Bank upgrades
		int bankUpgradesSbXp = 0;
		JsonElement completedTasks = higherDepth(player.profileJson(), "leveling.completed_tasks");
		if (completedTasks != null) {
			for (JsonElement task : completedTasks.getAsJsonArray()) {
				if (task.getAsString().startsWith("BANK_UPGRADE_")) {
					bankUpgradesSbXp += higherDepth(taskJson, "bank_upgrades_xp." + task.getAsString()).getAsInt();
				}
			}
		}

		// Core tasks total
		int categoryTotal = skillsSbXp + fairySoulSbXp + collectionsSbXp + minionsSbXp;
		LevelRecord levelRecord = new LevelRecord("core_task", eb, categoryTotal + magicPowerSbXp + petScoreSbXp, categoryTotal);
		eb.appendDescription("\n" + getEmoji("DIAMOND_SWORD") + " Skill Level Up: " + getFormatted(taskJson, "skill_level_up", skillsSbXp));
		eb.appendDescription("\n" + getEmoji("REVIVE_STONE") + " Fairy Souls: " + getFormatted(taskJson, "fairy_souls", fairySoulSbXp));
		eb.appendDescription("\n" + getEmoji("HEGEMONY_ARTIFACT") + " Accessory Bag: " + formatNumber(magicPowerSbXp));
		eb.appendDescription("\n" + getEmoji("BONE") + " Pet Score: " + formatNumber(petScoreSbXp));
		eb.appendDescription("\n" + getEmoji("PAINTING") + " Collections: " + getFormatted(taskJson, "collections", collectionsSbXp));
		eb.appendDescription(
			"\n" + getEmoji("COBBLESTONE_GENERATOR_1") + " Craft Minions: " + getFormatted(taskJson, "craft_minions", minionsSbXp)
		);
		eb.appendDescription(
			"\n" + getEmoji("PERSONAL_BANK_ITEM") + " Bank Upgrades: " + getFormatted(taskJson, "bank_upgrades", bankUpgradesSbXp)
		);
		eb.getDescriptionBuilder().insert(0, getEmoji("NETHER_STAR") + " **Core Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getEventTasksEmbed(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Event Tasks");
		JsonElement taskJson = higherDepth(getSbLevelsJson(), "event_task");

		// Mining fiesta
		int miningFiestaSbXp = higherDepth(player.profileJson(), "leveling.mining_fiesta_ores_mined", 0) / 5000;

		// Fishing festival
		int fishingFestivalSbXp = higherDepth(player.profileJson(), "leveling.fishing_festival_sharks_killed", 0) / 50;

		// Spooky festival
		int spookyFestivalSbXp = 0;
		JsonElement completedTasks = higherDepth(player.profileJson(), "leveling.completed_tasks");
		if (completedTasks != null) {
			for (JsonElement task : completedTasks.getAsJsonArray()) {
				if (task.getAsString().startsWith("SPOOKY_FESTIVAL_")) {
					spookyFestivalSbXp += higherDepth(taskJson, "spooky_festival_xp." + task.getAsString()).getAsInt();
				}
			}
		}

		// Event tasks total
		LevelRecord levelRecord = new LevelRecord("event_task", eb, miningFiestaSbXp + fishingFestivalSbXp + spookyFestivalSbXp);
		eb.appendDescription(
			"\n" + getEmoji("IRON_PICKAXE") + " Mining Fiesta: " + getFormatted(taskJson, "mining_fiesta", miningFiestaSbXp)
		);
		eb.appendDescription(
			"\n" + getEmoji("FISHING_ROD") + " Fishing Festival: " + getFormatted(taskJson, "fishing_festival", fishingFestivalSbXp)
		);
		eb.appendDescription(
			"\n" + getEmoji("JACK_O_LANTERN") + " Spooky Festival: " + getFormatted(taskJson, "spooky_festival", spookyFestivalSbXp)
		);
		eb.getDescriptionBuilder().insert(0, getEmoji("WATCH") + " **Event Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getDungeonTasks(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Dungeon Tasks");
		JsonElement taskJson = higherDepth(getSbLevelsJson(), "dungeon_task");

		// Catacombs
		int catacombsSbXp = 0;
		SkillsStruct cataSkill = player.getCatacombs();
		if (cataSkill != null) {
			for (int i = 1; i <= cataSkill.currentLevel(); i++) {
				if (i < 40) {
					catacombsSbXp += 20;
				} else if (i <= 50) {
					catacombsSbXp += 40;
				}
			}
		}

		// Classes
		int classSbXp = 0;
		for (String className : DUNGEON_CLASS_NAMES) {
			SkillsStruct classInfo = player.getDungeonClass(className);
			if (classInfo != null) {
				classSbXp += Math.min(classInfo.currentLevel(), 50) * higherDepth(taskJson, "class_xp").getAsInt();
			}
		}

		// Regular floor completions
		int floorCompletionSbXp = 0;
		JsonElement cataTierCompletions = higherDepth(player.profileJson(), "dungeons.dungeon_types.catacombs.tier_completions");
		if (cataTierCompletions != null) {
			for (Map.Entry<String, JsonElement> completion : cataTierCompletions.getAsJsonObject().entrySet()) {
				if (completion.getValue().getAsInt() > 0) {
					floorCompletionSbXp +=
						higherDepth(taskJson, "complete_catacombs.[" + Integer.parseInt(completion.getKey()) + "]").getAsInt();
				}
			}
		}

		// Master flor completions
		int masterFloorCompletionSbXp = 0;
		JsonElement masterTierCompletions = higherDepth(player.profileJson(), "dungeons.dungeon_types.master_catacombs.tier_completions");
		if (masterTierCompletions != null) {
			for (Map.Entry<String, JsonElement> completion : masterTierCompletions.getAsJsonObject().entrySet()) {
				if (completion.getValue().getAsInt() > 0) {
					masterFloorCompletionSbXp += higherDepth(taskJson, "complete_master_catacombs").getAsInt();
				}
			}
		}

		// Dungeon tasks total
		LevelRecord levelRecord = new LevelRecord(
			"dungeon_task",
			eb,
			catacombsSbXp + classSbXp + floorCompletionSbXp + masterFloorCompletionSbXp
		);
		eb.appendDescription(
			"\n" +
			DUNGEON_EMOJI_MAP.get("catacombs") +
			" Catacombs Level Up: " +
			getFormatted(taskJson, "catacombs_level_up", catacombsSbXp)
		);
		eb.appendDescription(
			"\n" + DUNGEON_EMOJI_MAP.get("catacombs") + " Class Level Up: " + getFormatted(taskJson, "class_level_up", classSbXp)
		);
		eb.appendDescription(
			"\n" + DUNGEON_EMOJI_MAP.get("catacombs") + " Complete The Catacombs: " + formatNumber(floorCompletionSbXp) + " / 190"
		);
		eb.appendDescription(
			"\n" +
			DUNGEON_EMOJI_MAP.get("catacombs") +
			" Complete The Catacombs Master Mode: " +
			formatNumber(masterFloorCompletionSbXp) +
			" / 350"
		);
		eb
			.getDescriptionBuilder()
			.insert(0, DUNGEON_EMOJI_MAP.get("catacombs") + " **Dungeon Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getEssenceShopTasks(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Essence Shop Tasks");
		JsonElement taskJson = higherDepth(getSbLevelsJson(), "essence_shop_task");

		int essenceSbXp = 0;
		for (Map.Entry<String, JsonElement> essenceShop : getEssenceShopsJson().entrySet()) {
			int shopSbXp = 0;
			for (Map.Entry<String, JsonElement> upgrade : essenceShop.getValue().getAsJsonObject().entrySet()) {
				int upgradeTier = higherDepth(player.profileJson(), "perks." + upgrade.getKey(), 0);

				int upgradeSbXp = 0;
				for (int i = 0; i < upgradeTier; i++) {
					upgradeSbXp += higherDepth(taskJson, "essence_shop_xp.[" + i + "]").getAsInt();
				}

				shopSbXp += upgradeSbXp;
			}

			essenceSbXp += shopSbXp;

			String essenceType = essenceShop.getKey().split("_")[1].toLowerCase();
			eb.appendDescription(
				"\n" +
				ESSENCE_EMOJI_MAP.get(essenceType) +
				" " +
				capitalizeString(essenceType) +
				" Essence Shop: " +
				getFormatted(taskJson, essenceShop.getKey().toLowerCase() + "_shop", shopSbXp)
			);
		}

		// Essence shop tasks total
		LevelRecord levelRecord = new LevelRecord("essence_shop_task", eb, essenceSbXp);
		eb
			.getDescriptionBuilder()
			.insert(0, ESSENCE_EMOJI_MAP.get("wither") + " **Essence Shop Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getSlayingTasks(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Slaying Tasks");
		JsonElement taskJson = higherDepth(getSbLevelsJson(), "slaying_task");

		// Slayer level up
		int slayerLevelUpSbXp = 0;
		for (String slayer : SLAYER_NAMES) {
			int slayerLevel = player.getSlayerLevel(slayer);
			for (int i = 0; i < slayerLevel; i++) {
				slayerLevelUpSbXp += higherDepth(taskJson, "slayer_level_up_xp.[" + i + "]").getAsInt();
			}
		}

		// Boss collections
		int bossCollectionsSbXp = 0;

		HashMap<String, Integer> floorToCompletions = new HashMap<>();
		for (String type : List.of("catacombs", "master_catacombs")) {
			JsonElement dungeonCompletions = higherDepth(player.profileJson(), "dungeons.dungeon_types." + type + ".tier_completions");
			if (dungeonCompletions != null) {
				for (Map.Entry<String, JsonElement> floorCompletions : dungeonCompletions.getAsJsonObject().entrySet()) {
					floorToCompletions.compute(
						floorCompletions.getKey(),
						(k, v) -> (v == null ? 0 : v) + floorCompletions.getValue().getAsInt()
					);
				}
			}
		}

		int[] bossLow = { 25, 50, 100, 150, 250, 1000 };
		int[] thorn = { 25, 50, 150, 250, 400, 1000 };
		int[] bossHigh = { 50, 100, 150, 250, 500, 750, 1000 };

		for (Map.Entry<String, Integer> levelCompletions : floorToCompletions.entrySet()) {
			bossCollectionsSbXp +=
				switch (levelCompletions.getKey()) {
					case "1", "2", "3" -> loopThroughCollection(bossLow, levelCompletions.getValue());
					case "4" -> loopThroughCollection(thorn, levelCompletions.getValue());
					case "5", "6", "7" -> loopThroughCollection(bossHigh, levelCompletions.getValue());
					default -> 0;
				};
		}

		// Kuudra
		int defeatKuudraSbXp = 0;

		JsonArray defeatKuudraXp = higherDepth(taskJson, "defeat_kuudra_xp").getAsJsonArray();
		JsonElement kuudraTiers = higherDepth(player.profileJson(), "nether_island_player_data.kuudra_completed_tiers");
		if (kuudraTiers != null) {
			int kuudraBossCollection = 0;

			for (Map.Entry<String, JsonElement> stringJsonElementEntry : kuudraTiers.getAsJsonObject().entrySet()) {
				String key = stringJsonElementEntry.getKey();
				int value = stringJsonElementEntry.getValue().getAsInt();
				if (key.equals("none")) {
					defeatKuudraSbXp += defeatKuudraXp.get(0).getAsInt();
					kuudraBossCollection += value;
				}
				if (key.equals("hot")) {
					defeatKuudraSbXp += defeatKuudraXp.get(1).getAsInt();
					kuudraBossCollection += 2 * value;
				}
				if (key.equals("burning")) {
					defeatKuudraSbXp += defeatKuudraXp.get(2).getAsInt();
					kuudraBossCollection += 3 * value;
				}
				if (key.equals("fiery")) {
					defeatKuudraSbXp += defeatKuudraXp.get(3).getAsInt();
					kuudraBossCollection += 4 * value;
				}
				if (key.equals("infernal")) {
					defeatKuudraSbXp += defeatKuudraXp.get(4).getAsInt();
					kuudraBossCollection += 5 * value;
				}
			}

			if (kuudraBossCollection >= 10) {
				bossCollectionsSbXp += 10;
			}
			if (kuudraBossCollection >= 100) {
				bossCollectionsSbXp += 15;
			}
			if (kuudraBossCollection >= 500) {
				bossCollectionsSbXp += 20;
			}
			if (kuudraBossCollection >= 2000) {
				bossCollectionsSbXp += 25;
			}
			if (kuudraBossCollection >= 5000) {
				bossCollectionsSbXp += 30;
			}
		}

		// Bestiary Progress
		int bestiarySbXp = player.getBestiaryTier() + (int) player.getBestiaryLevel() * 2;

		// Mythological Kills
		int mythologicalKillsSbXp = Math.min(higherDepth(player.profileJson(), "stats.mythos_kills", 0), 10000) / 100;

		// Slay dragons
		int dragonSlaySbXp = 0;
		for (Map.Entry<String, JsonElement> entry : higherDepth(taskJson, "slay_dragons_xp").getAsJsonObject().entrySet()) {
			if (higherDepth(player.profileJson(), "bestiary.kills_" + entry.getKey() + "_100", 0) > 0) {
				dragonSlaySbXp += entry.getValue().getAsInt();
			}
		}

		// Slayer kills
		int defeatSlayerSbXp = 0;
		JsonArray defeatSlayersXp = higherDepth(taskJson, "defeat_slayers_xp").getAsJsonArray();
		for (String slayer : SLAYER_NAMES) {
			for (int i = 0; i <= 4; i++) {
				if (player.getSlayerBossKills(slayer, i) > 0) {
					defeatSlayerSbXp += defeatSlayersXp.get(i).getAsInt();
				}
			}
		}

		// Arachne
		int defeatedArachneSbXp = 0;
		// Tier 1
		if (higherDepth(player.profileJson(), "bestiary.kills_arachne_300", 0) > 0) {
			defeatedArachneSbXp += 20;
		}
		// Tier 2
		if (higherDepth(player.profileJson(), "bestiary.kills_arachne_500", 0) > 0) {
			defeatedArachneSbXp += 40;
		}

		// Slaying tasks total
		int categorySbXp =
			slayerLevelUpSbXp +
			bossCollectionsSbXp +
			mythologicalKillsSbXp +
			dragonSlaySbXp +
			defeatSlayerSbXp +
			defeatKuudraSbXp +
			defeatedArachneSbXp;
		LevelRecord levelRecord = new LevelRecord("slaying_task", eb, categorySbXp + bestiarySbXp, categorySbXp);
		eb.appendDescription("\n" + getEmoji("EXP_BOTTLE") + " Slayer Level Up: " + formatNumber(slayerLevelUpSbXp) + " / 3,625");
		eb.appendDescription(
			"\n" + getEmoji("DIAMOND_THORN_HEAD") + " Boss Collections: " + formatNumber(bossCollectionsSbXp) + " / 1,015"
		);
		eb.appendDescription("\n" + getEmoji("ZOMBIE_HAT") + " Bestiary Progress: " + formatNumber(bestiarySbXp));
		eb.appendDescription(
			"\n" + getEmoji("BEASTMASTER_CREST_LEGENDARY") + " Mythological Kills: " + formatNumber(mythologicalKillsSbXp) + " / 100"
		);
		eb.appendDescription("\n" + getEmoji("DRAGON_EGG") + " Slay Dragon: " + formatNumber(dragonSlaySbXp) + " / 200");
		eb.appendDescription("\n" + getEmoji("AATROX_BATPHONE") + " Defeat Slayers: " + formatNumber(defeatSlayerSbXp) + " / 825");
		eb.appendDescription("\n" + getEmoji("KUUDRA_TIER_KEY") + " Defeat Kuudra: " + formatNumber(defeatKuudraSbXp) + " / 300");
		eb.appendDescription("\n" + getEmoji("ARACHNE_CRYSTAL") + " Defeat Arachne: " + formatNumber(defeatedArachneSbXp) + " / 60");
		eb.getDescriptionBuilder().insert(0, getEmoji("GOLD_SWORD") + " **Slaying Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getSkillRelatedTasks(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Skill Related Tasks");

		// Mining
		String miningStr = "";

		// Hotm
		int hotmSbXp = 0;
		SkillsStruct hotmSkill = player.getHOTM();
		if (hotmSkill != null) {
			for (int i = 1; i <= hotmSkill.currentLevel(); i++) {
				hotmSbXp +=
					switch (i) {
						case 1 -> 35;
						case 2 -> 45;
						case 3 -> 60;
						case 4 -> 75;
						case 5 -> 90;
						case 6 -> 110;
						case 7 -> 130;
						default -> 0;
					};
			}
		}

		// Powder
		int powderSbXp = 0;

		final int mithrilCap = 12500000;
		final int gemstoneCap = 20000000;
		final int normalCap = 350000;

		long mithrilPowder =
			higherDepth(player.profileJson(), "mining_core.powder_mithril", 0L) +
			higherDepth(player.profileJson(), "mining_core.powder_spent_mithril", 0L);
		powderSbXp += Math.min(mithrilPowder, normalCap) / 2400;
		if (mithrilPowder > normalCap) {
			powderSbXp +=
				3.75 *
				(Math.sqrt(1 + 8 * (Math.sqrt((1758267.0 / mithrilCap) * (Math.min(mithrilPowder, mithrilCap) - normalCap + 9)))) - 3);
		}

		long gemstonePowder =
			higherDepth(player.profileJson(), "mining_core.powder_gemstone", 0L) +
			higherDepth(player.profileJson(), "mining_core.powder_spent_gemstone", 0L);
		powderSbXp += Math.min(gemstonePowder, normalCap) / 2500;
		if (gemstonePowder > normalCap) {
			powderSbXp +=
				4.25 *
				(Math.sqrt(1 + 8 * (Math.sqrt((1758267.0 / gemstoneCap) * (Math.min(gemstonePowder, gemstoneCap) - normalCap + 9)))) - 3);
		}

		// Commissions
		int commissionsSbXp = 0;
		int[] commissionMilestoneXpArray = { 20, 30, 30, 50, 50, 75 };
		JsonElement tutorialArray = higherDepth(player.profileJson(), "tutorial");
		if (tutorialArray != null) {
			for (JsonElement tutorial : tutorialArray.getAsJsonArray()) {
				if (
					tutorial.getAsJsonPrimitive().isString() &&
					tutorial.getAsString().startsWith("commission_milestone_reward_skyblock_xp_tier")
				) for (int i = 1; i <= commissionMilestoneXpArray.length; i++) {
					if (tutorial.getAsString().equals("commission_milestone_reward_skyblock_xp_tier_" + i)) {
						commissionsSbXp += commissionMilestoneXpArray[i - 1];
					}
				}
			}
		}

		// Crystal nucleus runs
		int crystalNucleusRuns = higherDepth(player.profileJson(), "leveling.completions.NUCLEUS_RUNS", 0) * 4;

		// Peak of the mountain
		int peakOfTheMountainSbXp = 0;
		int potmLevel = higherDepth(player.profileJson(), "mining_core.nodes.special_0", 0);
		for (int i = 1; i <= potmLevel; i++) {
			peakOfTheMountainSbXp +=
				switch (i) {
					case 1 -> 25;
					case 2 -> 35;
					case 3 -> 50;
					case 4 -> 65;
					case 5 -> 75;
					case 6 -> 100;
					case 7 -> 125;
					default -> 9;
				};
		}

		// Rock pet milestones
		int rockPetSbXp = 0;

		int rockPetMilestone = higherDepth(player.profileJson(), "stats.pet_milestone_ores_mined", 0);
		int[] rockMilestonesRequired = { 2500, 7500, 20000, 100000, 250000 };
		for (int milestone : rockMilestonesRequired) {
			if (rockPetMilestone >= milestone) {
				rockPetSbXp += 20;
			}
		}

		int miningTotalSbXp = hotmSbXp + powderSbXp + commissionsSbXp + crystalNucleusRuns + peakOfTheMountainSbXp + rockPetSbXp;
		miningStr += "\n" + getEmoji("DIVAN_DRILL") + " Heart Of The Mountain: " + formatNumber(hotmSbXp) + " / 545";
		miningStr += "\n" + getEmoji("MITHRIL_ORE") + " Powder: " + formatNumber(powderSbXp) + " / 1,080";
		miningStr += "\n" + getEmoji("ROYAL_PIGEON") + " Commission Milestones: " + formatNumber(commissionsSbXp) + " / 255";
		miningStr += "\n" + getEmoji("NETHER_STAR") + " Crystal Nucleus Runs: " + formatNumber(crystalNucleusRuns) + " / 200";
		miningStr += "\n" + getEmoji("REDSTONE_BLOCK") + " Peak Of The Mountain: " + formatNumber(peakOfTheMountainSbXp) + " / 475";
		miningStr += "\n" + getEmoji("ROCK;4") + " Rock Milestones: " + formatNumber(rockPetSbXp) + " / 100";
		eb.addField("Mining | " + formatNumber(miningTotalSbXp) + " / 2,655", miningStr, false);

		// Farming
		String farmingStr = "";

		// Anita shop upgrades
		int doubleDrops = higherDepth(player.profileJson(), "jacob2.perks.double_drops", 0);
		int farmingLevelCap = player.getFarmingCapUpgrade();
		int anitaShopUpgradeSbXp = (doubleDrops + farmingLevelCap) * 10;

		farmingStr += "\n" + getEmoji("STICK") + " Anita's Shop Upgrades: " + formatNumber(anitaShopUpgradeSbXp) + " / 250";
		eb.addField("Farming | " + formatNumber(anitaShopUpgradeSbXp), farmingStr, false);

		// Fishing
		String fishingStr = "";

		// Trophy fishing
		int trophyFishingSbXp = 0;
		if (higherDepth(player.profileJson(), "trophy_fish") != null) {
			JsonObject trophyFish = higherDepth(player.profileJson(), "trophy_fish").getAsJsonObject();
			for (Map.Entry<String, JsonElement> tropyFishEntry : trophyFish.entrySet()) {
				String key = tropyFishEntry.getKey();
				if (tropyFishEntry.getValue().isJsonPrimitive()) {
					if (key.endsWith("_bronze")) {
						trophyFishingSbXp += 4;
					} else if (key.endsWith("_silver")) {
						trophyFishingSbXp += 8;
					} else if (key.endsWith("_gold")) {
						trophyFishingSbXp += 16;
					} else if (key.endsWith("_diamond")) {
						trophyFishingSbXp += 32;
					}
				}
			}
		}

		// Dolphin pet milestones
		int dolphinPetSbXp = 0;
		int dolphinMilestoneXp = higherDepth(player.profileJson(), "stats.pet_milestone_sea_creatures_killed", 0);
		int[] dolphinMilestoneRequired = { 250, 1000, 2500, 5000, 10000 };
		for (int milestone : dolphinMilestoneRequired) {
			if (dolphinMilestoneXp >= milestone) {
				dolphinPetSbXp += 20;
			}
		}

		int fishingTotalSbXp = trophyFishingSbXp + dolphinPetSbXp;
		fishingStr += "\n" + getEmoji("SLUGFISH_BRONZE") + " Trophy Fish: " + formatNumber(trophyFishingSbXp) + " / 1,080";
		fishingStr += "\n" + getEmoji("DOLPHIN;4") + " Dolphin Milestones: " + formatNumber(dolphinPetSbXp) + " / 100";
		eb.addField("Fishing | " + formatNumber(fishingTotalSbXp), fishingStr, false);

		// Skill related task total
		LevelRecord levelRecord = new LevelRecord("skill_related_task", eb, miningTotalSbXp + anitaShopUpgradeSbXp + fishingTotalSbXp);
		eb.getDescriptionBuilder().insert(0, getEmoji("DIAMOND_SWORD") + " **Skill Related Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getMiscellaneousTasks(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Miscellaneous Tasks");

		// Accessory bag upgrade count
		int accessoryBagUpgradeSbXp = higherDepth(player.profileJson(), "accessory_bag_storage.bag_upgrades_purchased", 0) * 2;

		// Reaper peppers
		int reaperPepperSbXp = higherDepth(player.profileJson(), "reaper_peppers_eaten", 0) * 10;

		// Unlocking accessory bag powers
		int unlockingPowersSbXp = 0;
		JsonElement unlockedPowers = higherDepth(player.profileJson(), "accessory_bag_storage.unlocked_powers");
		if (unlockedPowers != null) {
			unlockingPowersSbXp = unlockedPowers.getAsJsonArray().size() * 15;
		}

		// Dojo
		int dojoSbXp = 0;
		JsonElement dojoScores = higherDepth(player.profileJson(), "nether_island_player_data.dojo");
		if (dojoScores != null) {
			int dojoPoints = player.getDojoPoints();

			if (dojoPoints >= 7000) {
				dojoSbXp += 150;
			}
			if (dojoPoints >= 6000) {
				dojoSbXp += 100;
			}
			if (dojoPoints >= 4000) {
				dojoSbXp += 75;
			}
			if (dojoPoints >= 2000) {
				dojoSbXp += 50;
			}
			if (dojoPoints >= 1000) {
				dojoSbXp += 30;
			}
			dojoSbXp += 20;
		}

		// Harp
		int harpSbXp = 0;

		JsonObject harpSongToSbXp = higherDepth(getSbLevelsJson(), "miscellaneous_task.harp_songs_names").getAsJsonObject();
		JsonElement harpQuests = higherDepth(player.profileJson(), "harp_quest");
		if (harpQuests != null) {
			for (Map.Entry<String, JsonElement> harpSong : harpSongToSbXp.entrySet()) {
				if (harpQuests.getAsJsonObject().has(harpSong.getKey())) {
					harpSbXp += harpSong.getValue().getAsInt();
				}
			}
		}

		// Abiphone
		int abiphoneSbXp = 0;
		JsonElement abiphoneContacts = higherDepth(player.profileJson(), "nether_island_player_data.abiphone.active_contacts");
		if (abiphoneContacts != null) {
			abiphoneSbXp = abiphoneContacts.getAsJsonArray().size() * 10;
		}

		// Community shop
		int communityShopSbXp = 0;
		JsonObject communityShopUpgradesMax = higherDepth(getSbLevelsJson(), "miscellaneous_task.community_shop_upgrades_max")
			.getAsJsonObject();
		JsonElement communityUpgrades = higherDepth(player.getOuterProfileJson(), "community_upgrades.upgrade_states");
		if (communityUpgrades != null) {
			for (JsonElement upgradeState : communityUpgrades.getAsJsonArray()) {
				if (upgradeState.isJsonObject()) {
					JsonObject value = upgradeState.getAsJsonObject();
					String upgrade = value.get("upgrade").getAsString();
					int tier = value.get("tier").getAsInt();
					if (communityShopUpgradesMax.has(upgrade)) {
						int max = communityShopUpgradesMax.get(upgrade).getAsInt();
						if (max >= tier) {
							communityShopSbXp += 10;
						}
					}
				}
			}
		}

		// Personal bank upgrades
		int personalBankSbXp = 0;
		// Field is zero indexed
		int personalBankUpgrade = higherDepth(player.profileJson(), "personal_bank_upgrade", 0);
		for (int i = 1; i <= personalBankUpgrade; i++) {
			personalBankSbXp +=
				switch (i) {
					case 1 -> 25;
					case 2 -> 35;
					case 3 -> 50;
					default -> 0;
				};
		}

		// Miscellaneous tasks total
		int categoryTotalSbXp = reaperPepperSbXp + dojoSbXp + harpSbXp + abiphoneSbXp + communityShopSbXp + personalBankSbXp;
		LevelRecord levelRecord = new LevelRecord(
			"miscellaneous_task",
			eb,
			categoryTotalSbXp + accessoryBagUpgradeSbXp + unlockingPowersSbXp,
			categoryTotalSbXp
		);
		eb.appendDescription(
			"\n" + getEmoji("HEGEMONY_ARTIFACT") + " Accessory Bag Upgrades: " + formatNumber(accessoryBagUpgradeSbXp) + " / 396"
		);
		eb.appendDescription("\n" + getEmoji("REAPER_PEPPER") + " Reaper Peppers: " + formatNumber(reaperPepperSbXp) + " / 50");
		eb.appendDescription("\n" + getEmoji("SCORCHED_BOOKS") + " Unlocking Powers: " + formatNumber(unlockingPowersSbXp) + " / 255");
		eb.appendDescription("\n" + getEmoji("DOJO_BLACK_BELT") + " The Dojo: " + formatNumber(dojoSbXp) + " / 425");
		eb.appendDescription("\n" + getEmoji("ENCHANTED_BOOK") + " Harp Songs: " + formatNumber(harpSbXp) + " / 236");
		eb.appendDescription(
			"\n" + getEmoji("ABIPHONE_XIV_ENORMOUS_PURPLE") + " Abiphone Contacts: " + formatNumber(abiphoneSbXp) + " / 410"
		);
		eb.appendDescription("\n" + getEmoji("EMERALD") + " Community Shop Upgrades: " + formatNumber(communityShopSbXp) + " / 120");
		eb.appendDescription(
			"\n" + getEmoji("PERSONAL_BANK_ITEM") + " Personal Bank Upgrades: " + formatNumber(personalBankSbXp) + " / 110"
		);
		eb.getDescriptionBuilder().insert(0, getEmoji("EMPTY_MAP") + " **Miscellaneous Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	public static LevelRecord getStoryTasks(Player.Profile player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Story Tasks");
		JsonElement taskJson = higherDepth(getSbLevelsJson(), "story_task");

		// Objectives
		int objectivesSbXp = 0;
		JsonElement completedTasks = higherDepth(player.profileJson(), "leveling.completed_tasks");
		if (completedTasks != null) {
			for (JsonElement taskEle : higherDepth(taskJson, "complete_objectives_names").getAsJsonArray()) {
				String taskName = "OBJECTIVE_" + taskEle.getAsString().toUpperCase();
				if (streamJsonArray(completedTasks).anyMatch(e -> e.getAsString().equals(taskName))) {
					objectivesSbXp += higherDepth(taskJson, "complete_objectives_xp").getAsInt();
				}
			}
		}

		// Story tasks total
		LevelRecord levelRecord = new LevelRecord("story_task", eb, objectivesSbXp);
		eb.appendDescription(
			"\n" + getEmoji("BOOK") + " Complete Objectives: " + getFormatted(taskJson, "complete_objectives", objectivesSbXp)
		);
		eb.getDescriptionBuilder().insert(0, getEmoji("BOOK_AND_QUILL") + " **Story Tasks:** " + levelRecord.getFormatted() + "\n");

		return levelRecord;
	}

	private static int loopThroughCollection(int[] array, int value) {
		JsonArray dungeonCollectionXp = higherDepth(getSbLevelsJson(), "slaying_task.boss_collections_xp.dungeon_collection_xp")
			.getAsJsonArray();
		int gain = 0;
		for (int i = 0; i < array.length; i++) {
			if (value >= array[i]) {
				gain += dungeonCollectionXp.get(i).getAsInt();
			}
		}
		return gain;
	}

	private static String getFormatted(JsonElement taskJson, String name, int xp) {
		return formatNumber(xp) + " / " + formatNumber(higherDepth(taskJson, name).getAsInt());
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getLevel(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's Skyblock level")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public record LevelRecord(EmbedBuilder eb, int total, int categoryTotal, int max) {
		public LevelRecord(String name, EmbedBuilder eb, int total, int categoryTotal) {
			this(eb, total, categoryTotal, higherDepth(getSbLevelsJson(), "category_xp." + name).getAsInt());
		}

		public LevelRecord(String name, EmbedBuilder eb, int total) {
			this(name, eb, total, total);
		}

		public String getFormatted() {
			return formatNumber(categoryTotal) + " / " + formatNumber(max) + " (" + roundProgress((double) categoryTotal / max) + ")";
		}
	}
}
