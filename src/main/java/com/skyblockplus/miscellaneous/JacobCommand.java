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

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.features.jacob.JacobContest;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;

public class JacobCommand extends Command {

	public JacobCommand() {
		this.name = "jacob";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getJacobEmbed(PaginatorEvent event) {
		JacobData data = JacobHandler.getJacobData();

		if (data.getContests().isEmpty()) {
			return defaultEmbed("Jacob Contests")
				.setDescription("**Year:** " + data.getYear())
				.addField("Contests", "None left for this year!", false);
		}

		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS);
		for (JacobContest contest : data.getContests()) {
			extras.addEmbedField(
				"Contest",
				"**In:** <t:" + contest.getTimeInstant().getEpochSecond() + ":R>\n**Crops:**\n" + contest.getCropsFormatted(),
				false
			);
		}
		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(5);
		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				paginate(getJacobEmbed(new PaginatorEvent(event)));
			}
		}
			.queue();
	}
}
