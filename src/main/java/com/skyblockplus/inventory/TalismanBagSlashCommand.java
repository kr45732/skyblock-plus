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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.invalidEmbed;

public class TalismanBagSlashCommand extends SlashCommand {

	public TalismanBagSlashCommand() {
		this.name = "talisman";
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
					TalismanBagCommand.getPlayerTalismansList(
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
				Player player = event.getOptionStr("profile") == null
					? new Player(event.player)
					: new Player(event.player, event.getOptionStr("profile"));
				if (!player.isValid()) {
					event.embed(invalidEmbed(player.getFailCause()));
					return;
				}

				List<String[]> talismanBagPages = player.getTalismanBag();
				if (talismanBagPages != null) {
					event.getHook().deleteOriginal().queue();
					if (player.invMissing.length() > 0) {
						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Missing emojis").setDescription(player.invMissing).build())
							.queue();
					}

					new InventoryPaginator(talismanBagPages, event.getChannel(), event.getUser());
				} else {
					event.embed(invalidEmbed("Inventory API disabled"));
				}
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}

	@Override
	public CommandData getCommandData() {
		return new CommandData("talisman", "Main talisman bag command")
				.addSubcommands(
						new SubcommandData("list", "Get a list of the player's talisman bag with lore")
								.addOption(OptionType.STRING, "player", "Player username or mention")
								.addOption(OptionType.STRING, "profile", "Profile name")
								.addOption(OptionType.INTEGER, "slot", "Slot number"),
						new SubcommandData("emoji", "Get a player's talisman bag represented in emojis")
								.addOption(OptionType.STRING, "player", "Player username or mention")
								.addOption(OptionType.STRING, "profile", "Profile name")
				);
	}
}
