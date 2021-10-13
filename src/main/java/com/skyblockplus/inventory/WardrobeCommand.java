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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.ArmorStruct;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class WardrobeCommand extends Command {

	private String missingEmoji;

	public WardrobeCommand() {
		this.name = "wardrobe";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 4 || args.length == 3 || args.length == 2) && args[1].equals("list")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerWardrobeList(username, args.length == 4 ? args[3] : null, new PaginatorEvent(event)));
					return;
				} else if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					List<String[]> playerEnderChest = getPlayerWardrobe(username, args.length == 3 ? args[2] : null);
					if (playerEnderChest != null) {
						ebMessage.delete().queue();
						if (missingEmoji.length() > 0) {
							ebMessage
								.getChannel()
								.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(missingEmoji).build())
								.queue();
						}

						new InventoryPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor());
					} else {
						embed(invalidEmbed("Unable to fetch player data"));
					}
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private List<String[]> getPlayerWardrobe(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> talismanBagPages = player.getWardrobe();

			if (talismanBagPages != null) {
				this.missingEmoji = player.invMissing;
				return talismanBagPages;
			}
		}
		return null;
	}

	public static EmbedBuilder getPlayerWardrobeList(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, ArmorStruct> armorStructMap = player.getWardrobeList();
			if (armorStructMap != null) {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(4);

				for (Map.Entry<Integer, ArmorStruct> currentArmour : armorStructMap.entrySet()) {
					paginateBuilder.addItems(
						"**__Slot " +
						(currentArmour.getKey() + 1) +
						"__**\n" +
						currentArmour.getValue().getHelmet() +
						"\n" +
						currentArmour.getValue().getChestplate() +
						"\n" +
						currentArmour.getValue().getLeggings() +
						"\n" +
						currentArmour.getValue().getBoots() +
						"\n"
					);
				}
				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle(player.getUsername())
						.setEveryPageThumbnail(player.getThumbnailUrl())
						.setEveryPageTitleUrl(player.skyblockStatsLink())
				);
				event.paginate(paginateBuilder);
				return null;
			}
		}
		return player.getFailEmbed();
	}
}
