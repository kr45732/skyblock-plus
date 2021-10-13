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
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public class TalismanBagCommand extends Command {

	private String missingEmoji;

	public TalismanBagCommand() {
		this.name = "talisman";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "talismans" };
		this.botPermissions = defaultPerms();
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

				if (slotNumber != -1 && (args.length == 3 || args.length == 2 || args.length == 1)) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerTalismansList(username, args.length == 3 ? args[2] : null, slotNumber, new PaginatorEvent(event)));
					return;
				} else if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					List<String[]> playerEnderChest = getPlayerTalismansEmoji(username, args.length == 3 ? args[2] : null);
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

	private List<String[]> getPlayerTalismansEmoji(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> talismanBagPages = player.getTalismanBag();

			if (talismanBagPages != null) {
				this.missingEmoji = player.invMissing;
				return talismanBagPages;
			}
		}
		return null;
	}

	public static EmbedBuilder getPlayerTalismansList(String username, String profileName, int slotNum, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> talismanBagMap = player.getTalismanBagMap();
			if (talismanBagMap != null) {
				List<String> pageTitles = new ArrayList<>();
				List<String> pageThumbnails = new ArrayList<>();

				CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(1);

				for (Map.Entry<Integer, InvItem> currentTalisman : talismanBagMap.entrySet()) {
					InvItem currentTalismanStruct = currentTalisman.getValue();

					if (currentTalismanStruct == null) {
						pageTitles.add("Empty");
						pageThumbnails.add(null);
						paginateBuilder.addItems("**Slot:** " + (currentTalisman.getKey() + 1));
					} else {
						pageTitles.add(currentTalismanStruct.getName() + " x" + currentTalismanStruct.getCount());
						pageThumbnails.add("https://sky.shiiyu.moe/item.gif/" + currentTalismanStruct.getId());
						String itemString = "";
						itemString += "**Slot:** " + (currentTalisman.getKey() + 1);
						itemString += "\n\n**Lore:**\n" + currentTalismanStruct.getLore();
						if (currentTalismanStruct.isRecombobulated()) {
							itemString += "\n(Recombobulated)";
						}

						itemString += "\n\n**Item Creation:** " + currentTalismanStruct.getCreationTimestamp();
						paginateBuilder.addItems(itemString);
					}
				}
				paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles).setThumbnails(pageThumbnails));

				event.paginate(paginateBuilder, slotNum);
				return null;
			}
		}
		return player.getFailEmbed();
	}
}
