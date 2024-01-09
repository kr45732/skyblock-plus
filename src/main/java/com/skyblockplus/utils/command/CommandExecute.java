/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.utils.Utils.*;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public abstract class CommandExecute extends CommandEvent {

	protected String[] args;
	protected String player;

	public CommandExecute(CommandEvent event) {
		this(event, 0);
	}

	public CommandExecute(CommandEvent event, int limit) {
		super(event.getEvent(), event.getPrefix(), event.getArgs(), event.getClient());
		executor.submit(() -> {
			String content = getMessage().getContentRaw();
			if (content.startsWith(getSelfUser().getAsMention())) {
				content = content.substring(getSelfUser().getAsMention().length() + 1);
			}
			this.args = content.split("\\s+", limit);
			execute();
		});
	}

	protected abstract void execute();

	/**
	 * @param index which arg index the mention is located at or -1 if author
	 * @return embed if a mention was found and the mentioned user is not linked, otherwise null (no
	 *     mention found or the user is linked)
	 */
	protected EmbedBuilder getMentionedUsername(int index) {
		if (index == -1) {
			return getLinkedUser(getAuthor().getId());
		}

		player = args[index];
		Matcher matcher = Message.MentionType.USER.getPattern().matcher(args[index]);
		if (!matcher.matches()) {
			return null;
		}

		return getLinkedUser(matcher.group(1));
	}

	/**
	 * @param userId the user's Discord id
	 * @return embed if the provided userId is not linked to the bot, otherwise null (the provided
	 *     userId is linked)
	 */
	protected EmbedBuilder getLinkedUser(String userId) {
		LinkedAccount linkedUserUsername = database.getByDiscord(userId);
		if (linkedUserUsername != null) {
			player = linkedUserUsername.uuid();
			return null;
		}

		return errorEmbed(
			"<@" +
			userId +
			"> is not linked to the bot. Please specify a username or " +
			(getAuthor().getId().equals(userId) ? "" : "have them ") +
			"link using `/link`."
		);
	}
}
