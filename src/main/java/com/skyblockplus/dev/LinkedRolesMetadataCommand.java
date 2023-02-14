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

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import org.apache.http.message.BasicHeader;
import org.springframework.stereotype.Component;

@Component
public class LinkedRolesMetadataCommand extends Command {

	public LinkedRolesMetadataCommand() {
		this.name = "d-linked-roles";
		this.ownerCommand = true;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				JsonArray body = new JsonArray();

				JsonObject role = new JsonObject();
				role.addProperty("key", "verified");
				role.addProperty("name", "Verified");
				role.addProperty("description", "Hypixel account linked to the bot");
				role.addProperty("type", 7);
				body.add(role);

				body.add(generateNumericRole("level", "Skyblock Level"));
				body.add(generateNumericRole("networth", "Networth"));
				body.add(generateNumericRole("weight", "Senither Weight"));
				body.add(generateNumericRole("lily_weight", "Lily Weight"));

				JsonElement response = putJson(
					"https://discord.com/api/v10/applications/" + selfUserId + "/role-connections/metadata",
					body,
					new BasicHeader("Authorization", "Bot " + BOT_TOKEN)
				);
				event.getChannel().sendMessage(makeHastePost(response)).queue();
			}
		}
			.queue();
	}

	private static JsonObject generateNumericRole(String key, String name) {
		JsonObject role = new JsonObject();
		role.addProperty("key", key);
		role.addProperty("name", name);
		role.addProperty("description", name + " (set this to 1)");
		role.addProperty("type", 2);
		return role;
	}
}
