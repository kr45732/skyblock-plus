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

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;

public class PetsCommand extends Command {

	public PetsCommand() {
		this.name = "pets";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerPets(username, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public static EmbedBuilder getPlayerPets(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(3).setItemsPerPage(15);

			JsonArray playerPets = player.getPets();
			for (JsonElement pet : playerPets) {
				String petItem = null;
				try {
					petItem = idToName(higherDepth(pet, "heldItem").getAsString()).toLowerCase();
				} catch (Exception ignored) {}

				paginateBuilder.addItems(
					"**" +
					capitalizeString(higherDepth(pet, "type").getAsString().toLowerCase().replace("_", " ")) +
					" (" +
					petLevelFromXp(higherDepth(pet, "exp", 0L), higherDepth(pet, "tier").getAsString()) +
					")**" +
					"\nTier: " +
					higherDepth(pet, "tier").getAsString().toLowerCase() +
					(petItem != null ? "\nItem: " + petItem : "")
				);
			}
			paginateBuilder.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(player.getUsername())
					.setEveryPageThumbnail(player.getThumbnailUrl())
					.setEveryPageTitleUrl(player.skyblockStatsLink())
			);
			event.paginate(paginateBuilder);
			return null;
		}
		return player.getFailEmbed();
	}
}
