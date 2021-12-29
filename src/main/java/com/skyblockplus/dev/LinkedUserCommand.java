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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
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
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 4) {
					if (args[1].equals("delete")) {
						switch (args[2]) {
							case "discordId" -> {
								database.deleteLinkedUserByDiscordId(args[3]);
								embed(defaultEmbed("Done"));
								return;
							}
							case "username" -> {
								database.deleteLinkedUserByMinecraftUsername(args[3]);
								embed(defaultEmbed("Done"));
								return;
							}
							case "uuid" -> {
								database.deleteLinkedUserByMinecraftUuid(args[3]);
								embed(defaultEmbed("Done"));
								return;
							}
						}
					}
				} else if (args.length == 2) {
					if (args[1].equals("all")) {
						if (getAllLinkedUsers(event)) {
							ebMessage.delete().queue();
							return;
						}
					}
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}

	private boolean getAllLinkedUsers(CommandEvent event) {
		JsonElement allSettings = gson.toJsonTree(database.getLinkedUsers());
		if (allSettings == null) {
			return false;
		}

		try {
			event.getChannel().sendMessage(makeHastePost(formattedGson.toJson(allSettings)) + ".json").queue();
			return true;
		} catch (Exception ignored) {}
		return false;
	}
}
