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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.getReforgeStonesJson;

import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ReforgeStoneSlashCommand extends SlashCommand {

	public ReforgeStoneSlashCommand() {
		this.name = "reforgestone";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.embed(ReforgeStoneCommand.getReforgeStone(event.getOptionStr("item")));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get the reforge stone stats for each rarity")
			.addOption(OptionType.STRING, "item", "Reforge stone name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				getReforgeStonesJson().keySet().stream().map(Utils::idToName).collect(Collectors.toList())
			);
		}
	}
}
