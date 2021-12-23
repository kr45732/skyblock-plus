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

import static com.skyblockplus.features.listeners.MainListener.*;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.defaultPerms;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;

public class ReloadCommand extends Command {

	public ReloadCommand() {
		this.name = "reload";
		this.cooldown = 45;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(getReloadEmbed(event.getGuild()));
			}
		}
			.queue();
	}

	public static EmbedBuilder getReloadEmbed(Guild guild) {
		return defaultEmbed("Reload Settings for " + guild.getName())
			.addField("Apply settings reload status", onApplyReload(guild.getId()), false)
			.addField("Verify settings reload status", onVerifyReload(guild.getId()), false)
			.addField("Mee6 roles reload status", onMee6Reload(guild.getId()), false);
	}
}
