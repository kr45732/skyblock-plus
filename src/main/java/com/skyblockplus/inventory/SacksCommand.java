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

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.Comparator;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class SacksCommand extends Command {

	public SacksCommand() {
		this.name = "sacks";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerSacks(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<String, Integer> sacksMap = player.getPlayerSacks();
			if (sacksMap == null) {
				return invalidEmbed("Inventory API disabled");
			}

			CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(20);

			JsonElement bazaarPrices = higherDepth(getBazaarJson(), "products");

			final double[] total = { 0 };
			sacksMap
				.entrySet()
				.stream()
				.sorted(
					Comparator.comparingDouble(entry ->
						-higherDepth(bazaarPrices, entry.getKey() + ".sell_summary.[0].pricePerUnit", 0.0) * entry.getValue()
					)
				)
				.forEach(currentSack -> {
					double sackPrice =
						higherDepth(bazaarPrices, currentSack.getKey() + ".sell_summary.[0].pricePerUnit", 0.0) * currentSack.getValue();
					paginateBuilder.addItems(
						"**" +
						convertSkyblockIdName(currentSack.getKey()) +
						":** " +
						formatNumber(currentSack.getValue()) +
						" ➜ " +
						simplifyNumber(sackPrice)
					);
					total[0] += sackPrice;
				});

			paginateBuilder.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(player.getUsername())
					.setEveryPageThumbnail(player.getThumbnailUrl())
					.setEveryPageTitleUrl(player.skyblockStatsLink())
					.setEveryPageText("**Total value:** " + roundAndFormat(total[0]) + "\n")
			);
			event.paginate(paginateBuilder);
			return null;
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerSacks(username, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
