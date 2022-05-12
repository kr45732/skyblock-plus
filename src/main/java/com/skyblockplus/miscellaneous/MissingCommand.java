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
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class MissingCommand extends Command {

	public MissingCommand() {
		this.name = "missing";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getMissingTalismans(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			if (!player.isInventoryApiEnabled()) {
				return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
			}

			Set<String> playerItems = new HashSet<>();
			try {
				playerItems.addAll(
					player.getInventoryMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
			} catch (Exception ignored) {}
			try {
				playerItems.addAll(
					player.getEnderChestMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
			} catch (Exception ignored) {}
			try {
				playerItems.addAll(
					player.getStorageMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
			} catch (Exception ignored) {}
			try {
				playerItems.addAll(
					player.getTalismanBagMap().values().stream().filter(Objects::nonNull).map(InvItem::getId).collect(Collectors.toSet())
				);
			} catch (Exception ignored) {}

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

			missingInternalArr.removeAll(List.of("BURNING_KUUDRA_CORE", "FIERY_KUUDRA_CORE", "INFERNAL_KUUDRA_CORE")); // TODO: remove when obtainable

			List<String> soulboundTalisman = List.of("ODGERS_BRONZE_TOOTH",
					"WOLF_PAW",
					"ODGERS_GOLD_TOOTH",
					"FROZEN_CHICKEN",
					"CHEETAH_TALISMAN",
					"ODGERS_DIAMOND_TOOTH",
					"JACOBUS_REGISTER",
					"PIGS_FOOT",
					"ODGERS_SILVER_TOOTH",
					"LYNX_TALISMAN",
					"KING_TALISMAN",
					"CAT_TALISMAN",
					"MELODY_HAIR",
					"SURVIVOR_CUBE");
			List<String> unobtainableIronmanTalismans = List.of("DANTE_TALISMAN", "BLOOD_GOD_CREST", "PARTY_HAT_CRAB", "POTATO_TALISMAN");

			NetworthExecute calc = new NetworthExecute().initPrices();
			missingInternalArr.sort(
				Comparator.comparingDouble(o1 ->
					soulboundTalisman.contains(o1) ||
						o1.startsWith("WEDDING_RING_") ||
						o1.startsWith("CAMPFIRE_TALISMAN_") ||
						(player.isGamemode(Player.Gamemode.IRONMAN) && unobtainableIronmanTalismans.contains(o1))
						? Double.MAX_VALUE
						: calc.getLowestPrice(o1)
				)
			);

			JsonObject mappings = getInternalJsonMappings();
			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator().setItemsPerPage(25);
			double totalCost = 0;
			for (String curId : missingInternalArr) {
				String costOut;
				double cost = calc.getLowestPrice(curId);
				totalCost += cost;
				String wikiLink = higherDepth(mappings, curId + ".wiki", null);
				String name = idToName(curId);
				if (soulboundTalisman.contains(curId) || curId.startsWith("WEDDING_RING_") || curId.startsWith("CAMPFIRE_TALISMAN_")) {
					costOut = (cost != 0 ? " ➜ " + roundAndFormat(cost) : "") + " (Soulbound)";
				} else if (player.isGamemode(Player.Gamemode.IRONMAN) && unobtainableIronmanTalismans.contains(curId)) {
					costOut = " (Unobtainable)";
				} else {
					costOut = " ➜ " + roundAndFormat(cost);
				}
				paginateBuilder.addItems(
					getEmoji(curId) +
					" " +
					(wikiLink == null ? name : "[" + name + "](" + wikiLink + ")") +
					(higherDepth(talismanUpgrades, curId) != null ? "**\\***" : "") +
					costOut
				);
			}
			paginateBuilder
				.getPaginatorExtras()
				.setEveryPageText(
					"**Total Missing:** " +
					missingInternalArr.size() +
					"\n**Total Cost:** " +
					simplifyNumber(totalCost) +
					"\n**Note:** Talismans with a * have higher tiers\n"
				);

			event.paginate(paginateBuilder);
			return null;
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

					paginate(getMissingTalismans(player, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
