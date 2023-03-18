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
import static com.skyblockplus.utils.utils.Utils.errorEmbed;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class ArmorSlashCommand extends SlashCommand {

	public ArmorSlashCommand() {
		this.name = "armor";
	}

	@Override
	protected SlashCommandData getCommandData() {
		return Commands.slash(name, "Main armor command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class ListSubcommand extends Subcommand {

		public ListSubcommand() {
			this.name = "list";
		}

		public static EmbedBuilder getPlayerEquippedArmor(String username, String profileName, int slot, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				Map<Integer, InvItem> inventoryMap = player.getArmorMap();
				if (inventoryMap != null) {
					Map<Integer, InvItem> equipmentMap = player.getEquipmentMap();
					if (equipmentMap != null) {
						for (Map.Entry<Integer, InvItem> entry : equipmentMap.entrySet()) {
							inventoryMap.put(entry.getKey() + 4, entry.getValue());
						}
					}
					new InventoryListPaginator(player, inventoryMap, slot, event);
					return null;
				}
			}
			return player.getErrorEmbed();
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerEquippedArmor(event.player, event.getOptionStr("profile"), event.getOptionInt("slot", 0), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("list", "Get a list of the player's equipped armor and equipment with lore")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption)
				.addOption(OptionType.INTEGER, "slot", "Slot number");
		}
	}

	public static class EmojiSubcommand extends Subcommand {

		public EmojiSubcommand() {
			this.name = "emoji";
		}

		public static EmbedBuilder getPlayerArmor(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				Map<Integer, InvItem> playerArmor = player.getArmorMap();
				Map<Integer, InvItem> playerEquipment = player.getEquipmentMap();
				if (playerArmor != null || playerEquipment != null) {
					StringBuilder out = new StringBuilder();
					for (int i = 0; i < 8; i++) {
						if (i % 2 == 0) {
							try {
								out.append(getEmoji(playerEquipment.get(i / 2).getId(), "❓"));
							} catch (Exception e) {
								out.append(getEmoji("EMPTY"));
							}
						} else {
							try {
								out.append(getEmoji(playerArmor.get((i - 1) / 2).getId(), "❓")).append("\n");
							} catch (Exception e) {
								out.append(getEmoji("EMPTY")).append("\n");
							}
						}
					}

					event
						.getHook()
						.editOriginal(out.toString())
						.setEmbeds()
						.setActionRow(Button.link(player.skyblockStatsLink(), player.getUsername() + "'s Armor & Equipment"))
						.queue();
					return null;
				}
				return errorEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
			}
			return player.getErrorEmbed();
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerArmor(event.player, event.getOptionStr("profile"), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("emoji", "Get a player's equipped armor and equipment represented in emojis")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}
	}
}
