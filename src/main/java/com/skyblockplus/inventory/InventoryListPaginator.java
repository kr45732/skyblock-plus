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

import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorEvent;
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
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;

public class InventoryListPaginator {

	private static final File loreRenderDir = new File("src/main/java/com/skyblockplus/json/lore_renders");
	private final String key;
	private final Map<Integer, InvItem> items;
	private final PaginatorEvent event;
	private final Map<String, String> renderedCache;
	private final Player player;
	private final Message message;
	private final int maxPageNumber;
	private Instant lastEdit;
	private int pageNumber;

	public InventoryListPaginator(Player player, Map<Integer, InvItem> items, int slot, PaginatorEvent event) {
		this.items = items;
		this.event = event;
		this.renderedCache = new HashMap<>();
		this.key = Instant.now().getEpochSecond() + "_" + player.getUuid() + "_";
		this.player = player;
		this.maxPageNumber = items.size() - 1;
		this.lastEdit = Instant.now();
		this.pageNumber = Math.min(Math.max(0, slot - 1), maxPageNumber);

		MessageAction action;
		EmbedBuilder eb = player
			.defaultPlayerEmbed()
			.setThumbnail(null)
			.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null);
		InvItem item = items.get(pageNumber);
		if (item == null) {
			eb.setDescription("**Item:** empty\n**Slot:** " + (pageNumber + 1));
			action = event.getChannel().sendMessageEmbeds(eb.build());
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
				.setThumbnail("https://sky.shiiyu.moe/item.gif/" + item.getId())
				.setImage("attachment://lore.png");
			action = event.getChannel().sendMessageEmbeds(eb.build()).addFile(new File(getRenderedLore()), "lore.png");
		}
		this.message =
			action
				.setActionRow(
					Button
						.primary("inv_list_paginator_left_button", Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>"))
						.withDisabled(pageNumber == 0),
					Button
						.primary("inv_list_paginator_right_button", Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>"))
						.withDisabled(pageNumber == maxPageNumber)
				)
				.complete();

		waitForEvent();
	}

	public String getRenderedLore() {
		return renderedCache.computeIfAbsent(key + pageNumber, ignored -> getRenderedLore0());
	}

	public String getRenderedLore0() {
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

	private boolean condition(ButtonInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getUser().getId().equals(this.event.getUser().getId()) &&
			event.getMessageId().equals(message.getId())
		);
	}

	public void action(ButtonInteractionEvent event) {
		if (Instant.now().minusSeconds(1).isBefore(lastEdit)) {
			event.reply(client.getError() + " Please wait between switching pages").setEphemeral(true).queue();
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

			List<Button> curButtons = event.getMessage().getButtons();
			MessageEditCallbackAction action;
			EmbedBuilder eb = player
				.defaultPlayerEmbed()
				.setThumbnail(null)
				.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null);
			InvItem item = items.get(pageNumber);
			if (item == null) {
				eb.setDescription("**Item:** empty\n**Slot:** " + (pageNumber + 1));
				action = event.editMessageEmbeds(eb.build());
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
					.setThumbnail("https://sky.shiiyu.moe/item.gif/" + item.getId())
					.setImage("attachment://lore.png");
				action = event.editMessageEmbeds(eb.build()).retainFiles().addFile(new File(getRenderedLore()), "lore.png");
			}
			action
				.setActionRow(
					pageNumber == 0 ? curButtons.get(0).asDisabled() : curButtons.get(0).asEnabled(),
					pageNumber == (maxPageNumber) ? curButtons.get(1).asDisabled() : curButtons.get(1).asEnabled()
				)
				.queue();
		}

		waitForEvent();
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			ButtonInteractionEvent.class,
			this::condition,
			this::action,
			30,
			TimeUnit.SECONDS,
			() -> {
				message.editMessageComponents().queue(ignore, ignore);
				for (File file : loreRenderDir.listFiles(file -> file.getName().startsWith(key))) {
					file.delete();
				}
			}
		);
	}
}
