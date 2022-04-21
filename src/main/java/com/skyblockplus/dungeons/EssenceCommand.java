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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class EssenceCommand extends Command {

	public EssenceCommand() {
		this.name = "essence";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getEssenceInformation(String itemName) {
		JsonElement essenceCostsJson = getEssenceCostsJson();

		String itemId = nameToId(itemName);

		if (higherDepth(essenceCostsJson, itemId) == null) {
			String closestMatch = getClosestMatchFromIds(itemId, ESSENCE_ITEM_NAMES);
			itemId = closestMatch != null ? closestMatch : itemId;
		}

		JsonElement itemJson = higherDepth(essenceCostsJson, itemId);

		EmbedBuilder eb = defaultEmbed(idToName(itemId));
		if (itemJson != null) {
			String essenceType = higherDepth(itemJson, "type").getAsString().toLowerCase();
			for (String level : getJsonKeys(itemJson)) {
				switch (level) {
					case "type" -> eb.setDescription("**Essence Type:** " + essenceType);
					case "dungeonize" -> eb.appendDescription(
						"\n➜ **Dungeonize:** " + higherDepth(itemJson, level).getAsString() + " " + ESSENCE_EMOJI_MAP.get(essenceType)
					);
					case "1" -> eb.appendDescription(
						"\n➜ **" +
						level +
						" Star:** " +
						higherDepth(itemJson, level).getAsString() +
						" " +
						ESSENCE_EMOJI_MAP.get(essenceType)
					);
					default -> eb.appendDescription(
						"\n➜ **" +
						level +
						" Stars:** " +
						higherDepth(itemJson, level).getAsString() +
						" " +
						ESSENCE_EMOJI_MAP.get(essenceType)
					);
				}
			}
			eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);
			return eb;
		}
		return defaultEmbed("Invalid item name");
	}

	public static EmbedBuilder getPlayerEssence(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			StringBuilder amountsStr = new StringBuilder();
			for (Map.Entry<String, JsonElement> entry : player.profileJson().getAsJsonObject().entrySet()) {
				if (entry.getKey().startsWith("essence_")) {
					String essenceType = entry.getKey().split("essence_")[1];
					amountsStr
						.append(ESSENCE_EMOJI_MAP.get(essenceType))
						.append("** ")
						.append(capitalizeString(essenceType))
						.append(" Essence:** ")
						.append(formatNumber(entry.getValue().getAsInt()))
						.append("\n");
				}
			}
			eb.addField("Amounts", amountsStr.toString(), false);

			JsonElement essenceTiers = getConstant("ESSENCE_SHOP_TIERS");
			StringBuilder witherShopUpgrades = new StringBuilder();
			StringBuilder undeadShopUpgrades = new StringBuilder();
			for (Map.Entry<String, JsonElement> perk : higherDepth(player.profileJson(), "perks").getAsJsonObject().entrySet()) {
				JsonElement curPerk = higherDepth(essenceTiers, perk.getKey());
				JsonArray tiers = higherDepth(curPerk, "tiers").getAsJsonArray();
				String out =
					"\n" +
					ESSENCE_EMOJI_MAP.get(perk.getKey()) +
					"** " +
					capitalizeString(perk.getKey().replace("catacombs_", "").replace("_", " ")) +
					":** " +
					perk.getValue().getAsInt() +
					"/" +
					higherDepth(curPerk, "tiers").getAsJsonArray().size() +
					(
						perk.getValue().getAsInt() == tiers.size()
							? ""
							: (" (" + formatNumber(tiers.get(perk.getValue().getAsInt()).getAsInt()) + " for next)")
					);
				if (higherDepth(curPerk, "type").getAsString().equals("undead")) {
					undeadShopUpgrades.append(out);
				} else {
					witherShopUpgrades.append(out);
				}
			}
			eb.addField("Undead Essence Upgrades", undeadShopUpgrades.toString(), false);
			eb.addField("Wither Essence Upgrades", witherShopUpgrades.toString(), false);

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
						String closestMatch = getClosestMatchFromIds(itemId, ESSENCE_ITEM_NAMES);
						itemId = closestMatch != null ? closestMatch : itemId;
					}

					JsonElement itemJson = higherDepth(getEssenceCostsJson(), itemId);
					if (itemJson != null) {
						new EssenceHandler(itemId, itemJson, ebMessage, event.getAuthor());
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

					embed(getPlayerEssence(player, args.length == 4 ? args[3] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
