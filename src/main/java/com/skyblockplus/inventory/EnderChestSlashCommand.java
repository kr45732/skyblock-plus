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

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

import java.util.List;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

public class EnderChestSlashCommand extends SlashCommand {

	public EnderChestSlashCommand() {
		this.name = "enderchest";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		Player player = event.getOptionStr("profile") == null
			? new Player(event.player)
			: new Player(event.player, event.getOptionStr("profile"));
		if (!player.isValid()) {
			event.embed(invalidEmbed(player.getFailCause()));
			return;
		}

		List<String[]> playerEnderChest = player.getEnderChest();
		if (playerEnderChest != null) {
			event.getHook().deleteOriginal().queue();
			if (player.invMissing.length() > 0) {
				event.getChannel().sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build()).queue();
			}

			new InventoryPaginator(playerEnderChest, event.getChannel(), event.getUser());
		} else {
			event.embed(invalidEmbed("Inventory API disabled"));
		}
	}
}
