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
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.stream.Collectors;

import static com.skyblockplus.features.jacob.JacobContest.CROP_NAME_TO_EMOJI;

public class JacobSlashCommand extends SlashCommand {

	public JacobSlashCommand() {
		this.name = "jacob";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.paginate(JacobCommand.getJacobEmbed(event.getOptionStr("crop", "all"), new PaginatorEvent(event)));
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get a list of upcoming farming contests")
				.addOptions(new OptionData(OptionType.STRING, "crop", "Crop to filter by")
						.addChoices(CROP_NAME_TO_EMOJI.keySet().stream().map(c -> new Command.Choice(c, c)).collect(Collectors.toList()))
				);
	}
}
