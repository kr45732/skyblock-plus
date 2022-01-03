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

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class WeightSlashCommand extends SlashCommand {

	public WeightSlashCommand() {
		this.name = "weight";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "player" -> {
				if (event.invalidPlayerOption()) {
					return;
				}
				event.paginate(WeightCommand.getPlayerWeight(event.player, event.getOptionStr("profile"), new PaginatorEvent(event)));
			}
			case "calculate" -> event.embed(
				WeightCommand.calculateWeight(
					event.getOptionDouble("skill_average", 0),
					event.getOptionDouble("slayer", 0),
					event.getOptionDouble("catacombs", 0),
					event.getOptionDouble("average_class", 0)
				)
			);
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main weight command")
			.addSubcommands(
				new SubcommandData("player", "Get a player's weight")
					.addOption(OptionType.STRING, "player", "Player username or mention")
					.addOption(OptionType.STRING, "profile", "Profile name")
			)
			.addSubcommands(
				new SubcommandData("calculate", "Calculate predicted weight using given stats (not 100% accurate)")
					.addOptions(
						new OptionData(OptionType.NUMBER, "skill_average", "Player's skill average", true).setRequiredRange(0, 55),
						new OptionData(OptionType.NUMBER, "slayer", "Player's slayer XP", true).setRequiredRange(0, 500000000),
						new OptionData(OptionType.NUMBER, "dungeons", "Player's catacombs level", true).setRequiredRange(0, 50),
						new OptionData(OptionType.NUMBER, "average_class", "Player's average dungeon class level", true)
							.setRequiredRange(0, 50)
					)
			);
	}
}
