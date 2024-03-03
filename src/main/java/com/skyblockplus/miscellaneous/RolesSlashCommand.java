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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.settings.SettingsExecute.allAutomatedRoles;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.formatNumber;
import static com.skyblockplus.utils.utils.StringUtils.idToName;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import groovy.lang.Tuple3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class RolesSlashCommand extends SlashCommand {

	public RolesSlashCommand() {
		this.name = "roles";
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main roles command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class ClaimSubcommand extends Subcommand {

		public ClaimSubcommand() {
			this.name = "claim";
		}

		public static EmbedBuilder getRolesClaimEmbed(String profile, Member member) {
			LinkedAccount linkedInfo = database.getByDiscord(member.getId());
			if (linkedInfo == null) {
				return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
			}

			String username = linkedInfo.username();
			Player.Profile player = Player.create(username, profile);
			if (!player.isValid()) {
				return player.getErrorEmbed();
			}

			Tuple3<EmbedBuilder, List<Role>, List<Role>> out = updateRoles(player, null, member, false);
			if (out.getV2() == null) {
				return out.getV1();
			}

			try {
				member.getGuild().modifyMemberRoles(member, out.getV2(), out.getV3()).queue();
			} catch (PermissionException e) {
				return errorEmbed("Missing permission: " + e.getPermission().getName());
			}

			return out.getV1();
		}

		public static Tuple3<EmbedBuilder, List<Role>, List<Role>> updateRoles(
			Player.Profile player,
			DataObject cachedData,
			Member member,
			boolean skipRoles
		) {
			return updateRoles(player, cachedData, member, database.getRolesSettings(member.getGuild().getId()), skipRoles);
		}

		/**
		 * @param skipRoles Whether to skip certain roles: (guild roles, guild ranks, dungeon secrets,
		 *     player items)
		 * @return eb or [eb, toAdd, toRemove]
		 */
		public static Tuple3<EmbedBuilder, List<Role>, List<Role>> updateRoles(
			Player.Profile player,
			DataObject cachedData,
			Member member,
			JsonElement rolesSettings,
			boolean skipRoles
		) {
			if (!higherDepth(rolesSettings, "enable", false)) {
				return new Tuple3<>(errorEmbed("Automatic roles not setup or enabled (run `/setup` for help)"), null, null);
			}

			Guild guild = member.getGuild();
			Role botRole = guild.getSelfMember().getRoles().get(0);
			boolean useHighest = higherDepth(rolesSettings, "useHighest", false);

			StringBuilder disabledAPI = new StringBuilder();
			StringBuilder errorRoles = new StringBuilder();

			List<Role> toAdd = new ArrayList<>();
			List<Role> toRemove = new ArrayList<>();
			HypixelResponse guildResponse = null;

			for (JsonElement roleSettings : higherDepth(rolesSettings, "roles").getAsJsonArray()) {
				if (higherDepth(roleSettings, "levels.[0]") == null) {
					continue;
				}

				String roleName = higherDepth(roleSettings, "name").getAsString();
				if (!allAutomatedRoles.containsKey(roleName)) {
					continue;
				}

				if (
					skipRoles &&
					(roleName.equals("guild_member") ||
						roleName.equals("guild_ranks") ||
						roleName.equals("dungeon_secrets") ||
						roleName.equals("player_items"))
				) {
					continue;
				}

				switch (roleName) {
					case "guild_member" -> {
						if (guildResponse == null) {
							guildResponse = getGuildFromPlayer(player.getUuid());
						}

						if (guildResponse.isValid()) {
							JsonArray levelsArray = higherDepth(roleSettings, "levels").getAsJsonArray();
							String playerGuildId = guildResponse.get("_id").getAsString();

							for (JsonElement currentLevel : levelsArray) {
								String currentLevelValue = higherDepth(currentLevel, "value").getAsString();
								Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());
								if (currentLevelRole == null) {
									errorRoles.append(roleDeletedString(higherDepth(currentLevel, "roleId").getAsString()));
									continue;
								}

								if (playerGuildId.equals(currentLevelValue)) {
									if (!member.getRoles().contains(currentLevelRole)) {
										if (botRole.canInteract(currentLevelRole)) {
											toAdd.add(currentLevelRole);
										} else {
											errorRoles.append(roleChangeString(currentLevelRole));
										}
									}
								} else {
									if (member.getRoles().contains(currentLevelRole)) {
										if (botRole.canInteract(currentLevelRole)) {
											toRemove.add(currentLevelRole);
										} else {
											errorRoles.append(roleChangeString(currentLevelRole));
										}
									}
								}
							}
						}
					}
					case "guild_ranks" -> {
						if (guildResponse == null) {
							guildResponse = getGuildFromPlayer(player.getUuid());
						}

						if (guildResponse.isValid()) {
							JsonArray curLevels = higherDepth(roleSettings, "levels").getAsJsonArray();
							List<JsonElement> guildRoles = new ArrayList<>();
							for (JsonElement curLevel : curLevels) {
								guildRoles.add(database.getGuildSettings(guild.getId(), higherDepth(curLevel, "value").getAsString()));
							}

							for (JsonElement guildRoleSettings : guildRoles) {
								if (
									higherDepth(guildRoleSettings, "guildId").getAsString().equals(guildResponse.get("_id").getAsString())
								) {
									JsonArray guildRanks = higherDepth(guildRoleSettings, "guildRanks").getAsJsonArray();

									JsonArray guildMembers = guildResponse.get("members").getAsJsonArray();

									for (JsonElement guildMember : guildMembers) {
										if (higherDepth(guildMember, "uuid").getAsString().equals(player.getUuid())) {
											String guildMemberRank = higherDepth(guildMember, "rank").getAsString().replace(" ", "_");
											for (JsonElement guildRank : guildRanks) {
												Role currentLevelRole = guild.getRoleById(higherDepth(guildRank, "roleId").getAsString());
												if (currentLevelRole == null) {
													errorRoles.append(roleDeletedString(higherDepth(guildRank, "roleId").getAsString()));
													continue;
												}

												if (higherDepth(guildRank, "value").getAsString().equalsIgnoreCase(guildMemberRank)) {
													if (!member.getRoles().contains(currentLevelRole)) {
														if (botRole.canInteract(currentLevelRole)) {
															toAdd.add(currentLevelRole);
														} else {
															errorRoles.append(roleChangeString(currentLevelRole));
														}
													}
												} else {
													if (member.getRoles().contains(currentLevelRole)) {
														if (botRole.canInteract(currentLevelRole)) {
															toRemove.add(currentLevelRole);
														} else {
															errorRoles.append(roleChangeString(currentLevelRole));
														}
													}
												}
											}
											break;
										}
									}
								}
							}
						}
					}
					case "player_items" -> {
						JsonArray levelsArray = higherDepth(roleSettings, "levels").getAsJsonArray();
						List<String> items = streamJsonArray(levelsArray)
							.map(item -> higherDepth(item, "value").getAsString())
							.collect(Collectors.toCollection(ArrayList::new));
						Set<String> itemsPlayerHas = player.getItemsPlayerHas(items);

						if (itemsPlayerHas == null) {
							disabledAPI.append("• Inventory API disabled\n");
							continue;
						}

						for (int i = 0; i < levelsArray.size(); i++) {
							JsonElement currentLevel = levelsArray.get(i);
							String id = higherDepth(currentLevel, "value").getAsString();
							Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());
							if (currentLevelRole == null) {
								errorRoles.append(roleDeletedString(higherDepth(currentLevel, "roleId").getAsString()));
								continue;
							}

							if (itemsPlayerHas.contains(id)) {
								if (!member.getRoles().contains(currentLevelRole)) {
									if (botRole.canInteract(currentLevelRole)) {
										toAdd.add(currentLevelRole);
									} else {
										errorRoles.append(roleChangeString(currentLevelRole));
									}
								}
							} else {
								if (member.getRoles().contains(currentLevelRole)) {
									if (botRole.canInteract(currentLevelRole)) {
										toRemove.add(currentLevelRole);
									} else {
										errorRoles.append(roleChangeString(currentLevelRole));
									}
								}
							}
						}
					}
					case "gamemode" -> {
						JsonArray levelsArray = higherDepth(roleSettings, "levels").getAsJsonArray();
						for (int i = levelsArray.size() - 1; i >= 0; i--) {
							JsonElement currentLevel = levelsArray.get(i);
							String mode = higherDepth(currentLevel, "value").getAsString();
							Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());
							if (currentLevelRole == null) {
								errorRoles.append(roleDeletedString(higherDepth(currentLevel, "roleId").getAsString()));
								continue;
							}

							if (
								Player.Gamemode
									.of(mode)
									.isGamemode(
										player != null
											? player.getGamemode()
											: Player.Gamemode.values()[(int) cachedData.getDouble("gamemode")]
									)
							) {
								if (!member.getRoles().contains(currentLevelRole)) {
									if (botRole.canInteract(currentLevelRole)) {
										toAdd.add(currentLevelRole);
									} else {
										errorRoles.append(roleChangeString(currentLevelRole));
									}
								}
							} else {
								if (member.getRoles().contains(currentLevelRole)) {
									if (botRole.canInteract(currentLevelRole)) {
										toRemove.add(currentLevelRole);
									} else {
										errorRoles.append(roleChangeString(currentLevelRole));
									}
								}
							}
						}
					}
					default -> {
						double playerAmount;
						if (player != null) {
							playerAmount =
								switch (roleName) {
									case "wolf",
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
										"dungeon_secrets",
										"total_slayer",
										"maxed_slayers",
										"healer",
										"mage",
										"berserk",
										"archer",
										"tank",
										"class_average",
										"mage_reputation",
										"barbarian_reputation",
										"maxed_collections" -> player.getAmount(roleName, useHighest);
									case "coins" -> {
										playerAmount = player.getAmount("bank", useHighest);
										if (playerAmount == -1 && !disabledAPI.toString().contains("Banking")) {
											disabledAPI.append("• Banking API disabled\n");
										} else {
											playerAmount += player.getAmount("purse", useHighest);
										}
										yield playerAmount;
									}
									case "skill_average" -> {
										playerAmount = player.getAmount("skills", useHighest);
										if (playerAmount == -1 && !disabledAPI.toString().contains("Skills")) {
											disabledAPI.append("• Skills API disabled\n");
										}
										yield playerAmount;
									}
									case "alchemy",
										"combat",
										"fishing",
										"farming",
										"foraging",
										"carpentry",
										"mining",
										"taming",
										"enchanting",
										"social" -> {
										playerAmount = player.getAmount(roleName, useHighest);
										if (playerAmount == -1 && !disabledAPI.toString().contains("Skills")) {
											disabledAPI.append("• Skills API disabled\n");
										}
										yield playerAmount;
									}
									case "weight" -> {
										if (!useHighest && !player.isSkillsApiEnabled() && !disabledAPI.toString().contains("Skills")) {
											disabledAPI.append("• Skills API disabled\n");
										}
										playerAmount = useHighest ? player.getHighestAmount(roleName) : player.getWeight();
										yield playerAmount;
									}
									default -> -1;
								};
						} else {
							playerAmount =
								switch (roleName) {
									case "skill_average" -> cachedData.getDouble("skills", -1);
									case "total_slayer" -> cachedData.getDouble("slayer", -1);
									default -> cachedData.getDouble(roleName, -1);
								};
						}

						if (playerAmount == -1) {
							continue;
						}

						JsonArray levelsArray = collectJsonArray(
							streamJsonArray(higherDepth(roleSettings, "levels"))
								.sorted(Comparator.comparingLong(o -> higherDepth(o, "value").getAsLong()))
						);
						for (int i = levelsArray.size() - 1; i >= 0; i--) {
							JsonElement level = levelsArray.get(i);
							long levelValue = higherDepth(level, "value").getAsLong();
							String levelRoleId = higherDepth(level, "roleId").getAsString();
							Role levelRole = guild.getRoleById(levelRoleId);
							if (levelRole == null) {
								errorRoles.append(roleDeletedString(levelRoleId));
								continue;
							}

							if (playerAmount < levelValue) {
								if (member.getRoles().contains(levelRole)) {
									if (botRole.canInteract(levelRole)) {
										toRemove.add(levelRole);
									} else {
										errorRoles.append(roleChangeString(levelRole));
									}
								}
							} else {
								if (!member.getRoles().contains(levelRole)) {
									if (botRole.canInteract(levelRole)) {
										toAdd.add(levelRole);
									} else {
										errorRoles.append(roleChangeString(levelRole));
									}
								}

								for (int j = i - 1; j >= 0; j--) {
									String levelRemoveRoleId = higherDepth(levelsArray.get(j), "roleId").getAsString();
									Role levelRemoveRole = guild.getRoleById(levelRemoveRoleId);
									if (levelRemoveRole == null) {
										errorRoles.append(roleDeletedString(levelRemoveRoleId));
										continue;
									}

									if (member.getRoles().contains(levelRemoveRole)) {
										if (botRole.canInteract(levelRemoveRole)) {
											toRemove.add(levelRemoveRole);
										} else {
											errorRoles.append(roleChangeString(levelRemoveRole));
										}
									}
								}
								break;
							}
						}
					}
				}
			}

			EmbedBuilder eb = null;
			if (player != null) {
				eb = player.defaultPlayerEmbed().setDescription(useHighest ? "**Note: Using highest values across all profiles**" : "");

				if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
					if (!toAdd.isEmpty()) {
						eb.addField(
							"Added Roles (" + toAdd.size() + ")",
							truncateRolesString(toAdd.stream().map(IMentionable::getAsMention).collect(Collectors.joining("\n"))),
							false
						);
					}

					if (!toRemove.isEmpty()) {
						eb.addField(
							"Removed Roles (" + toRemove.size() + ")",
							truncateRolesString(toRemove.stream().map(IMentionable::getAsMention).collect(Collectors.joining("\n"))),
							false
						);
					}
				} else {
					eb.appendDescription("\n\n**No roles added or removed**");
				}

				if (!disabledAPI.isEmpty()) {
					eb.addField("Disabled APIs", disabledAPI.toString(), false);
				}

				if (!errorRoles.isEmpty()) {
					eb.addField("Error Roles", truncateRolesString(errorRoles), false);
				}
			}

			return new Tuple3<>(eb, toAdd, toRemove);
		}

		private static String truncateRolesString(CharSequence roles) {
			if (roles.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
				return roles.toString();
			}

			StringBuilder out = new StringBuilder();
			String[] split = roles.toString().split("\n");
			for (int i = 0; i < split.length; i++) {
				String role = split[i] + "\n";
				int rolesLeft = split.length - i;
				String endStr = "... " + rolesLeft + " more role" + (rolesLeft > 1 ? "s" : "");

				if (out.length() + role.length() > MessageEmbed.VALUE_MAX_LENGTH - endStr.length()) {
					out.append(endStr);
					break;
				}

				out.append(role);
			}

			return out.toString();
		}

		private static String roleChangeString(Role role) {
			return role.getAsMention() + "\n";
		}

		private static String roleDeletedString(String name) {
			return "<@&" + name + ">\n";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(getRolesClaimEmbed(event.getOptionStr("profile"), event.getMember()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Claim automatic Skyblock roles. The player must be linked to the bot")
				.addOptions(profilesCommandOption);
		}
	}

	public static class ListSubcommand extends Subcommand {

		public ListSubcommand() {
			this.name = "list";
		}

		public static EmbedBuilder listRoles(SlashCommandEvent event) {
			JsonElement rolesSettings = database.getRolesSettings(event.getGuild().getId());
			if (!higherDepth(rolesSettings, "enable", false)) {
				return defaultEmbed("Automatic roles not enabled for this server");
			}

			CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(30);
			for (JsonElement roleSettings : higherDepth(rolesSettings, "roles").getAsJsonArray()) {
				if (higherDepth(roleSettings, "levels.[0]") == null) {
					continue;
				}

				String currentRoleName = higherDepth(roleSettings, "name").getAsString();
				if (!allAutomatedRoles.containsKey(currentRoleName)) {
					continue;
				}

				if (currentRoleName.equals("guild_ranks")) {
					JsonArray curLevels = higherDepth(roleSettings, "levels").getAsJsonArray();
					for (JsonElement curLevel : curLevels) {
						JsonElement guildRoleSettings = database.getGuildSettings(
							event.getGuild().getId(),
							higherDepth(curLevel, "value").getAsString()
						);
						JsonArray guildRanks = higherDepth(guildRoleSettings, "guildRanks").getAsJsonArray();
						for (JsonElement guildRank : guildRanks) {
							paginateBuilder.addStrings("<@&" + higherDepth(guildRank, "roleId").getAsString() + ">");
						}
					}
				} else {
					JsonArray levelsArray = higherDepth(roleSettings, "levels").getAsJsonArray();
					for (JsonElement currentLevel : levelsArray) {
						String roleValue = higherDepth(currentLevel, "value").getAsString();
						if (currentRoleName.equals("player_items")) {
							roleValue = idToName(roleValue);
						} else {
							try {
								roleValue = formatNumber(Long.parseLong(roleValue));
							} catch (Exception ignored) {}
						}
						paginateBuilder.addStrings(
							"<@&" +
							higherDepth(currentLevel, "roleId").getAsString() +
							">" +
							(!currentRoleName.equals("guild_member") ? " - " + roleValue + " " + currentRoleName.replace("_", " ") : "")
						);
					}
				}
			}

			paginateBuilder.getExtras().setEveryPageTitle("Automatic roles list");
			event.paginate(paginateBuilder);
			return null;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(listRoles(event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "List all roles that can be claimed through the bot");
		}
	}
}
