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

package com.skyblockplus.inventory;

import static com.skyblockplus.Main.waiter;

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;

public class InventoryPaginator {

	private final List<String[]> enderChestPages;
	private final Message pagePart1;
	private final Message pagePart2;
	private final User user;
	private final int maxPageNumber;
	private int pageNumber = 0;

	public InventoryPaginator(List<String[]> enderChestPages, MessageChannel channel, User user) {
		this.enderChestPages = enderChestPages;
		this.user = user;
		this.maxPageNumber = enderChestPages.size() - 1;

		pagePart1 = channel.sendMessage(enderChestPages.get(0)[0]).complete();
		pagePart2 =
			channel
				.sendMessage(enderChestPages.get(0)[1])
				.setActionRow(
					Button.primary("inv_paginator_left_button", Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>")),
					Button.primary("inv_paginator_right_button", Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>"))
				)
				.complete();

		waitForEvent();
	}

	private boolean condition(ButtonClickEvent event) {
		return (
			event.isFromGuild() &&
			!event.getUser().isBot() &&
			event.getUser().getId().equals(user.getId()) &&
			event.getMessageId().equals(pagePart2.getId())
		);
	}

	public void action(ButtonClickEvent event) {
		if (event.getComponentId().equals("inv_paginator_left_button")) {
			if ((pageNumber - 1) >= 0) {
				pageNumber -= 1;
			}
		} else if (event.getComponentId().equals("inv_paginator_right_button")) {
			if ((pageNumber + 1) <= maxPageNumber) {
				pageNumber += 1;
			}
		}

		pagePart1.editMessage(enderChestPages.get(pageNumber)[0]).complete();
		event.editMessage(enderChestPages.get(pageNumber)[1]).complete();

		waitForEvent();
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			ButtonClickEvent.class,
			this::condition,
			this::action,
			30,
			TimeUnit.SECONDS,
			() -> pagePart2.editMessageComponents().queue()
		);
	}
}
