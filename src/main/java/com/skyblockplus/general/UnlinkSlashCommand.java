/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.Utils.database;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import groovy.lang.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class UnlinkSlashCommand extends SlashCommand {

	public UnlinkSlashCommand() {
		this.name = "unlink";
	}

	public static Tuple2<List<Role>, List<Role>> unlinkRoleChanges(
		Guild guild,
		JsonElement serverSettings,
		boolean updateVerify,
		boolean updateRoles,
		boolean updateGuild
	) {
		List<Role> toAdd = new ArrayList<>();
		List<Role> toRemove = new ArrayList<>();

		if (updateVerify) {
			JsonElement verifySettings = higherDepth(serverSettings, "automatedVerify");
			try {
				toAdd.add(guild.getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
			} catch (Exception ignored) {}

			for (JsonElement verifiedRole : higherDepth(verifySettings, "verifiedRoles").getAsJsonArray()) {
				try {
					toRemove.add(guild.getRoleById(verifiedRole.getAsString()));
				} catch (Exception ignored) {}
			}
		}

		if (updateRoles) {
			JsonElement rolesSettings = higherDepth(serverSettings, "automatedRoles");
			if (higherDepth(rolesSettings, "enable", false) && higherDepth(rolesSettings, "roles.[0]") != null) {
				for (JsonElement role : higherDepth(rolesSettings, "roles").getAsJsonArray()) {
					for (JsonElement level : higherDepth(role, "levels").getAsJsonArray()) {
						try {
							toRemove.add(guild.getRoleById(higherDepth(level, "roleId").getAsString()));
						} catch (Exception ignored) {}
					}
				}
			}
		}

		if (updateGuild) {
			for (JsonElement guildSettings : higherDepth(serverSettings, "automatedGuilds").getAsJsonArray()) {
				try {
					toRemove.add(guild.getRoleById(higherDepth(guildSettings, "guildMemberRole").getAsString()));
				} catch (Exception ignored) {}

				for (JsonElement guildRank : higherDepth(guildSettings, "guildRanks").getAsJsonArray()) {
					try {
						toRemove.add(guild.getRoleById(higherDepth(guildRank, "roleId").getAsString()));
					} catch (Exception ignored) {}
				}
			}
		}

		toAdd.removeIf(Objects::isNull);
		toRemove.removeIf(Objects::isNull);

		return new Tuple2<>(toAdd, toRemove);
	}

	public static EmbedBuilder unlinkAccount(Member member, JsonElement serverSettings) {
		if (serverSettings == null) { // This command was run by a user
			database.deleteByDiscord(member.getId());
			serverSettings = database.getServerSettings(member.getGuild().getId());
		}

		if (serverSettings != null && !serverSettings.isJsonNull()) {
			Tuple2<List<Role>, List<Role>> roleChanges = unlinkRoleChanges(member.getGuild(), serverSettings, true, true, true);

			if (!roleChanges.getV1().isEmpty() || !roleChanges.getV2().isEmpty()) {
				try {
					member.getGuild().modifyMemberRoles(member, roleChanges.getV1(), roleChanges.getV2()).queue();
				} catch (Exception ignored) {}
			}
		}

		return defaultEmbed("Success").setDescription("You were unlinked");
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(unlinkAccount(event.getMember(), null));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Unlink your account from this bot");
	}
}
