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
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.InvItem;

import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class ArmorCommand extends Command {

	public ArmorCommand() {
		this.name = "armor";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerEquippedArmor(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> inventoryMap = player.getArmorMap();
			if (inventoryMap != null) {
				Map<Integer, InvItem> equipmentMap = player.getEquipmentMap();
				if(equipmentMap != null){
					for (Map.Entry<Integer, InvItem> entry : equipmentMap.entrySet()) {
						inventoryMap.put(entry.getKey() + 4, entry.getValue());
					}
				}
				new InventoryListPaginator(player, inventoryMap, 0, event);
				return null;
			}
		}
		return player.getFailEmbed();
	}

	public static EmbedBuilder getPlayerArmor(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> playerArmor = player.getArmorMap();
			Map<Integer, InvItem> playerEquipment = player.getEquipmentMap();
			if (playerArmor != null || playerEquipment != null) {
				StringBuilder out = new StringBuilder();
				for(int i=0; i<8 ; i++){
					if(i%2 == 0){
						try{
							out.append(getEmojiOr(playerEquipment.get(i / 2).getId(), "❓"));
						}catch (Exception e){
							out.append(getEmoji("EMPTY"));
						}
					}else{
						try {
							out.append(getEmojiOr(playerArmor.get((i - 1) / 2).getId(), "❓")).append("\n");
						} catch (Exception e){
							out.append(getEmoji("EMPTY")).append("\n");
						}
					}
				}

				event
						.getChannel()
						.sendMessage(out)
						.setActionRow(Button.link(player.skyblockStatsLink(), player.getUsername() + "'s Armor & Equipment"))
						.queue();
				return null;
			}
			return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length >= 2 && args[1].equals("list")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					paginate(getPlayerEquippedArmor(player, args.length == 4 ? args[3] : null, getPaginatorEvent()));
				} else {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerArmor(player, args.length == 3 ? args[2] : null, getPaginatorEvent()), true);
				}
			}
		}
			.queue();
	}
}
