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

package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.database;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;

public class LinkedUserCommand extends Command {

	public LinkedUserCommand() {
		this.name = "d-linked";
		this.ownerCommand = true;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 4) {
					if (args[1].equals("delete")) {
						switch (args[2]) {
							case "discord" -> {
								event.getChannel().sendMessage("" + database.deleteByDiscord(args[3])).queue();
								return;
							}
							case "username" -> {
								event.getChannel().sendMessage("" + database.deleteByUsername(args[3])).queue();
								return;
							}
							case "uuid" -> {
								event.getChannel().sendMessage("" + database.deleteByUuid(args[3])).queue();
								return;
							}
						}
					}
				} else if (args.length == 2) {
					if (args[1].equals("all")) {
						event.getChannel().sendMessage(makeHastePost(formattedGson.toJson(database.getLinkedAccounts())) + ".json").queue();
						return;
					}
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
