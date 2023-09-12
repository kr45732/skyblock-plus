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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.formatOrSimplify;
import static com.skyblockplus.utils.utils.StringUtils.idToName;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.InvItem;
import java.util.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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
			for (Map.Entry<String, JsonElement> entry : getConstant("MUSEUM_PARENTS").getAsJsonObject().entrySet()) {
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
			for (Map.Entry<String, JsonElement> entry : getConstant("MUSEUM_PARENTS").getAsJsonObject().entrySet()) {
				List<String> value = streamJsonArray(entry.getValue()).map(JsonElement::getAsString).toList();
				for (int i = value.size() - 1; i >= 0; i--) {
					if (items.contains(value.get(i))) {
						items.add(entry.getKey());
						items.addAll(value.subList(0, i));
						break;
					}
				}
			}

			NetworthExecute calc = new NetworthExecute().initPrices();
			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(25);

			Map<String, Double> formattedToCost = new HashMap<>();

			for (Map.Entry<String, JsonElement> entry : getMuseumCategoriesJson().entrySet()) {
				if (entry.getKey().equals("weapons") || entry.getKey().equals("rarities") || entry.getKey().equals("armor")) {
					for (JsonElement item : entry.getValue().getAsJsonArray()) {
						String itemId = item.getAsString();

						if (items.contains(itemId)) {
							continue;
						}

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

							double cost = 0;
							for (String setItemId : setPieces) {
								cost += calc.getLowestPrice(setItemId);
							}

							formattedToCost.put(
								getEmoji(setPieces.get(0)) +
								" " +
								setName +
								(!isSoulbound ? ": " + formatOrSimplify(cost) : " (Soulbound)"),
								!isSoulbound ? cost : Double.MAX_VALUE
							);
						} else {
							double cost = calc.getLowestPrice(itemId);
							boolean isSoulbound = SOULBOUND_ITEMS.contains(itemId);
							formattedToCost.put(
								getEmoji(itemId) + " " + idToName(itemId) + (!isSoulbound ? ": " + formatOrSimplify(cost) : " (Soulbound)"),
								isSoulbound ? Double.MAX_VALUE : cost
							);
						}
					}
				}
			}

			if (formattedToCost.isEmpty()) {
				return player.defaultPlayerEmbed().setDescription("331/331 museum items donated");
			}

			for (Map.Entry<String, Double> entry : formattedToCost
				.entrySet()
				.stream()
				.sorted(Comparator.comparingDouble(Map.Entry::getValue))
				.toList()) {
				paginateBuilder.addStrings(entry.getKey());
			}

			paginateBuilder
				.getExtras()
				.setEveryPageText(
					"**Total Missing:** " +
					paginateBuilder.size() +
					"\n**Missing Cost:** " +
					formatOrSimplify(
						(long) formattedToCost.values().stream().mapToDouble(e -> e).filter(e -> e != Double.MAX_VALUE).sum()
					) +
					"\n"
				);
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
