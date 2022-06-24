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

import static com.skyblockplus.utils.Utils.database;
import static com.skyblockplus.utils.Utils.ignore;
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Utils;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class SlashCommandEvent extends SlashCommandInteractionEvent {

	private final SlashCommandClient slashCommandClient;
	public String player;

	public SlashCommandEvent(SlashCommandInteractionEvent event, SlashCommandClient slashCommandClient) {
		super(event.getJDA(), event.getResponseNumber(), event.getInteraction());
		this.slashCommandClient = slashCommandClient;
	}

	public void logCommand() {
		StringBuilder builder = new StringBuilder("/" + getName());
		if (getSubcommandGroup() != null) {
			builder.append(" ").append(getSubcommandGroup());
		}
		if (getSubcommandName() != null) {
			builder.append(" ").append(getSubcommandName());
		}
		for (OptionMapping o : getOptions()) {
			builder.append(" ").append(o.getName()).append(":").append(o.getAsString());
		}
		Utils.logCommand(getGuild(), getUser(), builder.toString());
	}

	public String getOptionStr(String name) {
		return getOptionStr(name, null);
	}

	public String getOptionStr(String name, String defaultValue) {
		OptionMapping option = getOption(name);
		return option == null ? defaultValue : option.getAsString();
	}

	public int getOptionInt(String name, int defaultValue) {
		OptionMapping option = getOption(name);
		return option == null ? defaultValue : (int) option.getAsLong();
	}

	public double getOptionDouble(String name, double defaultValue) {
		OptionMapping option = getOption(name);
		return option == null ? defaultValue : option.getAsDouble();
	}

	public boolean getOptionBoolean(String name, boolean defaultValue) {
		OptionMapping option = getOption(name);
		return option == null ? defaultValue : option.getAsBoolean();
	}

	public EmbedBuilder invalidCommandMessage() {
		return invalidEmbed("Invalid Command");
	}

	public void embed(EmbedBuilder eb) {
		getHook().editOriginalEmbeds(eb.build()).queue(ignore, ignore);
	}

	public void embed(Object ebOrMb) {
		if (ebOrMb instanceof EmbedBuilder eb) {
			embed(eb);
		} else if (ebOrMb instanceof MessageBuilder mb) {
			getHook().editOriginal(mb.build()).queue(ignore, ignore);
		} else {
			throw new IllegalArgumentException("Unexpected class: " + ebOrMb.getClass());
		}
	}

	private boolean getLinkedUser(String id) {
		LinkedAccount linkedUserUsername = database.getByDiscord(id);
		if (linkedUserUsername != null) {
			player = linkedUserUsername.uuid();
			return false;
		}

		embed(
			invalidEmbed(
				"<@" +
				id +
				"> is not linked to the bot. Please specify a username or " +
				(getUser().getId().equals(id) ? "" : "have them ") +
				"link using `/link`"
			)
		);
		return true;
	}

	public boolean invalidPlayerOption() {
		OptionMapping option = getOption("player");

		if (option == null) {
			return getLinkedUser(getUser().getId());
		}

		player = option.getAsString();

		Matcher matcher = Message.MentionType.USER.getPattern().matcher(option.getAsString());
		if (matcher.matches()) {
			return getLinkedUser(matcher.group(1));
		}

		return false;
	}

	public void paginate(EmbedBuilder failEmbed) {
		paginate(failEmbed, false);
	}

	public void paginate(EmbedBuilder failEmbed, boolean deleteOriginal) {
		if (failEmbed != null) {
			getHook().editOriginalEmbeds(failEmbed.build()).queue(ignore, ignore);
		} else if (deleteOriginal) {
			getHook().deleteOriginal().queue();
		}
	}

	public void paginate(Object ebOrMb) {
		paginate(ebOrMb, false);
	}

	public void paginate(Object ebOrMb, boolean deleteOriginal) {
		if (ebOrMb != null) {
			if (ebOrMb instanceof EmbedBuilder eb) {
				getHook().editOriginalEmbeds(eb.build()).queue(ignore, ignore);
			} else if (ebOrMb instanceof MessageBuilder mb) {
				getHook().editOriginal(mb.build()).queue(ignore, ignore);
			} else {
				throw new IllegalArgumentException("Unexpected class: " + ebOrMb.getClass());
			}
		} else if (deleteOriginal) {
			getHook().deleteOriginal().queue();
		}
	}

	public void string(String string) {
		getHook().editOriginal(string).queue(ignore, ignore);
	}

	public Member getSelfMember() {
		return getGuild().getSelfMember();
	}

	public boolean isOwner() {
		return slashCommandClient.isOwner(getUser().getId());
	}
}
