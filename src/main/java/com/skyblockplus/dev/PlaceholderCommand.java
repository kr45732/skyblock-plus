/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.dev;

import static com.skyblockplus.utils.utils.StringUtils.roundAndFormat;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.utils.Utils.defaultPerms;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(event) {
			@Override
			protected void execute() {
				String total = roundAndFormat(Runtime.getRuntime().totalMemory() / 1000000.0) + " MB";
				String free = roundAndFormat(Runtime.getRuntime().freeMemory() / 1000000.0) + " MB";
				String used = roundAndFormat((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) + " MB";
				if (args.length >= 2 && args[1].equals("gc")) {
					System.gc();
					total += " ➜ " + roundAndFormat(Runtime.getRuntime().totalMemory() / 1000000.0) + " MB";
					free += " ➜ " + roundAndFormat(Runtime.getRuntime().freeMemory() / 1000000.0) + " MB";
					used +=
					" ➜ " + roundAndFormat((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) + " MB";
				}

				getChannel()
					.sendMessageEmbeds(
						defaultEmbed("Debug")
							.addField("Total", total, false)
							.addField("Free", free, false)
							.addField("Used", used, false)
							.addField("Max", roundAndFormat(Runtime.getRuntime().maxMemory() / 1000000.0) + " MB", false)
							.build()
					)
					.queue();
			}
		};
	}
}
