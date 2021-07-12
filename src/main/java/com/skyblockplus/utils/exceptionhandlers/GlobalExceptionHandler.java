package com.skyblockplus.utils.exceptionhandlers;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

	private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public GlobalExceptionHandler() {
		this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		if (errorLogChannel == null) {
			errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
		}

		try {
			StringBuilder outputStr = new StringBuilder("```java\n");
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
		if (errorLogChannel == null) {
			errorLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("864156114060705814");
		}

		try {
			StringBuilder outputStr = new StringBuilder("```java\n");
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
