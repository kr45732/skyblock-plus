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

package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Constants.ESSENCE_ITEM_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class EssenceSlashCommand extends SlashCommand {

	public EssenceSlashCommand() {
		this.name = "essence";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "upgrade" -> {
				String itemId = nameToId(event.getOptionStr("item"));
				if (higherDepth(getEssenceCostsJson(), itemId) == null) {
					String closestMatch = getClosestMatch(itemId, ESSENCE_ITEM_NAMES);
					itemId = closestMatch != null ? closestMatch : itemId;
				}
				JsonElement itemJson = higherDepth(getEssenceCostsJson(), itemId);
				if (itemJson != null) {
					new EssenceWaiter(itemId, itemJson, event.getHook().retrieveOriginal().complete(), event.getUser());
				} else {
					event.embed(invalidEmbed("Invalid item name"));
				}
			}
			case "information" -> event.embed(EssenceCommand.getEssenceInformation(event.getOptionStr("item")));
			case "player" -> {
				if (event.invalidPlayerOption()) {
					return;
				}
				event.embed(EssenceCommand.getPlayerEssence(event.player, event.getOptionStr("profile")));
			}
			default -> event.embed(event.invalidCommandMessage());
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get essence upgrade information for an item")
			.addSubcommands(
				new SubcommandData("upgrade", "Interactive message to find the essence amount to upgrade an item")
					.addOption(OptionType.STRING, "item", "Item name", true),
				new SubcommandData("information", "Get the amount of essence to upgrade an item for each level")
					.addOption(OptionType.STRING, "item", "Item name", true),
				new SubcommandData("player", "Get the amount of each essence a player has")
					.addOption(OptionType.STRING, "player", "Player username or mention")
					.addOption(OptionType.STRING, "profile", "Profile name")
			);
	}
}
