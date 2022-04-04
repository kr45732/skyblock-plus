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

package com.skyblockplus.utils.command;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class SlashCommand {

	protected final Permission[] botPermissions = defaultPerms();
	protected String name = "null";
	protected int cooldown = -1;
	protected Permission[] userPermissions = new Permission[0];

	protected abstract void execute(SlashCommandEvent event);

	protected void run(SlashCommandEvent event) {
		if (cooldown == -1) {
			Command command = client.getCommands().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
			cooldown = command != null ? command.getCooldown() : globalCooldown;
		}

		if (!event.isOwner()) {
			for (Permission p : userPermissions) {
				if (p.isChannel()) {
					if (!event.getMember().hasPermission(event.getTextChannel(), p)) {
						event.embed(invalidEmbed("You must have the " + p.getName() + " permission in this channel to use that!"));
						return;
					}
				} else {
					if (p == Permission.ADMINISTRATOR) {
						if (!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())) {
							event.embed(invalidEmbed("You are missing the required permissions or roles to use this command"));
							return;
						}
					} else {
						if (!event.getMember().hasPermission(p)) {
							event.embed(invalidEmbed("You must have the " + p.getName() + " permission in this server to use that!"));
							return;
						}
					}
				}
			}
		}

		for (Permission p : botPermissions) {
			if (p.isChannel()) {
				if (!event.getSelfMember().hasPermission(event.getGuildChannel(), p)) {
					if (p == Permission.MESSAGE_SEND) {
						event
							.getUser()
							.openPrivateChannel()
							.queue(dm ->
								dm
									.sendMessageEmbeds(
										invalidEmbed(
											"I need the " + p.getName() + " permission in " + event.getGuildChannel().getAsMention() + "!"
										)
											.build()
									)
									.queue(ignore, ignore)
							);
					} else {
						event.embed(invalidEmbed("I need the " + p.getName() + " permission in this channel!"));
					}
					return;
				}
			} else {
				if (!event.getSelfMember().hasPermission(p)) {
					event.embed(invalidEmbed("I need the " + p.getName() + " permission in this server!"));
					return;
				}
			}
		}

		executor.submit(() -> execute(event));
	}

	public String getName() {
		return name;
	}

	public int getRemainingCooldown(SlashCommandEvent event) {
		if (!event.isOwner()) {
			String key = name + "|" + String.format("U:%d", event.getUser().getIdLong());
			int remaining = client.getRemainingCooldown(key);
			if (remaining > 0) {
				return remaining;
			}

			client.applyCooldown(key, cooldown);
		}

		return 0;
	}

	public void replyCooldown(SlashCommandEvent event, int remainingCooldown) {
		event
			.getHook()
			.editOriginalEmbeds(invalidEmbed("That command is on cooldown for " + remainingCooldown + " more seconds").build())
			.queue();
	}

	public abstract CommandData getCommandData();

	public void onAutoComplete(AutoCompleteEvent event) {}
}
