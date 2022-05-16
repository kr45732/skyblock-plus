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
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class EventGuild {

	public final AutomaticGuild parent;
	public boolean enable = false;
	public List<RoleObject> wantedEvents;
	public TextChannel channel;

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

				Set<String> roleMentions = new HashSet<>();
				List<MessageEmbed> filteredEmbeds = new ArrayList<>();
				for (RoleObject wantedEvent : wantedEvents) {
					if (embeds.containsKey(wantedEvent.getValue())) {
						filteredEmbeds.add(embeds.get(wantedEvent.getValue()));
						roleMentions.add("<@&" + wantedEvent.getRoleId() + ">");
					}
				}

				if (!filteredEmbeds.isEmpty()) {
					channel.sendMessage(String.join(" ", roleMentions)).setEmbeds(filteredEmbeds).queue();
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
				channel.getId();
				wantedEvents =
					gson.fromJson(higherDepth(eventSettings, "events").getAsJsonArray(), new TypeToken<List<RoleObject>>() {}.getType());
			}
		} catch (Exception e) {
			enable = false;
			AutomaticGuild.getLogger().error(parent.guildId, e);
		}
	}
}
