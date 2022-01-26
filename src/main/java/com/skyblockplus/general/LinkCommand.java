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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.ApiHandler.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.checkHypixelKey;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.utils.ApiHandler;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

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
			return invalidEmbed(playerInfo.failCause());
		}

		if (!member.getUser().getAsTag().equals(playerInfo.discordTag())) {
			return defaultEmbed("Discord tag mismatch")
				.setDescription(
					"**Player Username:** `" +
					playerInfo.username() +
					"`\n**API Discord Tag:** `" +
					playerInfo.discordTag() +
					"`\n**Your Discord Tag:** `" +
					member.getUser().getAsTag() +
					"`"
				);
		}

		LinkedAccount toAdd = new LinkedAccount(Instant.now().toEpochMilli(), member.getId(), playerInfo.uuid(), playerInfo.username());

		if (database.insertLinkedAccount(toAdd)) {
			JsonElement verifySettings = database.getVerifySettings(guild.getId());
			if (verifySettings != null) {
				try {
					String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname").getAsString();

					if (nicknameTemplate.contains("[IGN]")) {
						nicknameTemplate = nicknameTemplate.replace("[IGN]", toAdd.username());

						Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
						HypixelResponse playerGuild = null;
						Player player = null;
						String key = database.getServerHypixelApiKey(guild.getId());
						while (matcher.find()) {
							String category = matcher.group(1);
							String type = matcher.group(2);

							if (category.equals("GUILD") && (type.equals("NAME") || type.equals("TAG") || type.equals("RANK"))) {
								if (playerGuild == null) {
									playerGuild = getGuildFromPlayer(toAdd.uuid());
									if (!playerGuild.isNotValid()) {
										String gId = playerGuild.get("_id").getAsString();
										if (
											database.getAllGuildSettings(guild.getId()).stream().noneMatch(g -> g.getGuildId().equals(gId))
										) {
											playerGuild = new HypixelResponse();
										}
									}
								}

								if (!playerGuild.isNotValid()) {
									nicknameTemplate =
										nicknameTemplate.replace(
											matcher.group(0),
											switch (type) {
												case "NAME" -> playerGuild.get("name").getAsString();
												case "RANK" -> higherDepth(
													streamJsonArray(playerGuild.get("members").getAsJsonArray())
														.filter(g -> higherDepth(g, "uuid", "").equals(toAdd.uuid()))
														.findFirst()
														.orElse(null),
													"rank",
													""
												);
												default -> playerGuild.get("tag").getAsString();
											}
										);
								}
							} else if (
								category.equals("PLAYER") &&
								(
									type.equals("SKILLS") ||
									type.equals("CATACOMBS") ||
									type.equals("SLAYER") ||
									type.equals("WEIGHT") ||
									type.equals("CLASS")
								)
							) {
								if (key != null) {
									if (player == null) {
										HypixelResponse response = skyblockProfilesFromUuid(toAdd.uuid(), key);
										player =
											response.isNotValid()
												? new Player()
												: new Player(toAdd.uuid(), toAdd.username(), response.response());
									}

									if (player.isValid()) {
										nicknameTemplate =
											nicknameTemplate.replace(
												matcher.group(0),
												switch (type) {
													case "SKILLS" -> roundAndFormat(player.getSkillAverage());
													case "SLAYER" -> simplifyNumber(player.getTotalSlayer());
													case "WEIGHT" -> roundAndFormat(player.getWeight());
													case "CLASS" -> player.getSelectedDungeonClass().equals("none")
														? ""
														: "" + player.getSelectedDungeonClass().toUpperCase().charAt(0);
													default -> roundAndFormat(player.getCatacombs().getProgressLevel());
												}
											);
									}
								}
							}

							nicknameTemplate = nicknameTemplate.replace(matcher.group(0), "");
						}

						member.modifyNickname(nicknameTemplate).queue();
					}
				} catch (Exception ignored) {}

				try {
					List<Role> toAddRoles = streamJsonArray(higherDepth(verifySettings, "verifiedRoles").getAsJsonArray())
						.map(e -> guild.getRoleById(e.getAsString()))
						.collect(Collectors.toList());
					List<Role> toRemoveRoles = new ArrayList<>();
					try {
						toRemoveRoles.add(guild.getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
					} catch (Exception ignored) {}
					if (!toAddRoles.isEmpty() || !toRemoveRoles.isEmpty()) {
						guild.modifyMemberRoles(member, toAddRoles, toRemoveRoles).queue();
					}
				} catch (Exception ignored) {}
			}

			return defaultEmbed("Success").setDescription("You have been linked to `" + playerInfo.username() + "`");
		} else {
			return invalidEmbed("Error when inserting into database");
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
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
