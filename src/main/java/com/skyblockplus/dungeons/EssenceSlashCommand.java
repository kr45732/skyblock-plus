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
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class EssenceSlashCommand extends SlashCommand {

	public EssenceSlashCommand() {
		this.name = "essence";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "upgrade" -> new EssenceHandler(nameToId(event.getOptionStr("item")), event);
			case "information" -> event.embed(getEssenceInformation(event.getOptionStr("item")));
			case "player" -> {
				if (event.invalidPlayerOption()) {
					return;
				}
				event.embed(getPlayerEssence(event.player, event.getOptionStr("profile")));
			}
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get essence upgrade information for an item")
			.addSubcommands(
				new SubcommandData("upgrade", "Interactive message to find the essence amount to upgrade an item")
					.addOption(OptionType.STRING, "item", "Item name", true, true),
				new SubcommandData("information", "Get the amount of essence to upgrade an item for each level")
					.addOption(OptionType.STRING, "item", "Item name", true, true),
				new SubcommandData("player", "Get the amount of each essence a player has")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "profile", "Profile name")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				ESSENCE_ITEM_NAMES.stream().map(Utils::idToName).distinct().collect(Collectors.toList())
			);
		} else if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
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
			String essenceType = higherDepth(itemJson, "type", "None").toLowerCase();
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
			eb.setThumbnail(getItemThumbnail(itemId));
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
}
