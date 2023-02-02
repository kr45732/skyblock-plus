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

package com.skyblockplus.general;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class UnlinkSlashCommand extends SlashCommand {

	public UnlinkSlashCommand() {
		this.name = "unlink";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(unlinkAccount(event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Unlink your account from this bot");
	}

	public static EmbedBuilder unlinkAccount(SlashCommandEvent event) {
		database.deleteByDiscord(event.getUser().getId());

		JsonElement verifySettings = database.getVerifySettings(event.getGuild().getId());
		if (verifySettings != null && !verifySettings.isJsonNull()) {
			List<Role> toAdd = new ArrayList<>();
			try {
				toAdd.add(event.getGuild().getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
			} catch (Exception ignored) {}
			event
				.getGuild()
				.modifyMemberRoles(
					event.getMember(),
					toAdd,
					streamJsonArray(higherDepth(verifySettings, "verifiedRoles"))
						.map(r -> {
							try {
								return event.getGuild().getRoleById(r.getAsString());
							} catch (Exception e) {
								return null;
							}
						})
						.filter(Objects::nonNull)
						.toList()
				)
				.queue();
		}

		return defaultEmbed("Success").setDescription("You were unlinked");
	}
}
