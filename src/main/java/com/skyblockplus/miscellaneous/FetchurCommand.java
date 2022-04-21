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

import static com.skyblockplus.utils.Constants.FETCHUR_ITEMS;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.time.LocalDate;
import java.time.ZoneId;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class FetchurCommand extends Command {

	public FetchurCommand() {
		this.name = "fetchur";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getFetchurItem() {
		int index = LocalDate.now(ZoneId.of("America/New_York")).getDayOfMonth() % FETCHUR_ITEMS.size() - 1;
		if (index == -1) {
			index = FETCHUR_ITEMS.size() - 1;
		}

		String[] fetchurItem = FETCHUR_ITEMS.get(index).split("\\|");
		return defaultEmbed("Fetchur item")
			.setDescription(fetchurItem[0])
			.setThumbnail("https://sky.shiiyu.moe/item.gif/" + fetchurItem[1]);
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(getFetchurItem());
			}
		}
			.queue();
	}
}
