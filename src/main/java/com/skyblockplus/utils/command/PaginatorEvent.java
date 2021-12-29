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

package com.skyblockplus.utils.command;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class PaginatorEvent {

	private final boolean isSlashCommand;
	private final SlashCommandExecutedEvent slashCommand;
	private final CommandEvent command;

	public PaginatorEvent(Object event) {
		if (event instanceof SlashCommandExecutedEvent e) {
			isSlashCommand = true;
			slashCommand = e;
			command = null;
		} else if (event instanceof CommandEvent e) {
			isSlashCommand = false;
			slashCommand = null;
			command = e;
		} else {
			throw new IllegalArgumentException("Invalid event class type provided: " + event.getClass());
		}
	}

	public boolean isSlashCommand() {
		return isSlashCommand;
	}

	public SlashCommandExecutedEvent getSlashCommand() {
		return slashCommand;
	}

	public CommandEvent getCommand() {
		return command;
	}

	public User getUser() {
		return isSlashCommand ? slashCommand.getUser() : command.getAuthor();
	}

	public void paginate(CustomPaginator.Builder builder) {
		paginate(builder, 0);
	}

	public void paginate(CustomPaginator.Builder builder, int page) {
		if (isSlashCommand) {
			builder.build().paginate(slashCommand.getHook(), page);
		} else {
			builder.build().paginate(command.getChannel(), page);
		}
	}

	public Guild getGuild() {
		return isSlashCommand ? slashCommand.getGuild() : command.getGuild();
	}

	public MessageChannel getChannel() {
		return isSlashCommand ? slashCommand.getChannel() : command.getChannel();
	}

	public Member getMember() {
		return isSlashCommand ? slashCommand.getMember() : command.getMember();
	}
}
