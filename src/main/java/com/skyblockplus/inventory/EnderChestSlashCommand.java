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

import static com.skyblockplus.utils.Utils.invalidEmbed;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class EnderChestSlashCommand extends SlashCommand {

	public EnderChestSlashCommand() {
		this.name = "enderchest";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "list" -> event.paginate(
				getPlayerEnderChestList(event.player, event.getOptionStr("profile"), event.getOptionInt("slot", 0), event)
			);
			case "emoji" -> event.paginate(getPlayerEnderChest(event.player, event.getOptionStr("profile"), event));
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main ender chest command")
			.addSubcommands(
				new SubcommandData("list", "Get a list of the player's ender chest with lore")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "profile", "Profile name")
					.addOption(OptionType.INTEGER, "slot", "Slot number"),
				new SubcommandData("emoji", "Get a player's ender chest represented in emojis")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "profile", "Profile name")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getPlayerEnderChest(String username, String profileName, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			List<String[]> enderChestPages = player.getEnderChest();
			if (enderChestPages != null) {
				new InventoryEmojiPaginator(enderChestPages, "Ender Chest", player, event);
				return null;
			}
			return invalidEmbed(player.getUsernameFixed() + "'s inventory API is disabled");
		}
		return player.getFailEmbed();
	}

	public static EmbedBuilder getPlayerEnderChestList(String username, String profileName, int slotNum, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			Map<Integer, InvItem> echestMap = player.getEnderChestMap();
			if (echestMap != null) {
				new InventoryListPaginator(player, echestMap, slotNum, event);
				return null;
			}
		}
		return player.getFailEmbed();
	}
}
