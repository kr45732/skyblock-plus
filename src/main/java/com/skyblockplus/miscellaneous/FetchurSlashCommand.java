/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.getItemThumbnail;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class FetchurSlashCommand extends SlashCommand {

	public FetchurSlashCommand() {
		this.name = "fetchur";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getFetchurItem());
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get the item that fetchur wants today");
	}

	public static EmbedBuilder getFetchurItem() {
		int index = LocalDate.now(ZoneId.of("America/New_York")).getDayOfMonth() % FETCHUR_ITEMS.size() - 1;
		if (index == -1) {
			index = FETCHUR_ITEMS.size() - 1;
		}

		String[] fetchurItem = FETCHUR_ITEMS.get(index).split("\\|");
		return defaultEmbed("Fetchur item").setDescription(fetchurItem[0]).setThumbnail(getItemThumbnail(fetchurItem[1]));
	}
}
