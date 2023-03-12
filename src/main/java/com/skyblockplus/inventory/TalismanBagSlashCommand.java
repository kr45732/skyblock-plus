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
import static com.skyblockplus.utils.utils.Utils.defaultPaginator;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.InvItem;
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
			return new SubcommandData("list", "Get a list of the player's talisman bag with lore")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption)
				.addOption(OptionType.INTEGER, "slot", "Slot number");
		}
	}

	public static class EmojiSubcommand extends Subcommand {

		public EmojiSubcommand() {
			this.name = "emoji";
		}

		public static EmbedBuilder getPlayerTalismansEmoji(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				List<String[]> talismanBag = player.getTalismanBag();
				if (talismanBag != null) {
					new InventoryEmojiPaginator(talismanBag, "Talisman Bag", player, event);
					return null;
				}
				return errorEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
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
			return new SubcommandData("emoji", "Get a player's talisman bag represented in emojis")
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
				PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

				Map<Integer, InvItem> accessoryBagMap = player.getTalismanBagMap();
				List<InvItem> accessoryBag = accessoryBagMap == null
					? new ArrayList<>()
					: accessoryBagMap
						.values()
						.stream()
						.filter(Objects::nonNull)
						.sorted(Comparator.comparingInt(o -> Integer.parseInt(RARITY_TO_NUMBER_MAP.get(o.getRarity()).replace(";", ""))))
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

				StringBuilder accessoryStr = new StringBuilder();
				int magicPower = 0;
				for (Map.Entry<String, Integer> entry : Constants.rarityToMagicPower.entrySet()) {
					long count = accessoryBag.stream().filter(i -> i.getRarity().equals(entry.getKey())).count();
					long power = count * entry.getValue();
					accessoryStr
						.append("\n• ")
						.append(capitalizeString(entry.getKey().replace("_", " ")))
						.append(": ")
						.append(count)
						.append(" (")
						.append(power)
						.append(" magic power)");
					magicPower += power;
				}
				int hegemony = Constants.rarityToMagicPower.getOrDefault(
					accessoryBag.stream().filter(a -> a.getId().equals("HEGEMONY_ARTIFACT")).map(InvItem::getRarity).findFirst().orElse(""),
					0
				);
				if (hegemony != 0) {
					magicPower += hegemony;
					accessoryStr.append("\n• Hegemony Artifact: ").append(hegemony).append(" magic power");
				}
				double scaling = Math.pow(29.97 * (Math.log(0.0019 * magicPower + 1)), 1.2);

				String selectedPower = higherDepth(tuningJson, "selected_power", "");
				eb.setDescription(
					"**Selected Power:** " +
					(selectedPower.isEmpty() ? " None" : capitalizeString(selectedPower)) +
					"\n**Magic Power:** " +
					formatNumber(magicPower) +
					"\n**Unlocked Tuning Slots:** " +
					higherDepth(tuningJson, "tuning").getAsJsonObject().keySet().stream().filter(j -> j.startsWith("slot_")).count()
				);
				eb.addField("Accessory Counts", player.isInventoryApiEnabled() ? accessoryStr.toString() : "Inventory API disabled", false);
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
							.append(Constants.tuningStatToEmoji.get(entry.getKey()))
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
								.append(Constants.tuningStatToEmoji.get(stat.getKey()))
								.append(" ")
								.append(capitalizeString(stat.getKey().replace("_", " ")))
								.append(": ")
								.append(amountSpent)
								.append(
									amountSpent > 0
										? " (+" +
										roundAndFormat(amountSpent * Constants.tuningStatToMultiplier.getOrDefault(stat.getKey(), 1.0)) +
										")"
										: ""
								);
						}

						extras.addEmbedPage(eb.appendDescription("\n**Tuning Points Spent:** " + tuningPointsSpent + "\n" + statStr));
					}
				}

				event.paginate(defaultPaginator(event.getUser()).setPaginatorExtras(extras));
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
			return new SubcommandData("tuning", "Get a player's power stone stats and tuning stats")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}
}
