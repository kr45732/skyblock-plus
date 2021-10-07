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
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class ArmorCommand extends Command {

	public ArmorCommand() {
		this.name = "armor";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
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

					paginate(getPlayerEquippedArmor(username, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public static EmbedBuilder getPlayerEquippedArmor(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> inventoryMap = player.getInventoryArmorMap();
			if (inventoryMap != null) {
				List<String> pageTitles = new ArrayList<>();
				List<String> pageThumbnails = new ArrayList<>();

				CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(1);

				for (Map.Entry<Integer, InvItem> currentInvSlot : inventoryMap.entrySet()) {
					InvItem currentInvStruct = currentInvSlot.getValue();

					if (currentInvStruct == null) {
						pageTitles.add("Empty");
						pageThumbnails.add(null);

						String slotName = "";
						switch ((currentInvSlot.getKey())) {
							case 4:
								slotName = "Boots";
								break;
							case 3:
								slotName = "Leggings";
								break;
							case 2:
								slotName = "Chestplate";
								break;
							case 1:
								slotName = "Helmet";
								break;
						}

						paginateBuilder.addItems("**Slot:** " + slotName);
					} else {
						pageTitles.add(currentInvStruct.getName() + " x" + currentInvStruct.getCount());
						pageThumbnails.add("https://sky.shiiyu.moe/item.gif/" + currentInvStruct.getId());
						String itemString = "";

						String slotName = "";
						switch ((currentInvSlot.getKey())) {
							case 4:
								slotName = "Boots";
								break;
							case 3:
								slotName = "Leggings";
								break;
							case 2:
								slotName = "Chestplate";
								break;
							case 1:
								slotName = "Helmet";
								break;
						}

						itemString += "**Slot:** " + slotName;
						itemString += "\n\n**Lore:**\n" + currentInvStruct.getLore();
						if (currentInvStruct.isRecombobulated()) {
							itemString += "\n(Recombobulated)";
						}

						itemString += "\n\n**Item Creation:** " + currentInvStruct.getCreationTimestamp();
						paginateBuilder.addItems(itemString);
					}
				}
				paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles).setThumbnails(pageThumbnails));

				event.paginate(paginateBuilder);
				return null;
			}
		}
		return invalidEmbed(player.getFailCause());
	}
}
