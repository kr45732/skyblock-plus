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

package com.skyblockplus.skills;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.groovy.util.Maps;
import org.springframework.stereotype.Component;

@Component
public class CrimsonCommand extends Command {

	public CrimsonCommand() {
		this.name = "crimson";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCrimson(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();

			JsonElement crimsonJson = higherDepth(player.profileJson(), "nether_island_player_data");

			int rep = higherDepth(crimsonJson, higherDepth(crimsonJson, "selected_faction", "None") + "_reputation", 0);
			String title;
			if (rep <= -1000) {
				title = "Hostile";
			} else if (rep < 0) {
				title = "Unfriendly";
			} else if (rep == 0) {
				title = "Neutral";
			} else if (rep <= 1000) {
				title = "Friendly";
			} else if (rep <= 3000) {
				title = "Trusted";
			} else if (rep <= 6000) {
				title = "Honored";
			} else {
				title = "Hero";
			}
			EmbedBuilder eb = player
				.defaultPlayerEmbed()
				.setDescription(
					"**Faction:** " +
					capitalizeString(higherDepth(crimsonJson, "selected_faction", "none")) +
					"\n**Reputation:** " +
					formatNumber(rep) +
					"\n**Title:** " +
					title
				);
			pages.put(SelectOption.of("Stats", "stats"), eb);

			List<String> allFish = List.of(
				"gusher",
				"obfuscated_fish_3",
				"golden_fish",
				"mana_ray",
				"sulphur_skitter",
				"lava_horse",
				"moldfin",
				"volcanic_stonefish",
				"slugfish",
				"skeleton_fish",
				"obfuscated_fish_1",
				"soul_fish",
				"vanille",
				"steaming_hot_flounder",
				"flyfish",
				"karate_fish",
				"obfuscated_fish_2",
				"blobfish"
			);
			JsonElement fishJson = higherDepth(player.profileJson(), "trophy_fish");
			eb =
				player
					.defaultPlayerEmbed()
					.setDescription(
						"**Total Caught:** " +
						formatNumber(higherDepth(fishJson, "total_caught", 0)) +
						"\n**Status:** " +
						switch (higherDepth(fishJson, "rewards.[-1]", 0)) {
							case 1 -> "Novice Fisher";
							case 2 -> "Adept Fisher";
							case 3 -> "Expert Fisher";
							case 4 -> "Master Fisher";
							default -> "None";
						}
					);
			for (String fish : allFish) {
				eb.addField(
					getEmoji(fish.toUpperCase() + "_BRONZE") + " " + capitalizeString(fish.replace("_", " ")),
					"Diamond: " +
					higherDepth(fishJson, fish + "_diamond", 0) +
					"\n Gold: " +
					higherDepth(fishJson, fish + "_gold", 0) +
					"\n Silver: " +
					higherDepth(fishJson, fish + "_silver", 0) +
					"\nBronze: " +
					higherDepth(fishJson, fish + "_bronze", 0),
					true
				);
			}
			pages.put(SelectOption.of("Trophy Fishing", "trophy_fishing"), eb);

			int basic = higherDepth(crimsonJson, "kuudra_completed_tiers.none", 0);
			int hot = higherDepth(crimsonJson, "kuudra_completed_tiers.hot", 0);
			int burning = higherDepth(crimsonJson, "kuudra_completed_tiers.burning", 0);
			int fiery = higherDepth(crimsonJson, "kuudra_completed_tiers.fiery", 0);
			int infernal = higherDepth(crimsonJson, "kuudra_completed_tiers.infernal", 0);
			eb =
				player
					.defaultPlayerEmbed()
					.setDescription(
						"**Total Kuudra Completions:** " +
						(basic + hot + burning + fiery + infernal) +
						"\n\n" +
						getEmoji("KUUDRA;0") +
						" Basic: " +
						basic +
						"\n" +
						getEmoji("KUUDRA_HOT_TIER_KEY") +
						" Hot: " +
						hot +
						"\n" +
						getEmoji("KUUDRA_BURNING_TIER_KEY") +
						" Burning: " +
						burning +
						"\n" +
						getEmoji("KUUDRA_FIERY_TIER_KEY") +
						" Fiery: " +
						fiery +
						"\n" +
						getEmoji("KUUDRA_INFERNAL_TIER_KEY") +
						" Infernal: " +
						infernal
					);
			pages.put(SelectOption.of("Kuudra", "kuudra"), eb);

			eb = player.defaultPlayerEmbed();
			Map<String, String> dojoQuests = Maps.of(
				"snake",
				"Swiftness",
				"archer",
				"Mastery",
				"mob_kb",
				"Force",
				"fireball",
				"Tenacity",
				"wall_jump",
				"Stamina",
				"sword_swap",
				"Discipline"
			);
			int totalPoints = 0;
			for (Map.Entry<String, String> dojoQuest : dojoQuests.entrySet()) {
				int points = higherDepth(crimsonJson, "dojo.dojo_points_" + dojoQuest.getKey(), 0);
				totalPoints += points;
				eb.addField(
					dojoQuest.getValue(),
					"Points: " +
					formatNumber(points) +
					"\nTime: " +
					toPrettyTime(higherDepth(crimsonJson, "dojo.dojo_time_" + dojoQuest.getKey(), 0)),
					true
				);
			}
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
			eb.setDescription(
				"⭐ **Total Points:** " +
				formatNumber(totalPoints) +
				"\n" +
				getEmoji("DOJO_" + belt.toUpperCase() + "_BELT") +
				" **Belt:** " +
				belt
			);
			pages.put(SelectOption.of("Dojo", "dojo"), eb);

			new SelectMenuPaginator(pages, "stats", new PaginatorExtras(), event);
			return null;
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
					return;
				}

				paginate(getCrimson(player, args.length == 3 ? args[2] : null, getPaginatorEvent()));
			}
		}
			.queue();
	}
}
