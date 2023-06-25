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

import static com.skyblockplus.settings.SettingsExecute.isOneLevelRole;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.HypixelUtils.petLevelFromXp;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.formatNumber;
import static com.skyblockplus.utils.utils.StringUtils.idToName;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.*;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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
			this.cooldown = GLOBAL_COOLDOWN + 2;
		}

		public static EmbedBuilder updateRoles(String profile, Member member) {
			LinkedAccount linkedInfo = database.getByDiscord(member.getId());
			if (linkedInfo == null) {
				return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
			}

			String username = linkedInfo.username();
			Player.Profile player = Player.create(username, profile);
			if (!player.isValid()) {
				return player.getErrorEmbed();
			}

			Object out = updateRoles(player, member);
			if (out instanceof EmbedBuilder eb1) {
				return eb1;
			}

			Object[] outArr = ((Object[]) out);
			try {
				member.getGuild().modifyMemberRoles(member, (List<Role>) outArr[1], (List<Role>) outArr[2]).queue();
			} catch (PermissionException e) {
				return errorEmbed("Missing permission: " + e.getPermission().getName());
			}
			return (EmbedBuilder) outArr[0];
		}

		public static Object updateRoles(Player.Profile player, Member member) {
			return updateRoles(player, member, database.getRolesSettings(member.getGuild().getId()), false);
		}

		/**
		 * @param skipRoles Whether to skip roles that require extra API requests (guild roles, guild ranks, dungeon secrets)
		 * @return eb or [eb, toAdd, toRemove]
		 */
		public static Object updateRoles(Player.Profile player, Member member, JsonElement rolesSettings, boolean skipRoles) {
			if (!higherDepth(rolesSettings, "enable", false)) {
				return errorEmbed("Automatic roles not setup or enabled for this server");
			}

			Guild guild = member.getGuild();
			Role botRole = guild.getSelfMember().getRoles().get(0);
			boolean useHighest = higherDepth(rolesSettings, "useHighest", false);

			StringBuilder addedRoles = new StringBuilder();
			StringBuilder removedRoles = new StringBuilder();
			StringBuilder disabledAPI = new StringBuilder();
			StringBuilder errorRoles = new StringBuilder();

			List<Role> toAdd = new ArrayList<>();
			List<Role> toRemove = new ArrayList<>();
			JsonElement guildJson = null;

			for (JsonElement roleSettings : higherDepth(rolesSettings, "roles").getAsJsonArray()) {
				if (higherDepth(roleSettings, "levels.[0]") == null) {
					continue;
				}

				String roleName = higherDepth(roleSettings, "name").getAsString();

				if (
					skipRoles && (roleName.equals("guild_member") || roleName.equals("guild_ranks") || roleName.equals("dungeon_secrets"))
				) {
					continue;
				}

				switch (roleName) {
					case "guild_member" -> {
						if (guildJson == null) {
							HypixelResponse response = getGuildFromPlayer(player.getUuid());
							if (response.isValid()) {
								guildJson = response.response();
							}
						}

						if (guildJson != null) {
							JsonArray levelsArray = higherDepth(roleSettings, "levels").getAsJsonArray();
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
					}
					case "guild_ranks" -> {
						if (guildJson == null) {
							HypixelResponse response = getGuildFromPlayer(player.getUuid());
							if (response.isValid()) {
								guildJson = response.response();
							}
						}

						if (guildJson != null) {
							JsonArray curLevels = higherDepth(roleSettings, "levels").getAsJsonArray();
							List<JsonElement> guildRoles = new ArrayList<>();
							for (JsonElement curLevel : curLevels) {
								guildRoles.add(database.getGuildSettings(guild.getId(), higherDepth(curLevel, "value").getAsString()));
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
												Role currentLevelRole = guild.getRoleById(higherDepth(guildRank, "roleId").getAsString());
												if (currentLevelRole == null) {
													errorRoles.append(roleDeletedString(higherDepth(guildRank, "roleId").getAsString()));
													continue;
												}

												if (higherDepth(guildRank, "value").getAsString().equalsIgnoreCase(guildMemberRank)) {
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

							if (useHighest ? player.getHighestAmount(mode) == 1 : player.isGamemode(Player.Gamemode.of(mode))) {
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
										toRemove.add(currentLevelRole);
										removedRoles.append(roleChangeString(currentLevelRole.getName()));
									} else {
										errorRoles.append(roleChangeString(currentLevelRole.getName()));
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
							disabledAPI.append(roleChangeString("Inventory API disabled"));
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
										addedRoles.append(roleChangeString(currentLevelRole.getName()));
									} else {
										errorRoles.append(roleChangeString(currentLevelRole.getName()));
									}
								}
							} else {
								if (member.getRoles().contains(currentLevelRole)) {
									if (botRole.canInteract(currentLevelRole)) {
										toRemove.add(currentLevelRole);
										removedRoles.append(roleChangeString(currentLevelRole.getName()));
									} else {
										errorRoles.append(roleChangeString(currentLevelRole.getName()));
									}
								}
							}
						}
					}
					case "pet_enthusiast" -> {
						JsonArray playerPets = player.getPets();
						ArrayList<String> excludedPets = new ArrayList<>(List.of("guardian", "jellyfish", "parrot", "sheep"));

						Role petEnthusiastRole = guild.getRoleById(higherDepth(roleSettings, "levels.[0].roleId").getAsString());
						if (petEnthusiastRole == null) {
							errorRoles.append(roleDeletedString(higherDepth(roleSettings, "levels.[0].roleId").getAsString()));
							continue;
						}
						boolean isPetEnthusiast = false;

						for (JsonElement currentPet : playerPets) {
							String currentPetRarity = higherDepth(currentPet, "tier").getAsString().toLowerCase();
							if (currentPetRarity.equals("epic") || currentPetRarity.equals("legendary")) {
								if (!excludedPets.contains(higherDepth(currentPet, "type").getAsString().toLowerCase())) {
									if (
										petLevelFromXp(
											higherDepth(currentPet, "exp", 0L),
											currentPetRarity,
											higherDepth(currentPet, "type").getAsString()
										) ==
										100
									) {
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
					}
					default -> {
						double roleAmount =
							switch (roleName) {
								case "sven", "rev", "tara", "enderman", "blaze", "vampire" -> useHighest
									? player.getHighestAmount(roleName)
									: player.getSlayer(roleName);
								case "coins" -> {
									roleAmount = useHighest ? player.getHighestAmount("bank") : player.getBankBalance();
									if (roleAmount == -1 && !disabledAPI.toString().contains("Banking")) {
										disabledAPI.append(roleChangeString("Banking API disabled"));
									} else {
										roleAmount += useHighest ? player.getHighestAmount("purse") : player.getPurseCoins();
									}
									yield roleAmount;
								}
								case "skill_average" -> {
									roleAmount = useHighest ? player.getHighestAmount("skills") : player.getSkillAverage();
									if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
										disabledAPI.append(roleChangeString("Skills API disabled"));
									}
									yield roleAmount;
								}
								case "level" -> useHighest ? player.getHighestAmount(roleName) : player.getLevel();
								case "pet_score" -> useHighest ? player.getHighestAmount(roleName) : player.getPetScore();
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
									roleAmount =
										useHighest
											? player.getHighestAmount(roleName)
											: (player.getSkill(roleName) != null ? player.getSkill(roleName).getProgressLevel() : -1);
									if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
										disabledAPI.append(roleChangeString("Skills API disabled"));
									}
									yield roleAmount;
								}
								case "networth" -> useHighest ? player.getHighestAmount(roleName) : player.getNetworth();
								case "catacombs" -> useHighest
									? player.getHighestAmount(roleName)
									: player.getCatacombs().getProgressLevel();
								case "fairy_souls" -> useHighest ? player.getHighestAmount(roleName) : player.getFairySouls();
								case "slot_collector" -> useHighest ? player.getHighestAmount(roleName) : player.getNumberMinionSlots();
								case "dungeon_secrets" -> useHighest ? player.getHighestAmount(roleName) : player.getDungeonSecrets();
								case "accessory_count" -> useHighest ? player.getHighestAmount(roleName) : player.getAccessoryCount();
								case "weight" -> {
									if (
										!useHighest &&
										!player.isSkillsApiEnabled() &&
										!disabledAPI.toString().contains("Skills (for weight)")
									) {
										disabledAPI.append(roleChangeString("Skills (for weight) API disabled"));
									}
									roleAmount = useHighest ? player.getHighestAmount(roleName) : player.getWeight();
									yield roleAmount;
								}
								case "total_slayer" -> useHighest ? player.getHighestAmount(roleName) : player.getTotalSlayer();
								case "maxed_slayers" -> useHighest ? player.getHighestAmount(roleName) : player.getNumMaxedSlayers();
								case "maxed_collections" -> useHighest
									? player.getHighestAmount(roleName)
									: player.getNumMaxedCollections();
								case "healer", "mage", "berserk", "archer", "tank" -> useHighest
									? player.getHighestAmount(roleName)
									: player.getDungeonClass(roleName).getProgressLevel();
								case "mage_rep" -> useHighest ? player.getHighestAmount(roleName) : player.getMageRep();
								case "barbarian_rep" -> useHighest ? player.getHighestAmount(roleName) : player.getBarbarianRep();
								default -> -1;
							};

						if (roleAmount == -1) {
							continue;
						}

						JsonArray levelsArray = collectJsonArray(
							streamJsonArray(higherDepth(roleSettings, "levels"))
								.sorted(Comparator.comparingLong(o -> higherDepth(o, "value").getAsLong()))
						);
						for (int i = levelsArray.size() - 1; i >= 0; i--) {
							JsonElement currentLevel = levelsArray.get(i);
							long currentLevelValue = higherDepth(currentLevel, "value").getAsLong();
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
					}
				}
			}

			EmbedBuilder eb = player
				.defaultPlayerEmbed()
				.setDescription(
					(useHighest ? "**NOTE: Using highest values across all profiles**\n\n" : "") +
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

			return new Object[] { eb, toAdd, toRemove };
		}

		private static String roleChangeString(String name) {
			return "• " + name + "\n";
		}

		private static String roleDeletedString(String name) {
			return "• <@&" + name + "\n";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(updateRoles(event.getOptionStr("profile"), event.getMember()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("claim", "Claim automatic Skyblock roles. The player must be linked to the bot")
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
				} else if (isOneLevelRole(currentRoleName)) {
					paginateBuilder.addStrings(
						"<@&" + higherDepth(roleSettings, "levels.[0].roleId").getAsString() + "> - " + currentRoleName.replace("_", " ")
					);
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
			return new SubcommandData("list", "List all roles that can be claimed through the bot");
		}
	}
}
