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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.ALL_TALISMANS;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class MissingTalismansCommand extends Command {

	public MissingTalismansCommand() {
		this.name = "missing";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getMissingTalismans(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Set<String> playerItems;
			try {
				playerItems =
					player.getInventoryMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet());
				playerItems.addAll(
					player.getEnderChestMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
				playerItems.addAll(
					player.getStorageMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
				playerItems.addAll(
					player.getTalismanBagMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
			} catch (Exception e) {
				return invalidEmbed("Inventory API is disabled");
			}

			JsonObject talismanUpgrades = higherDepth(getMiscJson(), "talisman_upgrades").getAsJsonObject();
			Set<String> missingInternal = new HashSet<>(ALL_TALISMANS);

			for (String playerItem : playerItems) {
				missingInternal.remove(playerItem);
				for (Map.Entry<String, JsonElement> talismanUpgradesElement : talismanUpgrades.entrySet()) {
					JsonArray upgrades = talismanUpgradesElement.getValue().getAsJsonArray();
					for (int j = 0; j < upgrades.size(); j++) {
						String upgrade = upgrades.get(j).getAsString();
						if (playerItem.equals(upgrade)) {
							missingInternal.remove(talismanUpgradesElement.getKey());
							break;
						}
					}
				}
			}

			List<String> missingInternalArr = new ArrayList<>(missingInternal);
			List<String> missingInternalArrCopy = new ArrayList<>(missingInternalArr);

			missingInternalArrCopy.forEach(o1 -> {
				if (higherDepth(talismanUpgrades, o1) != null) {
					JsonArray curUpgrades = higherDepth(talismanUpgrades, o1).getAsJsonArray();
					for (JsonElement curUpgrade : curUpgrades) {
						missingInternalArr.remove(curUpgrade.getAsString());
					}
				}
			});

			JsonElement lowestBinJson = getLowestBinJson();
			missingInternalArr.sort(
				Comparator.comparingDouble(o1 -> higherDepth(lowestBinJson, o1) != null ? higherDepth(lowestBinJson, o1).getAsDouble() : 0)
			);

			StringBuilder ebStr = new StringBuilder(
				"Missing " +
				missingInternalArr.size() +
				" talisman" +
				(missingInternalArr.size() > 1 ? "s" : "") +
				". Sorted by ascending cost. Talismans with a * have higher tiers.\n\n"
			);

			JsonObject mappings = getInternalJsonMappings();
			for (String i : missingInternalArr) {
				String wikiLink = higherDepth(mappings, i + ".wiki", null);
				ebStr
					.append("• ")
					.append(wikiLink == null ? idToName(i) : "[" + idToName(i) + "](" + wikiLink + ")")
					.append(higherDepth(talismanUpgrades, i) != null ? "**\\***" : "")
					.append("\n");
			}
			return player.defaultPlayerEmbed().setDescription(ebStr.toString());
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getMissingTalismans(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
