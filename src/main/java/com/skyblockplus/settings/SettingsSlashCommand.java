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

package com.skyblockplus.settings;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SettingsSlashCommand extends SlashCommand {

	public SettingsSlashCommand() {
		this.name = "settings";
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		String content = "settings " + event.getOptionStr("command", "");
		if (!content.contains("hypixel_key")) {
			event.logCommand();
		}

		event.paginate(
			new SettingsExecute(event.getGuild(), event.getChannel(), event.getUser()).getSettingsEmbed(content, content.split("\\s+")),
			true
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Main settings command").addOption(OptionType.STRING, "command", "Subcommand to execute");
	}
}
