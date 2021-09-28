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

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

public class CommunityUpgradesCommand extends Command {

	public CommunityUpgradesCommand() {
		this.name = "upgrades";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
		this.ownerCommand = true;
	}

	public static EmbedBuilder getCommunityUpgrades(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			// TODO: finish this & remove ownerOnly when done
			EmbedBuilder eb = player.defaultPlayerEmbed();

			JsonElement communityUpgrades = higherDepth(player.getOuterProfileJson(), "community_upgrades");
			eb.setDescription("**Currently upgrading:** " + higherDepth(communityUpgrades, "currently_upgrading", "none"));
			//			JsonArray allUpgrades = higherDepth(communityUpgrades, "upgrade_states").getAsJsonArray();
			//			for (JsonElement upgrade : allUpgrades) {
			//
			//			}

			return eb;
		}
		return invalidEmbed(player.getFailCause());
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

					embed(getCommunityUpgrades(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
