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

import static com.skyblockplus.utils.Utils.invalidEmbed;

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class SlashCommandClient extends ListenerAdapter {

	private final List<SlashCommand> slashCommands;
	private String ownerId;

	public SlashCommandClient() {
		this.slashCommands = new ArrayList<>();
	}

	public SlashCommandClient addSlashCommands(SlashCommand... commands) {
		for (SlashCommand command : commands) {
			if (slashCommands.stream().anyMatch(auction -> auction.getName().equalsIgnoreCase(command.getName()))) {
				throw new IllegalArgumentException("Command added has a name that has already been indexed: " + command.getName());
			} else {
				slashCommands.add(command);
			}
		}
		return this;
	}

	public SlashCommandClient setOwnerId(String ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (!event.isFromGuild()) {
			event.replyEmbeds(invalidEmbed("This command cannot be used in direct messages").build()).queue();
			return;
		}
		if (event.getChannelType() != ChannelType.TEXT) {
			event.replyEmbeds(invalidEmbed("This command can only be used in text channels").build()).queue();
			return;
		}

		try {
			event.deferReply().complete();
		} catch (ErrorResponseException e) {
			if (e.getErrorCode() != ErrorResponse.UNKNOWN_INTERACTION.getCode()) {
				throw e;
			}
			return;
		}

		SlashCommandExecutedEvent slashCommandExecutedEvent = new SlashCommandExecutedEvent(event, this);
		for (SlashCommand command : slashCommands) {
			if (command.getName().equals(event.getName())) {
				int remainingCooldown = command.getRemainingCooldown(slashCommandExecutedEvent);
				if (remainingCooldown > 0) {
					command.replyCooldown(slashCommandExecutedEvent, remainingCooldown);
				} else {
					command.run(slashCommandExecutedEvent);
				}

				return;
			}
		}

		slashCommandExecutedEvent.getHook().editOriginalEmbeds(slashCommandExecutedEvent.invalidCommandMessage().build()).queue();
	}

	public List<SlashCommand> getSlashCommands() {
		return slashCommands;
	}

	public boolean isOwner(String userId) {
		return ownerId != null ? userId.equals(ownerId) : false;
	}
}
