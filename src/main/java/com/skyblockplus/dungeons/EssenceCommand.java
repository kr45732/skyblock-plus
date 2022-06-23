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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
					case "items" -> {}
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
						ESSENCE_EMOJI_MAP.get(essenceType) +
						(
							higherDepth(itemJson, "items.1") != null
								? streamJsonArray(higherDepth(itemJson, "items.1"))
									.map(i -> parseMcCodes(i.getAsString()))
									.collect(Collectors.joining(", ", ", ", ""))
								: ""
						)
					);
					default -> eb.appendDescription(
						"\n➜ **" +
						level +
						" Stars:** " +
						higherDepth(itemJson, level).getAsString() +
						" " +
						ESSENCE_EMOJI_MAP.get(essenceType) +
						(
							higherDepth(itemJson, "items." + level) != null
								? streamJsonArray(higherDepth(itemJson, "items." + level))
									.map(i -> parseMcCodes(i.getAsString()))
									.collect(Collectors.joining(", ", ", ", ""))
								: ""
						)
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
			for (String essence : List.of("ice", "gold", "dragon", "spider", "undead", "wither", "diamond", "crimson")) {
				amountsStr
					.append(ESSENCE_EMOJI_MAP.get(essence))
					.append("** ")
					.append(capitalizeString(essence))
					.append(" Essence:** ")
					.append(formatNumber(higherDepth(player.profileJson(), "essence_" + essence, 0)))
					.append("\n");
			}

			eb.addField("Amounts", amountsStr.toString(), false);

			if (higherDepth(player.profileJson(), "perks") != null) {
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
					new EssenceHandler(nameToId(args[2]), getPaginatorEvent());
				} else if (args.length == 3 && (args[1].equals("info") || args[1].equals("information"))) {
					embed(getEssenceInformation(args[2]));
				} else {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getPlayerEssence(player, args.length == 3 ? args[2] : null));
				}
			}
		}
			.queue();
	}
}
