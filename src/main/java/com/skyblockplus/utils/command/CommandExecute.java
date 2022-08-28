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

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Utils;
import java.util.List;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

public abstract class CommandExecute extends CommandEvent {

	private static final List<String> slashOnlyCommands = List.of(
		"forge",
		"guild-leaderboard",
		"calcweight",
		"sacks",
		"essence",
		"talisman",
		"setup",
		"armor",
		"jacob",
		"average",
		"event",
		"calcdrops",
		"bingo",
		"profiles",
		"calendar",
		"check-api",
		"party",
		"guild-statistics",
		"bids",
		"unlink",
		"guild-kicker",
		"wardrobe",
		"reload",
		"fetchur",
		"coinsperbit",
		"recipe",
		"pets",
		"calcdrags",
		"enderchest",
		"collections",
		"crimson",
		"viewauction",
		"uuid",
		"cakes",
		"bits",
		"storage",
		"guild-ranks",
		"harp",
		"check-guild-api",
		"categories",
		"bestiary",
		"reforge",
		"skyblock",
		"hotm",
		"hypixel",
		"inventory",
		"coins",
		"scammer",
		"calcslayer",
		"calcruns",
		"information",
		"slayer",
		"skills",
		"guild",
		"mayor",
		"price",
		"bazaar",
		"missing",
		"dungeons",
		"roles",
		"settings",
		"bin",
		"link",
		"help",
		"leaderboard",
		"weight",
		"auctions",
		"networth"
	);
	protected final Command command;
	protected Message ebMessage;
	protected String[] args;
	protected String player;
	protected EmbedBuilder eb;
	private final boolean sendLoadingEmbed;

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
		if (isMainBot() && slashOnlyCommands.contains(command.getName())) {
			getMessage()
				.reply(
					client.getError() +
					" This command can only be used through slash commands. If you do not see slash commands, make sure `Use Application Commands` is enabled for @ everyone or re-invite the bot using `" +
					getGuildPrefix(getGuild().getId()) +
					"invite`"
				)
				.queue(ignore, ignore);
			return;
		}

		executor.submit(() -> {
			if (sendLoadingEmbed) {
				this.ebMessage = getChannel().sendMessageEmbeds(loadingEmbed().build()).complete();
			}
			String content = getMessage().getContentRaw();
			if (content.startsWith(getSelfUser().getAsMention())) {
				content = content.substring(getSelfUser().getAsMention().length() + 1);
			}
			this.args = content.split("\\s+");
			execute();
		});
	}

	protected void logCommand() {
		Utils.logCommand(getGuild(), getAuthor(), getMessage().getContentRaw());
	}

	protected void setArgs(int limit) {
		setArgs(limit, false);
	}

	protected void setArgs(int limit, boolean useOriginal) {
		String content = String.join(" ", args);
		if (useOriginal) {
			content = getMessage().getContentRaw();
			if (content.startsWith(getSelfUser().getAsMention())) {
				content = content.substring(getSelfUser().getAsMention().length() + 1);
			}
		}
		args = content.split(" ", limit);
	}

	protected void embed(EmbedBuilder embedBuilder) {
		ebMessage.editMessageEmbeds(embedBuilder.build()).queue(ignore, ignore);
	}

	/**
	 * @param ebOrMb EmbedBuilder or MessageEditBuilder (for buttons)
	 */
	protected void embed(Object ebOrMb) {
		if (ebOrMb instanceof EmbedBuilder eb) {
			embed(eb);
		} else if (ebOrMb instanceof MessageEditBuilder mb) {
			ebMessage.editMessage(mb.build()).queue(ignore, ignore);
		} else {
			throw new IllegalArgumentException("Unexpected class: " + ebOrMb.getClass());
		}
	}

	protected void paginate(Object ebOrMb) {
		paginate(ebOrMb, false);
	}

	protected void paginate(Object ebOrMb, boolean deleteOriginal) {
		if (ebOrMb == null) {
			if (deleteOriginal) {
				ebMessage.delete().queue(ignore, ignore);
			}
		} else if (ebOrMb instanceof EmbedBuilder eb) {
			ebMessage.editMessageEmbeds(eb.build()).queue(ignore, ignore);
		} else if (ebOrMb instanceof MessageEditBuilder mb) {
			ebMessage.editMessage(mb.build()).queue(ignore, ignore);
		} else {
			throw new IllegalArgumentException("Unexpected class: " + ebOrMb.getClass());
		}
	}

	protected void paginate(EmbedBuilder embedBuilder) {
		paginate(embedBuilder, false);
	}

	protected void paginate(EmbedBuilder embedBuilder, boolean deleteOriginal) {
		if (embedBuilder == null) {
			if (deleteOriginal) {
				ebMessage.delete().queue(ignore, ignore);
			}
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

		ebMessage
			.editMessageEmbeds(
				invalidEmbed(
					"<@" +
					userId +
					"> is not linked to the bot. Please specify a username or " +
					(getAuthor().getId().equals(userId) ? "" : "have them ") +
					"link using `/link`"
				)
					.build()
			)
			.queue(ignore, ignore);
		return true;
	}
}
