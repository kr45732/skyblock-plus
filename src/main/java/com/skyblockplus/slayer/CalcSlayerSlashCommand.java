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

package com.skyblockplus.slayer;

import com.skyblockplus.dungeons.CalcRunsCommand;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CalcSlayerSlashCommand extends SlashCommand {

	public CalcSlayerSlashCommand() {
		this.name = "calcruns";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(
			CalcSlayerCommand.getCalcSlayer(
				event.player,
				event.getOptionStr("profile"),
				event.getOptionStr("type"),
				event.getOptionInt("level", -1),
				event.getOptionInt("xp", -1)
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Calculate the number of slayer bosses needed to reach a certain level or xp amount")
			.addOptions(
				new OptionData(OptionType.INTEGER, "type", "Slayer type", true)
					.addChoice("Sven packmaster", "sven")
					.addChoice("Revenant horror", "rev")
					.addChoice("Tarantula broodfather", "tara")
					.addChoice("Voidgloom seraph", "enderman"),
				new OptionData(OptionType.INTEGER, "level", "Target slayer level").setRequiredRange(1, 9),
				new OptionData(OptionType.INTEGER, "xp", "Target slayer xp").setMinValue(1)
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
