/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.DUNGEON_CLASS_NAMES;
import static com.skyblockplus.utils.utils.HypixelUtils.getPlayerDiscordInfo;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.miscellaneous.RolesSlashCommand;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.oauth.TokenData;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import groovy.lang.Tuple3;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class LinkSlashCommand extends SlashCommand {

	private static final List<String> rolesClaimLbTypes = List.of(
		"wolf",
		"zombie",
		"spider",
		"enderman",
		"blaze",
		"vampire",
		"level",
		"pet_score",
		"networth",
		"catacombs",
		"fairy_souls",
		"minion_slots",
		"total_slayer",
		"maxed_slayers",
		"healer",
		"mage",
		"berserk",
		"archer",
		"tank",
		"mage_reputation",
		"barbarian_reputation",
		"maxed_collections",
		"coins",
		"skills",
		"alchemy",
		"combat",
		"fishing",
		"farming",
		"foraging",
		"carpentry",
		"mining",
		"taming",
		"enchanting",
		"social",
		"weight"
	);
	private static final List<String> nicknameLbTypes = List.of("skills", "catacombs", "slayer", "weight", "selected_class", "level");

	public static List<String> getLbTypes(boolean rolesClaimEnabled) {
		return rolesClaimEnabled ? rolesClaimLbTypes : nicknameLbTypes;
	}

	public LinkSlashCommand() {
		this.name = "link";
	}

	public static Object linkAccount(String username, Member member, Guild guild) {
		DiscordInfoStruct playerInfo = getPlayerDiscordInfo(username);
		if (!playerInfo.isValid()) {
			return playerInfo.failCause().endsWith(" is not linked on Hypixel")
				? new MessageEditBuilder()
					.setEmbeds(playerInfo.getErrorEmbed().build())
					.setActionRow(Button.primary("verify_help_button", "Help Linking"))
				: playerInfo.getErrorEmbed();
		}

		boolean migratedUsername = member.getUser().getDiscriminator().equals("0000");
		String discord = migratedUsername ? member.getUser().getName() : member.getUser().getAsTag();
		if (!(migratedUsername ? discord.equalsIgnoreCase(playerInfo.discord()) : discord.equals(playerInfo.discord()))) {
			return new MessageEditBuilder()
				.setEmbeds(
					defaultEmbed("Error - Discord Mismatch")
						.setDescription(
							"**Player Username:** `" +
							playerInfo.username() +
							"`\n**In-Game Discord:** `" +
							playerInfo.discord() +
							"`\n**Your Discord:** `" +
							discord +
							"`"
						)
						.build()
				)
				.setActionRow(Button.primary("verify_help_button", "Help Linking"));
		}

		LinkedAccount toAdd = new LinkedAccount(Instant.now().toEpochMilli(), member.getId(), playerInfo.uuid(), playerInfo.username());

		JsonElement verifySettings = database.getVerifySettings(guild.getId());
		if (database.insertLinkedAccount(toAdd, member, verifySettings)) {
			EmbedBuilder eb;
			JsonElement blacklisted = streamJsonArray(guildMap.get(guild.getId()).getBlacklist())
				.filter(blacklist -> higherDepth(blacklist, "uuid").getAsString().equals(toAdd.uuid()))
				.findFirst()
				.orElse(null);
			if (blacklisted != null) {
				eb = errorEmbed("You have been blacklisted with reason `" + higherDepth(blacklisted, "reason").getAsString() + "`");
			} else {
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
					eb = defaultEmbed("Success").setDescription("You have been linked to `" + playerInfo.username() + "`\n" + out);
				} else {
					eb = defaultEmbed("Success").setDescription("You have been linked to `" + playerInfo.username() + "`");
				}
			}

			TokenData.updateLinkedRolesMetadata(toAdd.discord(), toAdd, null, true);
			return eb;
		} else {
			return errorEmbed("Error when inserting into database");
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

		boolean rolesClaimOnLink = higherDepth(verifySettings, "enableRolesClaim", false);
		DataObject player = null;
		boolean playerRequested = false;

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
									playerGuild = new HypixelResponse("");
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
											streamJsonArray(playerGuild.get("members"))
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
							type.equals("CLASS") ||
							type.equals("LEVEL")
						)
					) {
						if (!playerRequested) {
							player =
								leaderboardDatabase.getCachedPlayer(
									getLbTypes(rolesClaimOnLink),
									Player.Gamemode.SELECTED,
									linkedAccount.uuid()
								);
							playerRequested = true;
						}

						if (player != null) {
							nicknameTemplate =
								nicknameTemplate.replace(
									matcher.group(0),
									switch (type) {
										case "SKILLS", "WEIGHT", "CATACOMBS", "LEVEL" -> formatNumber((int) player.getDouble(type));
										case "SLAYER" -> simplifyNumber((long) player.getDouble("slayer"));
										case "CLASS" -> player.getDouble("selected_class") == -1
											? ""
											: "" +
											DUNGEON_CLASS_NAMES.get((int) player.getDouble("selected_class")).toUpperCase().charAt(0);
										default -> throw new IllegalStateException("Unexpected value: " + type);
									} +
									extra
								);
						}
					}

					nicknameTemplate = nicknameTemplate.replace(matcher.group(0), "");
				}

				member.modifyNickname(nicknameTemplate).queue();
				updatedNickname = "true";
			}
		} catch (PermissionException e) {
			updatedNickname = "error: missing permissions";
		} catch (Exception e) {
			updatedNickname = "error";
		}

		try {
			List<Role> toAdd = streamJsonArray(higherDepth(verifySettings, "verifiedRoles"))
				.map(e -> member.getGuild().getRoleById(e.getAsString()))
				.collect(Collectors.toCollection(ArrayList::new));
			List<Role> toRemove = new ArrayList<>();
			try {
				toRemove.add(member.getGuild().getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
			} catch (Exception ignored) {}

			if (rolesClaimOnLink) {
				try {
					if (!playerRequested) {
						player =
							leaderboardDatabase.getCachedPlayer(
								getLbTypes(rolesClaimOnLink),
								Player.Gamemode.SELECTED,
								linkedAccount.uuid()
							);
					}

					if (player != null) {
						Tuple3<EmbedBuilder, List<Role>, List<Role>> out = RolesSlashCommand.ClaimSubcommand.updateRoles(
							null,
							player,
							member,
							true
						);
						toAdd.addAll(out.getV2());
						toRemove.addAll(out.getV3());
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

					if (!updatedRoles.equals("error")) {
						updatedRoles = "true";
					}
				} else {
					member.getGuild().modifyMemberRoles(member, toAdd, toRemove).queue();
					updatedRoles = "true";
				}
			}
		} catch (PermissionException e) {
			updatedRoles = "error: missing permissions";
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

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(linkAccount(event.getOptionStr("player"), event.getMember(), event.getGuild()));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Link your Hypixel account to this bot")
			.addOption(OptionType.STRING, "player", "Player username", true);
	}
}
