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

package com.skyblockplus.miscellaneous.weight;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.miscellaneous.weight.senither.Weight;
import net.dv8tion.jda.api.EmbedBuilder;

public class WeightCommand extends Command {

	public WeightCommand() {
		this.name = "weight";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "we" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder calculateWeight(double skillAverage, double slayer, double catacombs, double averageDungeonClass) {
		try {
			EmbedBuilder eb = defaultEmbed("Weight Calculator");
			eb.setDescription("**Total Weight:** " + Weight.of(skillAverage, slayer, catacombs, averageDungeonClass));
			eb.addField("Slayer Weight", roundAndFormat(Weight.calculateSkillsWeight(skillAverage)), false);
			eb.addField("Skills Weight", roundAndFormat(Weight.calculateSlayerWeight(slayer)), false);
			eb.addField("Dungeons Weight", roundAndFormat(Weight.calculateDungeonsWeight(catacombs, averageDungeonClass)), false);
			return eb;
		} catch (NumberFormatException e) {
			return defaultEmbed("Invalid input");
		}
	}

	public static EmbedBuilder getPlayerWeight(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(3);
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

			Weight weight = new Weight(player);
			EmbedBuilder eb = player.defaultPlayerEmbed(" | Senither Weight");
			StringBuilder slayerStr = new StringBuilder();
			for (String slayerName : SLAYER_NAMES) {
				slayerStr
					.append(SLAYER_EMOJI_MAP.get(slayerName))
					.append(" ")
					.append(capitalizeString(slayerName))
					.append(": ")
					.append(weight.getSlayerWeight().getSlayerWeight(slayerName).getFormatted())
					.append("\n");
			}
			StringBuilder skillsStr = new StringBuilder();
			for (String skillName : SKILL_NAMES) {
				skillsStr
					.append(SKILLS_EMOJI_MAP.get(skillName))
					.append(" ")
					.append(capitalizeString(skillName))
					.append(": ")
					.append(weight.getSkillsWeight().getSkillsWeight(skillName).getFormatted())
					.append("\n");
			}
			StringBuilder dungeonsStr = new StringBuilder();

			dungeonsStr
				.append(DUNGEON_EMOJI_MAP.get("catacombs"))
				.append(" ")
				.append(capitalizeString("catacombs"))
				.append(": ")
				.append(weight.getDungeonsWeight().getDungeonWeight("catacombs").getFormatted())
				.append("\n");
			for (String dungeonClassName : DUNGEON_CLASS_NAMES) {
				dungeonsStr
					.append(DUNGEON_EMOJI_MAP.get(dungeonClassName))
					.append(" ")
					.append(capitalizeString(dungeonClassName))
					.append(": ")
					.append(weight.getDungeonsWeight().getClassWeight(dungeonClassName).getFormatted())
					.append("\n");
			}

			eb.addField("Slayer | " + weight.getSlayerWeight().getWeightStruct().getFormatted(), slayerStr.toString(), false);
			eb.addField("Skills | " + weight.getSkillsWeight().getWeightStruct().getFormatted(), skillsStr.toString(), false);
			eb.addField("Dungeons | " + weight.getDungeonsWeight().getWeightStruct().getFormatted(), dungeonsStr.toString(), false);
			eb.setDescription("**Total Weight:** " + weight.getTotalWeight().getFormatted());
			extras.addEmbedPage(eb);

			com.skyblockplus.miscellaneous.weight.lily.Weight lilyWeight = new com.skyblockplus.miscellaneous.weight.lily.Weight(player);
			EmbedBuilder lilyEb = player.defaultPlayerEmbed(" | Lily Weight");
			StringBuilder lilySlayerStr = new StringBuilder();
			for (String slayerName : SLAYER_NAMES) {
				lilySlayerStr
					.append(SLAYER_EMOJI_MAP.get(slayerName))
					.append(" ")
					.append(capitalizeString(slayerName))
					.append(": ")
					.append(lilyWeight.getSlayerWeight().getSlayerWeight(slayerName).getFormatted())
					.append("\n");
			}
			StringBuilder lilySkillsStr = new StringBuilder();
			for (String skillName : SKILL_NAMES) {
				lilySkillsStr
					.append(SKILLS_EMOJI_MAP.get(skillName))
					.append(" ")
					.append(capitalizeString(skillName))
					.append(": ")
					.append(lilyWeight.getSkillsWeight().getSkillsWeight(skillName).getFormatted())
					.append("\n");
			}
			String lilyDungeonsStr =
				DUNGEON_EMOJI_MAP.get("catacombs") +
				" Catacombs: " +
				lilyWeight.getDungeonsWeight().getDungeonWeight().getFormatted() +
				"\n" +
				DUNGEON_EMOJI_MAP.get("catacombs_1") +
				" Normal floor completions: " +
				lilyWeight.getDungeonsWeight().getDungeonCompletionWeight("normal").getFormatted() +
				"\n" +
				DUNGEON_EMOJI_MAP.get("master_catacombs_1") +
				" Master floor completions: " +
				lilyWeight.getDungeonsWeight().getDungeonCompletionWeight("master").getFormatted() +
				"\n";

			lilyEb.addField("Slayer | " + lilyWeight.getSlayerWeight().getWeightStruct().getFormatted(), lilySlayerStr.toString(), false);
			lilyEb.addField("Skills | " + lilyWeight.getSkillsWeight().getWeightStruct().getFormatted(), lilySkillsStr.toString(), false);
			lilyEb.addField("Dungeons | " + lilyWeight.getDungeonsWeight().getWeightStruct().getFormatted(), lilyDungeonsStr, false);
			lilyEb.setDescription("**Total Weight:** " + lilyWeight.getTotalWeight().getFormatted());
			extras.addEmbedPage(lilyEb);

			event.paginate(paginateBuilder.setPaginatorExtras(extras));
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

				if (args.length == 6 && args[1].equals("calculate")) {
					try {
						double skillAverage = Double.parseDouble(args[2]);
						double slayer = Double.parseDouble(args[3]);
						double catacombs = Double.parseDouble(args[4]);
						double averageDungeonClass = Double.parseDouble(args[5]);
						embed(calculateWeight(skillAverage, slayer, catacombs, averageDungeonClass));
					} catch (Exception e) {
						embed(invalidEmbed("One of the provided amounts are invalid."));
					}
					return;
				} else if (args.length == 4 || args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerWeight(username, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
