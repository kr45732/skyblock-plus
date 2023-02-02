/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

import static com.skyblockplus.features.listeners.MainListener.onApplyReload;
import static com.skyblockplus.features.listeners.MainListener.onVerifyReload;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class ReloadSlashCommand extends SlashCommand {

	public ReloadSlashCommand() {
		this.name = "reload";
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		this.cooldown = globalCooldown + 2;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getReloadEmbed(event.getGuild()));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Reload the guild application and verification settings");
	}

	public static EmbedBuilder getReloadEmbed(Guild guild) {
		return defaultEmbed("Reload Settings for " + guild.getName())
			.addField("Apply settings reload status", onApplyReload(guild.getId()), false)
			.addField("Verify settings reload status", onVerifyReload(guild.getId()), false);
	}
}
