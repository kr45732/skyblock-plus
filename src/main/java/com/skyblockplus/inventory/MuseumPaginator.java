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

package com.skyblockplus.inventory;

import static com.skyblockplus.utils.rendering.ChestRenderer.*;
import static com.skyblockplus.utils.utils.JsonUtils.getMuseumCategoriesJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.capitalizeString;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.apache.groovy.util.Maps;

public class MuseumPaginator {

	private static final Map<String, String> categoryToEmoji = Maps.of(
		"weapons",
		"DIAMOND_SWORD",
		"armor_sets",
		"DIAMOND_CHESTPLATE",
		"rarities",
		"JADERALD",
		"special_items",
		"CAKE"
	);
	private final Set<String> items;
	private final Set<String> bypassedItems;
	private final User user;
	private final Map<String, String> renderedCache;
	private final String key;
	private final String skyblockStatsLink;
	private final String username;
	private String category = "weapons";
	private int pageNumber = 0;
	private int maxPageNumber;
	private Message message;
	private Instant lastEdit;

	public MuseumPaginator(Set<String> items, Set<String> bypassedItems, Player.Profile player, SlashCommandEvent event) {
		this.items = items;
		this.bypassedItems = bypassedItems;
		this.user = event.getUser();
		this.lastEdit = Instant.now();
		this.maxPageNumber = (int) (getMuseumCategoriesJson().getAsJsonArray(category).size() / 28.0);
		this.renderedCache = new HashMap<>();
		this.key = "museum_paginator_" + Instant.now().toEpochMilli() + "_" + player.getUuid();
		this.skyblockStatsLink = player.skyblockStatsLink();
		this.username = player.getUsername();

		event
			.getHook()
			.editOriginal(getPageRender())
			.queue(m -> {
				message = m;
				waitForEvent();
			});
	}

	private boolean condition(GenericComponentInteractionCreateEvent event) {
		return event.isFromGuild() && event.getUser().getId().equals(user.getId()) && event.getMessageId().equals(message.getId());
	}

	private void action(GenericComponentInteractionCreateEvent event) {
		if (event instanceof ButtonInteractionEvent buttonEvent) {
			onButtonInteraction(buttonEvent);
		} else if (event instanceof StringSelectInteractionEvent stringSelectEvent) {
			onStringSelectInteraction(stringSelectEvent);
		}
	}

	private void onButtonInteraction(ButtonInteractionEvent event) {
		if (Instant.now().minusSeconds(2).isBefore(lastEdit)) {
			event
				.reply(client.getError() + " Please wait between switching pages")
				.setEphemeral(true)
				.queue(ignored -> waitForEvent(), ignored -> waitForEvent());
		} else {
			lastEdit = Instant.now();
			if (event.getComponentId().equals("museum_paginator_left_button")) {
				if ((pageNumber - 1) >= 0) {
					pageNumber -= 1;
				}
			} else if (event.getComponentId().equals("museum_paginator_right_button")) {
				if ((pageNumber + 1) <= maxPageNumber) {
					pageNumber += 1;
				}
			}

			event.editMessage(getPageRender()).queue(ignored -> waitForEvent(), ignored -> waitForEvent());
		}
	}

	private void onStringSelectInteraction(StringSelectInteractionEvent event) {
		if (Instant.now().minusSeconds(2).isBefore(lastEdit)) {
			event
				.reply(client.getError() + " Please wait between switching pages")
				.setEphemeral(true)
				.queue(ignored -> waitForEvent(), ignored -> waitForEvent());
		} else {
			lastEdit = Instant.now();

			category = event.getSelectedOptions().get(0).getValue();
			pageNumber = 0;
			maxPageNumber = (int) (getMuseumCategoriesJson().getAsJsonArray(category).size() / 28.0);

			event.editMessage(getPageRender()).queue(ignored -> waitForEvent(), ignored -> waitForEvent());
		}
	}

