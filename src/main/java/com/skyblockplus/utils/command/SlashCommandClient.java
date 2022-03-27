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
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.Main;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class SlashCommandClient extends ListenerAdapter {

	private final List<SlashCommand> slashCommands;
	private final Map<String, Integer> commandUses = new HashMap<>();
	private String ownerId;

	public SlashCommandClient() {
		this.slashCommands = new ArrayList<>();
	}

	public SlashCommandClient addCommands(SlashCommand... commands) {
		for (SlashCommand command : commands) {
			if (slashCommands.stream().anyMatch(auction -> auction.getName().equalsIgnoreCase(command.getName()))) {
				Main.log.error("", new IllegalArgumentException("Command added has a name that has already been indexed: " + command.getName()));
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
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			event.replyEmbeds(invalidEmbed("This command cannot be used in direct messages").build()).queue();
			return;
		}
		if (event.getChannelType() == ChannelType.PRIVATE) {
			event.replyEmbeds(invalidEmbed("This command can only be used in text channels or threads").build()).queue();
			return;
		}

		if (guildMap.get(event.getGuild().getId()).channelBlacklist.contains(event.getChannel().getId())) {
			event.replyEmbeds(invalidEmbed("Commands cannot be used in this channel").build()).setEphemeral(true).queue();
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

		SlashCommandEvent slashCommandEvent = new SlashCommandEvent(event, this);
		for (SlashCommand command : slashCommands) {
			if (command.getName().equals(event.getName())) {
				commandUses.put(command.getName(), commandUses.getOrDefault(command.getName(), 0) + 1);
				int remainingCooldown = command.getRemainingCooldown(slashCommandEvent);
				if (remainingCooldown > 0) {
					command.replyCooldown(slashCommandEvent, remainingCooldown);
				} else {
					command.run(slashCommandEvent);
				}

				return;
			}
		}

		slashCommandEvent.getHook().editOriginalEmbeds(slashCommandEvent.invalidCommandMessage().build()).queue();
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getCommandType().equals(Command.Type.SLASH)) {
			return;
		}

		slashCommands
			.stream()
			.filter(c -> c.getName().equals(event.getName()))
			.findFirst()
			.ifPresent(c -> c.onAutoComplete(new AutoCompleteEvent(event)));
	}

	public List<SlashCommand> getCommands() {
		return slashCommands;
	}

	public boolean isOwner(String userId) {
		return userId.equals(ownerId);
	}

	public int getCommandUses(SlashCommand command) {
		return commandUses.getOrDefault(command.getName(), 0);
	}

	public void setCommandUses(Map<String, Integer> commandUsage) {
		commandUsage.forEach((key, value) -> commandUses.merge(key, value, Integer::sum));
	}
}
