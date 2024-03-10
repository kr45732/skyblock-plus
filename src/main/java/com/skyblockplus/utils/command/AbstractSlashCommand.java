/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.utils.structs.AutoCompleteEvent;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.data.SerializableData;

public abstract class AbstractSlashCommand {

	protected final Permission[] botPermissions = defaultPerms();

	@Getter
	protected String name = "null";

	protected int cooldown = GLOBAL_COOLDOWN;
	protected Permission[] userPermissions = new Permission[0];

	protected void execute(SlashCommandEvent event) {}

	protected void run(SlashCommandEvent event) {
		slashCommandClient.getCommandUses().compute(getFullName(), (k, v) -> (v != null ? v : 0) + 1);

		if (!event.isOwner()) {
			int remainingCooldown = getRemainingCooldown(event);
			if (remainingCooldown > 0) {
				event
					.replyEmbeds(
						errorEmbed(
							"That command is on cooldown for " + remainingCooldown + " more second" + (remainingCooldown == 1 ? "" : "s")
						)
							.build()
					)
					.setEphemeral(true)
					.queue();
				return;
			}
		}

		for (Permission p : botPermissions) {
			if (p.isChannel()) {
				if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), p)) {
					event
						.replyEmbeds(errorEmbed("I need the " + p.getName() + " permission in this channel!").build())
						.setEphemeral(true)
						.queue();
					return;
				}
			} else if (!event.getGuild().getSelfMember().hasPermission(p)) {
				event
					.replyEmbeds(errorEmbed("I need the " + p.getName() + " permission in this server!").build())
					.setEphemeral(true)
					.queue();
				return;
			}
		}

		if (!event.isOwner()) {
			for (Permission p : userPermissions) {
				if (p.isChannel()) {
					if (!event.getMember().hasPermission(event.getGuildChannel(), p)) {
						event
							.replyEmbeds(
								errorEmbed("You must have the " + p.getName() + " permission in this channel to use that!").build()
							)
							.setEphemeral(true)
							.queue();
						return;
					}
				} else if (p == Permission.ADMINISTRATOR) {
					if (!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())) {
						event
							.replyEmbeds(errorEmbed("You are missing the required permissions or roles to use this command").build())
							.setEphemeral(true)
							.queue();
						return;
					}
				} else if (!event.getMember().hasPermission(p)) {
					event
						.replyEmbeds(errorEmbed("You must have the " + p.getName() + " permission in this server to use that!").build())
						.setEphemeral(true)
						.queue();
					return;
				}
			}
		}

		event.logCommand();

		executor.submit(() -> {
			try {
				event.deferReply().complete();
			} catch (Exception e) {
				if (e instanceof ErrorResponseException ex) {
					if (ex.getErrorResponse() != ErrorResponse.UNKNOWN_INTERACTION) {
						globalExceptionHandler.uncaughtException(event, e);
					}
				}
				return;
			}
			try {
				execute(event);
			} catch (Exception e) {
				String logMessageId = globalExceptionHandler.uncaughtException(event, e);
				event
					.getHook()
					.editOriginalEmbeds(
						defaultEmbed("Command Error").setDescription("Please report to this to developer with id: " + logMessageId).build()
					)
					.queue();
			}
		});
	}

	private int getRemainingCooldown(SlashCommandEvent event) {
		String key = getFullName() + "|" + String.format("U:%d", event.getUser().getIdLong());
		int remaining = client.getRemainingCooldown(key);
		if (remaining > 0) {
			return remaining;
		} else {
			client.applyCooldown(key, cooldown);
			return 0;
		}
	}

	protected abstract SerializableData getCommandData();

	protected void onAutoComplete(AutoCompleteEvent event) {}

	protected abstract String getFullName();
}
