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

package com.skyblockplus.features.setup;

import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetupSlashCommand extends SlashCommand {

	public SetupSlashCommand() {
		this.name = "setup";
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getSetupEmbed());
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Interactive walk-throughs on how to setup different features");
	}

	public static MessageEditBuilder getSetupEmbed() {
		return new MessageEditBuilder()
			.setEmbeds(
				defaultEmbed("Setup")
					.setDescription(
						"Choose one of the buttons below to setup the corresponding feature. Note that setting up a feature may override previous settings."
					)
					.build()
			)
			.setComponents(
				ActionRow.of(
					Button.primary("setup_command_verify", "Verification"),
					Button.primary("setup_command_guild", "Guild Application, Roles & Ranks"),
					Button.primary("setup_command_roles", "Skyblock Roles")
				),
				ActionRow.of(
					Button.primary("setup_command_jacob", "Farming Event Notifications"),
					Button.primary("setup_command_mayor", "Mayor Notifications"),
					Button.primary("setup_command_fetchur", "Fetchur Notifications")
				)
			);
	}
}
