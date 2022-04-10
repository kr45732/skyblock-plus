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
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class WardrobeCommand extends Command {

	public WardrobeCommand() {
		this.name = "wardrobe";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerWardrobeList(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, ArmorStruct> armorStructMap = player.getWardrobeList();
			if (armorStructMap != null) {
				CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator().setItemsPerPage(4);

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
				event.paginate(paginateBuilder);
				return null;
			}
			return invalidEmbed("API disabled");
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 4 || args.length == 3 || args.length == 2) && args[1].equals("list")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					paginate(getPlayerWardrobeList(player, args.length == 4 ? args[2] : null, new PaginatorEvent(event)));
					return;
				} else if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerWardrobe(player, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}

	public static EmbedBuilder getPlayerWardrobe(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> wardrobe = player.getWardrobe();
			if (wardrobe != null) {
				if (player.invMissing.length() > 0) {
					event.getChannel().sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build()).queue();
				}

				new InventoryPaginator(wardrobe, "Wardrobe", player, event);
				return null;
			}
			return invalidEmbed(player.getUsername() + "'s inventory API is disabled");
		}
		return player.getFailEmbed();
	}
}
