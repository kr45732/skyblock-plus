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
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class FixApplicationSlashCommand extends SlashCommand {

	public FixApplicationSlashCommand() {
		this.name = "fix-application";
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.embed(FixApplicationCommand.getFixApplicationEmbed((TextChannel) event.getOption("channel").getAsGuildChannel(), event.getOptionInt("state", -1), event.getGuild()));
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Fix an application if it's broken")
				.addOptions(new OptionData(OptionType.CHANNEL, "channel", "Application channel", true).setChannelTypes(ChannelType.TEXT) ,
						new OptionData(OptionType.INTEGER, "state", "State of the application", true).setRequiredRange(0, 3));
	}
}
