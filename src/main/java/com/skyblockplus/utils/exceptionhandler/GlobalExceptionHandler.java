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

package com.skyblockplus.utils.exceptionhandler;

import static com.skyblockplus.utils.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		try {
			if (errorLogChannel == null) {
				errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
			}

			String stackTrace = getStackTrace(e);
			String formattedStackTrace = "```java\n" + stackTrace + "\n```";

			errorLogChannel
				.sendMessageEmbeds(
					defaultEmbed("Error | " + t)
						.setDescription(
							formattedStackTrace.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH
								? "Stack Trace: " + makeHastePost(stackTrace)
								: formattedStackTrace
						)
						.build()
				)
				.queue();
		} catch (Exception ignored) {}

		log.error(e.getMessage(), e);
	}

	public String uncaughtException(SlashCommandEvent event, Throwable e) {
		if (errorLogChannel == null) {
			errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
		}

		String description = "**Guild:** " + event.getGuild().getName() + " (" + event.getGuild().getId() + ")";
		description += "\n**User:** " + event.getUser().getAsMention() + " (" + event.getUser().getId() + ")";
		description += "\n**Channel:** " + event.getChannel().getAsMention() + " (" + event.getChannel().getId() + ")";
		description += "\n**Command:** `" + event.getCommandFormatted() + "`";

		String stackTrace = getStackTrace(e);
		String formattedStackTrace = "```java\n" + stackTrace + "\n```";
		boolean stackTraceTooLarge = formattedStackTrace.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH;

		if (stackTraceTooLarge) {
			description += "\n**Stack Trace:** " + makeHastePost(stackTrace);
		}

		MessageCreateAction action = errorLogChannel.sendMessageEmbeds(
			defaultEmbed("Command error | " + event.getFullCommandName()).setDescription(description).build()
		);
		if (!stackTraceTooLarge) {
			action.addEmbeds(defaultEmbed(null).setDescription(formattedStackTrace).build());
		}
		String logMessageId = action.complete().getId();

		log.error("Command error | " + event.getFullCommandName() + " | " + logMessageId, e);

		return logMessageId;
	}

	public void uncaughtException(CommandEvent event, Command command, Throwable e) {
		if (errorLogChannel == null) {
			errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
		}

		String stackTrace = getStackTrace(e);
		String formattedStackTrace = "```java\n" + stackTrace + "\n```";

		String logMessageId = errorLogChannel
			.sendMessageEmbeds(
				defaultEmbed("Error | " + command.getName() + " | " + event.getGuild().getId())
					.setDescription(
						formattedStackTrace.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH
							? "Stack Trace: " + makeHastePost(stackTrace)
							: formattedStackTrace
					)
					.build()
			)
			.complete()
			.getId();

		log.error("Error | " + command.getName() + " | " + logMessageId, e);
	}
}
