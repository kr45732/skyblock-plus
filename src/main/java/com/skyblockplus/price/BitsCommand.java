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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

public class BitsCommand extends Command {

	public BitsCommand() {
		this.name = "bits";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "bit" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getBitPrices(String itemName) {
		String closestMatch = getClosestMatchFromIds(nameToId(itemName), BITS_ITEM_NAMES);
		if (closestMatch != null) {
			return defaultEmbed("Bits Price")
				.addField(idToName(closestMatch), formatNumber(higherDepth(getBitsJson(), closestMatch, 0L)), false);
		}

		return defaultEmbed("No bit price found for " + capitalizeString(itemName));
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();
				setArgs(2);

				if (args.length == 2) {
					embed(getBitPrices(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
