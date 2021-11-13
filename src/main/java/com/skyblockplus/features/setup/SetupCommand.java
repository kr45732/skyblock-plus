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

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

public class SetupCommand extends Command {

	public SetupCommand() {
		this.name = "setup";
		this.cooldown = globalCooldown;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		this.botPermissions = defaultPerms();
	}

	public static ActionRow getSetupActionRow() {
		return ActionRow.of(
			Button.primary("setup_command_verify", "Verification"),
			Button.primary("setup_command_apply", "Application"),
			Button.primary("setup_command_guild", "Guild Roles & Ranks"),
			Button.primary("setup_command_roles", "Skyblock Roles"),
			Button.primary("setup_command_prefix", "Prefix")
		);
	}

	public static EmbedBuilder getSetupEmbed() {
		return defaultEmbed("Setup").setDescription("Choose one of the buttons below to setup the corresponding feature");
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				ebMessage.editMessageEmbeds(getSetupEmbed().build()).setActionRows(getSetupActionRow()).queue();
			}
		}
			.queue();
	}
}
