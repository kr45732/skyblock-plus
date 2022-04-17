/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.features.event;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class EventGuild {

	public final AutomaticGuild parent;
	public boolean enable = false;
	public List<String> wantedEvents;
	public TextChannel channel;
	public Role role;

	public EventGuild(JsonElement eventSettings, AutomaticGuild parent) {
		this.parent = parent;
		reloadSettingsJson(eventSettings);
	}

	public void onEventNotif(Map<String, MessageEmbed> embeds) {
		try {
			if (enable) {
				if (!channel.canTalk()) {
					parent.logAction(
						defaultEmbed("Event Notifications")
							.setDescription("Missing permissions to view or send messages in " + channel.getAsMention())
					);
					return;
				}

				List<MessageEmbed> filteredEmbeds = embeds
					.entrySet()
					.stream()
					.filter(e -> wantedEvents.contains(e.getKey()))
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());

				if (!filteredEmbeds.isEmpty()) {
					if (role != null) {
						channel.sendMessage(role.getAsMention()).setEmbeds(filteredEmbeds).queue();
					} else {
						channel.sendMessageEmbeds(filteredEmbeds).queue();
					}
				}
			}
		} catch (Exception e) {
			AutomaticGuild.getLogger().error(parent.guildId, e);
		}
	}

	public void reloadSettingsJson(JsonElement eventSettings) {
		try {
			enable = higherDepth(eventSettings, "enable", false);
			if (enable) {
				channel = jda.getGuildById(parent.guildId).getTextChannelById(higherDepth(eventSettings, "channel").getAsString());
				try {
					role = jda.getGuildById(parent.guildId).getRoleById(higherDepth(eventSettings, "role").getAsString());
				} catch (Exception ignored) {}
				wantedEvents =
					streamJsonArray(higherDepth(eventSettings, "events").getAsJsonArray())
						.map(JsonElement::getAsString)
						.collect(Collectors.toList());
			}
		} catch (Exception e) {
			AutomaticGuild.getLogger().error(parent.guildId, e);
		}
	}
}
