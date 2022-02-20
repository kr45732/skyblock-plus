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

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
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
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "player" -> event.paginate(
				WeightCommand.getPlayerWeight(event.player, event.getOptionStr("profile"), new PaginatorEvent(event))
			);
			case "calculate" -> event.embed(
				WeightCommand.calculateWeight(
					event.player,
					event.getOptionStr("profile"),
					event.getOptionStr("type"),
					event.getOptionInt("amount", 0)
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
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "profile", "Profile name")
			)
			.addSubcommands(
				new SubcommandData("calculate", "Calculate predicted weight change for a reaching certain skill/slayer/catacombs amount")
					.addOptions(
						new OptionData(OptionType.STRING, "type", "Skill, slayer, or dungeon type to see change of", true)
							.addChoice("Sven", "sven")
							.addChoice("Revenant", "rev")
							.addChoice("Tarantula", "tara")
							.addChoice("Enderman", "enderman")
							.addChoice("Alchemy", "alchemy")
							.addChoice("Combat", "combat")
							.addChoice("Farming", "farming")
							.addChoice("Mining", "mining")
							.addChoice("Fishing", "fishing")
							.addChoice("Taming", "taming")
							.addChoice("Enchanting", "enchanting")
							.addChoice("Foraging", "foraging")
							.addChoice("Catacombs", "catacombs"),
						new OptionData(OptionType.INTEGER, "amount", "Target xp (slayers) or level", true).setRequiredRange(0, 500000000)
					)
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "profile", "Profile name")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
