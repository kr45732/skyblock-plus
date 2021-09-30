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

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.skyblockplus.utils.Utils.invalidEmbed;

public class SlashCommandClient extends ListenerAdapter {

	private final List<SlashCommand> slashCommands;
	private final OffsetDateTime startTime;

	public SlashCommandClient() {
		this.slashCommands = new ArrayList<>();
		this.startTime = OffsetDateTime.now();
	}

	public SlashCommandClient addSlashCommands(SlashCommand... commands) {
		slashCommands.addAll(Arrays.asList(commands));
		return this;
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (event.getGuild() == null) {
			event.replyEmbeds(invalidEmbed("This command cannot be used in direct messages").build()).queue();
			return;
		}
		event.deferReply().complete();

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

	public OffsetDateTime getStartTime() {
		return startTime;
	}
}
