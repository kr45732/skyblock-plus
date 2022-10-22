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
import com.google.gson.reflect.TypeToken;
import com.skyblockplus.api.serversettings.eventnotif.EventObject;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class EventGuild {

	public final AutomaticGuild parent;
	public boolean enable = false;
	public List<EventObject> wantedEvents;

	public EventGuild(JsonElement eventSettings, AutomaticGuild parent) {
		this.parent = parent;
		reloadSettingsJson(eventSettings);
	}

	public void onEventNotif(Map<String, MessageEmbed> embeds) {
		try {
			if (enable) {
				List<EventObject> filteredEvents = new ArrayList<>();
				for (EventObject wantedEvent : wantedEvents) {
					if (embeds.containsKey(wantedEvent.getValue())) {
						TextChannel channel = jda.getGuildById(parent.guildId).getTextChannelById(wantedEvent.getChannelId());
						if (channel == null || !channel.canTalk()) {
							parent.logAction(
								defaultEmbed("Event Notifications")
									.setDescription("Missing permissions to view or send messages in <#" + wantedEvent.getChannelId() + ">")
							);
						} else {
							filteredEvents.add(wantedEvent);
						}
					}
				}

				for (Map.Entry<String, List<EventObject>> channels : filteredEvents
					.stream()
					.collect(Collectors.groupingBy(EventObject::getChannelId))
					.entrySet()) {
					jda
						.getGuildById(parent.guildId)
						.getTextChannelById(channels.getKey())
						.sendMessage(
							channels.getValue().stream().map(e -> "<@&" + e.getRoleId() + ">").distinct().collect(Collectors.joining(" "))
						)
						.setEmbeds(channels.getValue().stream().map(e -> embeds.get(e.getValue())).collect(Collectors.toList()))
						.queue();
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
				wantedEvents =
					gson.fromJson(higherDepth(eventSettings, "events").getAsJsonArray(), new TypeToken<List<EventObject>>() {}.getType());
			}
		} catch (Exception e) {
			enable = false;
			AutomaticGuild.getLogger().error(parent.guildId, e);
		}
	}
}
