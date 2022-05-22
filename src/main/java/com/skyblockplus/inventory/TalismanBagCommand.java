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

package com.skyblockplus.inventory;

import static com.skyblockplus.utils.Constants.POWER_TO_BASE_STATS;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.groovy.util.Maps;
import org.springframework.stereotype.Component;

@Component
public class TalismanBagCommand extends Command {

	private static final Map<String, Double> statToMultiplier = Maps.of(
		"health",
		5.0,
		"walk_speed",
		1.5,
		"critical_chance",
		0.2,
		"attack_speed",
		0.2,
		"intelligence",
		2.0
	);
	private static final Map<String, String> statToEmoji = Maps.of(
		"health",
		"❤️",
		"defense",
		getEmoji("IRON_CHESTPLATE"),
		"strength",
		getEmoji("BLAZE_POWDER"),
		"walk_speed",
		getEmoji("SUGAR"),
		"critical_chance",
		"☣️",
		"critical_damage",
		"☠️",
		"intelligence",
		getEmoji("ENCHANTED_BOOK"),
		"attack_speed",
		"⚔️"
	);
	private static final Map<String, Integer> rarityToMagicPower = Maps.of(
		"COMMON",
		3,
		"UNCOMMON",
		5,
		"RARE",
		8,
		"EPIC",
		12,
		"LEGENDARY",
		16,
		"MYTHIC",
		22
	);

	public TalismanBagCommand() {
		this.name = "talisman";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "talismans" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerTalismansList(String username, String profileName, int slotNum, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> talismanBagMap = player.getTalismanBagMap();
			if (talismanBagMap != null) {
				new InventoryListPaginator(player, talismanBagMap, slotNum, event);
				return null;
			}
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				int slotNumber = -1;
				for (int i = 0; i < args.length; i++) {
					if (args[i].startsWith("slot:")) {
						try {
							slotNumber = Math.max(0, Integer.parseInt(args[i].split("slot:")[1]));
							removeArg(i);
						} catch (Exception ignored) {}
					}
				}

				if (args.length >= 2 && args[1].equals("tuning")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					paginate(getPlayerTuning(player, args.length == 4 ? args[3] : null, getPaginatorEvent()));
				} else if (slotNumber != -1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerTalismansList(player, args.length == 3 ? args[2] : null, slotNumber, getPaginatorEvent()));
				} else {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerTalismansEmoji(player, args.length == 3 ? args[2] : null, getPaginatorEvent()), true);
				}
			}
		}
			.queue();
	}

	public static EmbedBuilder getPlayerTalismansEmoji(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> talismanBag = player.getTalismanBag();
			if (talismanBag != null) {
				if (player.invMissing.length() > 0) {
					event.getChannel().sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build()).queue();
				}

				new InventoryEmojiPaginator(talismanBag, "Talisman Bag", player, event);
				return null;
			}
			return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
		}
		return player.getFailEmbed();
	}

	public static EmbedBuilder getPlayerTuning(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			JsonElement tuningJson = higherDepth(player.profileJson(), "accessory_bag_storage");
			EmbedBuilder eb = player.defaultPlayerEmbed();
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

			Map<Integer, InvItem> accessoryBagMap = player.getTalismanBagMap();
			List<InvItem> accessoryBag = accessoryBagMap == null
				? new ArrayList<>()
				: accessoryBagMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
			StringBuilder accessoryStr = new StringBuilder();
			int magicPower = 0;
			for (Map.Entry<String, Integer> entry : rarityToMagicPower.entrySet()) {
				long count = accessoryBag.stream().filter(i -> i.getRarity().equals(entry.getKey())).count();
				long power = count * entry.getValue();
				accessoryStr
					.append("\n• ")
					.append(capitalizeString(entry.getKey()))
					.append(": ")
					.append(count)
					.append(" (")
					.append(power)
					.append(" magic power)");
				magicPower += power;
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
				for (Map.Entry<String, JsonElement> entry : higherDepth(POWER_TO_BASE_STATS, selectedPower).getAsJsonObject().entrySet()) {
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
									? " (+" + roundAndFormat(amountSpent * statToMultiplier.getOrDefault(stat.getKey(), 1.0)) + ")"
									: ""
							);
					}

					extras.addEmbedPage(eb.appendDescription("\n**Tuning Points Spent:** " + tuningPointsSpent + "\n" + statStr));
				}
			}

			event.paginate(defaultPaginator(event.getUser()).setPaginatorExtras(extras));
			return null;
		}

		return player.getFailEmbed();
	}
}
