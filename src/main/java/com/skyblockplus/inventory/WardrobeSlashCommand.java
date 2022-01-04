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

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.List;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class WardrobeSlashCommand extends SlashCommand {

	public WardrobeSlashCommand() {
		this.name = "wardrobe";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "list" -> event.paginate(
				WardrobeCommand.getPlayerWardrobeList(event.player, event.getOptionStr("profile"), new PaginatorEvent(event))
			);
			case "emoji" -> {
				Player player = event.getOptionStr("profile") == null
					? new Player(event.player)
					: new Player(event.player, event.getOptionStr("profile"));
				if (!player.isValid()) {
					event.embed(player.getFailEmbed());
					return;
				}
				List<String[]> wardrobePages = player.getWardrobe();
				if (wardrobePages != null) {
					event.getHook().deleteOriginal().queue();
					if (player.invMissing.length() > 0) {
						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build())
							.queue();
					}

					new InventoryPaginator(wardrobePages, event.getChannel(), event.getUser());
				} else {
					event.embed(invalidEmbed("Inventory API disabled"));
				}
			}
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main wardrobe bag command")
			.addSubcommands(
				new SubcommandData("list", "Get a list of a player's wardrobe with lore")
					.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
					.addOption(OptionType.STRING, "profile", "Profile name"),
				new SubcommandData("emoji", "Get a player's wardrobe represented in emojis")
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
}
