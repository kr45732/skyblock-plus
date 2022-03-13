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

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CalcDropsSlashCommand extends SlashCommand {

	public CalcDropsSlashCommand() {
		this.name = "calcdrops";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.paginate(
			CalcDropsCommand.getCalcDrops(
				event.getOptionInt("floor", 1),
				event.getOptionInt("luck", 1),
				event.getOptionStr("accessory", "A"),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Calculate the number of runs needed to reach a catacombs level")
			.addOptions(
				new OptionData(OptionType.INTEGER, "floor", "Catacombs or master catacombs floor", true)
					.addChoice("Floor 1", 1)
					.addChoice("Floor 2", 2)
					.addChoice("Floor 3", 3)
					.addChoice("Floor 4", 4)
					.addChoice("Floor 5", 5)
					.addChoice("Floor 6", 6)
					.addChoice("Floor 7", 7)
					.addChoice("Master Floor 1", 8)
					.addChoice("Master Floor 2", 9)
					.addChoice("Master Floor 3", 10)
					.addChoice("Master Floor 4", 11)
					.addChoice("Master Floor 5", 12)
					.addChoice("Master Floor 6", 13)
					.addChoice("Master Floor 7", 14),
				new OptionData(OptionType.STRING, "accessory", "Catacombs accessory")
					.addChoice("None", "A")
					.addChoice("Talisman", "B")
					.addChoice("Ring", "C")
					.addChoice("Artifact", "D"),
				new OptionData(OptionType.INTEGER, "luck", "Boss luck level").setRequiredRange(1, 5)
			);
	}
}