	private MessageEditData getPageRender() {
		return new MessageEditBuilder()
			.setFiles(FileUpload.fromData(new File(getRenderedPage())))
			.setComponents(
				ActionRow.of(
					Button
						.primary("museum_paginator_left_button", Emoji.fromFormatted("<:left_button_arrow:885628386435821578>"))
						.withDisabled(pageNumber == 0),
					Button
						.primary("museum_paginator_right_button", Emoji.fromFormatted("<:right_button_arrow:885628386578423908>"))
						.withDisabled(pageNumber == (maxPageNumber)),
					Button.link(
						skyblockStatsLink,
						username +
						"'s Museum (" +
						capitalizeString(category.replace("_", " ")) +
						") • Page " +
						(pageNumber + 1) +
						"/" +
						(maxPageNumber + 1)
					)
				),
				ActionRow.of(
					StringSelectMenu
						.create("museum_paginator_category_select")
						.addOptions(
							categoryToEmoji
								.entrySet()
								.stream()
								.map(e ->
									SelectOption
										.of(capitalizeString(e.getKey().replace("_", " ")), e.getKey())
										.withEmoji(Emoji.fromFormatted(getEmoji(e.getValue())))
								)
								.toList()
						)
						.setDefaultValues(category)
						.build()
				)
			)
			.build();
	}

	/**
	 * @return path to image file
	 */
	private String getRenderedPage() {
		return renderedCache.computeIfAbsent(key + "_" + category + "_" + pageNumber, ignored -> computeRenderedPage());
	}

	private String computeRenderedPage() {
		try {
			int idx = pageNumber * 28;
			JsonArray categoryItems = getMuseumCategoriesJson().getAsJsonArray(category);
			List<Image> slots = new ArrayList<>();

			for (int j = 0; j < CHEST_ROWS; j++) {
				for (int i = 0; i < CHEST_COLUMNS; i++) {
					String itemId = null;
					if (i == 4 && j == 0) {
						itemId = categoryToEmoji.get(category);
					} else if (j == 5 && ((pageNumber > 0 && i == 0) || (i == 3) || (pageNumber < maxPageNumber - 1 && i == 8))) {
						itemId = "ARROW"; // Left, go back, right arrows
					} else if (i == 4 && j == 5) {
						itemId = "BARRIER"; // Close
					} else if (i == 0 || i == CHEST_COLUMNS - 1 || j == 0 || j == CHEST_ROWS - 1) {
						itemId = "STAINED_GLASS_PANE:15"; // Boundary
					} else {
						if (idx < categoryItems.size()) {
							itemId = categoryItems.get(idx).getAsString();
							if (items.contains(itemId)) {
								itemId = higherDepth(getMuseumCategoriesJson(), "armor_to_id." + itemId, itemId);
							} else if (bypassedItems.contains(itemId)) {
								itemId = "INK_SACK:10";
							} else {
								itemId = "INK_SACK:8";
							}
						}
						idx++;
					}
					slots.add(itemId == null ? null : getItemImage(itemId));
				}
			}

			BufferedImage chestRender = renderChest(slots);
			File file = new File(rendersDirectory + "/" + key + "_" + category + "_" + pageNumber + ".png");
			ImageIO.write(chestRender, "png", file);

			return file.getPath();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			GenericComponentInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> {
				try {
					message
						.editMessageComponents(
							ActionRow.of(
								Button.link(
									skyblockStatsLink,
									username +
									"'s Museum (" +
									capitalizeString(category.replace("_", " ")) +
									") • Page " +
									(pageNumber + 1) +
									"/" +
									(maxPageNumber + 1)
								)
							)
						)
						.queue(ignore, ignore);
				} catch (Exception ignored) {}
				try {
					for (File file : rendersDirectory.listFiles(file -> file.getName().startsWith(key))) {
						file.delete();
					}
				} catch (Exception ignored) {}
			}
		);
	}
}
