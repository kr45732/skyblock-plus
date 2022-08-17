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
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import java.util.List;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

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
		"fix-application",
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
		"roles"
	);
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
			if (isMainBot() && slashOnlyCommands.contains(command.getName())) {
				reply(
					client.getError() +
					" This command can only be used through slash commands. If you do not see slash commands, make sure `Use Application Commands` is enabled for @ everyone or re-invite the bot using `" +
					getGuildPrefix(getGuild().getId()) +
					"invite`"
				);
				return;
			}

			if (adminCommand && !guildMap.get(getGuild().getId()).isAdmin(getMember())) {
				reply("You are missing the required permissions or roles to use this command");
				return;
			}

			if (sendLoadingEmbed) {
				this.ebMessage =
					getChannel()
						.sendMessage(
							"**⚠️ Skyblock Plus will stop responding to message commands <t:1662004740:R>!** Please use slash commands instead. If you do not see slash commands, make sure `Use Application Commands` is enabled for @ everyone or re-invite the bot using `" +
							getGuildPrefix(getGuild().getId()) +
							"invite`"
						)
						.setEmbeds(loadingEmbed().build())
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
		args = String.join(" ", args).split(" ", limit);
		return args;
	}

	protected void embed(EmbedBuilder embedBuilder) {
		ebMessage.editMessageEmbeds(embedBuilder.build()).queue(ignore, ignore);
	}

	/**
	 * @param ebOrMb EmbedBuilder or MessageBuilder (for buttons)
	 */
	protected void embed(Object ebOrMb) {
		if (ebOrMb instanceof EmbedBuilder eb) {
			embed(eb);
		} else if (ebOrMb instanceof MessageBuilder mb) {
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
		} else if (ebOrMb instanceof MessageBuilder mb) {
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

	/**
	 * Matches a boolean flag
	 */
	protected boolean getBooleanOption(String match) {
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
				try {
					String arg = args[i].split(match + ":")[1];
					removeArg(i);
					return arg;
				} catch (Exception ignored) {}
			}
		}

		return defaultValue;
	}

	protected Player.Gamemode getGamemodeOption(String match, Player.Gamemode defaultValue) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(match + ":")) {
				try {
					Player.Gamemode arg = Player.Gamemode.of(args[i].split(match + ":")[1]);
					removeArg(i);
					return arg;
				} catch (Exception ignored) {}
			}
		}

		return defaultValue;
	}

	protected Player.WeightType getWeightTypeOption(String match, Player.WeightType defaultValue) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(match + ":")) {
				try {
					Player.WeightType arg = Player.WeightType.of(args[i].split(match + ":")[1]);
					removeArg(i);
					return arg;
				} catch (Exception ignored) {}
			}
		}

		return defaultValue;
	}

	protected int getIntOption(String match) {
		return getIntOption(match, -1);
	}

	protected int getIntOption(String match, int defaultValue) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(match + ":")) {
				try {
					int arg = Integer.parseInt(args[i].split(match + ":")[1]);
					removeArg(i);
					return arg;
				} catch (Exception ignored) {}
			}
		}

		return defaultValue;
	}

	protected double getDoubleOption(String match, double defaultValue) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith(match + ":")) {
				try {
					double arg = Double.parseDouble(args[i].split(match + ":")[1]);
					removeArg(i);
					return arg;
				} catch (Exception ignored) {}
			}
		}

		return defaultValue;
	}

	public CommandExecute setAdminCommand(boolean adminCommand) {
		this.adminCommand = adminCommand;
		return this;
	}

	public PaginatorEvent getPaginatorEvent() {
		return new PaginatorEvent(this);
	}
}
