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

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CalcDragsSlashCommand extends SlashCommand {

	public CalcDragsSlashCommand() {
		this.name = "calcdrags";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.paginate(
			CalcDragsCommand.getCalcDrags(
				event.getOptionInt("position", 1),
				event.getOptionDouble("ratio", 1),
				event.getOptionInt("eyes", 8),
				new PaginatorEvent(event)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Calculate your loot quality and loot from dragons in the end")
			.addOptions(
				new OptionData(OptionType.INTEGER, "position", "Your position on damage dealt").setRequiredRange(1, 25),
				new OptionData(OptionType.NUMBER, "ratio", "Ratio of your damage to the 1st place's damage").setRequiredRange(0.0, 1.0),
				new OptionData(OptionType.INTEGER, "eyes", "Number of eyes you placed").setRequiredRange(0, 8)
			);
	}
}
