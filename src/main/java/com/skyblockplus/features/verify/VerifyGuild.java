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

package com.skyblockplus.features.verify;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class VerifyGuild {

	public TextChannel messageChannel;
	public Message originalMessage;
	public boolean enable = true;

	public VerifyGuild(TextChannel messageChannel, Message originalMessage) {
		this.messageChannel = messageChannel;
		this.originalMessage = originalMessage;
	}

	public VerifyGuild() {
		this.enable = false;
	}

	public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!enable) {
			return false;
		}

		if (!event.getChannel().getId().equals(messageChannel.getId())) {
			return false;
		}

		if (event.getMessage().getId().equals(originalMessage.getId())) {
			return false;
		}

		if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			if (event.getAuthor().isBot()) {
				return false;
			}

			if (!event.getMessage().getContentRaw().startsWith(getGuildPrefix(event.getGuild().getId()) + "link ")) {
				event.getMessage().delete().queue();
				return true;
			}
		}

		event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
		return true;
	}
}
