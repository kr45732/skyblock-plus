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

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CalcRunsSlashCommand extends SlashCommand {

	public CalcRunsSlashCommand() {
		this.name = "calcruns";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(
			CalcRunsCommand.getCalcRuns(
				event.player,
				event.getOptionStr("profile"),
				event.getOptionInt("level", 1),
				event.getOptionInt("floor", 0),
				false
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Calculate the number of runs needed to reach a catacombs level")
			.addOptions(
				new OptionData(OptionType.INTEGER, "level", "Target catacombs level", true).setRequiredRange(1, 50),
				new OptionData(OptionType.INTEGER, "floor", "Catacombs or master catacombs floor", true)
					.addChoice("Entrance", 0)
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
			)
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
