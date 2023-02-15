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
import static com.skyblockplus.utils.Utils.invalidEmbed;

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
public class InventorySlashCommand extends SlashCommand {

	public InventorySlashCommand() {
		this.name = "inventory";
	}

	public static class ListSubcommand extends Subcommand {

		public ListSubcommand() {
			this.name = "list";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerInventoryList(event.player, event.getOptionStr("profile"), event.getOptionInt("slot", 0), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("list", "Get a list of the player's inventory with lore")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption)
				.addOption(OptionType.INTEGER, "slot", "Slot number");
		}

		public static EmbedBuilder getPlayerInventoryList(String username, String profileName, int slotNum, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				Map<Integer, InvItem> inventoryMap = player.getInventoryMap(true);
				if (inventoryMap != null) {
					new InventoryListPaginator(player, inventoryMap, slotNum, event);
					return null;
				}
			}
			return player.getFailEmbed();
		}
	}

	public static class EmojiSubcommand extends Subcommand {

		public EmojiSubcommand() {
			this.name = "emoji";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			if (event.invalidPlayerOption()) {
				return;
			}

			event.paginate(getPlayerInventory(event.player, event.getOptionStr("profile"), event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("emoji", "Get a player's inventory represented in emojis")
				.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
				.addOptions(profilesCommandOption);
		}

		public static EmbedBuilder getPlayerInventory(String username, String profileName, SlashCommandEvent event) {
			Player.Profile player = Player.create(username, profileName);
			if (player.isValid()) {
				String[] playerInventory = player.getInventory();
				if (playerInventory != null) {
					event.getHook().editOriginal(playerInventory[0]).setEmbeds().queue();
					event
						.getChannel()
						.sendMessage(playerInventory[1])
						.setActionRow(Button.link(player.skyblockStatsLink(), player.getUsername() + "'s Inventory"))
						.queue();
					return null;
				}
				return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
			}
			return player.getFailEmbed();
		}
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main inventory command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
