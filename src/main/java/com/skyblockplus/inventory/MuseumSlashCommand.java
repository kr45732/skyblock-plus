/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.simplifyNumber;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class MuseumSlashCommand extends SlashCommand {

	public MuseumSlashCommand() {
		this.name = "museum";
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main museum command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class ViewSubcommand extends Subcommand {

		public ViewSubcommand() {
			this.name = "view";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerMuseum(event.player, event.getOptionStr("profile"), event));
		}

		public static Object getPlayerMuseum(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (!player.isValid()) {
				return player.getErrorEmbed();
			}

			HypixelResponse hypixelResponse = player.getMuseum();
			if (!hypixelResponse.isValid()) {
				if (hypixelResponse.failCause().endsWith("museum API is disabled")) {
					return withApiHelpButton(hypixelResponse.getErrorEmbed());
				} else {
					return hypixelResponse.getErrorEmbed();
				}
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
			for (Map.Entry<String, List<String>> entry : MUSEUM_PARENTS.entrySet()) {
				for (int i = entry.getValue().size() - 1; i >= 0; i--) {
					if (items.contains(entry.getValue().get(i))) {
						bypassedItems.add(entry.getKey());
						bypassedItems.addAll(entry.getValue().subList(0, i));
						break;
					}
				}
			}
			bypassedItems.removeIf(items::contains);

			new MuseumPaginator(items, bypassedItems, player, event);
			return null;
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "View a player's museum items")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}

	public static class CheapestSubcommand extends Subcommand {

		public CheapestSubcommand() {
			this.name = "cheapest";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getCheapestMuseum(event.player, event.getOptionStr("profile"), event));
		}

		public static Object getCheapestMuseum(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (!player.isValid()) {
				return player.getErrorEmbed();
			}

			HypixelResponse hypixelResponse = player.getMuseum();
			if (!hypixelResponse.isValid()) {
				if (hypixelResponse.failCause().endsWith("museum API is disabled")) {
					return withApiHelpButton(hypixelResponse.getErrorEmbed());
				} else {
					return hypixelResponse.getErrorEmbed();
				}
			}

			JsonElement museumJson = hypixelResponse.get(player.getUuid());
			Set<String> items = new HashSet<>(higherDepth(museumJson, "items").getAsJsonObject().keySet());
			Set<String> itemsHighest = new HashSet<>();
			for (Map.Entry<String, List<String>> entry : MUSEUM_PARENTS.entrySet()) {
				List<String> v = entry.getValue();

				for (int i = v.size() - 1; i >= 0; i--) {
					if (items.contains(v.get(i))) {
						items.add(entry.getKey());
						items.addAll(v.subList(0, i));
						break;
					}
				}

				// If player doesn't have the highest item then add all below it
				if (!items.contains(v.get(v.size() - 1))) {
					itemsHighest.add(entry.getKey());
					itemsHighest.addAll(v.subList(0, v.size() - 1));
				}
			}

			NetworthExecute calc = new NetworthExecute().initPrices();
			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(25);

			Map<String, Double> formattedToCost = new HashMap<>();
			List<String> useForHighest = new ArrayList<>();

			for (Map.Entry<String, JsonElement> entry : getMuseumCategoriesJson().entrySet()) {
				if (entry.getKey().equals("weapons") || entry.getKey().equals("rarities") || entry.getKey().equals("armor")) {
					for (JsonElement item : entry.getValue().getAsJsonArray()) {
						String itemId = item.getAsString();

						if (items.contains(itemId)) {
							continue;
						}

						String name;
						double cost = 0;

						if (entry.getKey().equals("armor")) {
							List<String> setPieces = streamJsonArray(getConstant("MUSEUM_ARMOR_TO_SET." + itemId))
								.filter(e -> !e.isJsonNull())
								.map(JsonElement::getAsString)
								.toList();
							boolean isSoulbound = setPieces.stream().anyMatch(SOULBOUND_ITEMS::contains);
							String setName =
								switch (itemId) {
									case "MINER_OUTFIT" -> "Miner's Outfit";
									case "TANK_MINER" -> "Miner Armor";
									case "SALMON_NEW" -> "Salmon Armor";
									case "ARMOR_OF_YOG" -> "Yog Armor";
									case "MAGMA" -> "Crimson Hunter Set";
									case "VANQUISHED" -> "Vanquisher Set";
									case "MOLTEN" -> "Molten Set";
									case "LOTUS" -> "Lotus Set";
									case "SEYMOUR" -> "Seymour's Special Armor";
									case "TANK_WITHER" -> "Goldor's Armor";
									case "WISE_WITHER" -> "Storm's Armor";
									case "SPEED_WITHER" -> "Maxor's Armor";
									case "POWER_WITHER" -> "Necron's Armor";
									default -> idToName(itemId) +
									(itemId.contains("ARMOR") || itemId.endsWith("_SUIT") || itemId.endsWith("_TUXEDO") ? "" : " Armor");
								};

							for (String setItemId : setPieces) {
								cost += calc.getLowestPrice(setItemId);
							}

							name =
								getEmoji(setPieces.get(0)) +
								" " +
								setName +
								(!isSoulbound ? ": " + formatOrSimplify(cost) : " (Soulbound)");
							cost = !isSoulbound ? cost : Double.MAX_VALUE;
						} else {
							cost = calc.getLowestPrice(itemId);
							boolean isSoulbound = SOULBOUND_ITEMS.contains(itemId);
							name =
								getEmoji(itemId) + " " + idToName(itemId) + (!isSoulbound ? ": " + formatOrSimplify(cost) : " (Soulbound)");
							cost = isSoulbound ? Double.MAX_VALUE : cost;
						}

						formattedToCost.put(name, cost);

						if (!itemsHighest.contains(itemId)) {
							useForHighest.add(name);
						}
					}
				}
			}

			if (formattedToCost.isEmpty()) {
				return player.defaultPlayerEmbed().setDescription("331/331 museum items donated");
			}

			List<String> missing = new ArrayList<>();
			List<String> missingHighest = new ArrayList<>();
			double missingTotal = 0;
			double missingTotalHighest = 0;
			for (Map.Entry<String, Double> entry : formattedToCost
				.entrySet()
				.stream()
				.sorted(Comparator.comparingDouble(Map.Entry::getValue))
				.toList()) {
				missing.add(entry.getKey());
				if (entry.getValue() != Double.MAX_VALUE) {
					missingTotal += entry.getValue();
				}

				if (useForHighest.contains(entry.getKey())) {
					missingHighest.add(entry.getKey());
					if (entry.getValue() != Double.MAX_VALUE) {
						missingTotalHighest += entry.getValue();
					}
				}
			}

			double finalMissingTotalHighest = missingTotalHighest;
			double finalMissingTotal = missingTotal;

			paginateBuilder
				.getExtras()
				.addStrings(missing)
				.setEveryPageText(
					"**Total Missing:** " + missing.size() + "\n**Missing Cost:** " + formatOrSimplify((long) missingTotal) + "\n"
				);
			// Only show button if highest is different from all
			if (missing.size() != missingHighest.size()) {
				paginateBuilder
					.getExtras()
					.addReactiveButtons(
						new PaginatorExtras.ReactiveButton(
							Button.primary("reactive_museum_command_show_highest", "Show Highest Tier"),
							action -> {
								action
									.paginator()
									.getExtras()
									.setStrings(missingHighest)
									.setEveryPageText(
										"**Total Missing:** " +
										missingHighest.size() +
										"\n**Total Cost:** " +
										simplifyNumber((long) finalMissingTotalHighest) +
										"\n**Note:** Showing highest tier\n"
									)
									.toggleReactiveButton("reactive_museum_command_show_highest", false)
									.toggleReactiveButton("reactive_museum_command_show_all", true);
							},
							true
						),
						new PaginatorExtras.ReactiveButton(
							Button.primary("reactive_museum_command_show_all", "Show All Tiers"),
							action -> {
								action
									.paginator()
									.getExtras()
									.setStrings(missing)
									.setEveryPageText(
										"**Total Missing:** " +
										missing.size() +
										"\n**Total Cost:** " +
										simplifyNumber((long) finalMissingTotal) +
										"\n"
									)
									.toggleReactiveButton("reactive_museum_command_show_highest", true)
									.toggleReactiveButton("reactive_museum_command_show_all", false);
							},
							false
						)
					);
			}
			event.paginate(paginateBuilder);
			return null;
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get the cheapest items to donate to a player's museum")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}
}
