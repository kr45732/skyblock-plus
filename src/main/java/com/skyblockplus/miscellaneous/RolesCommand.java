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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class RolesCommand extends Command {

	public RolesCommand() {
		this.name = "roles";
		this.cooldown = 10;
		this.aliases = new String[] { "role" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder updateRoles(String profile, Guild guild, Member member) {
		EmbedBuilder eb;

		if (database.getLinkedUserByDiscordId(member.getId()).isJsonNull()) {
			return defaultEmbed("You must be linked to run this command. Use `" + getGuildPrefix(guild.getId()) + "link [IGN]` to link");
		}

		JsonElement linkedInfo = database.getLinkedUserByDiscordId(member.getId());
		DiscordInfoStruct discordInfo = getPlayerDiscordInfo(higherDepth(linkedInfo, "minecraftUuid").getAsString());

		if (discordInfo.isNotValid()) {
			return discordInfo.getFailEmbed();
		}

		if (!member.getUser().getAsTag().equals(discordInfo.getDiscordTag())) {
			return defaultEmbed("Discord tag mismatch")
				.setDescription(
					"**Player Username:** `" +
					discordInfo.getUsername() +
					"`\n**API Discord Tag:** `" +
					discordInfo.getDiscordTag() +
					"`\n**Your Discord Tag:** `" +
					member.getUser().getAsTag() +
					"`"
				);
		}

		String username = discordInfo.getUsername();
		Player player = profile == null ? new Player(username) : new Player(username, profile);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		JsonElement rolesJson = database.getRolesSettings(guild.getId());
		if (rolesJson == null) {
			return invalidEmbed("Error fetching server's settings");
		}

		try {
			if (!higherDepth(rolesJson, "enable", false)) {
				return invalidEmbed("Automatic roles not enabled for this server");
			}

			List<String> allRoleNames = getJsonKeys(rolesJson);
			allRoleNames.remove("enable");
			Role botRole = guild.getBotRole();
			if (botRole == null) {
				return invalidEmbed("My role in this server doesn't exist. This shouldn't happen!");
			}

			StringBuilder addedRoles = new StringBuilder();
			StringBuilder removedRoles = new StringBuilder();
			StringBuilder disabledAPI = new StringBuilder();
			StringBuilder errorRoles = new StringBuilder();

			List<Role> toAdd = new ArrayList<>();
			List<Role> toRemove = new ArrayList<>();
			JsonElement guildJson = null;

			for (String currentRoleName : allRoleNames) {
				JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
				if (!higherDepth(currentRole, "enable", false)) {
					continue;
				}

				switch (currentRoleName) {
					case "guild_member":
						{
							if (guildJson == null) {
								HypixelResponse response = getGuildFromPlayer(player.getUuid());
								if (!response.isNotValid()) {
									guildJson = response.getResponse();
								}
							}

							if (guildJson != null) {
								JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
								String playerGuildId = higherDepth(guildJson, "_id").getAsString();

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
												addedRoles.append(roleChangeString(currentLevelRole.getName()));
											} else {
												errorRoles.append(roleChangeString(currentLevelRole.getName()));
											}
										}
									} else {
										if (member.getRoles().contains(currentLevelRole)) {
											if (botRole.canInteract(currentLevelRole)) {
												removedRoles.append(roleChangeString(currentLevelRole.getName()));
												toRemove.add(currentLevelRole);
											} else {
												errorRoles.append(roleChangeString(currentLevelRole.getName()));
											}
										}
									}
								}
							}
							break;
						}
					case "guild_ranks":
						{
							if (guildJson == null) {
								HypixelResponse response = getGuildFromPlayer(player.getUuid());
								if (!response.isNotValid()) {
									guildJson = response.getResponse();
								}
							}

							if (guildJson != null) {
								JsonArray curLevels = higherDepth(currentRole, "levels").getAsJsonArray();
								List<JsonElement> guildRoles = new ArrayList<>();
								for (JsonElement curLevel : curLevels) {
									guildRoles.add(
										database.getGuildRoleSettings(guild.getId(), higherDepth(curLevel, "value").getAsString())
									);
								}

								for (JsonElement guildRoleSettings : guildRoles) {
									if (
										higherDepth(guildRoleSettings, "guildId")
											.getAsString()
											.equals(higherDepth(guildJson, "_id").getAsString())
									) {
										JsonArray guildRanks = higherDepth(guildRoleSettings, "guildRanks").getAsJsonArray();

										JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();

										for (JsonElement guildMember : guildMembers) {
											if (higherDepth(guildMember, "uuid").getAsString().equals(player.getUuid())) {
												String guildMemberRank = higherDepth(guildMember, "rank").getAsString().replace(" ", "_");
												for (JsonElement guildRank : guildRanks) {
													Role currentLevelRole = guild.getRoleById(
														higherDepth(guildRank, "discordRoleId").getAsString()
													);
													if (currentLevelRole == null) {
														errorRoles.append(
															roleDeletedString(higherDepth(guildRank, "discordRoleId").getAsString())
														);
														continue;
													}

													if (
														higherDepth(guildRank, "minecraftRoleName")
															.getAsString()
															.equalsIgnoreCase(guildMemberRank)
													) {
														if (!member.getRoles().contains(currentLevelRole)) {
															if (botRole.canInteract(currentLevelRole)) {
																toAdd.add(currentLevelRole);
																addedRoles.append(roleChangeString(currentLevelRole.getName()));
															} else {
																errorRoles.append(roleChangeString(currentLevelRole.getName()));
															}
														}
													} else {
														if (member.getRoles().contains(currentLevelRole)) {
															if (botRole.canInteract(currentLevelRole)) {
																removedRoles.append(roleChangeString(currentLevelRole.getName()));
																toRemove.add(currentLevelRole);
															} else {
																errorRoles.append(roleChangeString(currentLevelRole.getName()));
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
							break;
						}
					case "sven":
					case "rev":
					case "tara":
					case "coins":
					case "alchemy":
					case "combat":
					case "fishing":
					case "farming":
					case "foraging":
					case "carpentry":
					case "mining":
					case "taming":
					case "enchanting":
					case "catacombs":
					case "fairy_souls":
					case "skill_average":
					case "pet_score":
					case "dungeon_secrets":
					case "slot_collector":
					case "enderman":
					case "weight":
					case "total_slayer":
						{
							double roleAmount = -1;
							switch (currentRoleName) {
								case "sven":
								case "rev":
								case "tara":
								case "enderman":
									{
										roleAmount = player.getSlayer(currentRoleName);
										break;
									}
								case "coins":
									{
										roleAmount = player.getBankBalance();
										if (roleAmount == -1 && !disabledAPI.toString().contains("Banking")) {
											disabledAPI.append(roleChangeString("Banking API disabled"));
										} else {
											roleAmount += player.getPurseCoins();
										}
										break;
									}
								case "skill_average":
									{
										roleAmount = player.getSkillAverage();
										if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
											disabledAPI.append(roleChangeString("Skills API disabled"));
										}
										break;
									}
								case "pet_score":
									{
										roleAmount = player.getPetScore();
										break;
									}
								case "alchemy":
								case "combat":
								case "fishing":
								case "farming":
								case "foraging":
								case "carpentry":
								case "mining":
								case "taming":
								case "enchanting":
									{
										if (player.getSkill(currentRoleName) != null) {
											roleAmount = player.getSkill(currentRoleName).getCurrentLevel();
										}
										if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
											disabledAPI.append(roleChangeString("Skills API disabled"));
										}
										break;
									}
								case "catacombs":
									{
										roleAmount = player.getCatacombs().getCurrentLevel();
										break;
									}
								case "fairy_souls":
									{
										roleAmount = player.getFairySouls();
										break;
									}
								case "slot_collector":
									{
										roleAmount = player.getNumberMinionSlots();
										break;
									}
								case "dungeon_secrets":
									roleAmount = player.getDungeonSecrets();
									break;
								case "weight":
									if (player.getSkillAverage() == -1 && !disabledAPI.toString().contains("Skills (for weight)")) {
										disabledAPI.append(roleChangeString("Skills (for weight) API disabled"));
									}
									roleAmount = player.getWeight();
									break;
								case "total_slayer":
									roleAmount = player.getTotalSlayer();
									break;
							}

							if (roleAmount == -1) {
								continue;
							}

							JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
							for (int i = levelsArray.size() - 1; i >= 0; i--) {
								JsonElement currentLevel = levelsArray.get(i);
								int currentLevelValue = higherDepth(currentLevel, "value").getAsInt();
								Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());
								if (currentLevelRole == null) {
									errorRoles.append(roleDeletedString(higherDepth(currentLevel, "roleId").getAsString()));
									continue;
								}

								if (roleAmount < currentLevelValue) {
									if (member.getRoles().contains(currentLevelRole)) {
										if (botRole.canInteract(currentLevelRole)) {
											toRemove.add(currentLevelRole);
											removedRoles.append(roleChangeString(currentLevelRole.getName()));
										} else {
											errorRoles.append(roleChangeString(currentLevelRole.getName()));
										}
									}
								} else {
									if (!member.getRoles().contains(currentLevelRole)) {
										if (botRole.canInteract(currentLevelRole)) {
											toAdd.add(currentLevelRole);
											addedRoles.append(roleChangeString(currentLevelRole.getName()));
										} else {
											errorRoles.append(roleChangeString(currentLevelRole.getName()));
										}
									}

									for (int j = i - 1; j >= 0; j--) {
										JsonElement currentLevelRemoveStackable = levelsArray.get(j);
										Role currentLevelRoleRemoveStackable = guild.getRoleById(
											higherDepth(currentLevelRemoveStackable, "roleId").getAsString()
										);
										if (currentLevelRoleRemoveStackable == null) {
											errorRoles.append(
												roleDeletedString(higherDepth(currentLevelRemoveStackable, "roleId").getAsString())
											);
											continue;
										}

										if (member.getRoles().contains(currentLevelRoleRemoveStackable)) {
											if (botRole.canInteract(currentLevelRoleRemoveStackable)) {
												toRemove.add(currentLevelRoleRemoveStackable);
												removedRoles.append(roleChangeString(currentLevelRoleRemoveStackable.getName()));
											} else {
												errorRoles.append(roleChangeString(currentLevelRoleRemoveStackable.getName()));
											}
										}
									}
									break;
								}
							}
							break;
						}
					case "doom_slayer":
						{
							Role curRole = guild.getRoleById(higherDepth(currentRole, "levels.[0].roleId").getAsString());
							if (curRole == null) {
								errorRoles.append(roleDeletedString(higherDepth(currentRole, "levels.[0].roleId").getAsString()));
								continue;
							}

							if (
								(player.getSlayer("sven") >= 1000000) ||
								(player.getSlayer("rev") >= 1000000) ||
								(player.getSlayer("tara") >= 1000000) ||
								(player.getSlayer("enderman") >= 1000000)
							) {
								if (!member.getRoles().contains(curRole)) {
									if (botRole.canInteract(curRole)) {
										toAdd.add(curRole);
										addedRoles.append(roleChangeString(curRole.getName()));
									} else {
										errorRoles.append(roleChangeString(curRole.getName()));
									}
								}
							} else {
								if (member.getRoles().contains(curRole)) {
									if (botRole.canInteract(curRole)) {
										removedRoles.append(roleChangeString(curRole.getName()));
										toRemove.add(curRole);
									} else {
										errorRoles.append(roleChangeString(curRole.getName()));
									}
								}
							}
							break;
						}
					case "all_slayer_nine":
						{
							Role curRole = guild.getRoleById(higherDepth(currentRole, "levels.[0].roleId").getAsString());
							if (curRole == null) {
								errorRoles.append(roleDeletedString(higherDepth(currentRole, "levels.[0].roleId").getAsString()));
								continue;
							}

							if (
								(player.getSlayer("sven") >= 1000000) &&
								(player.getSlayer("rev") >= 1000000) &&
								(player.getSlayer("tara") >= 1000000) &&
								(player.getSlayer("enderman") >= 1000000)
							) {
								if (!member.getRoles().contains(curRole)) {
									if (botRole.canInteract(curRole)) {
										toAdd.add(curRole);
										addedRoles.append(roleChangeString(curRole.getName()));
									} else {
										errorRoles.append(roleChangeString(curRole.getName()));
									}
								}
							} else {
								if (member.getRoles().contains(curRole)) {
									if (botRole.canInteract(curRole)) {
										removedRoles.append(roleChangeString(curRole.getName()));
										toRemove.add(curRole);
									} else {
										errorRoles.append(roleChangeString(curRole.getName()));
									}
								}
							}
							break;
						}
					case "pet_enthusiast":
						{
							JsonArray playerPets = player.getPets();
							ArrayList<String> excludedPets = new ArrayList<>(Arrays.asList("guardian", "jellyfish", "parrot", "sheep"));

							Role petEnthusiastRole = guild.getRoleById(higherDepth(currentRole, "levels.[0].roleId").getAsString());
							if (petEnthusiastRole == null) {
								errorRoles.append(roleDeletedString(higherDepth(currentRole, "levels.[0].roleId").getAsString()));
								continue;
							}
							boolean isPetEnthusiast = false;

							for (JsonElement currentPet : playerPets) {
								String currentPetRarity = higherDepth(currentPet, "tier").getAsString().toLowerCase();
								if (currentPetRarity.equals("epic") || currentPetRarity.equals("legendary")) {
									if (!excludedPets.contains(higherDepth(currentPet, "type").getAsString().toLowerCase())) {
										if (petLevelFromXp(higherDepth(currentPet, "exp", 0L), currentPetRarity) == 100) {
											isPetEnthusiast = true;
											if (!member.getRoles().contains(petEnthusiastRole)) {
												if (botRole.canInteract(petEnthusiastRole)) {
													toAdd.add(petEnthusiastRole);
													addedRoles.append(roleChangeString(petEnthusiastRole.getName()));
												} else {
													errorRoles.append(roleChangeString(petEnthusiastRole.getName()));
												}
												break;
											}
										}
									}
								}
							}
							if (member.getRoles().contains(petEnthusiastRole) && !isPetEnthusiast) {
								if (botRole.canInteract(petEnthusiastRole)) {
									removedRoles.append(roleChangeString(petEnthusiastRole.getName()));
									toRemove.add(petEnthusiastRole);
								} else {
									errorRoles.append(roleChangeString(petEnthusiastRole.getName()));
								}
							}
							break;
						}
				}
			}

			eb =
				player
					.defaultPlayerEmbed()
					.setDescription(
						"**Added Roles (" +
						toAdd.size() +
						")**\n" +
						(addedRoles.length() > 0 ? addedRoles.toString() : "• None\n") +
						"\n**Removed Roles (" +
						toRemove.size() +
						")**\n" +
						(removedRoles.length() > 0 ? removedRoles.toString() : "• None")
					);
			if (disabledAPI.length() > 0) {
				eb.addField("Disabled APIs:", disabledAPI.toString(), false);
			}

			if (errorRoles.length() > 0) {
				eb.addField("Error roles:", errorRoles.toString(), false);
			}

			guild.modifyMemberRoles(member, toAdd, toRemove).queue();
		} catch (JsonIOException | JsonSyntaxException e) {
			eb = defaultEmbed("Error fetching data");
		}

		return eb;
	}

	private static String roleChangeString(String name) {
		return "• " + name + "\n";
	}

	private static String roleDeletedString(String name) {
		return "• <@" + name + ">\n";
	}

	public static EmbedBuilder listRoles(PaginatorEvent event) {
		JsonElement rolesJson = database.getRolesSettings(event.getGuild().getId());
		if (rolesJson == null || !higherDepth(rolesJson, "enable", false)) {
			return defaultEmbed("Automatic roles not enabled for this server");
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(30);
		List<String> rolesID = getJsonKeys(rolesJson);
		rolesID.remove("enable");
		for (String currentRoleName : rolesID) {
			JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
			if (!higherDepth(currentRole, "enable", false)) {
				continue;
			}

			if ("guild_ranks".equals(currentRoleName)) {
				JsonArray curLevels = higherDepth(currentRole, "levels").getAsJsonArray();
				for (JsonElement curLevel : curLevels) {
					JsonElement guildRoleSettings = database.getGuildRoleSettings(
						event.getGuild().getId(),
						higherDepth(curLevel, "value").getAsString()
					);
					JsonArray guildRanks = higherDepth(guildRoleSettings, "guildRanks").getAsJsonArray();
					for (JsonElement guildRank : guildRanks) {
						paginateBuilder.addItems(
							event.getGuild().getRoleById(higherDepth(guildRank, "discordRoleId").getAsString()).getAsMention()
						);
					}
				}
			} else if (
				"doom_slayer".equals(currentRoleName) ||
				"all_slayer_nine".equals(currentRoleName) ||
				"pet_enthusiast".equals(currentRoleName)
			) {
				paginateBuilder.addItems("<@&" + higherDepth(currentRole, "levels.[0].roleId").getAsString() + ">");
			} else {
				JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
				for (JsonElement currentLevel : levelsArray) {
					paginateBuilder.addItems("<@&" + higherDepth(currentLevel, "roleId").getAsString() + ">");
				}
			}
		}

		paginateBuilder.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Automatic roles list"));

		event.paginate(paginateBuilder);
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 3 || args.length == 2) && args[1].equals("claim")) {
					embed(updateRoles(args.length == 3 ? args[2] : null, event.getGuild(), event.getMember()));
					return;
				} else if (args.length == 2 && args[1].equals("list")) {
					paginate(listRoles(new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
