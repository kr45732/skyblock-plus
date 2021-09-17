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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandClient extends ListenerAdapter {

	private final List<SlashCommand> slashCommands;
	private final HashMap<String, OffsetDateTime> cooldowns;
	private final OffsetDateTime startTime;

	public SlashCommandClient() {
		this.slashCommands = new ArrayList<>();
		this.cooldowns = new HashMap<>();
		this.startTime = OffsetDateTime.now();
	}

	public void addSlashCommands(SlashCommand... commands) {
		slashCommands.addAll(Arrays.asList(commands));
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (event.getGuild() == null) {
			event.reply("❌ This command cannot be used in Direct messages").queue();
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
					command._execute(slashCommandExecutedEvent);
				}

				return;
			}
		}

		slashCommandExecutedEvent.getHook().editOriginalEmbeds(slashCommandExecutedEvent.invalidCommandMessage().build()).queue();
	}

	public int getRemainingCooldown(String name) {
		if (cooldowns.containsKey(name)) {
			int time = (int) Math.ceil(OffsetDateTime.now().until(cooldowns.get(name), ChronoUnit.MILLIS) / 1000D);
			if (time <= 0) {
				cooldowns.remove(name);
				return 0;
			}
			return time;
		}
		return 0;
	}

	public void applyCooldown(String name, int seconds) {
		cooldowns.put(name, OffsetDateTime.now().plusSeconds(seconds));
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}
}
