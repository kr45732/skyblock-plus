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

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.Permission;

public class CategoriesCommand extends Command {

	public CategoriesCommand() {
		this.name = "categories";
		this.cooldown = globalCooldown;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				StringBuilder ebString = new StringBuilder();
				for (net.dv8tion.jda.api.entities.Category category : event.getGuild().getCategories()) {
					ebString.append("\n• ").append(category.getName()).append(" ⇢ `").append(category.getId()).append("`");
				}

				embed(defaultEmbed("Guild Categories").setDescription(ebString.length() == 0 ? "None" : ebString.toString()));
			}
		}
			.submit();
	}
}
