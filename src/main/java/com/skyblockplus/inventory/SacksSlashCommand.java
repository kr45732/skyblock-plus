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

import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.HypixelUtils.getNpcSellPrice;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.Comparator;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class SacksSlashCommand extends SlashCommand {

	public SacksSlashCommand() {
		this.name = "sacks";
	}

	public static EmbedBuilder getPlayerSacks(String username, String profileName, boolean useNpcPrice, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			Map<String, Integer> sacksMap = player.getPlayerSacks();
			if (sacksMap == null) {
				return errorEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
			}

			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(20);

			JsonElement bazaarPrices = getBazaarJson();

			final double[] total = { 0, 0 };
			sacksMap
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue() > 0)
				.sorted(
					Comparator.comparingDouble(entry -> {
						double npcPrice = -1;
						if (useNpcPrice) {
							npcPrice = getNpcSellPrice(entry.getKey());
						}

						return (
							-(npcPrice != -1 ? npcPrice : higherDepth(bazaarPrices, entry.getKey() + ".sell_summary", 0.0)) *
							entry.getValue()
						);
					})
				)
				.forEach(currentSack -> {
					double npcPrice = -1;
					if (useNpcPrice) {
						npcPrice = getNpcSellPrice(currentSack.getKey());
					}
					double sackPrice =
						(npcPrice != -1 ? npcPrice : higherDepth(bazaarPrices, currentSack.getKey() + ".sell_summary", 0.0)) *
						currentSack.getValue();

					String emoji = higherDepth(
						getEmojiMap(),
						currentSack.getKey().equals("MUSHROOM_COLLECTION") ? "RED_MUSHROOM" : currentSack.getKey(),
						null
					);

					paginateBuilder.addItems(
						(emoji != null ? emoji + " " : "") +
						"**" +
						idToName(currentSack.getKey()) +
						":** " +
						formatNumber(currentSack.getValue()) +
						" ➜ " +
						simplifyNumber(sackPrice)
					);
					total[npcPrice != -1 ? 1 : 0] += sackPrice;
				});

			paginateBuilder
				.getExtras()
				.setEveryPageText(
					"**Total value:** " +
					roundAndFormat(total[0] + total[1]) +
					(useNpcPrice ? " (" + roundAndFormat(total[1]) + " npc + " + roundAndFormat(total[0]) + " bazaar)" : "") +
					"\n"
				);
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

		event.paginate(getPlayerSacks(event.player, event.getOptionStr("profile"), event.getOptionBoolean("npc", false), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's sacks' content bag represented in a list")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption)
			.addOption(OptionType.BOOLEAN, "npc", "Use npc sell prices (bazaar will be used for items that don't have an npc price)");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
