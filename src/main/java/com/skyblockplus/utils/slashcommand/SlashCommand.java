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

package com.skyblockplus.utils.slashcommand;

import static com.skyblockplus.Main.client;
import static com.skyblockplus.utils.Utils.*;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class SlashCommand {

	protected final int cooldown = globalCooldown;
	protected String name = "null";
	protected Permission[] botPermissions = defaultPerms();
	protected Permission[] userPermissions = new Permission[0];

	protected abstract void execute(SlashCommandExecutedEvent event);

	protected void run(SlashCommandExecutedEvent event) {
		for (Permission p : userPermissions) {
			if (p.isChannel()) {
				if (!event.getMember().hasPermission(event.getTextChannel(), p)) {
					event.embed(invalidEmbed("You must have the " + p.getName() + " permission in this channel to use that!"));
					return;
				}
			} else {
				if (!event.getMember().hasPermission(p)) {
					event.embed(invalidEmbed("You must have the " + p.getName() + " permission in this server to use that!"));
					return;
				}
			}
		}

		for (Permission p : botPermissions) {
			if (p.isChannel()) {
				if (!event.getSelfMember().hasPermission(event.getTextChannel(), p)) {
					if (p == Permission.MESSAGE_WRITE) {
						event
							.getUser()
							.openPrivateChannel()
							.queue(dm ->
								dm
									.sendMessageEmbeds(
										invalidEmbed(
											"I need the " + p.getName() + " permission in " + event.getTextChannel().getAsMention() + "!"
										)
											.build()
									)
									.queue(ignored -> {}, ignored -> {})
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

	public int getRemainingCooldown(SlashCommandExecutedEvent event) {
		String key = name + "|" + String.format("U:%d", event.getUser().getIdLong());
		int remaining = client.getRemainingCooldown(key);
		if (remaining > 0) {
			return remaining;
		} else {
			client.applyCooldown(key, cooldown);
		}

		return 0;
	}

	public void replyCooldown(SlashCommandExecutedEvent event, int remainingCooldown) {
		event
			.getHook()
			.editOriginalEmbeds(invalidEmbed("That command is on cooldown for " + remainingCooldown + " more seconds").build())
			.queue();
	}

	public abstract CommandData getCommandData();
}
