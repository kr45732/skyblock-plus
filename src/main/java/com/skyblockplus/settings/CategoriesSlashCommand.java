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

import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class CategoriesSlashCommand extends SlashCommand {

	public CategoriesSlashCommand() {
		this.name = "categories";
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getCategories(event.getGuild()));
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get a list mapping all visible category names to their ids'");
	}

	public static EmbedBuilder getCategories(Guild guild) {
		StringBuilder ebString = new StringBuilder();
		for (Category category : guild.getCategories()) {
			ebString.append("\n• ").append(category.getName()).append(" ⇢ `").append(category.getId()).append("`");
		}

		return defaultEmbed("Guild Categories").setDescription(ebString.length() == 0 ? "None" : ebString.toString());
	}
}
