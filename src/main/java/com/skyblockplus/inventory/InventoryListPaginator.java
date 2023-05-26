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

import static com.skyblockplus.utils.utils.StringUtils.getItemThumbnail;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.rendering.LoreRenderer;
import com.skyblockplus.utils.structs.InvItem;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;

public class InventoryListPaginator {

	private static final File loreRenderDir = new File("src/main/java/com/skyblockplus/json/renders");
	private final String key;
	private final Map<Integer, InvItem> items;
	private final SlashCommandEvent event;
	private final Map<String, String> renderedCache;
	private final Player.Profile player;
	private final Message message;
	private final int maxPageNumber;
	private Instant lastEdit = Instant.now();
	private int pageNumber;

	public InventoryListPaginator(Player.Profile player, Map<Integer, InvItem> items, int slot, SlashCommandEvent event) {
		this.items = items;
		this.event = event;
		this.renderedCache = new HashMap<>();
		this.key = "inv_list_paginator_" + Instant.now().toEpochMilli() + "_" + player.getUuid() + "_";
		this.player = player;
		this.maxPageNumber = items.size() - 1;
		this.pageNumber = Math.min(Math.max(0, slot - 1), maxPageNumber);
		this.message = event.getHook().retrieveOriginal().complete();

		WebhookMessageEditAction<Message> action;
		EmbedBuilder eb = player
			.defaultPlayerEmbed()
			.setThumbnail(null)
			.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null);
		InvItem item = items.get(pageNumber);
		if (item == null) {
			eb.setDescription("**Item:** empty\n**Slot:** " + (pageNumber + 1));
			action = event.getHook().editOriginalEmbeds(eb.build());
		} else {
			eb
				.setDescription(
					"**Item:** " +
					(item.getCount() > 1 ? (item.getName() + "x ") : "") +
					item.getName() +
					"\n**Slot:** " +
					(pageNumber + 1) +
					"\n**Item Creation:** " +
					item.getCreationTimestamp()
				)
				.setThumbnail(getItemThumbnail(item.getId()))
				.setImage("attachment://lore.png");
			action = event.getHook().editOriginalEmbeds(eb.build()).setFiles(FileUpload.fromData(new File(getRenderedLore()), "lore.png"));
		}

