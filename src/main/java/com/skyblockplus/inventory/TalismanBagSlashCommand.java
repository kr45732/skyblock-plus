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
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;
import static com.skyblockplus.utils.utils.Utils.withApiHelpButton;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.InvItem;
import groovy.lang.Tuple2;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class TalismanBagSlashCommand extends SlashCommand {

	public TalismanBagSlashCommand() {
		this.name = "talisman";
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main talisman bag command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class ListSubcommand extends Subcommand {

		public ListSubcommand() {
			this.name = "list";
		}

		public static EmbedBuilder getPlayerTalismansList(String username, String profileName, int slotNum, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				Map<Integer, InvItem> talismanBagMap = player.getTalismanBagMap();
				if (talismanBagMap != null) {
					new InventoryListPaginator(player, talismanBagMap, slotNum, event);
					return null;
				}
			}
			return player.getErrorEmbed();
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerTalismansList(event.player, event.getOptionStr("profile"), event.getOptionInt("slot", 0), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get a list of the player's talisman bag with lore")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption)
				.addOption(OptionType.INTEGER, "slot", "Slot number");
		}
	}

	public static class EmojiSubcommand extends Subcommand {

		public EmojiSubcommand() {
			this.name = "emoji";
		}

		public static Object getPlayerTalismansEmoji(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				List<String[]> talismanBag = player.getTalismanBag();
				if (talismanBag == null) {
					return withApiHelpButton(errorEmbed(player.getEscapedUsername() + "'s inventory API is disabled"));
				}

				new InventoryEmojiPaginator(talismanBag, "Talisman Bag", player, event);
				return null;
			}
			return player.getErrorEmbed();
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerTalismansEmoji(event.player, event.getOptionStr("profile"), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get a player's talisman bag represented in emojis")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}

	public static class TuningSubcommand extends Subcommand {

		public TuningSubcommand() {
			this.name = "tuning";
		}

		public static EmbedBuilder getPlayerTuning(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				JsonElement tuningJson = higherDepth(player.profileJson(), "accessory_bag_storage");
				EmbedBuilder eb = player.defaultPlayerEmbed();
				CustomPaginator.Builder paginateBuilder = event.getPaginator(PaginatorExtras.PaginatorType.EMBED_PAGES);
				PaginatorExtras extras = paginateBuilder.getExtras();

				Map<Integer, InvItem> accessoryBagMap = player.getTalismanBagMap();
				List<InvItem> accessoryBag = accessoryBagMap == null
					? new ArrayList<>()
					: accessoryBagMap
						.values()
						.stream()
						.filter(Objects::nonNull)
						.sorted(Comparator.comparingInt(o -> RARITY_TO_NUMBER_MAP.get(o.getRarity())))
						.collect(Collectors.toCollection(ArrayList::new));
				// Don't reverse the rarity because we are iterating reverse order
				Set<String> ignoredTalismans = new HashSet<>();
				for (int i = accessoryBag.size() - 1; i >= 0; i--) {
					String accessoryId = accessoryBag.get(i).getId();

					if (ignoredTalismans.contains(accessoryId)) {
						accessoryBag.remove(i);
					}

					ignoredTalismans.add(accessoryId);
					JsonElement children = higherDepth(getParentsJson(), accessoryId);
					if (children != null) {
						for (JsonElement child : children.getAsJsonArray()) {
							ignoredTalismans.add(child.getAsString());
						}
					}
				}

				Map<String, Tuple2<Integer, Integer>> rarityToCountMp = new HashMap<>();

				int magicPower = player.getMagicPower();
				Set<String> ignoredAccessories = new HashSet<>();
				boolean countedPartyHat = false;

				// Accessories are sorted from highest to lowest rarity
				// in case they have children accessories with lower rarities
				for (InvItem accessory : accessoryBag) {
					String accessoryId = accessory.getId();
					if (ignoredAccessories.contains(accessoryId)) {
						continue;
					}

					ignoredAccessories.add(accessoryId);
					JsonElement children = higherDepth(getParentsJson(), accessoryId);
					if (children != null) {
						for (JsonElement child : children.getAsJsonArray()) {
							ignoredAccessories.add(child.getAsString());
						}
					}

					int extraMagicPower = 0;
					if (accessoryId.equals("HEGEMONY_ARTIFACT")) {
						extraMagicPower += rarityToMagicPower.get(accessory.getRarity());
					} else if (accessoryId.equals("ABICASE")) {
						JsonElement activeContacts = higherDepth(
							player.profileJson(),
							"nether_island_player_data.abiphone.active_contacts"
						);
						if (activeContacts != null) {
							extraMagicPower += activeContacts.getAsJsonArray().size() / 2;
						}
					} else if (accessoryId.startsWith("PARTY_HAT")) {
						if (countedPartyHat) {
							// Only one party hat counts towards magic power
							continue;
						} else {
							countedPartyHat = true;
						}
					}

					int curMagicPower = rarityToMagicPower.get(accessory.getRarity()) + extraMagicPower;
					rarityToCountMp.compute(
						accessory.getRarity(),
						(k, v) -> new Tuple2<>((v != null ? v.getV1() : 0) + 1, (v != null ? v.getV2() : 0) + curMagicPower)
					);
				}

				StringBuilder accessoryStr = new StringBuilder();
				for (Map.Entry<String, Tuple2<Integer, Integer>> entry : rarityToCountMp
					.entrySet()
					.stream()
					.sorted(Comparator.comparingInt(e -> RARITY_TO_NUMBER_MAP.get(e.getKey())))
					.toList()) {
					accessoryStr
						.append("\n• ")
						.append(capitalizeString(entry.getKey().replace("_", " ")))
						.append(": ")
						.append(entry.getValue().getV1())
						.append(" (")
						.append(entry.getValue().getV2())
						.append(" magic power)");
				}

				String selectedPower = higherDepth(tuningJson, "selected_power", "");
				double scaling = Math.pow(29.97 * (Math.log(0.0019 * magicPower + 1)), 1.2);

				eb
					.setDescription(
						"**Selected Power:** " +
						(selectedPower.isEmpty() ? " None" : capitalizeString(selectedPower)) +
						"\n**Magic Power:** " +
						formatNumber(magicPower) +
						"\n**Unlocked Tuning Slots:** " +
						higherDepth(tuningJson, "tuning").getAsJsonObject().keySet().stream().filter(j -> j.startsWith("slot_")).count()
					)
					.addField(
						"Accessory Counts",
						player.isInventoryApiEnabled() ? accessoryStr.toString() : "Inventory API is disabled",
						false
					);
				if (higherDepth(tuningJson, "unlocked_powers") != null) {
					JsonArray unlockedPowers = higherDepth(tuningJson, "unlocked_powers").getAsJsonArray();
					eb.appendDescription("\n**Unlocked Power Stones:** " + unlockedPowers.size());
					eb.addField(
						"Unlocked Powers",
						streamJsonArray(unlockedPowers)
							.map(p -> capitalizeString(p.getAsString()))
							.collect(Collectors.joining("\n• ", "• ", "")),
						false
					);
				}
				if (!selectedPower.isEmpty()) {
					StringBuilder powerStoneStr = new StringBuilder();
					for (Map.Entry<String, JsonElement> entry : higherDepth(POWER_TO_BASE_STATS, selectedPower)
						.getAsJsonObject()
						.entrySet()) {
						powerStoneStr
							.append("\n")
							.append(statToEmoji.get(entry.getKey()))
							.append(" ")
							.append(capitalizeString(entry.getKey().replace("_", " ")))
							.append(": ")
							.append(roundAndFormat(entry.getValue().getAsDouble() * scaling));
					}
					eb.addField("Power Stone Bonuses", powerStoneStr.toString(), false);
				}
				extras.addEmbedPage(eb);

				for (Map.Entry<String, JsonElement> slot : higherDepth(tuningJson, "tuning").getAsJsonObject().entrySet()) {
					if (slot.getKey().startsWith("slot_")) {
						eb =
							player
								.defaultPlayerEmbed()
								.appendDescription("**Slot:** " + (Integer.parseInt(slot.getKey().split("slot_")[1]) + 1));

						int tuningPointsSpent = 0;
						StringBuilder statStr = new StringBuilder();
						for (Map.Entry<String, JsonElement> stat : slot.getValue().getAsJsonObject().entrySet()) {
							int amountSpent = stat.getValue().getAsInt();
							tuningPointsSpent += amountSpent;
							statStr
								.append("\n")
								.append(statToEmoji.get(stat.getKey()))
								.append(" ")
								.append(capitalizeString(stat.getKey().replace("_", " ")))
								.append(": ")
								.append(amountSpent)
								.append(
									amountSpent > 0
										? " (+" +
										roundAndFormat(amountSpent * tuningStatToMultiplier.getOrDefault(stat.getKey(), 1.0)) +
										")"
										: ""
								);
						}

						extras.addEmbedPage(eb.appendDescription("\n**Tuning Points Spent:** " + tuningPointsSpent + "\n" + statStr));
					}
				}

				event.paginate(paginateBuilder);
				return null;
			}

			return player.getErrorEmbed();
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerTuning(event.player, event.getOptionStr("profile"), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get a player's power stone stats and tuning stats")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}
}
