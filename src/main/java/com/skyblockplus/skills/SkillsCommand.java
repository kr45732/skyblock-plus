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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.EmbedBuilder;

public class SkillsCommand extends Command {

	public SkillsCommand() {
		this.name = "skills";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "skill" };
	}

	public static EmbedBuilder getPlayerSkill(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);

		if (player.isValid()) {
			double trueSA = 0;
			double progressSA = 0;
			EmbedBuilder eb = player.defaultPlayerEmbed();

			for (String skillName : ALL_SKILL_NAMES) {
				SkillsStruct skillInfo = player.getSkill(skillName);
				if (skillInfo != null) {
					eb.addField(
						SKILLS_EMOJI_MAP.get(skillName) + " " + capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel + ")",
						simplifyNumber(skillInfo.expCurrent) +
						" / " +
						simplifyNumber(skillInfo.expForNext) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.totalSkillExp) +
						"\nProgress: " +
						(skillInfo.skillLevel == skillInfo.maxSkillLevel ? "MAX" : roundProgress(skillInfo.progressToNext)),
						true
					);
					if (!COSMETIC_SKILL_NAMES.contains(skillName)) {
						trueSA += skillInfo.skillLevel;
						progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
					}
				} else {
					eb.addField(SKILLS_EMOJI_MAP.get(skillName) + " " + capitalizeString(skillName) + " (?) ", "Unable to retrieve", true);
				}
			}
			trueSA /= SKILL_NAMES.size();
			progressSA /= SKILL_NAMES.size();
			eb.setDescription("True skill average: " + roundAndFormat(trueSA) + "\nProgress skill average: " + roundAndFormat(progressSA));
			return eb;
		}
		return invalidEmbed(player.getFailCause());
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPlayerSkill(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
