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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;

public class CakesCommand extends Command {

	public CakesCommand() {
		this.name = "cakes";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCakes(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			List<String> missingCakes = new ArrayList<>(
				Arrays.asList(
					"cake_strength",
					"cake_pet_luck",
					"cake_health",
					"cake_walk_speed",
					"cake_magic_find",
					"cake_ferocity",
					"cake_defense",
					"cake_sea_creature_chance",
					"cake_intelligence"
				)
			);
			StringBuilder activeCakes = new StringBuilder();
			if (higherDepth(player.profileJson(), "temp_stat_buffs") != null) {
				for (JsonElement cake : higherDepth(player.profileJson(), "temp_stat_buffs").getAsJsonArray()) {
					Instant expires = Instant.ofEpochMilli(higherDepth(cake, "expire_at").getAsLong());
					if (expires.isAfter(Instant.now())) {
						String cakeName = higherDepth(cake, "key").getAsString();
						activeCakes
							.append("**• ")
							.append(capitalizeString(cakeName.split("cake_")[1].replace("_", " ")))
							.append(" Cake:** expires <t:")
							.append(Instant.ofEpochMilli(higherDepth(cake, "expire_at").getAsLong()).getEpochSecond())
							.append(":R>\n");
						missingCakes.remove(cakeName);
					}
				}
			}
			eb.addField("Active Cakes", activeCakes.length() > 0 ? activeCakes.toString() : "None", false);

			StringBuilder missingCakesStr = new StringBuilder();
			for (String missingCake : missingCakes) {
				missingCakesStr
					.append("**• ")
					.append(capitalizeString(missingCake.split("cake_")[1].replace("_", " ")))
					.append(" Cake**\n");
			}
			eb.addField("Inactive Cakes", missingCakesStr.length() > 0 ? missingCakesStr.toString() : "None", false);

			return eb;
		}
		return player.getFailEmbed();
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

					embed(getCakes(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
