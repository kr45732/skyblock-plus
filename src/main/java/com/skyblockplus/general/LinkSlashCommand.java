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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.ApiHandler.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.miscellaneous.RolesSlashCommand;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class LinkSlashCommand extends SlashCommand {

	public LinkSlashCommand() {
		this.name = "link";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(linkAccount(event.getOptionStr("player"), event.getMember(), event.getGuild()));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Link your Hypixel account to this bot")
			.addOption(OptionType.STRING, "player", "Player username", true);
	}

	public static Object linkAccount(String username, Member member, Guild guild) {
		DiscordInfoStruct playerInfo = getPlayerDiscordInfo(username);
		if (!playerInfo.isValid()) {
			return playerInfo.failCause().endsWith(" is not linked on Hypixel")
				? new MessageEditBuilder()
					.setEmbeds(invalidEmbed(playerInfo.failCause()).build())
					.setActionRow(Button.link("https://streamable.com/sdq8tp", "Help Linking"))
				: invalidEmbed(playerInfo.failCause());
		}

		if (!member.getUser().getAsTag().equals(playerInfo.discordTag())) {
			return new MessageEditBuilder()
				.setEmbeds(
					defaultEmbed("Discord tag mismatch")
						.setDescription(
							"**Player Username:** `" +
							playerInfo.username() +
							"`\n**In-Game Discord Tag:** `" +
							playerInfo.discordTag() +
							"`\n**Your Discord Tag:** `" +
							member.getUser().getAsTag() +
							"`"
						)
						.build()
				)
				.setActionRow(Button.link("https://streamable.com/sdq8tp", "Help Linking"));
		}

		LinkedAccount toAdd = new LinkedAccount(Instant.now().toEpochMilli(), member.getId(), playerInfo.uuid(), playerInfo.username());

		if (database.insertLinkedAccount(toAdd)) {
			JsonElement blacklisted = streamJsonArray(guildMap.get(guild.getId()).getBlacklist())
				.filter(blacklist -> higherDepth(blacklist, "uuid").getAsString().equals(toAdd.uuid()))
				.findFirst()
				.orElse(null);
			if (blacklisted != null) {
				return invalidEmbed("You have been blacklisted with reason `" + higherDepth(blacklisted, "reason").getAsString() + "`");
			}

			JsonElement verifySettings = database.getVerifySettings(guild.getId());
			if (verifySettings != null) {
				String[] result = updateLinkedUser(verifySettings, toAdd, member);
				String out =
					(
						!result[1].equals("false")
							? result[1].equals("true") ? "\n• Successfully synced your roles" : "\n• Error syncing your roles"
							: ""
					) +
					(
						!result[0].equals("false")
							? result[0].equals("true") ? "\n• Successfully synced your nickname" : "\n• Error syncing your nickname"
							: ""
					);
				return defaultEmbed("Success").setDescription("You have been linked to `" + playerInfo.username() + "`\n" + out);
			}

			return defaultEmbed("Success").setDescription("You have been linked to `" + playerInfo.username() + "`");
		} else {
			return invalidEmbed("Error when inserting into database");
		}
	}

	/**
	 * @return [nickname, roles]
	 */
	public static String[] updateLinkedUser(JsonElement verifySettings, LinkedAccount linkedAccount, Member member) {
		return updateLinkedUser(verifySettings, linkedAccount, member, false);
	}

	/**
	 * @return [nickname, roles]
	 */
	public static String[] updateLinkedUser(JsonElement verifySettings, LinkedAccount linkedAccount, Member member, boolean doNotUseCache) {
		String updatedNickname = "false";
		String updatedRoles = "false";

		Player player = null;
		String key = database.getServerHypixelApiKey(member.getGuild().getId());

		try {
			String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname", "none");
			if (nicknameTemplate.contains("[IGN]")) {
				nicknameTemplate = nicknameTemplate.replace("[IGN]", linkedAccount.username());

				Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
				HypixelResponse playerGuild = null;
				while (matcher.find()) {
					String category = matcher.group(1);
					String type = matcher.group(2);
					String extra = matcher.group(3) == null ? "" : matcher.group(3);

					if (category.equals("GUILD") && (type.equals("NAME") || type.equals("TAG") || type.equals("RANK"))) {
						if (playerGuild == null) {
							playerGuild = getGuildFromPlayer(linkedAccount.uuid());
							if (playerGuild.isValid()) {
								String gId = playerGuild.get("_id").getAsString();
								if (
									database
										.getAllGuildSettings(member.getGuild().getId())
										.stream()
										.noneMatch(g -> g.getGuildId().equals(gId))
								) {
									playerGuild = new HypixelResponse();
								}
							}
						}

						if (playerGuild.isValid()) {
							nicknameTemplate =
								nicknameTemplate.replace(
									matcher.group(0),
									switch (type) {
										case "NAME" -> playerGuild.get("name").getAsString();
										case "RANK" -> higherDepth(
											streamJsonArray(playerGuild.get("members").getAsJsonArray())
												.filter(g -> higherDepth(g, "uuid", "").equals(linkedAccount.uuid()))
												.findFirst()
												.orElse(null),
											"rank",
											""
										);
										default -> playerGuild.get("tag").getAsString();
									} +
									extra
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
								HypixelResponse response = skyblockProfilesFromUuid(linkedAccount.uuid(), key);
								player =
									!response.isValid()
										? new Player()
										: new Player(linkedAccount.uuid(), linkedAccount.username(), response.response());
							}

							if (player.isValid()) {
								nicknameTemplate =
									nicknameTemplate.replace(
										matcher.group(0),
										switch (type) {
											case "SKILLS" -> roundAndFormat((int) player.getSkillAverage());
											case "SLAYER" -> simplifyNumber(player.getTotalSlayer());
											case "WEIGHT" -> roundAndFormat((int) player.getWeight());
											case "CLASS" -> player.getSelectedDungeonClass().equals("none")
												? ""
												: "" + player.getSelectedDungeonClass().toUpperCase().charAt(0);
											default -> roundAndFormat((int) player.getCatacombs().getProgressLevel());
										}
									);
							}
						}
					}

					nicknameTemplate = nicknameTemplate.replace(matcher.group(0), "");
				}

				member.modifyNickname(nicknameTemplate).complete();
				updatedNickname = "true";
			}
		} catch (Exception e) {
			updatedNickname = "error";
		}

		try {
			List<Role> toAdd = streamJsonArray(higherDepth(verifySettings, "verifiedRoles").getAsJsonArray())
				.map(e -> member.getGuild().getRoleById(e.getAsString()))
				.collect(Collectors.toList());
			List<Role> toRemove = new ArrayList<>();
			try {
				toRemove.add(member.getGuild().getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
			} catch (Exception ignored) {}

			if (higherDepth(verifySettings, "enableRolesClaim", false)) {
				try {
					if (key != null) {
						if (player == null) {
							HypixelResponse response = skyblockProfilesFromUuid(linkedAccount.uuid(), key);
							player =
								!response.isValid()
									? new Player()
									: new Player(linkedAccount.uuid(), linkedAccount.username(), response.response());
						}

						if (player.isValid()) {
							Object[] out = (Object[]) RolesSlashCommand.updateRoles(player, member.getGuild(), member);
							toAdd.addAll((List<Role>) out[1]);
							toRemove.addAll((List<Role>) out[2]);
						}
					}
				} catch (Exception ignored) {}
			}
			if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
				toAdd.removeIf(Objects::isNull);
				toRemove.removeIf(Objects::isNull);
				if (doNotUseCache) {
					for (Role role : toAdd) {
						if (member.getGuild().getSelfMember().canInteract(role)) {
							member.getGuild().addRoleToMember(member, role).queue();
						} else {
							updatedRoles = "error";
						}
					}
					updatedRoles = updatedRoles.equals("error") ? updatedRoles : "true";
				} else {
					member.getGuild().modifyMemberRoles(member, toAdd, toRemove).complete();
					updatedRoles = "true";
				}
			}
		} catch (Exception e) {
			updatedRoles = "error";
		}

		guildMap
			.get(member.getGuild().getId())
			.logAction(
				defaultEmbed(linkedAccount.username() + " Verified", nameMcLink(linkedAccount.uuid()))
					.setDescription(
						(
							!updatedRoles.equals("false")
								? updatedRoles.equals("true") ? "\n• Successfully synced roles" : "\n• Error syncing roles"
								: ""
						) +
						(
							!updatedNickname.equals("false")
								? updatedNickname.equals("true") ? "\n• Successfully synced nickname" : "\n• Error syncing nickname"
								: ""
						)
					),
				member
			);

		return new String[] { updatedNickname, updatedRoles };
	}
}
