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

package com.skyblockplus.miscellaneous.weight;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.StringUtils.capitalizeString;

import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class WeightSlashCommand extends SlashCommand {

	public WeightSlashCommand() {
		this.name = "weight";
	}

	public static EmbedBuilder getPlayerWeight(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(3);
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

			SenitherWeight weight = new SenitherWeight(player);
			EmbedBuilder eb = player.defaultPlayerEmbed(" | Senither Weight");
			StringBuilder slayerStr = new StringBuilder();
			for (String slayerName : SLAYER_NAMES) {
				if (slayerName.equals("blaze")) {
					continue;
				}
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
				.append(weight.getDungeonsWeight().getDungeonWeight().getFormatted())
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
			eb.setDescription("**Total Weight:** " + weight.getTotalWeight().getFormatted() + "\n**Stage:** " + weight.getStage());
			extras.addEmbedPage(eb);

			LilyWeight lilyWeight = new LilyWeight(player);
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
			lilyEb.setDescription(
				"**Total Weight:** " + lilyWeight.getTotalWeight().getFormatted() + "\n**Stage:** " + lilyWeight.getStage()
			);
			extras.addEmbedPage(lilyEb);

			event.paginate(paginateBuilder.setPaginatorExtras(extras));
			return null;
		}
		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerWeight(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's weight")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
