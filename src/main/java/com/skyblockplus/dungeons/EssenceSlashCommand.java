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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.utils.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.springframework.stereotype.Component;

@Component
public class EssenceSlashCommand extends SlashCommand {

	public EssenceSlashCommand() {
		this.name = "essence";
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get essence upgrade information for an item");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				ESSENCE_ITEM_NAMES.stream().map(StringUtils::idToName).distinct().collect(Collectors.toCollection(ArrayList::new))
			);
		} else if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class UpgradeSubcommand extends Subcommand {

		public UpgradeSubcommand() {
			this.name = "upgrade";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			new EssenceHandler(nameToId(event.getOptionStr("item")), event);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Interactive message to find the essence amount to upgrade an item")
				.addOption(OptionType.STRING, "item", "Item name", true, true);
		}
	}

	public static class InformationSubcommand extends Subcommand {

		public InformationSubcommand() {
			this.name = "information";
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
				for (Map.Entry<String, JsonElement> level : itemJson.getAsJsonObject().entrySet()) {
					switch (level.getKey()) {
						case "items" -> {}
						case "type" -> eb.setDescription("**Essence Type:** " + capitalizeString(essenceType));
						case "dungeonize" -> eb.appendDescription(
							"\n➜ **Dungeonize:** " + ESSENCE_EMOJI_MAP.get(essenceType) + " x" + level.getValue().getAsString()
						);
						default -> eb.appendDescription(
							"\n➜ **" +
							level.getKey() +
							" Star" +
							(level.getKey().equals("1") ? "" : "s") +
							":** " +
							ESSENCE_EMOJI_MAP.get(essenceType) +
							" x" +
							level.getValue().getAsString() +
							(
								higherDepth(itemJson, "items." + level) != null
									? streamJsonArray(higherDepth(itemJson, "items." + level.getKey()))
										.map(i -> {
											String[] split = i.getAsString().split(":");
											return getEmoji(split[0], idToName(split[0])) + " x" + split[1];
										})
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

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(getEssenceInformation(event.getOptionStr("item")));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get the amount of essence to upgrade an item for each level")
				.addOption(OptionType.STRING, "item", "Item name", true, true);
		}
	}

	public static class PlayerSubcommand extends Subcommand {

		public PlayerSubcommand() {
			this.name = "player";
		}

		public static EmbedBuilder getPlayerEssence(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();

				StringBuilder amountsStr = new StringBuilder();
				for (String essence : essenceTypes) {
					amountsStr
						.append(ESSENCE_EMOJI_MAP.get(essence))
						.append("** ")
						.append(capitalizeString(essence))
						.append(" Essence:** ")
						.append(
							formatNumber(higherDepth(player.profileJson(), "currencies.essence." + essence.toUpperCase() + ".current", 0))
						)
						.append("\n");
				}
				pages.put(SelectOption.of("Amounts", "amounts"), player.defaultPlayerEmbed().setDescription(amountsStr));

				for (Map.Entry<String, JsonElement> essenceShop : getEssenceShopsJson().entrySet()) {
					StringBuilder ebStr = new StringBuilder();

					for (Map.Entry<String, JsonElement> essenceUpgrade : essenceShop.getValue().getAsJsonObject().entrySet()) {
						int level = higherDepth(player.profileJson(), "player_data.perks." + essenceUpgrade.getKey(), 0);
						JsonArray tiers = higherDepth(essenceUpgrade.getValue(), "costs").getAsJsonArray();

						ebStr
							.append("\n")
							.append(ESSENCE_EMOJI_MAP.getOrDefault(essenceUpgrade.getKey(), ""))
							.append("** ")
							.append(higherDepth(essenceUpgrade.getValue(), "name").getAsString())
							.append(":** ")
							.append(level)
							.append("/")
							.append(tiers.size());
						if (level < tiers.size()) {
							ebStr.append(" (").append(formatNumber(tiers.get(level).getAsInt())).append(" for next)");
						}
					}

					String essenceType = capitalizeString(essenceShop.getKey().split("_")[1]);
					pages.put(
						SelectOption.of(essenceType + " Essence Shop", essenceType),
						player.defaultPlayerEmbed(" | " + essenceType + " Essence Shop").setDescription(ebStr.toString())
					);
				}

				new SelectMenuPaginator("amounts", new PaginatorExtras().setSelectPages(pages), event);

				return null;
			}
			return player.getErrorEmbed();
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}
			event.paginate(getPlayerEssence(event.player, event.getOptionStr("profile"), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get the amount of each essence a player has")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}
}
