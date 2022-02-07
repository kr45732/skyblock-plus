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

import static com.skyblockplus.utils.Utils.getLowestBinJson;

import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class RecipeSlashCommand extends SlashCommand {

	public RecipeSlashCommand() {
		this.name = "recipe";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		event.embed(RecipeCommand.getRecipe(event.getOptionStr("item")));
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get the recipe of an item").addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				RecipeCommand.allRecipeIds.stream().map(Utils::idToName).collect(Collectors.toList())
			);
		}
	}
}
