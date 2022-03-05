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

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class HypixelSlashCommand extends SlashCommand {

	public HypixelSlashCommand() {
		this.name = "hypixel";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "player" -> event.embed(HypixelCommand.getHypixelStats(event.player));
			case "parkour" -> event.embed(HypixelCommand.getParkourStats(event.player));
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main hypixel command")
			.addSubcommands(
				new SubcommandData("player", "Get Hypixel information about a player")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			)
			.addSubcommands(
				new SubcommandData("parkour", "Get fastest Hypixel lobby parkour for a player")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
