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

import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;

public class UuidCommand extends Command {

	public UuidCommand() {
		this.name = "uuid";
		this.aliases = new String[] { "username" };
		this.cooldown = globalCooldown + 1;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getUuidPlayer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.getFailCause());
		}

		return defaultEmbed(usernameUuid.getUsername(), "https://plancke.io/hypixel/player/stats/" + usernameUuid.getUsername())
			.setDescription("**Username:** " + usernameUuid.getUsername() + "\n**Uuid:** " + usernameUuid.getUuid())
			.setThumbnail(usernameUuid.getAvatarlUrl());
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					embed(getUuidPlayer(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
