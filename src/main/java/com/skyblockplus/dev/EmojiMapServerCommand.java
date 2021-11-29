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

import static com.skyblockplus.utils.Utils.defaultPerms;
import static com.skyblockplus.utils.Utils.makeHastePost;

import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.entities.Emote;

public class EmojiMapServerCommand extends Command {

	public EmojiMapServerCommand() {
		this.name = "d-emojis";
		this.ownerCommand = true;
		this.aliases = new String[] { "d-emoji" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				logCommand();

				JsonObject toAdd = new JsonObject();
				for (Emote emote : event.getGuild().getEmotes()) {
					toAdd.addProperty(emote.getName().toUpperCase(), emote.getAsMention());
				}
				event.reply(makeHastePost(toAdd.toString()));
			}
		}
			.queue();
	}
}
