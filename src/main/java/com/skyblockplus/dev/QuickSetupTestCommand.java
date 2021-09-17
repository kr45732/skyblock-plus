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
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

public class QuickSetupTestCommand extends Command {

	public QuickSetupTestCommand() {
		this.name = "d-settings";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(4);

				if (args.length == 4) {
					if (args[1].equals("roles")) {
						embed(setRoleSettings(args[2], args[3], event));
						return;
					} else if (args[1].equals("delete")) {
						switch (args[2]) {
							case "server":
								embed(deleteServer(args[3]));
								return;
							case "apply_cache":
								setArgs(5);
								embed(deleteServerApplyCache(args[3], args[4]));
								return;
							case "skyblock_event":
								embed(deleteSkyblockEvent(args[3]));
								return;
						}
					}
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder deleteSkyblockEvent(String serverId) {
		return defaultEmbed("API returned response code " + database.setSkyblockEventSettings(serverId, new EventSettings()));
	}

	private EmbedBuilder deleteServerApplyCache(String serverId, String name) {
		if (database.getServerSettings(serverId) != null) {
			return defaultEmbed("API returned response code " + database.deleteApplyCacheSettings(serverId, name));
		}
		return defaultEmbed("Error updating settings");
	}

	private EmbedBuilder deleteServer(String serverId) {
		if (database.getServerSettings(serverId) != null) {
			return defaultEmbed("API returned response code " + database.deleteServerSettings(serverId));
		}
		return defaultEmbed("Error updating settings");
	}

	private EmbedBuilder setRoleSettings(String roleName, String json, CommandEvent event) {
		try {
			JsonElement jsonElement = gson.toJsonTree(gson.fromJson(json, RoleModel.class));
			if (higherDepth(database.getServerSettings(event.getGuild().getId()), "serverId") == null) {
				database.addNewServerSettings(
					event.getGuild().getId(),
					new ServerSettingsModel(event.getGuild().getName(), event.getGuild().getId())
				);
			}

			int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, jsonElement);
			return defaultEmbed("API returned response code: " + responseCode);
		} catch (Exception ignored) {}
		return defaultEmbed("Error updating settings");
	}
}
