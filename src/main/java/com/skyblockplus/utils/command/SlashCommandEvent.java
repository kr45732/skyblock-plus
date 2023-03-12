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

import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.utils.Utils;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

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
		return errorEmbed("Invalid Command");
	}

	public void embed(Object ebOrMb) {
		if (ebOrMb instanceof EmbedBuilder eb) {
			getHook().editOriginalEmbeds(eb.build()).queue(ignore, ignore);
		} else if (ebOrMb instanceof MessageEditBuilder mb) {
			getHook().editOriginal(mb.build()).queue(ignore, ignore);
		} else {
			throw new IllegalArgumentException("Unexpected class: " + ebOrMb.getClass());
		}
	}

	public void paginate(Object ebOrMb) {
		if (ebOrMb != null) {
			embed(ebOrMb);
		}
	}

	public void paginate(CustomPaginator.Builder builder) {
		paginate(builder, 1);
	}

	public void paginate(CustomPaginator.Builder builder, int page) {
		builder.build().paginate(getHook(), page);
	}

	public void string(String string) {
		getHook().editOriginal(string).queue(ignore, ignore);
	}

	public boolean invalidPlayerOption() {
		return invalidPlayerOption(false);
	}

	public boolean invalidPlayerOption(boolean onlyCheck) {
		OptionMapping option = getOption("player");

		if (option == null) {
			return getLinkedUser(getUser().getId(), onlyCheck);
		}

		player = option.getAsString();

		Matcher matcher = Message.MentionType.USER.getPattern().matcher(option.getAsString());
		if (matcher.matches()) {
			return getLinkedUser(matcher.group(1), onlyCheck);
		}

		return false;
	}

	private boolean getLinkedUser(String id, boolean onlyCheck) {
		LinkedAccount linkedUserUsername = database.getByDiscord(id);
		if (linkedUserUsername != null) {
			player = linkedUserUsername.uuid();
			return false;
		}

		if (!onlyCheck) {
			embed(
				errorEmbed(
					"<@" +
					id +
					"> is not linked to the bot. Please specify a username or " +
					(getUser().getId().equals(id) ? "" : "have them ") +
					"link using `/link`."
				)
			);
		}
		return true;
	}

	public boolean isOwner() {
		return slashCommandClient.isOwner(getUser().getId());
	}

	public CustomPaginator.Builder getPaginator() {
		return defaultPaginator(getUser()).setColumns(1).setItemsPerPage(1);
	}
}
