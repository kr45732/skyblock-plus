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

import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.Utils.nbtToItems;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class MuseumSlashCommand extends SlashCommand {

	public MuseumSlashCommand() {
		this.name = "museum";
	}

	public static EmbedBuilder getPlayerInventory(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (!player.isValid()) {
			return player.getErrorEmbed();
		}

		HypixelResponse hypixelResponse = player.getMuseum();
		if (!hypixelResponse.isValid()) {
			return hypixelResponse.getErrorEmbed();
		}

		JsonElement museumJson = hypixelResponse.get(player.getUuid());
		Set<String> items = new HashSet<>(higherDepth(museumJson, "items").getAsJsonObject().keySet());
		JsonElement specialItems = higherDepth(museumJson, "special");
		if (specialItems != null) {
			streamJsonArray(specialItems)
				.map(e -> nbtToItems(higherDepth(e, "items.data", null)))
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull)
				.map(InvItem::getId)
				.forEach(items::add);
		}

		Set<String> bypassedItems = new HashSet<>();
		for (Map.Entry<String, JsonElement> entry : getParentsJson().entrySet()) {
			List<String> value = streamJsonArray(entry.getValue()).map(JsonElement::getAsString).toList();
			for (int i = value.size() - 1; i >= 0; i--) {
				if (items.contains(value.get(i))) {
					bypassedItems.add(entry.getKey());
					bypassedItems.addAll(value.subList(0, i));
					break;
				}
			}
		}
		bypassedItems.removeIf(items::contains);

		new MuseumPaginator(items, bypassedItems, player, event);
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerInventory(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	protected SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's museum items")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
