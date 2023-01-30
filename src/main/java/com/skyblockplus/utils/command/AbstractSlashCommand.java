/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
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

import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.data.SerializableData;

public abstract class AbstractSlashCommand {

	protected final Permission[] botPermissions = defaultPerms(true);
	protected String name = "null";
	protected int cooldown = globalCooldown;
	protected boolean logCommand = true;
	protected Permission[] userPermissions = new Permission[0];

	protected void execute(SlashCommandEvent event) {}

	protected void run(SlashCommandEvent event) {
		slashCommandClient.getCommandUses().compute(getFullName(), (k, v) -> (v != null ? v : 0) + 1);

		int remainingCooldown = getRemainingCooldown(event);
		if (remainingCooldown > 0) {
			replyCooldown(event, remainingCooldown);
			return;
		}

		for (Permission p : botPermissions) {
			if (p.isChannel()) {
				if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), p)) {
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
				if (!event.getGuild().getSelfMember().hasPermission(p)) {
					event.embed(invalidEmbed("I need the " + p.getName() + " permission in this server!"));
					return;
				}
			}
		}

		if (!event.isOwner()) {
			for (Permission p : userPermissions) {
				if (event.getMember() == null) {
					continue;
				}

				if (p.isChannel()) {
					if (!event.getMember().hasPermission(event.getGuildChannel(), p)) {
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

		if (logCommand) {
			event.logCommand();
		}

		execute(event);
	}

	protected String getName() {
		return name;
	}

	private int getRemainingCooldown(SlashCommandEvent event) {
		if (!event.isOwner()) {
			String key = getFullName() + "|" + String.format("U:%d", event.getUser().getIdLong());
			int remaining = client.getRemainingCooldown(key);
			if (remaining > 0) {
				return remaining;
			}

			client.applyCooldown(key, cooldown);
		}

		return 0;
	}

	private void replyCooldown(SlashCommandEvent event, int remainingCooldown) {
		event
			.getHook()
			.editOriginalEmbeds(
				invalidEmbed("That command is on cooldown for " + remainingCooldown + " more second" + (remainingCooldown == 1 ? "" : "s"))
					.build()
			)
			.queue();
	}

	protected abstract SerializableData getCommandData();

	protected void onAutoComplete(AutoCompleteEvent event) {}

	protected abstract String getFullName();
}
