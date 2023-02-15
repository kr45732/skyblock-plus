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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.ESSENCE_ITEM_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

public class EssenceHandler {

	private final String itemId;
	private final String itemName;
	private final JsonElement itemJson;
	private final Message reactMessage;
	private final SlashCommandEvent event;
	private int startingLevel;

	public EssenceHandler(String itemId, SlashCommandEvent event) {
		if (higherDepth(getEssenceCostsJson(), itemId) == null) {
			itemId = getClosestMatchFromIds(itemId, ESSENCE_ITEM_NAMES);
		}

		this.itemId = itemId;
		this.itemName = idToName(itemId);
		this.itemJson = higherDepth(getEssenceCostsJson(), itemId);
		this.event = event;
		this.reactMessage = event.getHook().retrieveOriginal().complete();

		int max = 0;
		for (int i = 1; i <= 10; i++) {
			if (higherDepth(itemJson, "" + i) != null || higherDepth(itemJson, "items." + i) != null) {
				max = i;
			}
		}

		StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("essence_upgrade_command");
		if (higherDepth(itemJson, "dungeonize") != null) {
			menuBuilder.addOption("Dungeonize", "-1");
		}
		for (int i = 0; i <= max - 1; i++) {
			menuBuilder.addOption("" + i, "" + i);
		}

		event
			.getHook()
			.editOriginalEmbeds(
				defaultEmbed("Essence upgrade for " + itemName)
					.setDescription("Choose the current item level")
					.setThumbnail(getItemThumbnail(itemId))
					.build()
			)
			.setActionRow(menuBuilder.build())
			.queue();

		waiter.waitForEvent(
			StringSelectInteractionEvent.class,
			this::condition,
			this::actionOne,
			1,
			TimeUnit.MINUTES,
			() -> reactMessage.editMessageComponents().queue()
		);
	}

	private boolean condition(StringSelectInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getMessageId().equals(reactMessage.getId()) &&
			event.getUser().getId().equals(this.event.getUser().getId())
		);
	}

	private void actionOne(StringSelectInteractionEvent event) {
		startingLevel = Integer.parseInt(event.getSelectedOptions().get(0).getValue());

		StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("essence_upgrade_command");

		for (int i = startingLevel + 1; i <= 10; i++) {
			if (i == 0 || higherDepth(itemJson, "" + i) != null || higherDepth(itemJson, "items." + i) != null) {
				menuBuilder.addOption("" + i, "" + i);
			}
		}

		event
			.editMessageEmbeds(
				defaultEmbed("Essence upgrade for " + itemName)
					.setDescription("Choose the ending item level")
					.setThumbnail(getItemThumbnail(itemId))
					.build()
			)
			.setActionRow(menuBuilder.build())
			.queue();

		waiter.waitForEvent(
			StringSelectInteractionEvent.class,
			this::condition,
			this::actionTwo,
			1,
			TimeUnit.MINUTES,
			() -> reactMessage.editMessageComponents().queue()
		);
	}

	private void actionTwo(StringSelectInteractionEvent event) {
		int endingLevel = Integer.parseInt(event.getSelectedOptions().get(0).getValue());

		int totalEssence = 0;
		Map<String, Integer> items = new HashMap<>();
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
					items.compute(name, (k, v) -> (v != null ? v : 0) + finalCount);
				}
			}
		}
		if (totalEssence > 0) {
			items.put(higherDepth(itemJson, "type").getAsString().toLowerCase() + " essence", totalEssence);
		}

		event
			.editMessageEmbeds(
				defaultEmbed("Essence upgrade for " + itemName)
					.setThumbnail(getItemThumbnail(itemId))
					.addField(
						"From " +
						(startingLevel == -1 ? "not dungeonized" : startingLevel + (startingLevel == 1 ? " star" : " stars")) +
						" to " +
						endingLevel +
						(endingLevel == 1 ? " star" : " stars"),
						items
							.entrySet()
							.stream()
							.map(e -> e.getValue() + " " + e.getKey())
							.collect(Collectors.joining(items.size() == 1 ? "" : "\n• ", items.size() == 1 ? "" : "\n• ", "")),
						false
					)
					.build()
			)
			.setComponents()
			.queue();
	}
}
