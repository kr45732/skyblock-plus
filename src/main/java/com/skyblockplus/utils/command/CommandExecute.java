/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;

public abstract class CommandExecute extends CommandEvent {

	protected final Command command;
	protected Message ebMessage;
	protected String[] args;
	protected String player;
	protected EmbedBuilder eb;
	private final boolean sendLoadingEmbed;
	private boolean adminCommand;

	public CommandExecute(Command command, CommandEvent event) {
		this(command, event, true);
	}

	public CommandExecute(Command command, CommandEvent event, boolean sendLoadingEmbed) {
		super(event.getEvent(), event.getPrefix(), event.getArgs(), event.getClient());
		this.command = command;
		this.sendLoadingEmbed = sendLoadingEmbed;
	}

	protected abstract void execute();

	public void queue() {
		executor.submit(() -> {
			if (adminCommand && !guildMap.get(getGuild().getId()).isAdmin(getMember())) {
				reply("You are missing the required permissions or roles to use this command");
				return;
			}

			if (sendLoadingEmbed) {
				this.ebMessage =
					getChannel()
						//						.sendMessage(
						//							"**⚠️ Skyblock Plus will stop responding to message commands <t:1651377600:R>!** Please use slash commands instead. If you do not see slash commands from this bot, then please re-invite the bot using the link in " +
						//							getGuildPrefix(event.getGuild().getId()) +
						//							"invite."
						//						)
						.sendMessageEmbeds(loadingEmbed().build())
						.complete();
			}
			this.args = getMessage().getContentRaw().split("\\s+");
			execute();
		});
	}

	protected void logCommand() {
		com.skyblockplus.utils.Utils.logCommand(getGuild(), getAuthor(), getMessage().getContentRaw());
	}

	protected String[] setArgs(int limit) {
		args = getMessage().getContentRaw().split("\\s+", limit);
		return args;
	}

	protected void embed(EmbedBuilder embedBuilder) {
		ebMessage.editMessageEmbeds(embedBuilder.build()).queue(ignore, ignore);
	}

	protected void paginate(EmbedBuilder embedBuilder) {
		if (embedBuilder == null) {
			ebMessage.delete().queue(ignore, ignore);
		} else {
			ebMessage.editMessageEmbeds(embedBuilder.build()).queue(ignore, ignore);
		}
	}

	protected void sendErrorEmbed() {
		ebMessage.editMessageEmbeds(errorEmbed(command.getName()).build()).queue(ignore, ignore);
	}

	/**
	 * @return true if the command's author is not linked, false otherwise (the
	 * command's author is linked)
	 */
	protected boolean getAuthorUsername() {
		return getLinkedUser(getAuthor().getId());
	}

	/**
	 * @param index which arg index the mention is located at or -1 if author
	 * @return true if a mention was found and the mentioned user is not linked,
	 * otherwise false (no mention found or the user is linked)
	 */
	protected boolean getMentionedUsername(int index) {
		if (index == -1) {
			return getAuthorUsername();
		}

		player = args[index];
		Matcher matcher = Message.MentionType.USER.getPattern().matcher(args[index]);
		if (!matcher.matches()) {
			return false;
		}

		return getLinkedUser(matcher.group(1));
	}

	/**
	 * @param userId the user's Discord id
	 * @return true if the provided userId is not linked to the bot, otherwise false
	 * (the provided userId is linked)
	 */
	protected boolean getLinkedUser(String userId) {
		LinkedAccount linkedUserUsername = database.getByDiscord(userId);
		if (linkedUserUsername != null) {
			player = linkedUserUsername.uuid();
			return false;
		}

		ebMessage.editMessageEmbeds(invalidEmbed("<@" + userId + "> is not linked to the bot.").build()).queue(ignore, ignore);
		return true;
	}

	protected void removeArg(int index) {
		String[] newArgs = new String[args.length - 1];
		for (int i = 0, j = 0; i < args.length; i++) {
			if (i != index) {
				newArgs[j] = args[i];
				j++;
			}
		}
		args = newArgs;
	}

	protected boolean getBooleanArg(String match) {
		boolean arg = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(match)) {
				arg = true;
				removeArg(i);
			}
		}

		return arg;
	}

	protected String getStringOption(String match) {
		return getStringOption(match, null);
	}

	protected String getStringOption(String match, String defaultValue) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(match + ":")) {
				String arg = args[i].split(match + ":")[1];
				removeArg(i);
				return arg;
			}
		}

		return defaultValue;
	}

	protected int getIntOption(String match) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(match + ":")) {
				int arg = Integer.parseInt(args[i].split(match + ":")[1]);
				removeArg(i);
				return arg;
			}
		}

		return -1;
	}

	public CommandExecute setAdminCommand(boolean adminCommand) {
		this.adminCommand = adminCommand;
		return this;
	}
}
