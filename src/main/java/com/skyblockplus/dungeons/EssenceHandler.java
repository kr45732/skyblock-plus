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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.ESSENCE_ITEM_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.PaginatorEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

public class EssenceHandler {

	private final String itemId;
	private final String itemName;
	private final JsonElement itemJson;
	private final Message reactMessage;
	private final PaginatorEvent event;
	private int startingLevel;

	public EssenceHandler(String itemId, PaginatorEvent event) {
		if (higherDepth(getEssenceCostsJson(), itemId) == null) {
			itemId = getClosestMatchFromIds(itemId, ESSENCE_ITEM_NAMES);
		}

		this.itemId = itemId;
		this.itemName = idToName(itemId);
		this.itemJson = higherDepth(getEssenceCostsJson(), itemId);
		this.event = event;
		this.reactMessage = event.getLoadingMessage();

		int max = 0;
		for (int i = 1; i <= 10; i++) {
			if (higherDepth(itemJson, "" + i) != null) {
				max = i;
			}
		}

		SelectMenu.Builder menuBuilder = SelectMenu.create("essence_upgrade_command");
		if (higherDepth(itemJson, "dungeonize") != null) {
			menuBuilder.addOption("Dungeonize", "-1");
		}
		for (int i = 0; i <= max - 1; i++) {
			if (i == 0 || higherDepth(itemJson, "" + i) != null) {
				menuBuilder.addOption("" + i, "" + i);
			}
		}

		event
			.getAction()
			.editMessageEmbeds(
				defaultEmbed("Essence upgrade for " + itemName)
					.setDescription("Choose the current item level")
					.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId)
					.build()
			)
			.setActionRow(menuBuilder.build())
			.get()
			.queue();

		waiter.waitForEvent(
			SelectMenuInteractionEvent.class,
			this::condition,
			this::actionOne,
			1,
			TimeUnit.MINUTES,
			() -> reactMessage.editMessageComponents().queue()
		);
	}

	private boolean condition(SelectMenuInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getMessageId().equals(reactMessage.getId()) &&
			event.getUser().getId().equals(this.event.getUser().getId())
		);
	}

	private void actionOne(SelectMenuInteractionEvent event) {
		startingLevel = Integer.parseInt(event.getSelectedOptions().get(0).getValue());

		SelectMenu.Builder menuBuilder = SelectMenu.create("essence_upgrade_command");

		for (int i = startingLevel + 1; i <= 10; i++) {
			if (i == 0 || higherDepth(itemJson, "" + i) != null) {
				menuBuilder.addOption("" + i, "" + i);
			}
		}

		event
			.editMessageEmbeds(
				defaultEmbed("Essence upgrade for " + itemName)
					.setDescription("Choose the ending item level")
					.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId)
					.build()
			)
			.setActionRow(menuBuilder.build())
			.queue();

		waiter.waitForEvent(
			SelectMenuInteractionEvent.class,
			this::condition,
			this::actionTwo,
			1,
			TimeUnit.MINUTES,
			() -> reactMessage.editMessageComponents().queue()
		);
	}

	private void actionTwo(SelectMenuInteractionEvent event) {
		int endingLevel = Integer.parseInt(event.getSelectedOptions().get(0).getValue());

		int totalEssence = 0;
		Map<String, Integer> otherItems = new HashMap<>();
		for (int i = (startingLevel + 1); i <= endingLevel; i++) {
			if (i == 0) {
				totalEssence += higherDepth(itemJson, "dungeonize", 0);
			} else {
				totalEssence += higherDepth(itemJson, "" + i, 0);
			}

			if (higherDepth(itemJson, "items." + i) != null) {
				for (JsonElement upgrade : higherDepth(itemJson, "items." + i).getAsJsonArray()) {
					String strUpgrade = upgrade.getAsString();
					String name = parseMcCodes(strUpgrade);
					int count = 1;
					if (strUpgrade.contains(" §8x")) {
						String[] nameCountSplit = strUpgrade.split(" §8x");
						name = parseMcCodes(nameCountSplit[0]);
						count = Integer.parseInt(nameCountSplit[1]);
					}
					int finalCount = count;
					otherItems.compute(name, (k, v) -> (v != null ? v : 0) + finalCount);
				}
			}
		}

		event
			.editMessageEmbeds(
				defaultEmbed("Essence upgrade for " + itemName)
					.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId)
					.addField(
						"From " +
						(startingLevel == -1 ? "not dungeonized" : startingLevel + (startingLevel == 1 ? " star" : " stars")) +
						" to " +
						endingLevel +
						(endingLevel == 1 ? " star" : " stars"),
						(!otherItems.isEmpty() ? "• " : "") +
						totalEssence +
						" " +
						higherDepth(itemJson, "type").getAsString().toLowerCase() +
						" essence" +
						(
							!otherItems.isEmpty()
								? otherItems
									.entrySet()
									.stream()
									.map(e -> e.getValue() + " " + e.getKey())
									.collect(Collectors.joining("\n• ", "\n• ", ""))
								: ""
						),
						false
					)
					.build()
			)
			.setActionRows()
			.queue();
	}
}
