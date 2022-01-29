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

package com.skyblockplus.features.verify;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.ApiHandler.skyblockProfilesFromUuid;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class VerifyGuild {

	public final String guildId;
	public TextChannel messageChannel;
	public Message originalMessage;
	public JsonElement verifySettings;
	public final boolean enable;

	public VerifyGuild(TextChannel messageChannel, Message originalMessage, JsonElement verifySettings, String guildId) {
		this.messageChannel = messageChannel;
		this.originalMessage = originalMessage;
		this.verifySettings = verifySettings;
		this.guildId = guildId;
		this.enable = true;
	}

	public VerifyGuild(String guildId) {
		this.enable = false;
		this.guildId = guildId;
	}

	public boolean onGuildMessageReceived(MessageReceivedEvent event) {
		if (!enable) {
			return false;
		}

		if (!event.getChannel().getId().equals(messageChannel.getId())) {
			return false;
		}

		if (event.getMessage().getId().equals(originalMessage.getId())) {
			return false;
		}

		if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			if (event.getAuthor().isBot()) {
				return false;
			}

			String guildPrefix = getGuildPrefix(event.getGuild().getId());
			if (
				!event.getMessage().getContentRaw().startsWith(guildPrefix + "link ") ||
				!event.getMessage().getContentRaw().startsWith(guildPrefix + "verify ")
			) {
				event.getMessage().delete().queue();
				return true;
			}
		}

		event.getMessage().delete().queueAfter(8, TimeUnit.SECONDS, ignore, ignore);
		return true;
	}

	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (!higherDepth(verifySettings, "enableAutomaticSync", "false").equals("true")) {
			return;
		}

		LinkedAccount linkedUser = database.getByDiscord(event.getUser().getId());
		if (linkedUser == null) {
			return;
		}

		String updatedNickname = "false";
		String updatedRoles = "false";

		try {
			String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname", "none");
			if (nicknameTemplate.contains("[IGN]")) {
				nicknameTemplate = nicknameTemplate.replace("[IGN]", linkedUser.username());

				Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
				HypixelResponse playerGuild = null;
				Player player = null;
				String key = database.getServerHypixelApiKey(event.getGuild().getId());
				while (matcher.find()) {
					String category = matcher.group(1);
					String type = matcher.group(2);
					String extra = matcher.group(3) == null ? "" : matcher.group(3);

					if (category.equals("GUILD") && (type.equals("NAME") || type.equals("TAG") || type.equals("RANK"))) {
						if (playerGuild == null) {
							playerGuild = getGuildFromPlayer(linkedUser.uuid());
							if (!playerGuild.isNotValid()) {
								String gId = playerGuild.get("_id").getAsString();
								if (
									database
										.getAllGuildSettings(event.getGuild().getId())
										.stream()
										.noneMatch(g -> g.getGuildId().equals(gId))
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
												.filter(g -> higherDepth(g, "uuid", "").equals(linkedUser.uuid()))
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
								HypixelResponse response = skyblockProfilesFromUuid(linkedUser.uuid(), key);
								player =
									response.isNotValid()
										? new Player()
										: new Player(linkedUser.uuid(), linkedUser.username(), response.response());
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

				event.getMember().modifyNickname(nicknameTemplate).complete();
				updatedNickname = "true";
			}
		} catch (Exception e) {
			updatedNickname = "error";
		}

		try {
			List<Role> toAdd = streamJsonArray(higherDepth(verifySettings, "verifiedRoles").getAsJsonArray())
				.map(e -> event.getGuild().getRoleById(e.getAsString()))
				.collect(Collectors.toList());
			List<Role> toRemove = new ArrayList<>();
			try {
				toRemove.add(event.getGuild().getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
			} catch (Exception ignored) {}
			if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
				event.getGuild().modifyMemberRoles(event.getMember(), toAdd, toRemove).complete();
				updatedRoles = "true";
			}
		} catch (Exception e) {
			updatedRoles = "error";
		}

		String finalUpdatedRoles = updatedRoles;
		String finalUpdatedNickname = updatedNickname;
		event
			.getUser()
			.openPrivateChannel()
			.queue(privateChannel ->
				privateChannel
					.sendMessageEmbeds(
						defaultEmbed("Member synced")
							.setDescription(
								"You have automatically been synced in `" +
								event.getGuild().getName() +
								"`" +
								(
									!finalUpdatedRoles.equals("false")
										? finalUpdatedRoles.equals("true")
											? "\n• Successfully synced your roles"
											: "\n• Error syncing your roles"
										: ""
								) +
								(
									!finalUpdatedNickname.equals("false")
										? finalUpdatedNickname.equals("true")
											? "\n• Successfully synced your nickname"
											: "\n• Error syncing your nickname"
										: ""
								)
							)
							.build()
					)
					.queue(ignore, ignore)
			);
	}

	public void reloadSettingsJson(JsonElement newVerifySettings) {
		if (higherDepth(newVerifySettings, "enableAutomaticSync", "").equals("true")) {
			verifySettings = database.getVerifySettings(guildId);
		}
	}
}
