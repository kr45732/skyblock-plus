/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

import static com.skyblockplus.utils.Utils.ignore;
import static com.skyblockplus.utils.Utils.waiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class SelectMenuPaginator {

	private final Map<String, EmbedBuilder> pages;
	private final PaginatorEvent event;
	private Message message;
	private String page;

	public SelectMenuPaginator(Map<SelectOption, EmbedBuilder> pages, String page, PaginatorExtras extras, PaginatorEvent event) {
		this.pages = pages.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue));
		this.page = page;
		this.event = event;

		List<ActionRow> actionRows = new ArrayList<>();
		if (!extras.getButtons().isEmpty()) {
			actionRows.add(ActionRow.of(extras.getButtons()));
		}
		actionRows.add(ActionRow.of(SelectMenu.create("select_menu_paginator").addOptions(pages.keySet()).build()));
		event
			.getAction()
			.editMessageEmbeds(this.pages.get(page).build())
			.setActionRows(actionRows)
			.get()
			.queue(m -> {
				message = m;
				waitForEvent();
			});
	}

	public boolean condition(SelectMenuInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getUser().getId().equals(this.event.getUser().getId()) &&
			event.getMessageId().equals(message.getId())
		);
	}

	public void action(SelectMenuInteractionEvent event) {
		page = event.getSelectedOptions().get(0).getValue();
		event
			.editMessageEmbeds(this.pages.get(page).build())
			.queue(m -> {
				message = event.getMessage();
				waitForEvent();
			});
	}

	public void waitForEvent() {
		waiter.waitForEvent(
			SelectMenuInteractionEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> {
				if (!message.getActionRows().isEmpty()) {
					List<Button> buttons = message
						.getButtons()
						.stream()
						.filter(b -> b.getStyle() == ButtonStyle.LINK)
						.collect(Collectors.toList());
					if (buttons.isEmpty()) {
						message.editMessageComponents().queue(ignore, ignore);
					} else {
						message.editMessageComponents(ActionRow.of(buttons)).queue(ignore, ignore);
					}
				}
			}
		);
	}
}
