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

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.skyblockplus.utils.Utils.*;

public class CheckApiCommand extends Command {

	public CheckApiCommand() {
		this.name = "check-api";
		this.aliases = new String[]{"api", "checkapi"};
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCheckApi(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			boolean invEnabled =  player.getInventoryMap() != null;
			boolean bankEnabled = player.getBankBalance() != -1;
			boolean collectionsEnabled = false;
			try{collectionsEnabled = higherDepth(player.profileJson(), "collection").getAsJsonObject() != null;}catch(Exception ignored){}
			boolean vaultEnabled = player.getPersonalVaultMap() != null;
			boolean skillsEnabled = player.getSkillAverage("", -1) != -1;

			eb.setDescription(
					"**All APIs Enabled:** " + (invEnabled && bankEnabled && collectionsEnabled && vaultEnabled && skillsEnabled)
					+ "\n" + (invEnabled ? client.getSuccess() : client.getError()) + " Inventory API"
							+ "\n" + (bankEnabled ? client.getSuccess() : client.getError()) + " Bank API"
							+ "\n" + (collectionsEnabled ? client.getSuccess() : client.getError()) + " Collections API"
							+ "\n" + (skillsEnabled ? client.getSuccess() : client.getError()) + " Skills API"
							+ "\n" + (vaultEnabled ? client.getSuccess() : client.getError()) + " Personal Vault API"
			);

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

					embed(getCheckApi(player, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
