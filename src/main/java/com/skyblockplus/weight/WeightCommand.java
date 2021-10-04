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

package com.skyblockplus.weight;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.weight.senither.Weight;
import net.dv8tion.jda.api.EmbedBuilder;

public class WeightCommand extends Command {

	public WeightCommand() {
		this.name = "weight";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "we" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder calculateWeight(String skillAverage, String slayer, String catacombs, String averageDungeonClass) {
		try {
			double skillAverageD = Double.parseDouble(skillAverage);
			double slayerD = Double.parseDouble(slayer);
			double catacombsD = Double.parseDouble(catacombs);
			double averageDungeonClassD = Double.parseDouble(averageDungeonClass);
			EmbedBuilder eb = defaultEmbed("Weight Calculator");
			eb.setDescription("**Total Weight:** " + Weight.of(skillAverageD, slayerD, catacombsD, averageDungeonClassD));
			eb.addField("Slayer Weight", roundAndFormat(Weight.calculateSkillsWeight(skillAverageD)), false);
			eb.addField("Skills Weight", roundAndFormat(Weight.calculateSlayerWeight(slayerD)), false);
			eb.addField("Dungeons Weight", roundAndFormat(Weight.calculateDungeonsWeight(catacombsD, averageDungeonClassD)), false);
			return eb;
		} catch (NumberFormatException e) {
			return defaultEmbed("Invalid input");
		}
	}

	public static EmbedBuilder getPlayerWeight(String username, String profileName, Player.WeightType weightType) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			if(weightType == Player.WeightType.SENITHER) {
				Weight weight = new Weight(player);
				EmbedBuilder eb = player.defaultPlayerEmbed(" | Senither Weight");
				StringBuilder slayerStr = new StringBuilder();
				for (String slayerName : SLAYER_NAMES) {
					slayerStr
							.append(capitalizeString(slayerName))
							.append(": ")
							.append(weight.getSlayerWeight().getSlayerWeight(slayerName).getFormatted())
							.append("\n");
				}
				StringBuilder skillsStr = new StringBuilder();
				for (String skillName : SKILL_NAMES) {
					skillsStr
							.append(capitalizeString(skillName))
							.append(": ")
							.append(weight.getSkillsWeight().getSkillsWeight(skillName).getFormatted())
							.append("\n");
				}
				StringBuilder dungeonsStr = new StringBuilder();
				dungeonsStr
						.append(capitalizeString("catacombs"))
						.append(": ")
						.append(weight.getDungeonsWeight().getDungeonWeight("catacombs").getFormatted())
						.append("\n");
				for (String dungeonClassName : DUNGEON_CLASS_NAMES) {
					dungeonsStr
							.append(capitalizeString(dungeonClassName))
							.append(": ")
							.append(weight.getDungeonsWeight().getClassWeight(dungeonClassName).getFormatted())
							.append("\n");
				}

				eb.addField("Slayer | " + weight.getSlayerWeight().getWeightStruct().getFormatted(), slayerStr.toString(), false);
				eb.addField("Skills | " + weight.getSkillsWeight().getWeightStruct().getFormatted(), skillsStr.toString(), false);
				eb.addField("Dungeons | " + weight.getDungeonsWeight().getWeightStruct().getFormatted(), dungeonsStr.toString(), false);
				eb.setDescription("**Total Weight:** " + weight.getTotalWeight(false).getFormatted());
				return eb;
			}else{
				com.skyblockplus.weight.lily.Weight weight = new com.skyblockplus.weight.lily.Weight(player);
				EmbedBuilder eb = player.defaultPlayerEmbed(" | Lily Weight");
				StringBuilder slayerStr = new StringBuilder();
				for (String slayerName : SLAYER_NAMES) {
					slayerStr
							.append(capitalizeString(slayerName))
							.append(": ")
							.append(weight.getSlayerWeight().getSlayerWeight(slayerName).getFormatted())
							.append("\n");
				}
				StringBuilder skillsStr = new StringBuilder();
				for (String skillName : SKILL_NAMES) {
					skillsStr
							.append(capitalizeString(skillName))
							.append(": ")
							.append(weight.getSkillsWeight().getSkillsWeight(skillName).getFormatted())
							.append("\n");
				}
				String dungeonsStr = "Catacombs: " +
						weight.getDungeonsWeight().getDungeonWeight().getFormatted() +
						"\n" +
						"Normal floor completions: " +
						weight.getDungeonsWeight().getDungeonCompletionWeight("normal").getFormatted() +
						"\n" +
						"Master floor completions: " +
						weight.getDungeonsWeight().getDungeonCompletionWeight("master").getFormatted() +
						"\n";

				eb.addField("Slayer | " + weight.getSlayerWeight().getWeightStruct().getFormatted(), slayerStr.toString(), false);
				eb.addField("Skills | " + weight.getSkillsWeight().getWeightStruct().getFormatted(), skillsStr.toString(), false);
				eb.addField("Dungeons | " + weight.getDungeonsWeight().getWeightStruct().getFormatted(), dungeonsStr, false);
				eb.setDescription("**Total Weight:** " + weight.getTotalWeight(false).getFormatted());
				return eb;
			}
		}
		return invalidEmbed(player.getFailCause());
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 6 && args[1].equals("calculate")) {
					embed(calculateWeight(args[2], args[3], args[4], args[5]));
					return;
				} else if (args.length == 4 || args.length == 3 || args.length == 2 || args.length == 1) {
					Player.WeightType type = Player.WeightType.SENITHER;
					for (int i = 0; i < args.length; i++) {
						if (args[i].startsWith("type:")) {
							type = args[i].split("type:")[1].equals("lily") ? Player.WeightType.LILY : Player.WeightType.SENITHER;
							removeArg(i);
						}
					}

					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPlayerWeight(username, args.length == 3 ? args[2] : null, type));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
