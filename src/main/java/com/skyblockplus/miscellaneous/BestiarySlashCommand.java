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

import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.formatNumber;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.groovy.util.Maps;
import org.springframework.stereotype.Component;

@Component
public class BestiarySlashCommand extends SlashCommand {

	public static final Map<String, List<String>> locations = Maps.of(
		"Private Island",
		List.of(
			"family_cave_spider",
			"family_enderman_private",
			"family_skeleton",
			"family_slime",
			"family_spider",
			"family_witch",
			"family_zombie"
		),
		"Hub",
		List.of("family_unburried_zombie", "family_old_wolf", "family_ruin_wolf", "family_zombie_villager"),
		"Spiders Den",
		List.of(
			"family_arachne",
			"family_arachne_brood",
			"family_arachne_keeper",
			"family_brood_mother_spider",
			"family_dasher_spider",
			"family_respawning_skeleton",
			"family_random_slime",
			"family_spider_jockey",
			"family_splitter_spider",
			"family_voracious_spider",
			"family_weaver_spider"
		),
		"The End",
		List.of(
			"family_dragon",
			"family_enderman",
			"family_endermite",
			"family_corrupted_protector",
			"family_obsidian_wither",
			"family_voidling_extremist",
			"family_voidling_fanatic",
			"family_watcher",
			"family_zealot_enderman"
		),
		"Crimson Isles",
		List.of(
			"family_ashfang",
			"family_barbarian_duke_x",
			"family_bladesoul",
			"family_blaze",
			"family_flaming_spider",
			"family_ghast",
			"family_mage_outlaw",
			"family_magma_cube",
			"family_magma_cube_boss",
			"family_matcho",
			"family_charging_mushroom_cow",
			"family_pigman",
			"family_wither_skeleton",
			"family_wither_spectre"
		),
		"Deep Caverns",
		List.of(
			"family_automaton",
			"family_butterfly",
			"family_emerald_slime",
			"family_caverns_ghost",
			"family_goblin",
			"family_team_treasurite",
			"family_ice_walker",
			"family_lapis_zombie",
			"family_diamond_skeleton",
			"family_diamond_zombie",
			"family_redstone_pigman",
			"family_sludge",
			"family_invisible_creeper",
			"family_thyst",
			"family_treasure_hoarder",
			"family_worms",
			"family_yog"
		),
		"The Park",
		List.of("family_howling_spirit", "family_pack_spirit", "family_soul_of_the_alpha"),
		"Spooky",
		List.of(
			"family_batty_witch",
			"family_headless_horseman",
			"family_phantom_spirit",
			"family_scary_jerry",
			"family_trick_or_treater",
			"family_wither_gourd",
			"family_wraith"
		),
		"Catacombs",
		List.of(
			"family_diamond_guy",
			"family_cellar_spider",
			"family_crypt_dreadlord",
			"family_crypt_lurker",
			"family_crypt_souleater",
			"family_king_midas",
			"family_lonely_spider",
			"family_lost_adventurer",
			"family_scared_skeleton",
			"family_shadow_assassin",
			"family_skeleton_grunt",
			"family_skeleton_master",
			"family_skeleton_soldier",
			"family_skeletor",
			"family_sniper_skeleton",
			"family_super_archer",
			"family_super_tank_zombie",
			"family_crypt_tank_zombie",
			"family_watcher_summon_undead",
			"family_dungeon_respawning_skeleton",
			"family_crypt_witherskeleton",
			"family_zombie_commander",
			"family_zombie_grunt",
			"family_zombie_knight",
			"family_zombie_soldier"
		)
	);
	public static final List<String> bosses = List.of(
		"family_arachne",
		"family_brood_mother_spider",
		"family_dragon",
		"family_corrupted_protector",
		"family_ashfang",
		"family_barbarian_duke_x",
		"family_bladesoul",
		"family_mage_outlaw",
		"family_magma_cube_boss",
		"family_headless_horseman"
	);

	public BestiarySlashCommand() {
		this.name = "bestiary";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getBestiary(event.player, event.getOptionStr("profile"), new PaginatorEvent(event)));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's bestiary stats")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getBestiary(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();

			String bestiaryLevel = roundAndFormat(player.getBestiaryLevel());
			for (Map.Entry<String, List<String>> location : locations.entrySet()) {
				EmbedBuilder eb = player.defaultPlayerEmbed();
				eb.setDescription("**Bestiary Level:** " + bestiaryLevel + "\n**Location:** " + location.getKey());
				for (String mob : location.getValue()) {
					int kills = higherDepth(player.profileJson(), "bestiary.kills_" + mob, 0);
					String type = "MOB";
					if (location.getKey().equals("Private Island")) {
						type = "ISLAND";
					} else if (bosses.contains(mob)) {
						type = "BOSS";
					}
					String mobName = capitalizeString(mob.replace("family_", "").replace("_", " "));
					mobName =
						switch (mobName) {
							case "Enderman Private" -> "Enderman";
							case "Unburried Zombie" -> "Crypt Ghoul";
							case "Ruin Wolf" -> "Wolf";
							case "Arachne Brood" -> "Arachne's Brood";
							case "Arachne Keeper" -> "Arachne's Keeper";
							case "Brood Mother Spider" -> "Brood Mother";
							case "Respawning Skeleton" -> "Gravel Skeleton";
							case "Random Slime" -> "Rain Slime";
							case "Corrupted Protector" -> "Endstone Protector";
							case "Obsidian Wither" -> "Obsidian Defender";
							case "Zealot Enderman" -> "Zealot";
							case "Charging Mushroom Cow" -> "Mushroom Bull";
							case "Caverns Ghost" -> "Ghost";
							case "Team Treasurite" -> "Grunt";
							case "Diamond Skeleton" -> "Miner Skeleton";
							case "Diamond Zombie" -> "Miner Zombie";
							case "Invisible Creeper" -> "Sneaky Creeper";
							case "Worms" -> "Worm";
							case "Batty Witch" -> "Crazy Witch";
							case "Diamond Guy" -> "Angry Archeologist";
							case "Sniper Skeleton" -> "Sniper";
							case "Crypt Tank Zombie" -> "Tank Zombie";
							case "Watcher Summon Undead" -> "Undead";
							case "Dungeon Respawning Skeleton" -> "Undead Skeleton";
							case "Crypt Witherskeleton" -> "Withermancer";
							default -> mobName;
						};
					SkillsStruct level = levelingInfoFromExp(
						kills,
						"bestiary." + type,
						higherDepth(getLevelingJson(), "bestiary.caps." + type).getAsInt()
					);
					eb.addField(
						mobName,
						"Level: " +
						roundAndFormat(level.getProgressLevel()) +
						"\nKills: " +
						formatNumber(kills) +
						(
							(level.expForNext() - level.expCurrent() > 0)
								? "\nNext: " + formatNumber(level.expForNext() - level.expCurrent())
								: ""
						),
						true
					);
				}
				pages.put(SelectOption.of(location.getKey(), location.getKey().toLowerCase().replace(" ", "_")), eb);
			}

			new SelectMenuPaginator(pages, "private_island", new PaginatorExtras(), event);
			return null;
		}
		return player.getFailEmbed();
	}
}
