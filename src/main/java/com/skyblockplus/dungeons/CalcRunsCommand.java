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

package com.skyblockplus.dungeons;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import static com.skyblockplus.utils.Utils.*;

public class CalcRunsCommand extends Command {

	public CalcRunsCommand() {
		this.name = "calcruns";
		this.cooldown = globalCooldown;
		this.aliases = new String[]{"runs"};
		this.botPermissions = defaultPerms();
	}

	public static Object getCalcRuns(String username, String profileName, int targetLevel, int floor, boolean useRing) {
		if (targetLevel <= 0 || targetLevel > 50) {
			return invalidEmbed("Target level must be between 0 and 50");
		}
		if (floor < 0 || floor > 13) {
			return invalidEmbed("Invalid floor");
		}

		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			SkillsStruct current = player.getCatacombs();
			SkillsStruct target = player.skillInfoFromLevel(targetLevel, "catacombs");
			if (current.totalExp() >= target.totalExp()) {
				return invalidEmbed("You are already level " + targetLevel);
			}

			int completions = higherDepth(player.profileJson(), floor > 7 ? "dungeons.dungeon_types.master_catacombs.tier_completions." + (floor - 7) : "dungeons.dungeon_types.catacombs.tier_completions." + floor, 0);
			int runs = 0;

			int completionsCap = switch (floor){
				case 0, 1, 2, 3, 4, 5 -> 150;
				case 6 -> 100;
				default -> 50;
			};
			int baseXp = switch (floor){
				case 0 -> 50;
				case 1 -> 80;
				case 2 -> 160;
				case 3 -> 400;
				case 4 -> 1420;
				case 5 -> 2000;
				case 6 -> 4000;
				case 7 -> 20000;
				case 8 -> 10000;
				case 9 -> 15000;
				case 10 -> 36500;
				case 11 -> 48500;
				case 12 -> 70000;
				default -> 100000;
			};

			double xpNeeded = target.totalExp() - current.totalExp();
			for (int i = completions + 1; i <= completionsCap; i++) { // First 0 to completionsCap give different xp per run than after completionsCap
				double xpPerRun = (useRing ? 1.1 : 1.0) *  baseXp * (i / 100.0 + 1);
				xpNeeded -= xpPerRun;
				if (xpNeeded <= 0) {
					runs = i;
					break;
				}
			}

			if (xpNeeded > 0) {
				double xpPerRun = (useRing ? 1.1 : 1.0) * baseXp * (completionsCap / 100.0 + 1);
				runs = 	Math.max(0, completionsCap - completions) + (int) Math.ceil(xpNeeded / xpPerRun);
			}

			MessageBuilder mb = new MessageBuilder().setEmbeds(player.defaultPlayerEmbed().setDescription("**Current Level:** "  + roundAndFormat(player.getCatacombs().getProgressLevel()) + "\n**Target Level:** " + target.getProgressLevel() + "\n**XP Needed:** " + formatNumber(target.totalExp() - current.totalExp())  +"\n**" + (floor > 7 ? "M" + (floor - 7) : "F" + floor) + " Runs Needed:** " + formatNumber(runs)).build());
			if(!useRing) {
				mb.setActionRows(ActionRow.of(Button.primary("calc_runs_ring_" + player.getUuid() + "_" + player.getProfileName() + "_" + targetLevel + "_" + floor, "Calculate With Catacombs Expert Ring")));
			}
			return mb;
		}

		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				int level = getIntOption("level");
				String floor = getStringOption("floor", "").toLowerCase();
				if (level == -1 || floor.isEmpty()) {
					embed(invalidEmbed("Please provide the target level and floor"));
					return;
				}

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					int floorInt = -1;
					try {
						if (floor.contains("m")) {
							floorInt = 7 + Integer.parseInt(floor.split("m")[1]);
							if(floorInt <= 7){
								floorInt = -1;
							}
						} else {
							floorInt = Integer.parseInt(floor.split("f")[1]);
							if(floorInt > 7){
								floorInt = -1;
							}
						}
					} catch (Exception ignored) {}

					embed(getCalcRuns(player, args.length == 3 ? args[2] : null, level, floorInt, false));
					return;
				}

				sendErrorEmbed();
			}
		}
				.queue();
	}
}
