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

package com.skyblockplus.utils.command;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public abstract class CommandExecute {

	protected final CommandEvent event;
	protected final Command command;
	protected Message ebMessage;
	protected String[] args;
	protected String username;
	protected EmbedBuilder eb;
	private boolean sendLoadingEmbed = true;

	public CommandExecute(Command command, CommandEvent event) {
		this.command = command;
		this.event = event;
	}

	public CommandExecute(Command command, CommandEvent event, boolean sendLoadingEmbed) {
		this.command = command;
		this.event = event;
		this.sendLoadingEmbed = sendLoadingEmbed;
	}

	protected abstract void execute();

	public void submit() {
		executor.submit(
			() -> {
				if (sendLoadingEmbed) {
					this.ebMessage =
						event
							.getChannel()
							.sendMessage(
								"**⚠️ Skyblock Plus will stop responding to message commands <t:1643806800:R>!** Please use slash commands instead. If you do not see slash commands from this bot, then please re-invite the bot using the link in " +
								getGuildPrefix(event.getGuild().getId()) +
								"invite."
							)
							.setEmbeds(loadingEmbed().build())
							.complete();
				}
				this.args = event.getMessage().getContentRaw().split("\\s+", 0);
				execute();
			}
		);
	}

	protected void logCommand() {
		com.skyblockplus.utils.Utils.logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());
	}

	protected void setArgs(int limit) {
		args = event.getMessage().getContentRaw().split("\\s+", limit);
	}

	protected void embed(EmbedBuilder embedBuilder) {
		ebMessage.editMessageEmbeds(embedBuilder.build()).queue();
	}

	protected void paginate(EmbedBuilder embedBuilder) {
		if (embedBuilder == null) {
			ebMessage.delete().queue();
		} else {
			ebMessage.editMessageEmbeds(embedBuilder.build()).queue();
		}
	}

	protected void sendErrorEmbed() {
		ebMessage.editMessageEmbeds(errorEmbed(command.getName()).build()).queue();
	}

	/**
	 * @return true if the command's author is not linked, false otherwise (the command's author is linked)
	 */
	protected boolean getAuthorUsername() {
		return getLinkedUser(event.getAuthor().getId());
	}

	/**
	 * @param index which arg index the mention is located at
	 * @return true if a mention was found and the mentioned user is not linked, otherwise false (no mention found or the user is linked)
	 */
	protected boolean getMentionedUsername(int index) {
		if (index == -1) {
			return getAuthorUsername();
		}

		username = args[index];
		Matcher matcher = Message.MentionType.USER.getPattern().matcher(args[index]);
		if (!matcher.matches()) {
			return false;
		}

		return getLinkedUser(matcher.group(1));
	}

	/**
	 * @param userId the user's Discord id
	 * @return true if the provided userId is not linked to the bot, otherwise false (the provided userId is linked)
	 */
	protected boolean getLinkedUser(String userId) {
		JsonElement linkedUserUsername = higherDepth(database.getLinkedUserByDiscordId(userId), "minecraftUuid");
		if (linkedUserUsername != null) {
			username = linkedUserUsername.getAsString();
			return false;
		}

		ebMessage.editMessageEmbeds(invalidEmbed("<@" + userId + "> is not linked to the bot.").build()).queue();
		return true;
	}
}
