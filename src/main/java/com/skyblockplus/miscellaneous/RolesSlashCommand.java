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

import static com.skyblockplus.settings.SettingsExecute.isOneLevelRole;
import static com.skyblockplus.utils.ApiHandler.getGuildFromPlayer;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class RolesSlashCommand extends SlashCommand {

	public RolesSlashCommand() {
		this.name = "roles";
		this.cooldown = globalCooldown + 2;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		switch (event.getSubcommandName()) {
			case "claim" -> event.embed(updateRoles(event.getOptionStr("profile"), event.getMember()));
			case "list" -> event.paginate(listRoles(event));
			default -> event.invalidCommandMessage();
		}
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Main roles command")
			.addSubcommands(
				new SubcommandData("claim", "Claim automatic Skyblock roles. The player must be linked to the bot")
					.addOption(OptionType.STRING, "profile", "Profile name"),
				new SubcommandData("list", "List all roles that can be claimed through the bot")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static Object updateRoles(Player player, Member member) {
		return updateRoles(player, member, database.getRolesSettings(member.getGuild().getId()), false);
	}

	/**
	 * @param skipRoles Whether to skip roles that require extra API requests (guild roles, guild ranks, dungeon secrets)
	 * @return eb or [eb, toAdd, toRemove]
	 */
	public static Object updateRoles(Player player, Member member, JsonElement rolesJson, boolean skipRoles) {
		if (rolesJson == null || rolesJson.isJsonNull()) {
			return invalidEmbed("Unable to fetch roles settings");
		}

		if (!higherDepth(rolesJson, "enable", false)) {
			return invalidEmbed("Automatic roles not setup or enabled for this server");
		}

		Guild guild = member.getGuild();
		List<String> allRoleNames = getJsonKeys(rolesJson);
		allRoleNames.remove("enable");
		Role botRole = guild.getSelfMember().getRoles().get(0);
		if (botRole == null) {
			return invalidEmbed("My role in this server doesn't exist. Please report this to the developer.");
		}
		boolean useHighest = higherDepth(rolesJson, "useHighest", false);

		StringBuilder addedRoles = new StringBuilder();
		StringBuilder removedRoles = new StringBuilder();
		StringBuilder disabledAPI = new StringBuilder();
		StringBuilder errorRoles = new StringBuilder();

		List<Role> toAdd = new ArrayList<>();
		List<Role> toRemove = new ArrayList<>();
		JsonElement guildJson = null;

		//		List<String> skippedRoles = List.of("guild_member", "guild_ranks", "")

		for (String currentRoleName : allRoleNames) {
			JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
			if (!higherDepth(currentRole, "enable", false)) {
				continue;
			}

			if (
				skipRoles &&
				(
					currentRoleName.equals("guild_member") ||
					currentRoleName.equals("guild_ranks") &&
					currentRoleName.equals("dungeon_secrets")
				)
			) {
				continue;
			}

			switch (currentRoleName) {
				case "guild_member" -> {
					if (guildJson == null) {
						HypixelResponse response = getGuildFromPlayer(player.getUuid());
						if (response.isValid()) {
							guildJson = response.response();
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
				}
				case "guild_ranks" -> {
					if (guildJson == null) {
						HypixelResponse response = getGuildFromPlayer(player.getUuid());
						if (response.isValid()) {
							guildJson = response.response();
						}
					}

					if (guildJson != null) {
						JsonArray curLevels = higherDepth(currentRole, "levels").getAsJsonArray();
						List<JsonElement> guildRoles = new ArrayList<>();
						for (JsonElement curLevel : curLevels) {
							guildRoles.add(database.getGuildSettings(guild.getId(), higherDepth(curLevel, "value").getAsString()));
						}

						for (JsonElement guildRoleSettings : guildRoles) {
							if (
								higherDepth(guildRoleSettings, "guildId").getAsString().equals(higherDepth(guildJson, "_id").getAsString())
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
				case "sven",
					"rev",
					"tara",
					"blaze",
					"coins",
					"alchemy",
					"combat",
					"fishing",
					"farming",
					"foraging",
					"carpentry",
					"mining",
					"taming",
					"enchanting",
					"catacombs",
					"healer",
					"mage",
					"berserk",
					"archer",
					"tank",
					"fairy_souls",
					"skill_average",
					"pet_score",
					"dungeon_secrets",
					"slot_collector",
					"level",
					"enderman",
					"weight",
					"total_slayer",
					"accessory_count",
					"networth",
					"maxed_collections",
					"mage_rep",
					"barbarian_rep",
					"slayer_nine" -> {
					double roleAmount = -1;
					switch (currentRoleName) {
						case "sven", "rev", "tara", "enderman", "blaze" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getSlayer(currentRoleName);
						case "coins" -> {
							roleAmount = useHighest ? player.getHighestAmount("bank") : player.getBankBalance();
							if (roleAmount == -1 && !disabledAPI.toString().contains("Banking")) {
								disabledAPI.append(roleChangeString("Banking API disabled"));
							} else {
								roleAmount += useHighest ? player.getHighestAmount("purse") : player.getPurseCoins();
							}
						}
						case "skill_average" -> {
							roleAmount = useHighest ? player.getHighestAmount("skills") : player.getSkillAverage();
							if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
								disabledAPI.append(roleChangeString("Skills API disabled"));
							}
						}
						case "level" -> roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getLevel();
						case "pet_score" -> roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getPetScore();
						case "alchemy", "combat", "fishing", "farming", "foraging", "carpentry", "mining", "taming", "enchanting" -> {
							if (useHighest) {
								roleAmount = player.getHighestAmount(currentRoleName);
							} else if (player.getSkill(currentRoleName) != null) {
								roleAmount = player.getSkill(currentRoleName).getProgressLevel();
							}
							if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
								disabledAPI.append(roleChangeString("Skills API disabled"));
							}
						}
						case "networth" -> roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getNetworth();
						case "catacombs" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getCatacombs().getProgressLevel();
						case "fairy_souls" -> roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getFairySouls();
						case "slot_collector" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getNumberMinionSlots();
						case "dungeon_secrets" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getDungeonSecrets();
						case "accessory_count" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getAccessoryCount();
						case "weight" -> {
							if (!useHighest && !player.isSkillsApiEnabled() && !disabledAPI.toString().contains("Skills (for weight)")) {
								disabledAPI.append(roleChangeString("Skills (for weight) API disabled"));
							}
							roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getWeight();
						}
						case "total_slayer" -> roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getTotalSlayer();
						case "slayer_nine" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getNumLvlNineSlayers();
						case "maxed_collections" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getNumMaxedCollections();
						case "healer", "mage", "berserk", "archer", "tank" -> roleAmount =
							useHighest
								? player.getHighestAmount(currentRoleName)
								: player.getDungeonClass(currentRoleName).getProgressLevel();
						case "mage_rep" -> roleAmount = useHighest ? player.getHighestAmount(currentRoleName) : player.getMageRep();
						case "barbarian_rep" -> roleAmount =
							useHighest ? player.getHighestAmount(currentRoleName) : player.getBarbarianRep();
					}

					if (roleAmount == -1) {
						continue;
					}

					JsonArray levelsArray = collectJsonArray(
						streamJsonArray(higherDepth(currentRole, "levels").getAsJsonArray())
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
									errorRoles.append(roleDeletedString(higherDepth(currentLevelRemoveStackable, "roleId").getAsString()));
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
				case "gamemode" -> {
					JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
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
					JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
					List<String> items = streamJsonArray(levelsArray).map(item -> higherDepth(item, "value").getAsString()).toList();
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

	public static EmbedBuilder updateRoles(String profile, Member member) {
		LinkedAccount linkedInfo = database.getByDiscord(member.getId());
		if (linkedInfo == null) {
			return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
		}

		String username = linkedInfo.username();
		Player player = profile == null ? new Player(username) : new Player(username, profile);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		Object out = updateRoles(player, member);
		if (out instanceof EmbedBuilder eb1) {
			return eb1;
		}

		Object[] outArr = ((Object[]) out);
		try {
			member.getGuild().modifyMemberRoles(member, (List<Role>) outArr[1], (List<Role>) outArr[2]).queue();
		} catch (InsufficientPermissionException e) {
			return invalidEmbed("Missing permission: " + e.getPermission().getName());
		}
		return (EmbedBuilder) outArr[0];
	}

	private static String roleChangeString(String name) {
		return "• " + name + "\n";
	}

	private static String roleDeletedString(String name) {
		return "• <@&" + name + "\n";
	}

	public static EmbedBuilder listRoles(SlashCommandEvent event) {
		JsonElement rolesJson = database.getRolesSettings(event.getGuild().getId());
		if (rolesJson == null || !higherDepth(rolesJson, "enable", false)) {
			return defaultEmbed("Automatic roles not enabled for this server");
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(30);
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
					JsonElement guildRoleSettings = database.getGuildSettings(
						event.getGuild().getId(),
						higherDepth(curLevel, "value").getAsString()
					);
					JsonArray guildRanks = higherDepth(guildRoleSettings, "guildRanks").getAsJsonArray();
					for (JsonElement guildRank : guildRanks) {
						paginateBuilder.addItems("<@&" + higherDepth(guildRank, "roleId").getAsString() + ">");
					}
				}
			} else if (isOneLevelRole(currentRoleName)) {
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
}
