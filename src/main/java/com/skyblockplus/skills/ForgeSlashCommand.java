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

import static com.skyblockplus.utils.Constants.FORGE_TIMES;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.roundAndFormat;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.time.Instant;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class ForgeSlashCommand extends SlashCommand {

	public ForgeSlashCommand() {
		this.name = "forge";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getForge(event.player, event.getOptionStr("profile")));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's forge items")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getForge(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			JsonElement forgeItems = higherDepth(player.profileJson(), "forge.forge_processes.forge_1");
			if (forgeItems == null) {
				return defaultEmbed(player.getUsernameFixed() + " has no items in the forge");
			}
			int forgeTime = higherDepth(player.profileJson(), "mining_core.nodes.forge_time", 0);
			double bonus;
			if (forgeTime <= 1) {
				bonus = 1;
			} else if (forgeTime <= 10) {
				bonus = 0.85;
			} else if (forgeTime <= 19) {
				bonus = 0.805;
			} else {
				bonus = 0.7;
			}
			for (JsonElement forgeItem : forgeItems.getAsJsonObject().entrySet().stream().map(Map.Entry::getValue).toList()) {
				String itemId = higherDepth(forgeItem, "id").getAsString();
				itemId = itemId.equals("PET") ? "AMMONITE;4" : itemId;
				eb.addField(
					getEmoji(itemId) + " " + idToName(itemId),
					"Slot: " +
					higherDepth(forgeItem, "slot", 0) +
					"\nEnd: <t:" +
					Instant
						.ofEpochMilli(higherDepth(forgeItem, "startTime").getAsLong())
						.plusMillis((long) (FORGE_TIMES.get(itemId) * bonus))
						.getEpochSecond() +
					":R>",
					false
				);
			}
			if (eb.getFields().size() == 0) {
				return defaultEmbed(player.getUsernameFixed() + " has no items in the forge");
			}
			if (bonus != 1) {
				eb.setDescription("**Quick Forge:** " + roundAndFormat(100 - bonus * 100.0) + "% less forge time");
			}
			return eb;
		}
		return player.getFailEmbed();
	}
}
