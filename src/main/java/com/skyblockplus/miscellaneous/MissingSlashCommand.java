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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

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

	private static final List<String> unobtainableIronmanTalismans = List.of("BLOOD_GOD_CREST", "POTATO_TALISMAN");
	private static final List<String> crabHatColors = List.of(
		"RED",
		"AQUA",
		"LIME",
		"PINK",
		"BLACK",
		"GREEN",
		"ORANGE",
		"PURPLE",
		"YELLOW"
	);

	public MissingSlashCommand() {
		this.name = "missing";
	}

	public static EmbedBuilder getMissingTalismans(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			if (!player.isInventoryApiEnabled()) {
				return errorEmbed(player.getEscapedUsername() + "'s inventory API is disabled");
			}

			Map<Integer, InvItem> talismanBag = player.getTalismanBagMap();
			if (talismanBag == null) {
				return errorEmbed(player.getEscapedUsername() + " has not unlocked the talisman bag");
			}

			Set<String> playerItems = talismanBag
				.values()
				.stream()
				.filter(Objects::nonNull)
				.map(InvItem::getId)
				.collect(Collectors.toSet());

			JsonObject talismanUpgrades = higherDepth(getMiscJson(), "talisman_upgrades").getAsJsonObject();
			List<String> missingTalismans = new ArrayList<>(ALL_TALISMANS);

			for (String playerItem : playerItems) {
				if (playerItem.startsWith("PARTY_HAT_CRAB_")) {
					if (playerItem.endsWith("_ANIMATED")) {
						missingTalismans.remove("PARTY_HAT_CRAB_ANIMATED");
					} else {
						missingTalismans.remove("PARTY_HAT_CRAB");
					}
				} else {
					missingTalismans.remove(playerItem);
				}
				for (Map.Entry<String, JsonElement> talismanUpgradesElement : talismanUpgrades.entrySet()) {
					JsonArray upgrades = talismanUpgradesElement.getValue().getAsJsonArray();
					for (int j = 0; j < upgrades.size(); j++) {
						String upgrade = upgrades.get(j).getAsString();
						if (playerItem.equals(upgrade)) {
							missingTalismans.remove(talismanUpgradesElement.getKey());
							break;
						}
					}
				}
			}

			List<String> missingTalismansCopy = new ArrayList<>(missingTalismans);
			for (String talisman : missingTalismansCopy) {
				if (higherDepth(talismanUpgrades, talisman) != null) {
					for (JsonElement curUpgrade : higherDepth(talismanUpgrades, talisman).getAsJsonArray()) {
						missingTalismans.remove(curUpgrade.getAsString());
					}
				}
			}

			NetworthExecute calc = new NetworthExecute().initPrices();

			Map.Entry<Double, List<String>> missing = getMissingInfo(missingTalismans, player, calc);

			for (int i = 0; i < missingTalismans.size(); i++) {
				String name = missingTalismans.get(i);
				if (name.startsWith("PARTY_HAT_CRAB")) {
					continue;
				}

				String highestValue = higherDepth(talismanUpgrades, name + ".[-1]", null);
				if (highestValue != null) {
					missingTalismans.set(i, highestValue);
				}
			}

			Map.Entry<Double, List<String>> missingHighest = getMissingInfo(missingTalismans, player, calc);

			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(25);
			paginateBuilder
				.getExtras()
				.addStrings(missing.getValue())
				.setEveryPageText(
					"**Total Missing:** " +
					missingTalismans.size() +
					"\n**Total Cost:** " +
					simplifyNumber(missing.getKey()) +
					"\n**Note:** Talismans with a * have higher tiers\n"
				)
				.addReactiveButtons(
					new PaginatorExtras.ReactiveButton(
						Button.primary("reactive_missing_command_show_highest", "Show Highest Tier"),
						paginator ->
							paginator
								.getExtras()
								.setStrings(missingHighest.getValue())
								.setEveryPageText(
									"**Total Missing:** " +
									missingTalismans.size() +
									"\n**Total Cost:** " +
									simplifyNumber(missingHighest.getKey()) +
									"\n**Note:** Showing highest tiers\n"
								)
								.toggleReactiveButton("reactive_missing_command_show_highest", false)
								.toggleReactiveButton("reactive_missing_command_show_next", true),
						true
					),
					new PaginatorExtras.ReactiveButton(
						Button.primary("reactive_missing_command_show_next", "Show Next Tier"),
						paginator ->
							paginator
								.getExtras()
								.setStrings(missing.getValue())
								.setEveryPageText(
									"**Total Missing:** " +
									missingTalismans.size() +
									"\n**Total Cost:** " +
									simplifyNumber(missing.getKey()) +
									"\n**Note:** Talismans with a * have higher tiers\n"
								)
								.toggleReactiveButton("reactive_missing_command_show_highest", true)
								.toggleReactiveButton("reactive_missing_command_show_next", false),
						false
					)
				);

			event.paginate(paginateBuilder);
			return null;
		}
		return player.getErrorEmbed();
	}

	public static Map.Entry<Double, List<String>> getMissingInfo(
		List<String> missingInternalArr,
		Player.Profile player,
		NetworthExecute calc
	) {
		// Only one edition is needed (if all are missing, it means they don't have any edition)
		boolean missing2021 = missingInternalArr.remove("PARTY_HAT_CRAB");
		boolean missing2022 = missingInternalArr.remove("PARTY_HAT_CRAB_ANIMATED");
		boolean missing2023 = missingInternalArr.remove("PARTY_HAT_SLOTH");
		if (missing2021 && missing2022 && missing2023) {
			double partyHatCost = -1;
			String partyHatId = "";
			for (String color : crabHatColors) {
				for (String edition : List.of("", "_ANIMATED")) {
					String id = "PARTY_HAT_CRAB_" + color + edition;
					double price = calc.getLowestPrice(id);
					if (price > 0 && (partyHatCost == -1 || price < partyHatCost)) {
						partyHatCost = price;
						partyHatId = id;
					}
				}
			}

			double slothHatCost = calc.getLowestPrice("PARTY_HAT_SLOTH");
			if (slothHatCost < partyHatCost) {
				partyHatId = "PARTY_HAT_SLOTH";
			}

			missingInternalArr.add(partyHatId);
		}

		missingInternalArr.sort(
			Comparator.comparingDouble(o1 ->
				SOULBOUND_ITEMS.contains(o1) || (player.isGamemode(Player.Gamemode.IRONMAN) && unobtainableIronmanTalismans.contains(o1))
					? Double.MAX_VALUE
					: calc.getLowestPrice(o1)
			)
		);

		double totalCost = 0;
		List<String> out = new ArrayList<>();
		for (String curId : missingInternalArr) {
			double cost = calc.getLowestPrice(curId);
			totalCost += cost;

			String wikiLink = higherDepth(getInternalJsonMappings(), curId + ".wiki", null);
			String name = idToName(curId);
			String costOut;
			if (SOULBOUND_ITEMS.contains(curId)) {
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
				(!curId.startsWith("PARTY_HAT_CRAB") && higherDepth(talismanUpgrades, curId) != null ? "**\\***" : "") +
				costOut
			);
		}

		return new AbstractMap.SimpleEntry<>(totalCost, out);
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
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
