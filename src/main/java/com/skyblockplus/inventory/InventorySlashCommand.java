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

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class InventorySlashCommand extends SlashCommand {

	public InventorySlashCommand() {
		this.name = "inventory";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "list":
				event.paginate(
					InventoryCommand.getPlayerInventoryList(
						event.player,
						event.getOptionStr("profile"),
						event.getOptionInt("slot", 0),
						event.getUser(),
						null,
						event.getHook()
					)
				);
				break;
			case "emoji":
				String[] playerInventory = InventoryCommand.getPlayerInventory(event.player, event.getOptionStr("profile"));
				if (playerInventory != null) {
					event.getHook().deleteOriginal().queue();
					event.getChannel().sendMessage(playerInventory[0]).complete();
					event.getChannel().sendMessage(playerInventory[1]).queue();
					if (playerInventory[2].length() > 0) {
						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(playerInventory[2]).build())
							.queue();
					}
				} else {
					event.embed(invalidEmbed("Inventory API disabled"));
				}
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
