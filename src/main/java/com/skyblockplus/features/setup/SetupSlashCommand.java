/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.features.setup;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class SetupSlashCommand extends SlashCommand {

	public SetupSlashCommand() {
		this.name = "setup";
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		new SetupCommandHandler(event.getHook(), event.getOptionStr("feature"));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Interactive walk-through on how to setup different features of the bot")
			.addOptions(
				new OptionData(OptionType.STRING, "feature", "Feature to setup", true)
					.addChoice("Automatic Verification", "verify")
					.addChoice("Automatic Guild Application, Roles & Ranks", "guild_name")
					.addChoice("Automatic Roles", "roles")
					.addChoice("Jacob Event Notifications", "jacob")
					.addChoice("Mayor Notifications", "mayor")
					.addChoice("Fetchur Notifications", "fetchur")
			);
	}
}
