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

package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.BITS_ITEM_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class BitsSlashCommand extends SlashCommand {

	public BitsSlashCommand() {
		this.name = "bits";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getBitPrices(event.getOptionStr("item")));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get the price of an item from the bits shop")
			.addOption(OptionType.STRING, "item", "Item name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				BITS_ITEM_NAMES.stream().map(Utils::idToName).distinct().collect(Collectors.toList())
			);
		}
	}

	public static EmbedBuilder getBitPrices(String itemName) {
		String closestMatch = getClosestMatchFromIds(nameToId(itemName), BITS_ITEM_NAMES);
		if (closestMatch != null) {
			return defaultEmbed("Bits Price")
				.addField(idToName(closestMatch), formatNumber(higherDepth(getBitsJson(), closestMatch, 0L)), false);
		}

		return defaultEmbed("No bit price found for " + capitalizeString(itemName));
	}
}
