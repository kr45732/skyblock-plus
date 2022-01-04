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

import static com.skyblockplus.utils.Constants.ESSENCE_EMOJI_MAP;
import static com.skyblockplus.utils.Constants.ESSENCE_ITEM_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Locale;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class EssenceCommand extends Command {

	public EssenceCommand() {
		this.name = "essence";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getEssenceInformation(String itemName) {
		JsonElement essenceCostsJson = getEssenceCostsJson();

		String preFormattedItem = itemName.replace("'s", "").replace(" ", "_").toUpperCase();
		preFormattedItem = nameToId(preFormattedItem);

		if (higherDepth(essenceCostsJson, preFormattedItem) == null) {
			String closestMatch = getClosestMatch(preFormattedItem, ESSENCE_ITEM_NAMES);
			preFormattedItem = closestMatch != null ? closestMatch : preFormattedItem;
		}

		JsonElement itemJson = higherDepth(essenceCostsJson, preFormattedItem);

		EmbedBuilder eb = defaultEmbed("Essence information for " + itemName);
		if (itemJson != null) {
			String essenceType = higherDepth(itemJson, "type").getAsString().toLowerCase();
			for (String level : getJsonKeys(itemJson)) {
				switch (level) {
					case "type" -> eb.setDescription("**Essence Type:** " + capitalizeString(essenceType) + " essence");
					case "dungeonize" -> eb.addField(
						"Dungeonize item",
						higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence",
						false
					);
					case "1" -> eb.addField(
						level + " star",
						higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence",
						false
					);
					default -> eb.addField(
						level + " stars",
						higherDepth(itemJson, level).getAsString() + " " + essenceType + " essence",
						false
					);
				}
			}
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + preFormattedItem);
			return eb;
		}
		return defaultEmbed("Invalid item name");
	}

	public static EmbedBuilder getPlayerEssence(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			for (Map.Entry<String, JsonElement> entry : player.profileJson().getAsJsonObject().entrySet()) {
				if (entry.getKey().startsWith("essence_")) {
					String essenceType = entry.getKey().split("essence_")[1];
					eb.appendDescription(
						ESSENCE_EMOJI_MAP.get(essenceType) +
						"** " +
						capitalizeString(essenceType) +
						" essence:** " +
						formatNumber(entry.getValue().getAsInt()) +
						"\n"
					);
				}
			}

			for (Map.Entry<String, JsonElement> perk : higherDepth(player.profileJson(), "perks").getAsJsonObject().entrySet()) {
				eb.appendDescription(
					"\n" +
					ESSENCE_EMOJI_MAP.get(perk.getKey()) +
					"** " +
					capitalizeString(perk.getKey().replace("_", " ")) +
					":** " +
					perk.getValue().getAsInt()
				);
			}

			return eb;
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(3);

				if (args.length == 3 && args[1].equals("upgrade")) {
					String itemId = nameToId(args[2]);

					if (higherDepth(getEssenceCostsJson(), itemId) == null) {
						String closestMatch = getClosestMatch(itemId, ESSENCE_ITEM_NAMES);
						itemId = closestMatch != null ? closestMatch : itemId;
					}

					JsonElement itemJson = higherDepth(getEssenceCostsJson(), itemId);
					if (itemJson != null) {
						new EssenceWaiter(itemId, itemJson, ebMessage, event.getAuthor());
					} else {
						embed(invalidEmbed("Invalid item name"));
					}
					return;
				} else if (args.length == 3 && (args[1].equals("info") || args[1].equals("information"))) {
					embed(getEssenceInformation(args[2]));
					return;
				} else if ((args.length == 4 || args.length == 3 || args.length == 2) && args[1].equals("player")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					embed(getPlayerEssence(username, args.length == 4 ? args[3] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
