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

package com.skyblockplus.link;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.PaginatorEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class UnlinkCommand extends Command {

	public UnlinkCommand() {
		this.name = "unlink";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
		this.aliases = new String[] { "unverify" };
	}

	public static EmbedBuilder unlinkAccount(PaginatorEvent event) {
		JsonElement verifySettings = database.getVerifySettings(event.getGuild().getId());
		try {
			for (JsonElement verifyRole : higherDepth(verifySettings, "verifiedRoles").getAsJsonArray()) {
				try {
					event
						.getGuild()
						.removeRoleFromMember(event.getMember().getId(), event.getGuild().getRoleById(verifyRole.getAsString()))
						.complete();
				} catch (Exception e) {
					System.out.println(verifyRole);
					e.printStackTrace();
				}
			}
		} catch (Exception ignored) {}
		database.deleteLinkedUserByDiscordId(event.getUser().getId());
		return defaultEmbed("Success").setDescription("You were unlinked");
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(unlinkAccount(new PaginatorEvent(event)));
			}
		}
			.queue();
	}
}
