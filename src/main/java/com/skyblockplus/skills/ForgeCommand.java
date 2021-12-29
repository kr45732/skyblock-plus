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

package com.skyblockplus.skills;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

public class ForgeCommand extends Command {

	public ForgeCommand() {
		this.name = "forge";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getForge(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			JsonObject forgeItems = higherDepth(player.profileJson(), "forge.forge_processes.forge_1").getAsJsonObject();
			for (JsonElement forgeItem : forgeItems.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList())) {
				String itemId = higherDepth(forgeItem, "id").getAsString();
				eb.addField(idToName(itemId),
						"Slot: " + higherDepth(forgeItem, "slot", 0)
						+ "\nEnd: <t:" + Instant.ofEpochMilli(higherDepth(forgeItem, "startTime").getAsLong()).plusMillis(FORGE_TIMES.get(itemId)).getEpochSecond()  + ":R>"
						,false
				);
			}
			if(eb.getFields().size() == 0){
				return defaultEmbed(player.getUsername() + " has no items in the forge");
			}
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

					embed(getForge(username, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
