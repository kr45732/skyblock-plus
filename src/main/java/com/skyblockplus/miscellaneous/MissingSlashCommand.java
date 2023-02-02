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
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class MissingSlashCommand extends SlashCommand {

	private static List<String> soulboundItems;

	public MissingSlashCommand() {
		this.name = "missing";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getMissingTalismans(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's missing talismans")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(Constants.profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getMissingTalismans(String username, String profileName, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			if (!player.isInventoryApiEnabled()) {
				return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
			}

			Map<Integer, InvItem> talismanBag = player.getTalismanBagMap();

			if (talismanBag == null) {
				return invalidEmbed(player.getUsernameFixed() + " has not unlocked the talisman bag");
			}

			Set<String> playerItems = talismanBag
				.values()
				.stream()
				.filter(Objects::nonNull)
				.map(InvItem::getId)
				.collect(Collectors.toSet());

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

			NetworthExecute calc = new NetworthExecute().initPrices();

			Map.Entry<Double, List<String>> missing = getMissingInfo(missingInternalArr, player, calc);

			for (int i = 0; i < missingInternalArr.size(); i++) {
				String highestValue = higherDepth(talismanUpgrades, missingInternalArr.get(i) + ".[-1]", null);
				if (highestValue != null) {
					missingInternalArr.set(i, highestValue);
				}
			}

			Map.Entry<Double, List<String>> missingHighest = getMissingInfo(missingInternalArr, player, calc);

			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(25);
			paginateBuilder
				.addItems(missing.getValue())
				.getExtras()
				.setEveryPageText(
					"**Total Missing:** " +
					missingInternalArr.size() +
					"\n**Total Cost:** " +
					simplifyNumber(missing.getKey()) +
					"\n**Note:** Talismans with a * have higher tiers\n"
				)
				.addReactiveButtons(
					new PaginatorExtras.ReactiveButton(
						Button.primary("reactive_missing_command_show_highest", "Show Highest Tier"),
						paginator -> {
							paginator.setStrings(missingHighest.getValue());
							paginator
								.getExtras()
								.setEveryPageText(
									"**Total Missing:** " +
									missingInternalArr.size() +
									"\n**Total Cost:** " +
									simplifyNumber(missingHighest.getKey()) +
									"\n**Note:** Showing highest tiers\n"
								)
								.toggleReactiveButton("reactive_missing_command_show_highest", false)
								.toggleReactiveButton("reactive_missing_command_show_next", true);
						},
						true
					),
					new PaginatorExtras.ReactiveButton(
						Button.primary("reactive_missing_command_show_next", "Show Next Tier"),
						paginator -> {
							paginator.setStrings(missing.getValue());
							paginator
								.getExtras()
								.setEveryPageText(
									"**Total Missing:** " +
									missingInternalArr.size() +
									"\n**Total Cost:** " +
									simplifyNumber(missing.getKey()) +
									"\n**Note:** Talismans with a * have higher tiers\n"
								)
								.toggleReactiveButton("reactive_missing_command_show_highest", true)
								.toggleReactiveButton("reactive_missing_command_show_next", false);
						},
						false
					)
				);

			event.paginate(paginateBuilder);
			return null;
		}
		return player.getFailEmbed();
	}

	public static Map.Entry<Double, List<String>> getMissingInfo(List<String> missingInternalArr, Player player, NetworthExecute calc) {
		if (soulboundItems == null) {
			soulboundItems =
				getSkyblockItemsJson()
					.entrySet()
					.stream()
					.filter(e -> higherDepth(e.getValue(), "soulbound", null) != null)
					.map(Map.Entry::getKey)
					.toList();
		}

		List<String> unobtainableIronmanTalismans = List.of("DANTE_TALISMAN", "BLOOD_GOD_CREST", "PARTY_HAT_CRAB", "POTATO_TALISMAN");

		List<String> crabHatColors = List.of("RED", "AQUA", "LIME", "PINK", "BLACK", "GREEN", "ORANGE", "PURPLE", "YELLOW");
		double crabHatCost = -1;
		double animatedCrabHatCost = -1;
		for (String crabHatColor : crabHatColors) {
			double price = calc.getLowestPrice("PARTY_HAT_CRAB_" + crabHatColor);
			if (price > 0 && (crabHatCost == -1 || price < crabHatCost)) {
				crabHatCost = price;
			}
			double animatedPrice = calc.getLowestPrice("PARTY_HAT_CRAB_ANIMATED_" + crabHatColor);
			if (animatedPrice > 0 && (animatedCrabHatCost == -1 || animatedPrice < animatedCrabHatCost)) {
				animatedCrabHatCost = animatedPrice;
			}
		}
		crabHatCost = Math.max(0, crabHatCost);
		animatedCrabHatCost = Math.max(0, animatedCrabHatCost);

		double finalCrabHatCost = crabHatCost;
		double finalAnimatedCrabHatCost = animatedCrabHatCost;
		missingInternalArr.sort(
			Comparator.comparingDouble(o1 -> {
				if (
					soulboundItems.contains(o1) || (player.isGamemode(Player.Gamemode.IRONMAN) && unobtainableIronmanTalismans.contains(o1))
				) {
					return Double.MAX_VALUE;
				} else if (o1.equals("PARTY_HAT_CRAB")) {
					return finalCrabHatCost;
				} else if (o1.equals("PARTY_HAT_CRAB_ANIMATED")) {
					return finalAnimatedCrabHatCost;
				}
				return calc.getLowestPrice(o1);
			})
		);

		JsonObject mappings = getInternalJsonMappings();
		double totalCost = 0;
		List<String> out = new ArrayList<>();
		for (String curId : missingInternalArr) {
			double cost = calc.getLowestPrice(curId);
			if (curId.equals("PARTY_HAT_CRAB")) {
				cost = finalCrabHatCost;
			} else if (curId.equals("PARTY_HAT_CRAB_ANIMATED")) {
				cost = finalAnimatedCrabHatCost;
			}
			totalCost += cost;

			String wikiLink = higherDepth(mappings, curId + ".wiki", null);
			String name = idToName(curId);
			String costOut;
			if (soulboundItems.contains(curId)) {
				costOut = (cost != 0 ? " ➜ " + roundAndFormat(cost) : "") + " (Soulbound)";
			} else if (player.isGamemode(Player.Gamemode.IRONMAN) && unobtainableIronmanTalismans.contains(curId)) {
				costOut = " (Unobtainable)";
			} else {
				costOut = " ➜ " + roundAndFormat(cost);
			}

			JsonObject talismanUpgrades = higherDepth(getMiscJson(), "talisman_upgrades").getAsJsonObject();
			out.add(
				getEmoji(curId) +
				" " +
				(wikiLink == null ? name : "[" + name + "](" + wikiLink + ")") +
				(higherDepth(talismanUpgrades, curId) != null ? "**\\***" : "") +
				costOut
			);
		}

		return new AbstractMap.SimpleEntry<>(totalCost, out);
	}
}
