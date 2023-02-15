/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022-2023 kr45732
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
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

public class SelectMenuPaginator {

	private final PaginatorExtras extras;
	private final GenericInteractionCreateEvent event;
	private Message message;
	private String page;

	public SelectMenuPaginator(String page, PaginatorExtras extras, GenericInteractionCreateEvent event) {
		this.page = page;
		this.extras = extras;
		this.event = event;

		List<LayoutComponent> actionRows = new ArrayList<>();
		if (!extras.getButtons().isEmpty()) {
			actionRows.add(ActionRow.of(extras.getButtons()));
		}
		actionRows.add(ActionRow.of(StringSelectMenu.create("select_menu_paginator").addOptions(extras.getSelectPages().keySet()).build()));
		(event instanceof SlashCommandEvent ev ? ev : ((ButtonInteractionEvent) event)).getHook()
			.editOriginalEmbeds(getPage(page).build())
			.setComponents(actionRows)
			.queue(
				m -> {
					message = m;
					waitForEvent();
				},
				ignore
			);
	}

	public boolean condition(GenericComponentInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getUser().getId().equals(this.event.getUser().getId()) &&
				event.getMessageId().equals(message.getId())
			);
		} else if (genericEvent instanceof ButtonInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getUser().getId().equals(this.event.getUser().getId()) &&
				event.getMessageId().equals(message.getId()) &&
				extras.getReactiveButtons().stream().anyMatch(b -> b.isReacting() && event.getComponentId().equals(b.getId()))
			);
		}
		return false;
	}

	public void action(GenericComponentInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			onStringSelectInteraction(event);
		} else if (genericEvent instanceof ButtonInteractionEvent event) {
			onButtonInteraction(event);
		}
	}

	public void onButtonInteraction(ButtonInteractionEvent event) {
		extras
			.getReactiveButtons()
			.stream()
			.filter(b -> b.isReacting() && event.getComponentId().equals(b.getId()))
			.map(PaginatorExtras.ReactiveButton::getAction)
			.findFirst()
			.orElse(ignored -> {})
			.accept(null);

		List<LayoutComponent> actionRows = new ArrayList<>();
		if (!extras.getButtons().isEmpty()) {
			actionRows.add(ActionRow.of(extras.getButtons()));
		}
		actionRows.add(ActionRow.of(StringSelectMenu.create("select_menu_paginator").addOptions(extras.getSelectPages().keySet()).build()));
		event
			.editMessageEmbeds(getPage(page).build())
			.setComponents(actionRows)
			.queue(
				m -> {
					message = event.getMessage();
					waitForEvent();
				},
				ignore
			);
	}

	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		page = event.getSelectedOptions().get(0).getValue();
		event
			.editMessageEmbeds(getPage(page).build())
			.queue(
				m -> {
					message = event.getMessage();
					waitForEvent();
				},
				ignore
			);
	}

	private EmbedBuilder getPage(String page) {
		return extras
			.getSelectPages()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue))
			.get(page);
	}

	public void waitForEvent() {
		waiter.waitForEvent(
			GenericComponentInteractionCreateEvent.class,
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
						.collect(Collectors.toCollection(ArrayList::new));
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
