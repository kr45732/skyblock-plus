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

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
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
			case "player":
				if (event.invalidPlayerOption()) {
					return;
				}

				event.embed(
					WeightCommand.getPlayerWeight(
						event.player,
						event.getOptionStr("profile"),
						event.getOptionStr("type", "senither").equals("senither") ? Player.WeightType.SENITHER : Player.WeightType.LILY
					)
				);
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

	@Override
	public CommandData getCommandData() {
		return new CommandData("weight", "Main weight command")
			.addSubcommands(
				new SubcommandData("player", "Get a player's weight")
					.addOption(OptionType.STRING, "player", "Player username or mention")
					.addOption(OptionType.STRING, "profile", "Profile name")
					.addOptions(
						new OptionData(OptionType.STRING, "type", "The weight system which should be used")
							.addChoice("Senither", "senither")
							.addChoice("Lily", "lily")
					)
			)
			.addSubcommands(
				new SubcommandData("calculate", "Calculate predicted weight using given stats (not 100% accurate)")
					.addOption(OptionType.STRING, "skill_average", "Player's skill average", true)
					.addOption(OptionType.STRING, "slayer", "Player's slayer XP", true)
					.addOption(OptionType.STRING, "dungeons", "Player's catacombs level", true)
					.addOption(OptionType.STRING, "average_class", "Player's average dungeon class level", true)
			);
	}
}
