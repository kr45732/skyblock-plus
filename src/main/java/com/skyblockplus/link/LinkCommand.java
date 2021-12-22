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
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class LinkCommand extends Command {

	public LinkCommand() {
		this.name = "link";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "verify" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder linkAccount(String username, Member member, Guild guild) {
		DiscordInfoStruct playerInfo = getPlayerDiscordInfo(username);
		if (playerInfo.isNotValid()) {
			return invalidEmbed(playerInfo.getFailCause());
		}

		if (!member.getUser().getAsTag().equals(playerInfo.getDiscordTag())) {
			EmbedBuilder eb = defaultEmbed("Discord tag mismatch");
			eb.setDescription(
				"**Player Username:** `" +
				playerInfo.getUsername() +
				"`\n**API Discord Tag:** `" +
				playerInfo.getDiscordTag() +
				"`\n**Your Discord Tag:** `" +
				member.getUser().getAsTag() +
				"`"
			);
			return eb;
		}

		LinkedAccountModel toAdd = new LinkedAccountModel(
			"" + Instant.now().toEpochMilli(),
			member.getId(),
			playerInfo.getUuid(),
			playerInfo.getUsername()
		);

		if (database.addLinkedUser(toAdd) == 200) {
			JsonElement verifySettings = database.getVerifySettings(guild.getId());
			if (verifySettings != null) {
				try {
					String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname").getAsString();
					if (!nicknameTemplate.equalsIgnoreCase("none") && !nicknameTemplate.isEmpty()) {
						nicknameTemplate = nicknameTemplate.replace("[IGN]", playerInfo.getUsername());

						if (nicknameTemplate.contains("[GUILD_RANK]")) {
							try {
								HypixelResponse playerGuild = getGuildFromPlayer(playerInfo.getUuid());
								if (!playerGuild.isNotValid()) {
									AutomatedGuild settingsGuildId = database
										.getAllGuildSettings(guild.getId())
										.stream()
										.filter(guildRole -> guildRole.getGuildId().equalsIgnoreCase(playerGuild.get("_id").getAsString()))
										.findFirst()
										.orElse(null);

									if (settingsGuildId != null) {
										JsonArray guildMembers = playerGuild.get("members").getAsJsonArray();
										for (JsonElement guildMember : guildMembers) {
											if (higherDepth(guildMember, "uuid").getAsString().equals(playerInfo.getUuid())) {
												nicknameTemplate =
													nicknameTemplate.replace(
														"[GUILD_RANK]",
														higherDepth(guildMember, "rank").getAsString()
													);
												break;
											}
										}
									}
								}
							} catch (Exception ignored) {}
						}

						member.modifyNickname(nicknameTemplate).queue();
					}
				} catch (Exception ignored) {}

				try {
					JsonArray verifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();
					for (JsonElement verifyRole : verifyRoles) {
						try {
							guild.addRoleToMember(member.getId(), guild.getRoleById(verifyRole.getAsString())).complete();
						} catch (Exception e) {
							System.out.println(verifyRole);
							e.printStackTrace();
						}
					}
					try {
						guild
							.removeRoleFromMember(
								member,
								guild.getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString())
							)
							.queue();
					} catch (Exception ignored) {}
				} catch (Exception ignored) {}
			}

			return defaultEmbed("Success")
				.setDescription("`" + member.getUser().getAsTag() + "` was linked to `" + playerInfo.getUsername() + "`");
		} else {
			return invalidEmbed("Error linking `" + member.getUser().getAsTag() + " to `" + playerInfo.getUsername() + "`");
		}
	}

	public static EmbedBuilder getLinkedAccount(User user) {
		JsonElement userInfo = database.getLinkedUserByDiscordId(user.getId());

		try {
			return defaultEmbed("Linked information")
				.setDescription(
					"`" + user.getAsTag() + "` is linked to `" + (higherDepth(userInfo, "minecraftUsername").getAsString()) + "`"
				);
		} catch (Exception e) {
			return invalidEmbed("`" + user.getAsTag() + "` is not linked");
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					embed(linkAccount(args[1], event.getMember(), event.getGuild()));
					return;
				} else if (args.length == 1) {
					embed(getLinkedAccount(event.getAuthor()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
