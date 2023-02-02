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

import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.springframework.stereotype.Component;

@Component
public class BestiarySlashCommand extends SlashCommand {

	public BestiarySlashCommand() {
		this.name = "bestiary";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getBestiary(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's bestiary stats")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(Constants.profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getBestiary(String username, String profileName, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();

			String bestiaryLevel = roundAndFormat(player.getBestiaryLevel());
			for (Map.Entry<String, List<String>> location : Constants.bestiaryLocationToFamilies.entrySet()) {
				EmbedBuilder eb = player.defaultPlayerEmbed();
				eb.setDescription("**Bestiary Level:** " + bestiaryLevel + "\n**Location:** " + location.getKey());
				for (String mob : location.getValue()) {
					int kills = higherDepth(player.profileJson(), "bestiary.kills_" + mob, 0);
					String type = "MOB";
					if (location.getKey().equals("Private Island")) {
						type = "ISLAND";
					} else if (Constants.bestiaryBosses.contains(mob)) {
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

			new SelectMenuPaginator("private_island", new PaginatorExtras().setSelectPages(pages), event);
			return null;
		}
		return player.getFailEmbed();
	}
}