		action
			.setActionRow(
				Button
					.primary("inv_list_paginator_left_button", Emoji.fromFormatted("<:left_button_arrow:885628386435821578>"))
					.withDisabled(pageNumber == 0),
				Button.primary("inv_list_paginator_search_button", "Search").withEmoji(Emoji.fromFormatted("\uD83D\uDD0E")),
				Button
					.primary("inv_list_paginator_right_button", Emoji.fromFormatted("<:right_button_arrow:885628386578423908>"))
					.withDisabled(pageNumber == maxPageNumber)
			)
			.queue(ignored -> waitForEvent(), ignore);
	}

	private boolean condition(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof ButtonInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getUser().getId().equals(this.event.getUser().getId()) &&
				event.getMessageId().equals(message.getId())
			);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getUser().getId().equals(this.event.getUser().getId()) &&
				event.getModalId().equals("inv_list_search_modal_" + message.getId())
			);
		}
		return false;
	}

	private void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof ButtonInteractionEvent event) {
			onButtonInteraction(event);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			onModalInteraction(event);
		}
	}

	private void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getComponentId().equals("inv_list_paginator_search_button")) {
			event
				.replyModal(
					Modal
						.create("inv_list_search_modal_" + message.getId(), "Search")
						.addActionRow(TextInput.create("item", "Item Name", TextInputStyle.SHORT).build())
						.build()
				)
				.queue(ignored -> waitForEvent(), ignore);
			return;
		}

		if (Instant.now().minusMillis(1500).isBefore(lastEdit)) {
			event
				.reply(client.getError() + " Please wait between switching pages")
				.setEphemeral(true)
				.queue(ignored -> waitForEvent(), ignore);
		} else {
			lastEdit = Instant.now();
			if (event.getComponentId().equals("inv_list_paginator_left_button")) {
				if ((pageNumber - 1) >= 0) {
					pageNumber -= 1;
				}
			} else if (event.getComponentId().equals("inv_list_paginator_right_button")) {
				if ((pageNumber + 1) <= maxPageNumber) {
					pageNumber += 1;
				}
			}

			MessageEditCallbackAction action;
			EmbedBuilder eb = player
				.defaultPlayerEmbed()
				.setThumbnail(null)
				.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null);
			InvItem item = items.get(pageNumber);
			if (item == null) {
				eb.setDescription("**Item:** empty\n**Slot:** " + (pageNumber + 1));
				action = event.editMessageEmbeds(eb.build()).setFiles();
			} else {
				eb
					.setDescription(
						"**Item:** " +
						(item.getCount() > 1 ? (item.getName() + "x ") : "") +
						item.getName() +
						"\n**Slot:** " +
						(pageNumber + 1) +
						"\n**Item Creation:** " +
						item.getCreationTimestamp()
					)
					.setThumbnail(getItemThumbnail(item.getId()))
					.setImage("attachment://lore.png");
				action = event.editMessageEmbeds(eb.build()).setFiles(FileUpload.fromData(new File(getRenderedLore()), "lore.png"));
			}
			List<Button> curButtons = event.getMessage().getButtons();
			action
				.setActionRow(
					curButtons.get(0).withDisabled(pageNumber == 0),
					curButtons.get(1),
					curButtons.get(2).withDisabled(pageNumber == (maxPageNumber))
				)
				.queue(ignored -> waitForEvent(), ignore);
		}
	}

	private void onModalInteraction(ModalInteractionEvent event) {
		lastEdit = Instant.now();
		String itemSearch = event.getValue("item").getAsString();
		pageNumber =
			FuzzySearch
				.extractOne(
					itemSearch,
					items.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toCollection(ArrayList::new)),
					i -> i.getValue().getName()
				)
				.getReferent()
				.getKey();

		MessageEditCallbackAction action;
		EmbedBuilder eb = player
			.defaultPlayerEmbed()
			.setThumbnail(null)
			.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null);
		InvItem item = items.get(pageNumber);
		if (item == null) {
			eb.setDescription("**Item:** empty\n**Slot:** " + (pageNumber + 1));
			action = event.editMessageEmbeds(eb.build()).setFiles();
		} else {
			eb
				.setDescription(
					"**Item:** " +
					(item.getCount() > 1 ? (item.getName() + "x ") : "") +
					item.getName() +
					"\n**Slot:** " +
					(pageNumber + 1) +
					"\n**Item Creation:** " +
					item.getCreationTimestamp()
				)
				.setThumbnail(getItemThumbnail(item.getId()))
				.setImage("attachment://lore.png");
			action = event.editMessageEmbeds(eb.build()).setFiles(FileUpload.fromData(new File(getRenderedLore()), "lore.png"));
		}
		action
			.setActionRow(
				Button
					.primary("inv_list_paginator_left_button", Emoji.fromFormatted("<:left_button_arrow:885628386435821578>"))
					.withDisabled(pageNumber == 0),
				Button.primary("inv_list_paginator_search_button", "Search").withEmoji(Emoji.fromFormatted("\uD83D\uDD0E")),
				Button
					.primary("inv_list_paginator_right_button", Emoji.fromFormatted("<:right_button_arrow:885628386578423908>"))
					.withDisabled(pageNumber == maxPageNumber)
			)
			.queue(ignored -> waitForEvent(), ignore);
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			GenericInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> {
				try {
					message.editMessageComponents().queue(ignore, ignore);
				} catch (Exception ignored) {}
				try {
					for (File file : loreRenderDir.listFiles(file -> file.getName().startsWith(key))) {
						file.delete();
					}
				} catch (Exception ignored) {}
			}
		);
	}

	/**
	 * @return path to image file
	 */
	private String getRenderedLore() {
		return renderedCache.computeIfAbsent(key + pageNumber, ignored -> computeRenderedLore());
	}

	private String computeRenderedLore() {
		InvItem item = items.get(pageNumber);

		List<String> loreWithName = new ArrayList<>();
		loreWithName.add(item.getName(false));
		loreWithName.addAll(item.getLore());

		BufferedImage bufferedImage = LoreRenderer.renderLore(loreWithName);
		File file = new File(loreRenderDir + "/" + key + pageNumber + ".png");
		try {
			ImageIO.write(bufferedImage, "png", file);
			return file.getPath();
		} catch (Exception e) {
			return null;
		}
	}
}
