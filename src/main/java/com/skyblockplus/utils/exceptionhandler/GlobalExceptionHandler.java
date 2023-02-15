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

package com.skyblockplus.utils.exceptionhandler;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	public GlobalExceptionHandler() {
		this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		try {
			if (errorLogChannel == null) {
				errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
			}

			StringBuilder outputStr = new StringBuilder("```java\n" + e.toString() + ": " + e.getMessage() + "\n");
			for (StackTraceElement s : e.getStackTrace()) {
				outputStr.append(s.toString()).append("\n");
			}
			outputStr.append("\n```");

			errorLogChannel
				.sendMessageEmbeds(
					defaultEmbed("Error | " + t)
						.setDescription(
							outputStr.length() >= 4000
								? "Output is too large: " + makeHastePost(outputStr.toString())
								: outputStr.toString()
						)
						.build()
				)
				.queue();
		} catch (Exception ignored) {}

		if (t != null) {
			uncaughtExceptionHandler.uncaughtException(t, e);
		} else {
			log.error(e.getMessage(), e);
		}
	}

	public void uncaughtException(CommandEvent event, Command command, Throwable e) {
		try {
			if (errorLogChannel == null) {
				errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
			}

			StringBuilder outputStr = new StringBuilder("```java\n" + e.toString() + ": " + e.getMessage() + "\n");
			for (StackTraceElement s : e.getStackTrace()) {
				outputStr.append(s.toString()).append("\n");
			}
			outputStr.append("\n```");
			errorLogChannel
				.sendMessageEmbeds(
					defaultEmbed(
						"Error | " +
						(command != null ? command.getName() : "null") +
						" | " +
						(event != null ? event.getGuild().getId() : "null")
					)
						.setDescription(
							outputStr.length() >= 4000
								? "Output is too large: " + makeHastePost(outputStr.toString())
								: outputStr.toString()
						)
						.build()
				)
				.queue();
		} catch (Exception ignored) {}

		log.error(
			"Error | " + (command != null ? command.getName() : "null") + " | " + (event != null ? event.getGuild().getId() : "null"),
			e
		);
	}
}
