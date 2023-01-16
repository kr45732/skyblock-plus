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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.springframework.stereotype.Component;

@Component
public class LevelSlashCommand extends SlashCommand {

	public LevelSlashCommand() {
		this.name = "level";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getLevel(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's Skyblock level")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getLevel(String username, String profileName, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		LevelRecord coreTasks = getCoreTasksEmbed(player);
		LevelRecord eventTasks = getEventTasks(player);
		LevelRecord dungeonTasks = getDungeonTasks(player);
		LevelRecord essenceShopTasks = getEssenceShopTasks(player);
		LevelRecord slayingTasks = getSlayingTasks(player);
		LevelRecord skillRelatedTasks = getSkillRelatedTasks(player);
		LevelRecord miscellaneousTasks = getMiscellaneousTasks(player);
		LevelRecord storyTasks = getStoryTasks(player);

		EmbedBuilder eb = player.defaultPlayerEmbed();
		eb.setDescription(
			"**Level:** " +
			roundAndFormat(player.getLevel()) +
			"\n**Level Color:** ?" +
			"\n\nCore Tasks: " +
			coreTasks.total() +
			"\nEvent Tasks: " +
			eventTasks.total() +
			"\nDungeon Tasks: " +
			dungeonTasks.total() +
			"\nEssence Shop Tasks: " +
			essenceShopTasks.total() +
			"\nSlaying Tasks: " +
			slayingTasks.total() +
			"\nSkill Related Tasks: " +
			skillRelatedTasks.total() +
			"\nMiscellaneous Tasks: " +
			miscellaneousTasks.total() +
			"\nStory Tasks: " +
			storyTasks.total()
		);

		Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();
		pages.put(SelectOption.of("Overview", "overview"), eb);
		pages.put(SelectOption.of("Core Tasks", "core_tasks"), coreTasks.eb());
		pages.put(SelectOption.of("Event Tasks", "event_tasks"), eventTasks.eb());
		pages.put(SelectOption.of("Dungeon Tasks", "dungeon_tasks"), dungeonTasks.eb());
		pages.put(SelectOption.of("Essence Shop Tasks", "essence_shop_tasks"), essenceShopTasks.eb());
		pages.put(SelectOption.of("Slaying Tasks", "slaying_tasks"), slayingTasks.eb());
		pages.put(SelectOption.of("Skill Related Tasks", "skill_related_tasks"), skillRelatedTasks.eb());
		pages.put(SelectOption.of("Miscellaneous Tasks", "miscellaneous_tasks"), miscellaneousTasks.eb());
		pages.put(SelectOption.of("Story Tasks", "story_tasks"), storyTasks.eb());

		new SelectMenuPaginator("overview", new PaginatorExtras().setSelectPages(pages), event);

		return null;
	}

	private static LevelRecord getCoreTasksEmbed(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Core Tasks");

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
		eb.appendDescription("\nSkill Level Up: " + formatNumber(skillsSbXp) + " / 7,500");

		// Museum
		eb.appendDescription("\nMuseum Progression: ? / 2,700");

		// Fairy souls
		int fairySoulSbXp = player.getFairySouls() / 5 * 10;
		eb.appendDescription("\nFairy Souls: " + formatNumber(fairySoulSbXp) + " / 470");

		// Accessories
		int magicPowerSbXp = player.getMagicPower();
		eb.appendDescription("\nAccessory Bag: " + formatNumber(magicPowerSbXp));

		// Pets
		int petScoreSbXp = player.getPetScore() * 3;
		eb.appendDescription("\nPet Score: " + formatNumber(petScoreSbXp));

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
						collectionsSbXp += 4;
					} else {
						break;
					}
				}
			}
		}
		eb.appendDescription("\nCollections: " + formatNumber(collectionsSbXp) + " / 2,452");

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
		eb.appendDescription("\nCraft Minions: " + formatNumber(minionsSbXp) + " / 2,801");

		// Bank upgrades
		eb.appendDescription("\nBank Upgrades: ? / 200");

		// Core tasks total
		String totalSbXp =
			formatNumber(skillsSbXp + fairySoulSbXp + magicPowerSbXp + petScoreSbXp + collectionsSbXp + minionsSbXp) + " / 15,430";
		eb.getDescriptionBuilder().insert(0, "Core Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getEventTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Event Tasks");

		// Mining fiesta
		eb.appendDescription("\nMining Fiesta: ? / 200");

		// Fishing festival
		eb.appendDescription("\nFishing Festival: ? / 100");

		// Spooky festival
		eb.appendDescription("\nSpooky Festival: ? / 225");

		// Event tasks total
		String totalSbXp = "? / 525";
		eb.getDescriptionBuilder().insert(0, "Event Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getDungeonTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Dungeon Tasks");

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
		eb.appendDescription("\nCatacombs Level Up: " + formatNumber(catacombsSbXp) + " / 1220");

		// Classes
		int classSbXp = 0;
		for (String className : DUNGEON_CLASS_NAMES) {
			SkillsStruct classInfo = player.getDungeonClass(className);
			if (classInfo != null) {
				classSbXp += Math.min(classInfo.currentLevel(), 50) * 4;
			}
		}
		eb.appendDescription("\nClass Level Up: " + formatNumber(classSbXp) + " / 1000");

		// Regular floor completions
		int floorCompletionSbXp = 0;
		for (Map.Entry<String, JsonElement> completion : higherDepth(
			player.profileJson(),
			"dungeons.dungeon_types.catacombs.tier_completions"
		)
			.getAsJsonObject()
			.entrySet()) {
			if (completion.getValue().getAsInt() > 0) {
				if (Integer.parseInt(completion.getKey()) <= 4) {
					floorCompletionSbXp += 20;
				} else {
					floorCompletionSbXp += 30;
				}
			}
		}
		eb.appendDescription("\nComplete The Catacombs: " + formatNumber(floorCompletionSbXp) + " / 190");

		// Master flor completions
		int masterFloorCompletionSbXp = 0;
		for (Map.Entry<String, JsonElement> completion : higherDepth(
			player.profileJson(),
			"dungeons.dungeon_types.master_catacombs.tier_completions"
		)
			.getAsJsonObject()
			.entrySet()) {
			if (completion.getValue().getAsInt() > 0) {
				masterFloorCompletionSbXp += 50;
			}
		}
		eb.appendDescription("\nComplete The Catacombs Master Mode: " + formatNumber(masterFloorCompletionSbXp) + " / 350");

		// Dungeon tasks total
		String totalSbXp = formatNumber(catacombsSbXp + classSbXp + floorCompletionSbXp + masterFloorCompletionSbXp) + " / 2,760";
		eb.getDescriptionBuilder().insert(0, "Dungeon Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getEssenceShopTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Essence Shop Tasks");

		// Essence shop tasks total
		String totalSbXp = formatNumber(0) + " / 856";
		eb.getDescriptionBuilder().insert(0, "Essence Shop Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getSlayingTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Slaying Tasks");

		// Slaying tasks total
		String totalSbXp = formatNumber(0) + " / 6,125";
		eb.getDescriptionBuilder().insert(0, "Slaying Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getSkillRelatedTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Skill Related Tasks");

		// 3 fields: mining, farming, fishing (field title has sum of each skill xp)

		// Skill related tasks total
		String totalSbXp = formatNumber(0) + " / 4,085";
		eb.getDescriptionBuilder().insert(0, "Skill Related Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getMiscellaneousTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Miscellaneous Tasks");

		// Miscellaneous tasks total
		String totalSbXp = formatNumber(0) + " / 1,351";
		eb.getDescriptionBuilder().insert(0, "Miscellaneous Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private static LevelRecord getStoryTasks(Player player) {
		EmbedBuilder eb = player.defaultPlayerEmbed(" | Story Tasks");

		// Story tasks total
		String totalSbXp = formatNumber(0) + " / 105";
		eb.getDescriptionBuilder().insert(0, "Story Tasks: " + totalSbXp + "\n");

		return new LevelRecord(eb, totalSbXp);
	}

	private record LevelRecord(EmbedBuilder eb, String total) {}
}
