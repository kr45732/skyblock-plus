/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.settings.SettingsExecute;
import com.skyblockplus.utils.command.CommandExecute;
import org.springframework.stereotype.Component;

@Component
public class DevSettingsCommand extends Command {

	public DevSettingsCommand() {
		this.name = "d-settings";
		this.ownerCommand = true;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				setArgs(4);

				if (args.length == 4) {
					switch (args[1]) {
						case "delete":
							switch (args[2]) {
								case "server" -> {
									embed(defaultEmbed("API returned response code " + database.deleteServerSettings(args[3])));
									return;
								}
								case "apply_cache" -> {
									setArgs(5);
									embed(
										defaultEmbed("API returned response code " + database.deleteApplyCacheSettings(args[3], args[4]))
									);
									return;
								}
								case "skyblock_event" -> {
									embed(
										defaultEmbed(
											"API returned response code " + database.setSkyblockEventSettings(args[3], new EventSettings())
										)
									);
									return;
								}
							}
							break;
						case "verify":
							if (args[2].equals("disable")) {
								embed(new SettingsExecute(jda.getGuildById(args[3]), event.getEvent()).setVerifyEnable(false));
								guildMap.get(args[3]).reloadVerifyGuild(args[3]);
								return;
							}
							break;
						case "jacob":
							if (args[2].equals("disable")) {
								embed(new SettingsExecute(jda.getGuildById(args[3]), event.getEvent()).setJacobEnable(false));
								return;
							}
							break;
						case "mayor":
							if (args[2].equals("disable")) {
								embed(new SettingsExecute(jda.getGuildById(args[3]), event.getEvent()).setMayorChannel("none"));
								return;
							}
							break;
						case "apply":
							if (args[2].equals("disable")) {
								setArgs(5);
								embed(
									new SettingsExecute(jda.getGuildById(args[3]), event.getEvent())
										.setApplyEnable(database.getGuildSettings(args[3], args[4]).getAsJsonObject(), false)
								);
								guildMap.get(args[3]).reloadApplyGuilds(args[3]);
								return;
							}
							break;
					}
				}

				embed(errorEmbed("Invalid input"));
			}
		}
			.queue();
	}
}
