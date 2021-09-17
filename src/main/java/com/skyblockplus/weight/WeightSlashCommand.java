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

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class WeightSlashCommand extends SlashCommand {

	public WeightSlashCommand() {
		this.name = "weight";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "player":
				if (event.invalidPlayerOption()) {
					return;
				}

				event.embed(WeightCommand.getPlayerWeight(event.player, event.getOptionStr("profile")));
				break;
			case "calculate":
				event.embed(
					WeightCommand.calculateWeight(
						event.getOptionStr("skill_average"),
						event.getOptionStr("slayer"),
						event.getOptionStr("catacombs"),
						event.getOptionStr("average_class")
					)
				);
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
