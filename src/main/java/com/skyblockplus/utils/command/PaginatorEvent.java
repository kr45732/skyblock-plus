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

import static com.skyblockplus.utils.Utils.defaultPaginator;

import com.jagrosh.jdautilities.command.CommandEvent;
import java.io.File;
import java.util.Collection;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;

public class PaginatorEvent {

	private final SlashCommandEvent slashCommand;
	private final CommandEvent command;
	private Message loadingMessage;

	public PaginatorEvent(Object event) {
		this(event, event instanceof CommandExecute e ? e.ebMessage : null);
	}

	public PaginatorEvent(Object event, Message loadingMessage) {
		this.loadingMessage = loadingMessage;
		if (event instanceof SlashCommandEvent e) {
			slashCommand = e;
			command = null;
		} else if (event instanceof CommandEvent e) {
			slashCommand = null;
			command = e;
		} else {
			throw new IllegalArgumentException("Invalid event class type provided: " + event.getClass());
		}
	}

	public Message getLoadingMessage() {
		return loadingMessage == null && isSlashCommand()
			? (loadingMessage = slashCommand.getHook().retrieveOriginal().complete())
			: loadingMessage;
	}

	public boolean isSlashCommand() {
		return slashCommand != null;
	}

	public SlashCommandEvent getSlashCommand() {
		return slashCommand;
	}

	public User getUser() {
		return isSlashCommand() ? slashCommand.getUser() : command.getAuthor();
	}

	public void paginate(CustomPaginator.Builder builder) {
		paginate(builder, 1);
	}

	public void paginate(CustomPaginator.Builder builder, int page) {
		if (isSlashCommand()) {
			builder.build().paginate(slashCommand.getHook(), page);
		} else if (loadingMessage != null) {
			builder.build().paginate(loadingMessage, page);
		} else {
			builder.build().paginate(command.getChannel(), page);
		}
	}

	public Guild getGuild() {
		return isSlashCommand() ? slashCommand.getGuild() : command.getGuild();
	}

	public MessageChannel getChannel() {
		return isSlashCommand() ? slashCommand.getChannel() : command.getChannel();
	}

	public Member getMember() {
		return isSlashCommand() ? slashCommand.getMember() : command.getMember();
	}

	public CustomPaginator.Builder getPaginator() {
		return defaultPaginator(getUser()).setColumns(1).setItemsPerPage(1);
	}

	public PaginatorAction getAction() {
		return new PaginatorAction(this);
	}

	public static class PaginatorAction {

		private final PaginatorEvent event;
		private RestAction<Message> action;

		public PaginatorAction(PaginatorEvent event) {
			this.event = event;
		}

		public PaginatorAction editMessageEmbeds(MessageEmbed... embeds) {
			action =
				event.isSlashCommand()
					? event.getSlashCommand().getHook().editOriginalEmbeds(embeds)
					: event.getLoadingMessage().editMessageEmbeds(embeds);
			return this;
		}

		public PaginatorAction setActionRow(ItemComponent... components) {
			action =
				event.isSlashCommand() ? getSlashCommandAction().setActionRow(components) : getCommandAction().setActionRow(components);
			return this;
		}

		public PaginatorAction setActionRows(ActionRow... rows) {
			action = event.isSlashCommand() ? getSlashCommandAction().setActionRows(rows) : getCommandAction().setActionRows(rows);
			return this;
		}

		public PaginatorAction setActionRows(Collection<ActionRow> rows) {
			action = event.isSlashCommand() ? getSlashCommandAction().setActionRows(rows) : getCommandAction().setActionRows(rows);
			return this;
		}

		public WebhookMessageUpdateAction<Message> getSlashCommandAction() {
			return (WebhookMessageUpdateAction<Message>) action;
		}

		public MessageAction getCommandAction() {
			return (MessageAction) action;
		}

		public RestAction<Message> get() {
			return action;
		}

		public PaginatorAction addFile(File file, String name) {
			action = event.isSlashCommand() ? getSlashCommandAction().addFile(file, name) : getCommandAction().addFile(file, name);
			return this;
		}
	}
}
