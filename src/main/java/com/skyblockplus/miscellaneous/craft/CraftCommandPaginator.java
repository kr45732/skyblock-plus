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

package com.skyblockplus.miscellaneous.craft;

import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.rendering.LoreRenderer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

public class CraftCommandPaginator {

	private static final File loreRenderDir = new File("src/main/java/com/skyblockplus/json/renders");
	private final String key;
	private final JsonArray items;
	private final ButtonInteractionEvent event;
	private final Map<String, String> renderedCache;
	private final int maxScore;
	private Message message;
	private final int maxPageNumber;
	private Instant lastEdit = Instant.now();
	private int pageNumber;

	public CraftCommandPaginator(JsonArray items, int maxScore, ButtonInteractionEvent event) {
		this.items = items;
		this.event = event;
		this.maxScore = maxScore;
		this.renderedCache = new HashMap<>();
		this.key = "craft_command_paginator_" + Instant.now().toEpochMilli() + "_" + event.getUser().getId() + "_";
		this.maxPageNumber = items.size() - 1;
		this.pageNumber = 0;

		JsonElement item = items.get(pageNumber);

		double score = higherDepth(item, "score").getAsDouble();
		if (score == 0) {
			event.getHook().editOriginalEmbeds(errorEmbed("No matches found on the auction house").build()).queue();
			return;
		}
		boolean isBin = higherDepth(item, "bin", false);
		double startingBid = higherDepth(item, "starting_bid").getAsDouble();
		double highestBid = higherDepth(item, "highest_bid").getAsDouble();
		int count = higherDepth(item, "count", 1);
		String itemName = higherDepth(item, "item_name").getAsString();
		String itemId = higherDepth(item, "item_id").getAsString();

		EmbedBuilder eb = defaultEmbed("Auction House Matches")
			.addField(
				getEmoji(itemId) + " " + (count > 1 ? count + "x " : "") + itemName,
				"**Match:** " +
				roundProgress(score / maxScore) +
				"\n**Price:** " +
				roundAndFormat(Math.max(startingBid, highestBid)) +
				"\n**Ends:** <t:" +
				Instant.ofEpochMilli(higherDepth(item, "end_t").getAsLong()).getEpochSecond() +
				":R>\n**" +
				(isBin ? "Bin" : "Auction") +
				":** `/viewauction " +
				higherDepth(item, "uuid").getAsString() +
				"`",
				false
			)
			.setThumbnail(getItemThumbnail(itemId))
			.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null)
			.setImage("attachment://lore.png");

		event
			.getHook()
			.editOriginalEmbeds(eb.build())
			.setFiles(FileUpload.fromData(new File(getRenderedLore()), "lore.png"))
			.setActionRow(
				Button
					.primary("craft_command_paginator_left_button", Emoji.fromFormatted("<:left_button_arrow:885628386435821578>"))
					.withDisabled(pageNumber == 0),
				Button
					.primary("craft_command_paginator_right_button", Emoji.fromFormatted("<:right_button_arrow:885628386578423908>"))
					.withDisabled(pageNumber == maxPageNumber)
			)
			.queue(
				m -> {
					this.message = m;
					waitForEvent();
				},
				ignore
			);
	}

	private boolean condition(ButtonInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getUser().getId().equals(this.event.getUser().getId()) &&
			event.getMessageId().equals(message.getId())
		);
	}

	private void action(ButtonInteractionEvent event) {
		if (Instant.now().minusMillis(1500).isBefore(lastEdit)) {
			event
				.reply(client.getError() + " Please wait between switching pages")
				.setEphemeral(true)
				.queue(ignored -> waitForEvent(), ignore);
		} else {
			lastEdit = Instant.now();
			if (event.getComponentId().equals("craft_command_paginator_left_button")) {
				if ((pageNumber - 1) >= 0) {
					pageNumber -= 1;
				}
			} else if (event.getComponentId().equals("craft_command_paginator_right_button")) {
				if ((pageNumber + 1) <= maxPageNumber) {
					pageNumber += 1;
				}
			}

			JsonElement item = items.get(pageNumber);

			boolean isBin = higherDepth(item, "bin", false);
			double startingBid = higherDepth(item, "starting_bid").getAsDouble();
			double highestBid = higherDepth(item, "highest_bid").getAsDouble();
			int count = higherDepth(item, "count", 1);
			String itemName = higherDepth(item, "item_name").getAsString();
			String itemId = higherDepth(item, "item_id").getAsString();

			EmbedBuilder eb = defaultEmbed("Auction House Matches")
				.addField(
					getEmoji(itemId) + " " + (count > 1 ? count + "x " : "") + itemName,
					"**Match:** " +
					roundProgress(higherDepth(item, "score").getAsDouble() / maxScore) +
					"\n**Price:** " +
					roundAndFormat(Math.max(startingBid, highestBid)) +
					"\n**Ends:** <t:" +
					Instant.ofEpochMilli(higherDepth(item, "end_t").getAsLong()).getEpochSecond() +
					":R>\n**" +
					(isBin ? "Bin" : "Auction") +
					":** `/viewauction " +
					higherDepth(item, "uuid").getAsString() +
					"`",
					false
				)
				.setThumbnail(getItemThumbnail(itemId))
				.setFooter("By CrypticPlasma • Page " + (pageNumber + 1) + "/" + items.size() + " • dsc.gg/sb+", null)
				.setImage("attachment://lore.png");

			event
				.editMessageEmbeds(eb.build())
				.setFiles(FileUpload.fromData(new File(getRenderedLore()), "lore.png"))
				.setActionRow(
					Button
						.primary("craft_command_paginator_left_button", Emoji.fromFormatted("<:left_button_arrow:885628386435821578>"))
						.withDisabled(pageNumber == 0),
					Button
						.primary("craft_command_paginator_right_button", Emoji.fromFormatted("<:right_button_arrow:885628386578423908>"))
						.withDisabled(pageNumber == maxPageNumber)
				)
				.queue(m -> waitForEvent(), ignore);
		}
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			ButtonInteractionEvent.class,
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
		JsonElement item = items.get(pageNumber);
		List<String> lore = List.of(higherDepth(item, "lore").getAsString().split("\n"));

		BufferedImage bufferedImage = LoreRenderer.renderLore(lore);
		File file = new File(loreRenderDir + "/" + key + pageNumber + ".png");
		try {
			ImageIO.write(bufferedImage, "png", file);
			return file.getPath();
		} catch (Exception e) {
			return null;
		}
	}
}
