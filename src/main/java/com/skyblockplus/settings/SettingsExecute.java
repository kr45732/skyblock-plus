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

package com.skyblockplus.settings;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.automatedapply.ApplyBlacklist;
import com.skyblockplus.api.serversettings.automatedapply.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRank;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import com.vdurmont.emoji.EmojiParser;
import java.util.*;
import java.util.Map.Entry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class SettingsExecute {

	private final Guild guild;
	private final Message message;
	private final MessageChannel channel;
	private final User author;
	private final String guildPrefix;
	private final JsonObject serverSettings;

	public SettingsExecute(CommandEvent event) {
		this(event.getGuild(), event.getMessage(), event.getChannel(), event.getAuthor());
	}

	public SettingsExecute(Guild guild, MessageReceivedEvent event) {
		this(guild, event.getMessage(), event.getChannel(), event.getAuthor());
	}

	public SettingsExecute(Guild guild, Message message, MessageChannel channel, User author) {
		this.guild = guild;
		this.message = message;
		this.channel = channel;
		this.author = author;
		this.guildPrefix = getGuildPrefix(guild.getId());

		if (!database.serverByServerIdExists(guild.getId())) {
			database.addNewServerSettings(guild.getId(), new ServerSettingsModel(guild.getName(), guild.getId()));
		}
		this.serverSettings = database.getServerSettings(guild.getId()).getAsJsonObject();
	}

	public void execute(Command command, CommandEvent event) {
		new CommandExecute(command, event) {
			@Override
			protected void execute() {
				String content = message.getContentRaw();
				if (!content.contains("hypixel_key")) {
					logCommand();
				}

				JsonElement currentSettings = database.getServerSettings(guild.getId());
				if (content.split(" ", 4).length == 4 && args[1].equals("set")) {
					if (args[2].equals("hypixel_key")) {
						eb = setHypixelKey(args[3]);
					} else if (args[2].equals("prefix")) {
						eb = setPrefix(content.split(" ", 4)[3]);
					}
				} else if (
					(args.length == 4 || args.length == 5 || content.split(" ", 6).length == 6) &&
					args[1].equals("apply") &&
					args[2].equals("blacklist")
				) {
					args = content.split(" ", 6);
					if (args.length == 4 && args[3].equals("list")) {
						eb = listApplyBlacklist();
					} else if ((args.length == 5 || args.length == 6) && args[3].equals("add")) {
						eb = addApplyBlacklist(args[4], args.length == 6 ? args[5] : "not provided");
					} else if (args.length == 5 && args[3].equals("remove")) {
						eb = removeApplyBlacklist(args[4]);
					}
				} else if (args.length >= 2 && args[1].equals("mee6")) {
					if (args.length == 2) {
						eb = getMee6DataSettings();
					} else if (args.length == 3) {
						if (args[2].equals("enable")) {
							eb = setMee6Enable(true);
						} else if (args[2].equals("disable")) {
							eb = setMee6Enable(false);
						}
					} else if (args.length == 4 && args[2].equals("remove")) {
						eb = removeMee6Role(args[3]);
					} else if (args.length == 5 && args[2].equals("add")) {
						eb = addMee6Role(args[3], args[4]);
					}

					if (eb == null) {
						eb = errorEmbed("settings mee6");
					}
				} else if (args.length == 3 && args[1].equals("delete")) {
					switch (args[2]) {
						case "all":
							if (database.deleteServerSettings(guild.getId()) == 200) {
								eb = defaultEmbed("Success").setDescription("Server settings deleted");
							} else {
								eb = invalidEmbed("Error deleting server settings");
							}
							break;
						case "hypixel_key":
							eb = deleteHypixelKey();
							break;
						case "prefix":
							eb = resetPrefix();
							break;
					}

					if (eb == null) {
						eb = errorEmbed("settings delete");
					}
				} else if (args.length == 1) {
					eb = defaultEmbed("Settings");
					eb.addField("Verify Settings", "Use `" + guildPrefix + "settings verify` to see the current settings", false);
					eb.addField("Apply Settings", "Use `" + guildPrefix + "settings apply` to see the current settings", false);
					eb.addField("Roles Settings", "Use `" + guildPrefix + "settings roles` to see the current settings", false);
					eb.addField("Guild Role/Ranks Settings", "Use `" + guildPrefix + "settings guild` to see the current settings", false);
					eb.addField("Mee6 Roles Settings", "Use `" + guildPrefix + "settings mee6` to see the current settings", false);
				} else if (args.length >= 2 && args[1].equals("roles")) {
					if (args.length == 2) {
						if (higherDepth(currentSettings, "automatedRoles") != null) {
							ebMessage.delete().queue();
							getCurrentRolesSettings(higherDepth(currentSettings, "automatedRoles")).build().paginate(channel, 0);
							return;
						} else {
							eb = defaultEmbed("Settings").addField("Roles Settings", "Error! Data not found", false);
						}
					} else if (args.length == 3) {
						if (args[2].equals("enable")) {
							if (allowRolesEnable()) {
								eb = setRolesEnable("true");
							} else {
								eb = invalidEmbed("No roles enabled");
							}
						} else if (args[2].equals("disable")) {
							eb = setRolesEnable("false");
						} else {
							eb = getCurrentRoleSettings(args[2]);
							if (eb == null) {
								ebMessage.delete().queue();
								return;
							}
						}
					} else if (args.length == 4) {
						if (args[2].equals("enable")) {
							eb = setRoleEnable(args[3], "true");
						} else if (args[2].equals("disable")) {
							eb = setRoleEnable(args[3], "false");
						}
					} else if (args.length == 5) {
						if (args[2].equals("remove")) {
							eb = removeRoleLevel(args[3], args[4]);
						} else if (args[2].equals("set")) {
							eb = setOneLevelRole(args[3], args[4]);
						} else if (args[2].equals("add") && args[3].equals("guild_ranks")) {
							eb = addRoleLevel(args[3], args[4], null);
						}
					} else if (args.length == 6 && args[2].equals("add")) {
						eb = addRoleLevel(args[3], args[4], args[5]);
					}

					if (eb == null) {
						eb = errorEmbed("settings roles");
					}
				} else if (content.split(" ", 5).length >= 2 && content.split(" ", 5)[1].equals("apply")) {
					args = content.split(" ", 5);
					if (args.length == 2) {
						eb = defaultEmbed("Settings");
						eb.addField(
							"Automatic Apply One",
							(
								higherDepth(currentSettings, "automatedApplicationOne.name") != null
									? "Name: " +
									higherDepth(currentSettings, "automatedApplicationOne.name").getAsString() +
									"\nCommand: `" +
									guildPrefix +
									"settings apply " +
									higherDepth(currentSettings, "automatedApplicationOne.name").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
						eb.addField(
							"Automatic Apply Two",
							(
								higherDepth(currentSettings, "automatedApplicationTwo.name") != null
									? "Name: " +
									higherDepth(currentSettings, "automatedApplicationTwo.name").getAsString() +
									"\nCommand: `" +
									guildPrefix +
									"settings apply " +
									higherDepth(currentSettings, "automatedApplicationTwo.name").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
					} else if (args.length == 3) {
						JsonElement applySettings = database.getApplySettings(guild.getId(), args[2]);
						if (applySettings != null && !applySettings.isJsonNull()) {
							eb = getCurrentApplySettings(applySettings);
						}
					} else if (args.length == 4) {
						if (args[2].equals("create")) {
							eb = createApplyGuild(args[3]);
						} else if (args[2].equals("delete")) {
							eb = deleteApplyGuild(args[3]);
						} else {
							JsonElement applySettings = database.getApplySettings(guild.getId(), args[2]);
							if (applySettings != null && !applySettings.isJsonNull()) {
								if (args[3].equals("enable")) {
									if (allowApplyEnable(args[2])) {
										eb = setApplyEnable(args[2], "true");
									} else {
										eb = invalidEmbed("All other apply settings must be set before " + "enabling apply!");
									}
								} else if (args[3].equals("disable")) {
									eb = setApplyEnable(args[2], "false");
								}
							}
						}
					} else if (args.length == 5) {
						JsonElement applySettings = database.getApplySettings(guild.getId(), args[2]);
						if (applySettings != null && !applySettings.isJsonNull()) {
							switch (args[3]) {
								case "message":
									eb = setApplyMessageText(args[2], args[4]);
									break;
								case "staff_role":
									eb = setApplyStaffPingRoleId(args[2], args[4]);
									break;
								case "channel":
									eb = setApplyMessageTextChannelId(args[2], args[4]);
									break;
								case "category":
									eb = setApplyNewChannelCategory(args[2], args[4]);
									break;
								case "staff_channel":
									eb = setApplyMessageStaffChannelId(args[2], args[4]);
									break;
								case "accept_message":
									eb = setApplyAcceptMessageText(args[2], args[4]);
									break;
								case "waitlist_message":
									eb = setApplyWaitListMessageText(args[2], args[4]);
									break;
								case "deny_message":
									eb = setApplyDenyMessageText(args[2], args[4]);
									break;
								case "reqs":
								case "req":
								case "requirements":
									args = content.split(" ");
									if (args.length >= 6) {
										if (args[4].equals("add")) {
											eb = addApplyRequirement(args[2], content.split(" ", 6)[5]);
										} else if (args[4].equals("remove")) {
											eb = removeApplyRequirement(args[2], args[5]);
										}
									}
									break;
								case "ironman":
									eb = setIsIronman(args[2], args[4]);
									break;
								case "waiting_channel":
									eb = setWaitingChannel(args[2], args[4]);
									break;
							}
						}
					}

					if (eb == null) {
						eb = errorEmbed("settings apply");
					}
				} else if (content.split(" ", 4).length >= 2 && content.split(" ", 4)[1].equals("verify")) {
					args = content.split(" ", 4);
					if (args.length == 2) {
						eb = defaultEmbed("Settings");
						if (higherDepth(currentSettings, "automatedVerify") != null) {
							eb.setDescription(getCurrentVerifySettings(higherDepth(currentSettings, "automatedVerify")));
						} else {
							eb.setDescription("Error! Data not found");
						}
					} else if (args.length == 3) {
						if (args[2].equals("enable")) {
							if (allowVerifyEnable()) {
								eb = setVerifyEnable("true");
							} else {
								eb = invalidEmbed("All other verify settings must be set before " + "enabling verify!");
							}
						} else if (args[2].equals("disable")) {
							eb = setVerifyEnable("false");
						}
					} else if (args.length == 4) {
						switch (args[2]) {
							case "message":
								eb = setVerifyMessageText(args[3]);
								break;
							case "channel":
								eb = setVerifyMessageTextChannelId(args[3]);
								break;
							case "nickname":
								eb = setVerifyNickname(args[3]);
								break;
							case "role":
								args = content.split(" ");
								if (args[3].equals("add")) {
									eb = addVerifyRole(args[4]);
								} else if (args[3].equals("remove")) {
									eb = removeVerifyRole(args[4]);
								} else {
									eb = invalidEmbed("Invalid setting");
								}
								break;
							case "enable":
								if (args[3].equals("sync")) {
									eb = setVerifySyncEnable("true");
								}
								break;
							case "disable":
								if (args[3].equals("sync")) {
									eb = setVerifySyncEnable("false");
								}
								break;
						}
					}

					if (eb == null) {
						eb = errorEmbed("settings verify");
					}
				} else if ((args.length >= 2) && args[1].equals("guild")) {
					if (args.length == 2) {
						eb = defaultEmbed("Settings");
						eb.addField(
							"Guild Roles One",
							(
								higherDepth(currentSettings, "automaticGuildRolesOne.name") != null
									? "Name: " +
									higherDepth(currentSettings, "automaticGuildRolesOne.name").getAsString() +
									"\nCommand: `" +
									guildPrefix +
									"settings guild " +
									higherDepth(currentSettings, "automaticGuildRolesOne.name").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
						eb.addField(
							"Guild Roles Two",
							(
								higherDepth(currentSettings, "automaticGuildRolesTwo.name") != null
									? "Name: " +
									higherDepth(currentSettings, "automaticGuildRolesTwo.name").getAsString() +
									"\nCommand: `" +
									guildPrefix +
									"settings guild " +
									higherDepth(currentSettings, "automaticGuildRolesTwo.name").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
					} else if (args.length == 3) {
						eb = getCurrentGuildRoleSettings(args[2]);
					} else if (args.length == 4) {
						if (args[2].equals("create")) {
							eb = createGuildRoles(args[3]);
						}
					} else if (args.length == 5) {
						JsonElement guildRoleSettings = database.getGuildRoleSettings(guild.getId(), args[2]);
						if (guildRoleSettings != null && !guildRoleSettings.isJsonNull()) {
							switch (args[3]) {
								case "set":
									eb = setGuildRoleId(args[2], args[4]);
									break;
								case "role":
									eb = setGuildRoleName(args[2], args[4]);
									break;
								case "enable":
									switch (args[4]) {
										case "role":
											eb = setGuildRoleEnable(args[2], true);
											break;
										case "rank":
											eb = setGuildRankEnable(args[2], true);
											break;
										case "counter":
											eb = setGuildCounterEnable(args[2], true);
											break;
									}
									break;
								case "disable":
									switch (args[4]) {
										case "role":
											eb = setGuildRoleEnable(args[4], false);
											break;
										case "rank":
											eb = setGuildRankEnable(args[4], false);
											break;
										case "counter":
											eb = setGuildCounterEnable(args[2], false);
											break;
									}
									break;
								case "remove":
									eb = removeGuildRank(args[2], args[4]);
									break;
							}
						}
					} else if (args.length == 6) {
						JsonElement guildRoleSettings = database.getGuildRoleSettings(guild.getId(), args[2]);
						if (guildRoleSettings == null || guildRoleSettings.isJsonNull()) {
							eb = invalidEmbed("Invalid name");
						} else if (args[3].equals("add")) {
							eb = addGuildRank(args[2], args[4], args[5]);
						}
					}

					if (eb == null) {
						eb = errorEmbed("settings guild");
					}
				}

				if (eb == null) {
					eb = errorEmbed("settings");
				}

				embed(eb);
			}
		}
			.submit();
	}

	private EmbedBuilder removeApplyBlacklist(String username) {
		UsernameUuidStruct uuidStruct = usernameToUuid(username);
		if (uuidStruct.isNotValid()) {
			return invalidEmbed(uuidStruct.getFailCause());
		}

		JsonArray currentBlacklist = database.getApplyBlacklist(guild.getId());
		for (int i = 0; i < currentBlacklist.size(); i++) {
			if (
				higherDepth(currentBlacklist.get(i), "uuid").getAsString().equals(uuidStruct.getUuid()) ||
				higherDepth(currentBlacklist.get(i), "username").getAsString().equals(uuidStruct.getUsername())
			) {
				currentBlacklist.remove(i);
				int responseCode = database.setApplyBlacklist(guild.getId(), currentBlacklist);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed()
					.setDescription("Removed " + nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) + " from the blacklist");
			}
		}

		return invalidEmbed("" + nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) + " is not blacklisted");
	}

	private EmbedBuilder listApplyBlacklist() {
		JsonArray currentBlacklist = database.getApplyBlacklist(guild.getId());
		EmbedBuilder eb = defaultSettingsEmbed();
		if (currentBlacklist.size() == 0) {
			return eb.setDescription("No one is blacklisted.");
		}

		for (JsonElement blacklisted : currentBlacklist) {
			eb.appendDescription(
				"• " +
				nameMcHyperLink(higherDepth(blacklisted, "username").getAsString(), higherDepth(blacklisted, "uuid").getAsString()) +
				" - " +
				higherDepth(blacklisted, "reason").getAsString() +
				"\n"
			);
		}
		return eb;
	}

	public EmbedBuilder addApplyBlacklist(String username, String reason) {
		UsernameUuidStruct uuidStruct = usernameToUuid(username);
		if (uuidStruct.isNotValid()) {
			return invalidEmbed(uuidStruct.getFailCause());
		}

		JsonArray currentBlacklist = database.getApplyBlacklist(guild.getId());
		JsonElement blacklistedUser = streamJsonArray(currentBlacklist)
			.filter(blacklist ->
				higherDepth(blacklist, "uuid").getAsString().equals(uuidStruct.getUuid()) ||
				higherDepth(blacklist, "username").getAsString().equals(uuidStruct.getUsername())
			)
			.findFirst()
			.orElse(null);
		if (blacklistedUser != null) {
			return invalidEmbed(
				nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) +
				" is already blacklisted with reason `" +
				higherDepth(blacklistedUser, "reason").getAsString() +
				"`"
			);
		}

		currentBlacklist.add(gson.toJsonTree(new ApplyBlacklist(uuidStruct.getUsername(), uuidStruct.getUuid(), reason)));
		int responseCode = database.setApplyBlacklist(guild.getId(), currentBlacklist);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed()
			.setDescription(
				"Blacklisted " + nameMcHyperLink(uuidStruct.getUsername(), uuidStruct.getUuid()) + " with reason `" + reason + "`"
			);
	}

	public String nameMcHyperLink(String username, String uuid) {
		return "[**" + username + "**](https://mine.ly/" + uuid + ")";
	}

	/* Guild Role Settings */
	public EmbedBuilder getCurrentGuildRoleSettings(String name) {
		JsonElement settings = database.getGuildRoleSettings(guild.getId(), name);
		if (settings == null || settings.isJsonNull()) {
			return defaultSettingsEmbed().setDescription("Error! Data not found. Please report this to the developer.");
		}

		String settingsString =
			"**" +
			displaySettings(settings, "enableGuildRole") +
			"**" +
			"\n**• Guild Name:** " +
			displaySettings(settings, "guildId") +
			"\n**• Guild Role:** " +
			displaySettings(settings, "roleId") +
			"\n\n**" +
			displaySettings(settings, "enableGuildRanks") +
			"**";

		StringBuilder guildRanksString = new StringBuilder();
		for (JsonElement guildRank : higherDepth(settings, "guildRanks").getAsJsonArray()) {
			guildRanksString
				.append("\n• ")
				.append(higherDepth(guildRank, "minecraftRoleName").getAsString())
				.append(" - ")
				.append("<@&")
				.append(higherDepth(guildRank, "discordRoleId").getAsString())
				.append(">");
		}
		settingsString += guildRanksString.length() > 0 ? guildRanksString.toString() : "\n• No guild ranks set";

		return defaultSettingsEmbed().setDescription(settingsString);
	}

	public EmbedBuilder setGuildRoleEnable(String name, boolean enable) {
		JsonObject settings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();

		if (!enable) {
			settings.addProperty("enableGuildRole", "false");
			int responseCode = database.setGuildRoleSettings(guild.getId(), settings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed().setDescription(("Disabled") + " automatic guild role.");
		}

		if (higherDepth(settings, "guildId", "").isEmpty() || higherDepth(settings, "roleId", "").isEmpty()) {
			return invalidEmbed("The guild name and role must be set.");
		}

		settings.addProperty("enableGuildRole", "true");
		int responseCode = database.setGuildRoleSettings(guild.getId(), settings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed().setDescription("Enabled automatic guild role.");
	}

	public EmbedBuilder setGuildRankEnable(String name, boolean enable) {
		JsonObject settings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();

		if (!enable) {
			settings.addProperty("enableGuildRanks", "false");
			int responseCode = database.setGuildRoleSettings(guild.getId(), settings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed().setDescription("Disabled automatic guild ranks.");
		}

		if ((higherDepth(settings, "guildId", "").isEmpty()) || (higherDepth(settings, "guildRanks").getAsJsonArray().size() == 0)) {
			return invalidEmbed("The guild name and a at least one guild rank must be set.");
		}

		settings.addProperty("enableGuildRanks", "true");
		int responseCode = database.setGuildRoleSettings(guild.getId(), settings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultEmbed("Settings").setDescription("Enabled automatic guild ranks.");
	}

	public EmbedBuilder setGuildCounterEnable(String name, boolean enable) {
		JsonObject settings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();

		if (!enable) {
			try {
				guild.getVoiceChannelById(settings.get("guildUserChannelId").getAsString()).delete().complete();
			} catch (Exception ignored) {}

			settings.addProperty("enableGuildUserCount", "false");
			int responseCode = database.setGuildRoleSettings(guild.getId(), settings);
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed().setDescription("Disabled automatic guild member counter.");
		}

		if (higherDepth(settings, "guildId", "").isEmpty()) {
			return defaultEmbed("Guild name must be set before enabling.");
		}

		HypixelResponse guildJson = getGuildFromId(higherDepth(settings, "guildId").getAsString());
		if (guildJson.isNotValid()) {
			return invalidEmbed(guildJson.getFailCause());
		}

		VoiceChannel guildMemberCounterChannel = guild
			.createVoiceChannel(
				guildJson.get("name").getAsString() + " Members: " + guildJson.get("members").getAsJsonArray().size() + "/125"
			)
			.addPermissionOverride(guild.getPublicRole(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
			.addMemberPermissionOverride(
				jda.getSelfUser().getIdLong(),
				EnumSet.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT),
				null
			)
			.complete();
		settings.addProperty("enableGuildUserCount", "true");
		settings.addProperty("guildUserChannelId", guildMemberCounterChannel.getId());

		int responseCode = database.setGuildRoleSettings(guild.getId(), settings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed().setDescription("Enabled automatic guild member counter.");
	}

	public EmbedBuilder addGuildRank(String name, String rankName, String roleMention) {
		Role role;
		try {
			role = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((role.isPublicRole() || role.isManaged())) {
				return invalidEmbed("The role cannot be managed or @everyone.");
			}
		} catch (Exception e) {
			return invalidEmbed("The provided role does not exist.");
		}

		JsonObject currentSettings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();

		if (higherDepth(currentSettings, "guildId", "").isEmpty()) {
			return invalidEmbed("The guild name must set.");
		}

		String guildId = higherDepth(currentSettings, "guildId").getAsString();

		HypixelResponse guildJson = getGuildFromId(guildId);

		if (guildJson.isNotValid()) {
			return invalidEmbed(guildJson.getFailCause());
		}

		JsonArray guildRanks = higherDepth(guildJson.getResponse(), "ranks").getAsJsonArray();

		StringBuilder guildRanksString = new StringBuilder();
		for (JsonElement guildRank : guildRanks) {
			guildRanksString.append("\n• ").append(higherDepth(guildRank, "name").getAsString().replace(" ", "_"));
			if (higherDepth(guildRank, "name").getAsString().equalsIgnoreCase(rankName.replace("_", " "))) {
				JsonArray currentGuildRanks = currentSettings.get("guildRanks").getAsJsonArray();

				for (JsonElement level : currentGuildRanks) {
					if (higherDepth(level, "minecraftRoleName").getAsString().equalsIgnoreCase(rankName)) {
						currentGuildRanks.remove(level);
						break;
					}
				}

				currentGuildRanks.add(gson.toJsonTree(new GuildRank(rankName.toLowerCase(), role.getId())));

				currentSettings.add("guildRanks", currentGuildRanks);

				int responseCode = database.setGuildRoleSettings(guild.getId(), currentSettings);
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Guild rank added:** " + higherDepth(guildRank, "name").getAsString() + " - " + role.getAsMention());
				return eb;
			}
		}

		return defaultEmbed("Invalid guild rank")
			.setDescription((guildRanksString.length() > 0 ? "Valid guild ranks are: " + guildRanksString : "No guild ranks found"));
	}

	public EmbedBuilder removeGuildRank(String name, String rankName) {
		JsonObject currentSettings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();
		JsonArray currentGuildRanks = currentSettings.get("guildRanks").getAsJsonArray();

		for (JsonElement guildRank : currentGuildRanks) {
			if (higherDepth(guildRank, "minecraftRoleName").getAsString().equalsIgnoreCase(rankName)) {
				JsonArray currentGuildRanksTemp = currentSettings.get("guildRanks").getAsJsonArray();
				currentGuildRanksTemp.remove(guildRank);

				if (currentGuildRanksTemp.size() == 0) {
					currentSettings.addProperty("enableGuildRanks", "false");
				}

				currentSettings.add("guildRanks", currentGuildRanksTemp);

				int responseCode = database.setGuildRoleSettings(guild.getId(), currentSettings);
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Guild rank removed:** " + rankName);
				return eb;
			}
		}

		return invalidEmbed("Invalid rank name");
	}

	public EmbedBuilder setGuildRoleId(String name, String guildName) {
		HypixelResponse guildJson = getGuildFromName(guildName);
		if (guildJson.isNotValid()) {
			return invalidEmbed(guildJson.getFailCause());
		}

		String guildId = guildJson.get("_id").getAsString();
		JsonObject currentSettings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();
		currentSettings.addProperty("guildId", guildId);
		int responseCode = database.setGuildRoleSettings(guild.getId(), currentSettings);
		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("**Guild set to:** " + guildJson.get("name").getAsString());
		return eb;
	}

	public EmbedBuilder setGuildRoleName(String name, String roleMention) {
		try {
			Role verifyGuildRole = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
				JsonObject currentSettings = database.getGuildRoleSettings(guild.getId(), name).getAsJsonObject();
				currentSettings.addProperty("roleId", verifyGuildRole.getId());
				int responseCode = database.setGuildRoleSettings(guild.getId(), currentSettings);

				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Guild role set to:** " + verifyGuildRole.getAsMention());
				return eb;
			}
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Role");
	}

	public EmbedBuilder createGuildRoles(String name) {
		if (name.length() > 25) {
			return invalidEmbed("Name cannot be more than 25 letters");
		}

		if (name.contains(" ")) {
			return invalidEmbed("Name cannot contain spaces");
		}

		List<GuildRole> currentGuildRoles = database.getAllGuildRoles(guild.getId());
		currentGuildRoles.removeIf(o1 -> o1.getGuildId() == null);

		if (currentGuildRoles.size() == 2) {
			return invalidEmbed("You can reached the max amount of automatic guilds (2/2)");
		}

		for (GuildRole currentGuildRole : currentGuildRoles) {
			if (currentGuildRole.getName() != null && currentGuildRole.getName().equalsIgnoreCase(name)) {
				return invalidEmbed(name + " name is taken");
			}
		}

		GuildRole newGuildRole = new GuildRole(name);

		int responseCode = database.setGuildRoleSettings(guild.getId(), newGuildRole);
		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Created new guild role with name `" + name + "`");
		return eb;
	}

	/* Roles Settings */
	public EmbedBuilder getCurrentRoleSettings(String roleName) {
		Map<String, Integer> rolePageMap = new HashMap<>();
		rolePageMap.put("sven", 2);
		rolePageMap.put("rev", 3);
		rolePageMap.put("tara", 4);
		rolePageMap.put("bank_coins", 5);
		rolePageMap.put("alchemy", 6);
		rolePageMap.put("combat", 7);
		rolePageMap.put("fishing", 8);
		rolePageMap.put("farming", 9);
		rolePageMap.put("foraging", 10);
		rolePageMap.put("carpentry", 11);
		rolePageMap.put("mining", 12);
		rolePageMap.put("taming", 13);
		rolePageMap.put("enchanting", 14);
		rolePageMap.put("catacombs", 15);
		rolePageMap.put("guild_member", 16);
		rolePageMap.put("fairy_souls", 17);
		rolePageMap.put("slot_collector", 18);
		rolePageMap.put("pet_enthusiast", 19);
		rolePageMap.put("doom_slayer", 20);
		rolePageMap.put("all_slayer_nine", 21);
		rolePageMap.put("skill_average", 22);
		rolePageMap.put("pet_score", 23);
		rolePageMap.put("dungeon_secrets", 24);
		rolePageMap.put("guild_ranks", 25);
		rolePageMap.put("enderman", 26);
		rolePageMap.put("weight", 27);

		if (rolePageMap.containsKey(roleName)) {
			CustomPaginator.Builder currentRoleSettings = getCurrentRolesSettings(database.getRolesSettings(guild.getId()));
			currentRoleSettings.build().paginate(channel, rolePageMap.get(roleName));
			return null;
		} else {
			try {
				if (rolePageMap.containsValue(Integer.parseInt(roleName))) {
					CustomPaginator.Builder currentRoleSettings = getCurrentRolesSettings(database.getRolesSettings(guild.getId()));
					currentRoleSettings.build().paginate(channel, rolePageMap.get(roleName));
					return null;
				}
			} catch (Exception ignored) {}
		}

		return invalidEmbed("Invalid role name");
	}

	public CustomPaginator.Builder getCurrentRolesSettings(JsonElement rolesSettings) {
		CustomPaginator.Builder paginateBuilder = defaultPaginator(author).setColumns(1).setItemsPerPage(1);

		ArrayList<String> pageTitles = new ArrayList<>();
		pageTitles.add("Roles Settings");

		ArrayList<String> roleNames = getJsonKeys(rolesSettings);

		StringBuilder pageNumbers = new StringBuilder();
		for (int i = 1; i < roleNames.size(); i++) {
			pageNumbers.append("\n**Page ").append(i + 1).append(":** ").append(roleNames.get(i));
		}

		paginateBuilder.addItems(
			"**Automated Roles " +
			(higherDepth(rolesSettings, "enable").getAsString().equals("true") ? "Enabled" : "Disabled") +
			"**" +
			pageNumbers
		);
		roleNames.remove("enable");
		for (String roleName : roleNames) {
			JsonElement currentRoleSettings = higherDepth(rolesSettings, roleName);
			StringBuilder ebFieldString = new StringBuilder();

			if (higherDepth(currentRoleSettings, "enable") == null) {
				database.setRoleSettings(guild.getId(), roleName, gson.toJsonTree(new RoleModel()));
				currentRoleSettings = database.getRoleSettings(guild.getId(), roleName);
			}

			switch (roleName) {
				case "guild_member":
					{
						ebFieldString
							.append("**Member role for Hypixel guilds**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add guild_member skyblock_forceful @sbf guild member`\n");
						break;
					}
				case "sven":
					{
						ebFieldString
							.append("**A player's sven packmaster slayer xp**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add sven 1000000 @sven 9`\n");
						break;
					}
				case "rev":
					{
						ebFieldString
							.append("**A player's revenant horror xp slayer**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add rev 400000 @rev 8`\n");
						break;
					}
				case "tara":
					{
						ebFieldString
							.append("**A player's tarantula broodfather slayer xp**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add tara 100000 @tara 7`\n");
						break;
					}
				case "bank_coins":
					{
						ebFieldString
							.append("**Coins in a player's bank**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add bank_coins 1000000 @millionaire`\n");
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
				case "skill_average":
				case "pet_score":
				case "catacombs":
					{
						ebFieldString
							.append("**A player's ")
							.append(roleName)
							.append(" level**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add ")
							.append(roleName)
							.append(" 30 @")
							.append(roleName)
							.append(" 30`\n");
						break;
					}
				case "fairy_souls":
					{
						ebFieldString
							.append("**Amount of collected fairy souls**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add fairy_souls 50 @50 souls collected`\n");
						break;
					}
				case "slot_collector":
					{
						ebFieldString
							.append("**Number of minion slots excluding upgrades (__not fully working__)**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add slot_collector 24 @maxed minion slots`\n");
						break;
					}
				case "pet_enthusiast":
					{
						ebFieldString
							.append("**Having a level 100 epic or legendary pet that is not an enchanting or alchemy pet**\nExample: `")
							.append(guildPrefix)
							.append("settings roles set pet_enthusiast @level 100 pet`\n");
						break;
					}
				case "doom_slayer":
					{
						ebFieldString
							.append("**Having at least one level nine slayer**\nExample: `")
							.append(guildPrefix)
							.append("settings roles set doom_slayer @level nine slayer`\n");
						break;
					}
				case "guild_ranks":
					{
						ebFieldString
							.append("**If a player is in the guild set in `")
							.append(guildPrefix)
							.append(
								"settings guild`, they will be given the corresponding rank role set there**\nNote: this role can only be enabled, disabled, and linked here. To modify guild ranks use `"
							)
							.append(guildPrefix)
							.append("settings guild [name]`");
						break;
					}
				case "all_slayer_nine":
					{
						ebFieldString
							.append("**Having all level nine slayers**\nExample: `")
							.append(guildPrefix)
							.append("settings roles set all_slayer_nine @role`\n");
						break;
					}
				case "dungeon_secrets":
					{
						ebFieldString
							.append("**A player's dungeon secrets count**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add dungeon_secrets 25000 @secret sweat`\n");
						break;
					}
				case "enderman":
					{
						ebFieldString
							.append("**A player's voidgloom seraph slayer xp**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add enderman 100000 @enderman 7`\n");
						break;
					}
				case "weight":
					{
						ebFieldString
							.append("**A player's weight**\nExample: `")
							.append(guildPrefix)
							.append("settings roles add weight 5000 @5k weight`\n");
						break;
					}
				case "slayer":
					ebFieldString
						.append("**A player's total slayer**\nExample: `")
						.append(guildPrefix)
						.append("settings roles add total_slayer 1000000 @1m slayer`\n");
					break;
			}

			ebFieldString.append("\nCurrent Settings:\n");

			ebFieldString.append(
				higherDepth(currentRoleSettings, "enable") != null &&
					higherDepth(currentRoleSettings, "enable").getAsString().equals("true")
					? "• Enabled"
					: "• Disabled"
			);
			if (roleName.equals("guild_ranks")) {
				if (higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() == 0) {
					ebFieldString.append("\n• No ranks added");
				} else {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						String rName = higherDepth(roleLevel, "value").getAsString();
						ebFieldString
							.append("\n• ")
							.append(rName)
							.append(" (view the ranks in ")
							.append(guildPrefix)
							.append("`settings guild ")
							.append(rName)
							.append("`)");
					}
				}
				pageTitles.add(roleName);
			} else if (isOneLevelRole(roleName)) {
				try {
					ebFieldString
						.append("\n• default - ")
						.append("<@&")
						.append(higherDepth(higherDepth(currentRoleSettings, "levels").getAsJsonArray().get(0), "roleId").getAsString())
						.append(">");
				} catch (Exception ignored) {}
				pageTitles.add(roleName + " (__one level role__)");
			} else {
				if (roleName.equals("guild_member")) {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						String guildId = higherDepth(roleLevel, "value").getAsString();
						HypixelResponse guildJson = getGuildFromId(guildId);
						if (!guildJson.isNotValid()) {
							ebFieldString
								.append("\n• ")
								.append(guildJson.get("name").getAsString())
								.append(" - ")
								.append("<@&")
								.append(higherDepth(roleLevel, "roleId").getAsString())
								.append(">");
						} else {
							ebFieldString
								.append("\n• ")
								.append("Invalid guild")
								.append(" - ")
								.append("<@&")
								.append(higherDepth(roleLevel, "roleId").getAsString())
								.append(">");
						}
					}
				} else {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						ebFieldString
							.append("\n• ")
							.append(higherDepth(roleLevel, "value").getAsString())
							.append(" - ")
							.append("<@&")
							.append(higherDepth(roleLevel, "roleId").getAsString())
							.append(">");
					}
				}

				if (higherDepth(currentRoleSettings, "levels").getAsJsonArray().size() == 0) {
					ebFieldString.append("\n• No levels set");
				}

				pageTitles.add(roleName);
			}
			paginateBuilder.addItems(ebFieldString.toString());
		}

		return paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
	}

	public boolean allowRolesEnable() {
		JsonObject currentSettings = database.getRolesSettings(guild.getId()).getAsJsonObject();
		currentSettings.remove("enable");
		for (String role : getJsonKeys(currentSettings)) {
			if (higherDepth(currentSettings, role + ".enable").getAsBoolean()) {
				return true;
			}
		}
		return false;
	}

	public EmbedBuilder setRolesEnable(String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			JsonObject newRolesJson = database.getRolesSettings(guild.getId()).getAsJsonObject();
			newRolesJson.addProperty("enable", enable);
			int responseCode = database.setRolesSettings(guild.getId(), newRolesJson);
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Roles:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setRoleEnable(String roleName, String enable) {
		JsonObject currentRoleSettings = null;
		try {
			currentRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception ignored) {}

		if (currentRoleSettings == null) {
			return invalidEmbed("Invalid role name");
		}

		if (currentRoleSettings.get("levels").getAsJsonArray().size() != 0) {
			currentRoleSettings.addProperty("enable", enable);
			int responseCode = database.setRoleSettings(guild.getId(), roleName, currentRoleSettings);
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**" + roleName + " role:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
			return eb;
		} else {
			currentRoleSettings.addProperty("enable", "false");
			database.setRoleSettings(guild.getId(), roleName, currentRoleSettings);
		}
		return invalidEmbed("Specified role must have at least one configuration!");
	}

	public EmbedBuilder addRoleLevel(String roleName, String roleValue, String roleMention) {
		String guildName = "";
		if (roleName.equals("guild_member")) {
			HypixelResponse guildJson = getGuildFromName(roleValue);
			if (guildJson.isNotValid()) {
				return invalidEmbed(guildJson.getFailCause());
			}
			roleValue = guildJson.get("_id").getAsString();
			guildName = guildJson.get("name").getAsString();
		} else if (roleName.equals("guild_ranks")) {
			JsonObject newRoleSettings = database.getRoleSettings(guild.getId(), "guild_ranks").getAsJsonObject();
			JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

			if (currentLevels.size() >= 2) {
				return invalidEmbed("This role has reached the max limit of levels (2/2)");
			}
			JsonElement guildRoleSettings = database.getGuildRoleSettings(guild.getId(), roleValue);
			if (
				guildRoleSettings != null &&
				!guildRoleSettings.isJsonNull() &&
				higherDepth(guildRoleSettings, "enableGuildRanks").getAsString().equalsIgnoreCase("true")
			) {
				for (JsonElement level : currentLevels) {
					if (higherDepth(level, "value").getAsString().equals(roleValue)) {
						currentLevels.remove(level);
						break;
					}
				}

				currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, roleValue)));

				newRoleSettings.add("levels", currentLevels);

				int responseCode = database.setRoleSettings(guild.getId(), roleName, newRoleSettings);
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				return defaultEmbed("Settings").setDescription("Added guild ranks for guild roles with name `" + roleValue + "`");
			}

			return invalidEmbed("Invalid guild role name or guild ranks not enabled");
		} else if (isOneLevelRole(roleName)) {
			return defaultEmbed(
				"These roles do not support levels. Use `" + guildPrefix + "settings roles set [roleName] [@role]` instead"
			);
		} else {
			try {
				Integer.parseInt(roleValue);
			} catch (Exception e) {
				return invalidEmbed("Role value must be an integer");
			}
		}

		Role role = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
		if (role == null) {
			return invalidEmbed("Invalid role mention");
		}

		if (role.isPublicRole() || role.isManaged()) {
			return invalidEmbed("Role cannot be managed or @everyone!");
		}
		JsonObject newRoleSettings;
		try {
			newRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed("Invalid role");
		}

		int totalRoleCount = 0;
		JsonObject allRoleSettings = database.getRolesSettings(guild.getId()).getAsJsonObject();

		for (Entry<String, JsonElement> i : allRoleSettings.entrySet()) {
			try {
				totalRoleCount += higherDepth(i.getValue(), "levels").getAsJsonArray().size();
			} catch (Exception ignored) {}
		}

		JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

		if (totalRoleCount >= 120) {
			return invalidEmbed("You have reached the max amount of total levels (120/120)");
		}

		for (JsonElement level : currentLevels) {
			if (higherDepth(level, "value").getAsString().equals(roleValue)) {
				currentLevels.remove(level);
				break;
			}
		}

		currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, role.getId())));

		if (!roleName.equals("guild_member")) {
			RoleObject[] temp = gson.fromJson(currentLevels, new TypeToken<RoleObject[]>() {}.getType());
			Arrays.sort(temp, Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
			currentLevels = gson.toJsonTree(temp).getAsJsonArray();
		}

		newRoleSettings.add("levels", currentLevels);

		int responseCode = database.setRoleSettings(guild.getId(), roleName, newRoleSettings);
		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		if (roleName.equals("guild_member")) {
			roleValue = guildName;
		}

		return defaultEmbed("Settings").setDescription(roleName + " " + roleValue + " set to " + role.getAsMention());
	}

	public EmbedBuilder removeRoleLevel(String roleName, String value) {
		if (isOneLevelRole(roleName)) {
			return defaultEmbed(
				"These roles do not support levels. Use `" + guildPrefix + "settings roles set [roleName] [@role]` instead"
			);
		}

		JsonObject currentRoleSettings;
		try {
			currentRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed("Invalid role name");
		}

		JsonArray currentLevels = currentRoleSettings.get("levels").getAsJsonArray();
		for (JsonElement level : currentLevels) {
			String currentValue = higherDepth(level, "value").getAsString();
			if (roleName.equals("guild_member")) {
				HypixelResponse guildJson = getGuildFromId(higherDepth(level, "value").getAsString());
				if (guildJson.isNotValid()) {
					return invalidEmbed(guildJson.getFailCause());
				}
				currentValue = guildJson.get("name").getAsString();
			}

			if (currentValue.equalsIgnoreCase(value.replace("_", " "))) {
				currentLevels.remove(level);
				currentRoleSettings.add("levels", currentLevels);
				int responseCode = database.setRoleSettings(guild.getId(), roleName, currentRoleSettings);
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				currentRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();

				if (currentRoleSettings.get("levels").getAsJsonArray().size() == 0) {
					setRoleEnable(roleName, "false");
				}

				if (!allowRolesEnable()) {
					setRolesEnable("false");
				}

				return defaultEmbed("Settings").setDescription(roleName + " " + value + " removed");
			}
		}
		return invalidEmbed("Invalid role value");
	}

	public EmbedBuilder setOneLevelRole(String roleName, String roleMention) {
		if (!isOneLevelRole(roleName)) {
			return invalidEmbed(
				"This role does is not a one level role. Use `" + guildPrefix + "settings roles add [roleName] [value] [@role]` instead"
			);
		}

		Role role = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
		if (role == null) {
			return invalidEmbed("Invalid role mention");
		}
		if (role.isPublicRole() || role.isManaged()) {
			return invalidEmbed("Role cannot be managed or @everyone!");
		}

		JsonObject newRoleSettings;
		try {
			newRoleSettings = database.getRoleSettings(guild.getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return invalidEmbed("Invalid role");
		}

		JsonArray currentLevels = new JsonArray();
		currentLevels.add(gson.toJsonTree(new RoleObject("default", role.getId())));
		newRoleSettings.add("levels", currentLevels);

		int responseCode = database.setRoleSettings(guild.getId(), roleName, newRoleSettings);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		return defaultEmbed("Settings").setDescription(roleName + " set to " + role.getAsMention());
	}

	public boolean isOneLevelRole(String roleName) {
		return (roleName.equals("pet_enthusiast") || roleName.equals("doom_slayer") || roleName.equals("all_slayer_nine"));
	}

	/* Verify Settings */
	public String getCurrentVerifySettings(JsonElement verifySettings) {
		String ebFieldString = "";
		ebFieldString += "**" + displaySettings(verifySettings, "enable") + "**";
		ebFieldString += "\n**• Message Text:** " + displaySettings(verifySettings, "messageText");
		ebFieldString += "\n**• Channel:** " + displaySettings(verifySettings, "messageTextChannelId");
		ebFieldString += "\n**• Verified Role(s):** " + displaySettings(verifySettings, "verifiedRoles");
		ebFieldString += "\n**• Nickname Template:** " + displaySettings(verifySettings, "verifiedNickname");
		ebFieldString += "\n**• Member join sync:** " + displaySettings(verifySettings, "enableMemberJoinSync");
		return ebFieldString;
	}

	public boolean allowVerifyEnable() {
		JsonObject currentSettings = database.getVerifySettings(guild.getId()).getAsJsonObject();
		currentSettings.remove("previousMessageId");

		try {
			for (Entry<String, JsonElement> key : currentSettings.entrySet()) {
				if (key.getValue().getAsString().length() == 0) {
					return false;
				}
			}
		} catch (Exception ignored) {}
		return true;
	}

	public EmbedBuilder setVerifyEnable(String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			int responseCode = updateVerifySettings("enable", enable);
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Verify:** " +
				(enable.equalsIgnoreCase("true") ? "enabled" : "disabled") +
				"\nRun `" +
				guildPrefix +
				"reload` to reload the settings"
			);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setVerifyMessageText(String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return invalidEmbed("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateVerifySettings("messageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Verify message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setVerifyMessageTextChannelId(String textChannel) {
		try {
			TextChannel verifyMessageTextChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			try {
				verifyMessageTextChannel.getManager().setSlowmode(5).queue();
			} catch (Exception ignored) {}
			int responseCode = updateVerifySettings("messageTextChannelId", verifyMessageTextChannel.getId());
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Verify text channel set to:** " + verifyMessageTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	public EmbedBuilder setVerifyNickname(String nickname) {
		if (!nickname.contains("[IGN]")) {
			if (nickname.equalsIgnoreCase("none")) {
				int responseCode = updateVerifySettings("verifiedNickname", "none");

				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Verify nickname disabled**");
				return eb;
			}
			return invalidEmbed("Nickname must contain [IGN] parameter");
		}

		if (nickname.contains("[GUILD_RANK]")) {
			List<GuildRole> guildRoleSettings = database.getAllGuildRoles(guild.getId());
			guildRoleSettings.removeIf(o1 -> {
				try {
					return !o1.getEnableGuildRanks().equalsIgnoreCase("true");
				} catch (Exception e) {
					return true;
				}
			});
			if (guildRoleSettings.size() == 0) {
				return invalidEmbed(
					"At least one guild ranks must be enabled in " + guildPrefix + "`settings guild [name]` to use the [GUILD_RANK] prefix"
				);
			}
		}

		if (nickname.replace("[IGN]", "").length() > 15) {
			return invalidEmbed("Nickname prefix and/or postfix must be less than or equal to 15 letters");
		}

		int responseCode = updateVerifySettings("verifiedNickname", nickname);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("**Verify nickname set to:** " + nickname);
		return eb;
	}

	public EmbedBuilder removeVerifyRole(String roleMention) {
		Role verifyRole;
		try {
			verifyRole = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((verifyRole.isPublicRole() || verifyRole.isManaged())) {
				return invalidEmbed("Invalid role");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonElement verifySettings = database.getVerifySettings(guild.getId());
		JsonArray currentVerifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();

		for (int i = currentVerifyRoles.size() - 1; i >= 0; i--) {
			if (currentVerifyRoles.get(i).getAsString().equals(verifyRole.getId())) {
				currentVerifyRoles.remove(i);
			}
		}

		int responseCode = database.setVerifyRolesSettings(guild.getId(), currentVerifyRoles);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(verifySettings);

		if (currentVerifyRoles.size() == 0) {
			updateVerifySettings("enable", "false");
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("**Removed verify role:** " + verifyRole.getAsMention());
	}

	public EmbedBuilder addVerifyRole(String roleMention) {
		Role verifyRole;
		try {
			verifyRole = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((verifyRole.isPublicRole() || verifyRole.isManaged())) {
				return invalidEmbed("Role cannot be managed or @everyone");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonElement verifySettings = database.getVerifySettings(guild.getId());
		JsonArray currentVerifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();
		if (currentVerifyRoles.size() >= 3) {
			return defaultEmbed("You have reached the max number of verify roles (3/3)");
		}

		currentVerifyRoles.add(verifyRole.getId());
		int responseCode = database.setVerifyRolesSettings(guild.getId(), currentVerifyRoles);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}
		guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(verifySettings);

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("**Verify role added:** " + verifyRole.getAsMention());
	}

	public EmbedBuilder setVerifySyncEnable(String enable) {
		JsonObject currentSettings = database.getVerifySettings(guild.getId()).getAsJsonObject();

		String nickname = higherDepth(currentSettings, "verifiedNickname").getAsString();
		if ((nickname.isEmpty() || nickname.equals("none")) && higherDepth(currentSettings, "verifiedRoles").getAsJsonArray().size() == 0) {
			return invalidEmbed("You must have at least on verify role or a nickname template set.");
		}

		int responseCode = updateVerifySettings("enableMemberJoinSync", enable);
		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("Member join sync " + (enable.equals("true") ? "enabled" : "disabled"));
	}

	public int updateVerifySettings(String key, String newValue) {
		JsonObject newVerifySettings = database.getVerifySettings(guild.getId()).getAsJsonObject();
		newVerifySettings.addProperty(key, newValue);
		if (key.equals("verifiedNickname") || key.equals("enableMemberJoinSync")) {
			guildMap.get(guild.getId()).verifyGuild.reloadSettingsJson(newVerifySettings);
		}
		return database.setVerifySettings(guild.getId(), newVerifySettings);
	}

	/* Apply Settings */
	public EmbedBuilder getCurrentApplySettings(JsonElement applySettings) {
		EmbedBuilder eb = defaultEmbed("Apply Settings");
		eb.setDescription("**" + displaySettings(applySettings, "enable").replace("•", "").trim() + "**");
		eb.addField("Button Message Channel", displaySettings(applySettings, "messageTextChannelId"), true);
		eb.addField("Staff Message Channel", displaySettings(applySettings, "messageStaffChannelId"), true);
		eb.addField("Waiting For Invite Channel", displaySettings(applySettings, "waitingChannelId"), true);
		eb.addField("Staff Ping Role", displaySettings(applySettings, "staffPingRoleId"), true);
		eb.addField("New Channel Category", displaySettings(applySettings, "newChannelCategory"), true);
		eb.addField("Ironman Only", displaySettings(applySettings, "ironmanOnly"), true);
		eb.addField("Button Message Text", displaySettings(applySettings, "messageText"), true);
		eb.addField("Accepted Message", displaySettings(applySettings, "acceptMessageText"), true);
		eb.addField("Waitlisted Message", displaySettings(applySettings, "waitlistedMessageText"), true);
		eb.addField("Denied Message", displaySettings(applySettings, "denyMessageText"), true);
		eb.addField("Requirements", displaySettings(applySettings, "applyReqs"), true);
		return eb;
	}

	public EmbedBuilder setWaitingChannel(String name, String textChannel) {
		try {
			TextChannel applyMessageTextChannel = null;
			if (!textChannel.equalsIgnoreCase("none")) {
				applyMessageTextChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			}

			int responseCode = updateApplySettings(
				name,
				"waitingChannelId",
				textChannel.equalsIgnoreCase("none") ? "none" : applyMessageTextChannel.getId()
			);
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Apply waiting for invite channel set to:** " +
				(textChannel.equalsIgnoreCase("none") ? "none" : applyMessageTextChannel.getAsMention())
			);
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	public EmbedBuilder createApplyGuild(String name) {
		if (name.length() > 25) {
			return invalidEmbed("Name cannot be more than 25 letters");
		}

		if (name.contains(" ")) {
			return invalidEmbed("Name cannot contain spaces");
		}

		List<AutomatedApply> currentApplications = database.getAllApplySettings(guild.getId());
		currentApplications.removeIf(o1 -> o1.getName() == null);

		if (currentApplications.size() == 2) {
			return invalidEmbed("You can reached the max amount of apply guilds (2/2)");
		}

		for (AutomatedApply currentApply : currentApplications) {
			if (currentApply.getName().equalsIgnoreCase(name)) {
				return invalidEmbed(name + " name is taken");
			}
		}

		AutomatedApply newApply = new AutomatedApply(name);

		int responseCode = database.setApplySettings(guild.getId(), newApply);
		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Created new apply guild with name `" + name + "`");
		return eb;
	}

	public boolean allowApplyEnable(String name) {
		JsonObject currentSettings = database.getApplySettings(guild.getId(), name).getAsJsonObject();
		currentSettings.remove("previousMessageId");
		currentSettings.remove("applyUsersCache");
		currentSettings.remove("waitlistedMessageText");
		currentSettings.remove("applyReqs");
		currentSettings.remove("ironmanOnly");
		currentSettings.remove("waitingChannelId");
		currentSettings.remove("staffPingRoleId");

		try {
			for (String key : getJsonKeys(currentSettings)) {
				if (higherDepth(currentSettings, key).getAsString().length() == 0) {
					return false;
				}
			}
		} catch (Exception ignored) {}
		return true;
	}

	public EmbedBuilder setApplyEnable(String name, String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			int responseCode = updateApplySettings(name, "enable", enable);
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Apply:** " +
				(enable.equalsIgnoreCase("true") ? "enabled" : "disabled") +
				"\nRun `" +
				guildPrefix +
				"reload` to reload the settings"
			);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setIsIronman(String name, String isIronman) {
		if (isIronman.equalsIgnoreCase("true") || isIronman.equalsIgnoreCase("false")) {
			int responseCode = updateApplySettings(name, "ironmanOnly", isIronman.toLowerCase());
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Ironman only:** " + (isIronman.equalsIgnoreCase("true") ? "enabled" : "disabled"));
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setApplyMessageTextChannelId(String name, String textChannel) {
		try {
			TextChannel applyMessageTextChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));

			int responseCode = updateApplySettings(name, "messageTextChannelId", applyMessageTextChannel.getId());
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply text channel set to:** " + applyMessageTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	public EmbedBuilder setApplyMessageStaffChannelId(String name, String textChannel) {
		try {
			TextChannel staffTextChannel = guild.getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			int responseCode = updateApplySettings(name, "messageStaffChannelId", staffTextChannel.getId());
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply staff channel set to:** " + staffTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	public EmbedBuilder setApplyMessageText(String name, String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return invalidEmbed("Text cannot be longer than 1500 letters!");
			}
			int responseCode = updateApplySettings(name, "messageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setApplyAcceptMessageText(String name, String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return invalidEmbed("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateApplySettings(name, "acceptMessageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply accept message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setApplyWaitListMessageText(String name, String verifyText) {
		if (verifyText.length() > 0) {
			if (verifyText.equalsIgnoreCase("none")) {
				int responseCode = updateVerifySettings("waitlistedMessageText", "none");

				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Waitlist message disabled**");
				return eb;
			}

			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return invalidEmbed("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateApplySettings(name, "waitlistedMessageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Waitlisted message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setApplyDenyMessageText(String name, String denyText) {
		if (denyText.length() > 0) {
			if (EmojiParser.parseToAliases(denyText).length() > 1500) {
				return invalidEmbed("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateApplySettings(name, "denyMessageText", EmojiParser.parseToAliases(denyText));
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply deny message set to:** " + denyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	public EmbedBuilder setApplyStaffPingRoleId(String name, String staffPingRoleMention) {
		try {
			if (staffPingRoleMention.equalsIgnoreCase("none")) {
				int responseCode = updateApplySettings(name, "staffPingRoleId", "none");
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Apply staff ping role set to:** none");
				return eb;
			}

			Role verifyGuildRole = guild.getRoleById(staffPingRoleMention.replaceAll("[<@&>]", ""));
			if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
				int responseCode = updateApplySettings(name, "staffPingRoleId", verifyGuildRole.getId());
				if (responseCode != 200) {
					return invalidEmbed("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Apply staff ping role set to:** " + verifyGuildRole.getAsMention());
				return eb;
			}
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Role");
	}

	public EmbedBuilder setApplyNewChannelCategory(String name, String messageCategory) {
		try {
			net.dv8tion.jda.api.entities.Category applyCategory = guild.getCategoryById(messageCategory.replaceAll("[<#>]", ""));
			int responseCode = updateApplySettings(name, "newChannelCategory", applyCategory.getId());
			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply new channel category set to:** <#" + applyCategory.getId() + ">");
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Guild Category");
	}

	public EmbedBuilder removeApplyRequirement(String name, String reqNumber) {
		JsonArray currentReqs;
		try {
			currentReqs = database.getApplyReqs(guild.getId(), name).getAsJsonArray();
		} catch (Exception ignored) {
			return invalidEmbed("Unable to get current settings");
		}

		try {
			JsonElement req = currentReqs.get(Integer.parseInt(reqNumber) - 1);
			currentReqs.remove(Integer.parseInt(reqNumber) - 1);

			int responseCode = database.setApplyReqs(guild.getId(), name, currentReqs);

			if (responseCode != 200) {
				return invalidEmbed("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Removed application requirement of:**\n• Slayer - " +
				higherDepth(req, "slayerReq", 0) +
				"\n• Skills - " +
				higherDepth(req, "skillsReq", 0) +
				"\n• Catacombs - " +
				higherDepth(req, "catacombsReq", 0) +
				"\n• Weight - " +
				higherDepth(req, "weightReq", 0)
			);
			return eb;
		} catch (Exception ignored) {
			return invalidEmbed(
				"Invalid requirement number. Run `" + guildPrefix + "settings apply` to see the current apply requirements"
			);
		}
	}

	public EmbedBuilder addApplyRequirement(String name, String reqArgs) {
		JsonArray currentReqs;
		try {
			currentReqs = database.getApplyReqs(guild.getId(), name).getAsJsonArray();
		} catch (Exception ignored) {
			return invalidEmbed("Unable to get current settings");
		}

		if (currentReqs.size() >= 3) {
			return invalidEmbed("You can only have up to 3 requirements");
		}

		int slayerReq = 0;
		int skillsReq = 0;
		int cataReq = 0;
		int weightReq = 0;

		try {
			slayerReq = Integer.parseInt(reqArgs.split("slayer:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		try {
			skillsReq = Integer.parseInt(reqArgs.split("skills:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		try {
			cataReq = Integer.parseInt(reqArgs.split("catacombs:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		try {
			weightReq = Integer.parseInt(reqArgs.split("weight:")[1].split(" ")[0]);
		} catch (Exception ignored) {}

		ApplyRequirements toAddReq = new ApplyRequirements("" + slayerReq, "" + skillsReq, "" + cataReq, "" + weightReq);

		currentReqs.add(gson.toJsonTree(toAddReq));

		int responseCode = database.setApplyReqs(guild.getId(), name, currentReqs);

		if (responseCode != 200) {
			return invalidEmbed("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription(
			"**Application requirement added:**\n• Slayer - " +
			slayerReq +
			"\n• Skills - " +
			skillsReq +
			"\n• Catacombs - " +
			cataReq +
			"\n• Weight - " +
			weightReq
		);
		return eb;
	}

	public EmbedBuilder deleteApplyGuild(String name) {
		if (database.getApplySettings(guild.getId(), name) != null) {
			int responseCode = database.removeApplySettings(guild.getId(), name);
			if (responseCode == 200) {
				return defaultEmbed("Settings").setDescription("Apply settings with name `" + name + "` was deleted");
			}

			return invalidEmbed("API returned response code " + responseCode);
		}

		return invalidEmbed("Invalid Name");
	}

	public int updateApplySettings(String name, String key, String newValue) {
		JsonObject newApplyJson = database.getApplySettings(guild.getId(), name).getAsJsonObject();
		newApplyJson.addProperty(key, newValue);
		return database.setApplySettings(guild.getId(), newApplyJson);
	}

	/* Mee6 */
	public EmbedBuilder getMee6DataSettings() {
		JsonObject settings = getMee6Json();

		EmbedBuilder eb = defaultSettingsEmbed()
			.appendDescription(higherDepth(settings, "enable", "false").equals("true") ? "**Enabled**" : "**Disabled");
		JsonArray curRoles = higherDepth(settings, "mee6Ranks").getAsJsonArray();
		if (curRoles.size() == 0) {
			eb.appendDescription("\n**• Leveling roles:** none");
		} else {
			for (JsonElement curRole : curRoles) {
				eb.appendDescription(
					"\n• **Level " +
					higherDepth(curRole, "value").getAsString() +
					":** <@&" +
					higherDepth(curRole, "roleId").getAsString() +
					">"
				);
			}
		}

		return eb;
	}

	public EmbedBuilder setMee6Enable(boolean enable) {
		if (!enable) {
			int responseCode = setMee6Settings("enable", "false");
			if (responseCode != 200) {
				return apiFailMessage(responseCode);
			}

			return defaultSettingsEmbed().setDescription("Disabled Mee6 automatic leveling roles.");
		}

		JsonElement settings = getMee6Json();

		if (higherDepth(settings, "mee6Ranks").getAsJsonArray().size() == 0) {
			return invalidEmbed("You must set at least one leveling role.");
		}
		try {
			if (
				higherDepth(getJson("https://mee6.xyz/api/plugins/levels/leaderboard/" + guild.getId()), "players")
					.getAsJsonArray()
					.size() ==
				0
			) {
				return invalidEmbed("The Mee6 leveling leaderboard must be public for this server.");
			}
		} catch (Exception e) {
			return invalidEmbed("The Mee6 leveling leaderboard must be public for this server.");
		}

		int responseCode = setMee6Settings("enable", "true");
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed().setDescription("Enabled Mee6 automatic leveling roles.");
	}

	public EmbedBuilder addMee6Role(String level, String roleMention) {
		Role role = guild.getRoleById(roleMention.replaceAll("[<@&>]", ""));
		if (role == null) {
			return invalidEmbed("The provided role does not exist.");
		} else if (role.isPublicRole()) {
			return invalidEmbed("The role cannot be the everyone role.");
		} else if (role.isManaged()) {
			return invalidEmbed("The role cannot be a managed role");
		}

		int intLevel;
		try {
			intLevel = Integer.parseInt(level);
			if (intLevel <= 0 || intLevel >= 250) {
				return invalidEmbed("The level must be between 0 and 50.");
			}
		} catch (Exception e) {
			return invalidEmbed("The level must be an integer.");
		}

		JsonObject settings = getMee6Json();

		JsonArray ranks = settings.getAsJsonArray("mee6Ranks");
		if (ranks.size() >= 10) {
			return defaultEmbed("You have reached the max amount of Mee6 roles (10/10).");
		}
		for (int i = ranks.size() - 1; i >= 0; i--) {
			if (higherDepth(ranks.get(i), "value").getAsInt() == intLevel) {
				ranks.remove(i);
			}
		}

		ranks.add(gson.toJsonTree(new RoleObject("" + intLevel, role.getId())));
		settings.add("mee6Ranks", ranks);

		int responseCode = setMee6Settings(settings);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed().setDescription("Added a level " + intLevel + " Mee6 role as " + role.getAsMention() + ".");
	}

	public EmbedBuilder removeMee6Role(String level) {
		int intLevel;
		try {
			intLevel = Integer.parseInt(level);
			if (intLevel <= 0 || intLevel >= 250) {
				return invalidEmbed("The level must be between 0 and 50.");
			}
		} catch (Exception e) {
			return invalidEmbed("The level must be an integer.");
		}

		JsonObject curSettings = getMee6Json();
		JsonArray curRanks = curSettings.get("mee6Ranks").getAsJsonArray();

		for (JsonElement rank : curRanks) {
			if (higherDepth(rank, "value").getAsInt() == intLevel) {
				curRanks.remove(rank);
				if (curRanks.size() == 0) {
					curSettings.addProperty("enable", "false");
				}
				curSettings.add("mee6Ranks", curRanks);

				int responseCode = database.setMee6Settings(guild.getId(), curSettings);
				if (responseCode != 200) {
					return apiFailMessage(responseCode);
				}

				return defaultSettingsEmbed().setDescription("Removed Mee6 role for level " + intLevel + ".");
			}
		}

		return invalidEmbed("There is no role set for level " + intLevel + ".");
	}

	public JsonObject getMee6Json() {
		return serverSettings.getAsJsonObject("mee6Data");
	}

	public int setMee6Settings(String key, String newValue) {
		JsonObject newSettings = getMee6Json();
		newSettings.addProperty(key, newValue);
		return database.setMee6Settings(guild.getId(), newSettings);
	}

	public int setMee6Settings(JsonElement newSettings) {
		return database.setMee6Settings(guild.getId(), newSettings);
	}

	/* Miscellaneous */
	public EmbedBuilder setHypixelKey(String newKey) {
		try {
			higherDepth(getJson("https://api.hypixel.net/key?key=" + newKey), "record.key").getAsString();
		} catch (Exception e) {
			return invalidEmbed("Provided Hypixel API key is invalid.");
		}

		int responseCode = database.setServerHypixelApiKey(guild.getId(), newKey);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed()
			.setDescription("Set the Hypixel API key. Note that no one can view the key for the privacy of the key owner.");
	}

	public EmbedBuilder deleteHypixelKey() {
		int responseCode = database.setServerHypixelApiKey(guild.getId(), "");
		if (responseCode != 200) {
			apiFailMessage(responseCode);
		}

		return defaultSettingsEmbed().setDescription("Deleted the server's Hypixel API key.");
	}

	public EmbedBuilder setPrefix(String prefix) {
		if (prefix.length() == 0 || prefix.length() > 5) {
			return invalidEmbed("The prefix must be a least one character and no more than five.");
		}

		int responseCode = database.setPrefix(guild.getId(), prefix);
		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		guildMap.get(guild.getId()).setPrefix(prefix);
		return defaultSettingsEmbed().setDescription("**Set the server's prefix to:** " + prefix);
	}

	public EmbedBuilder resetPrefix() {
		int responseCode = database.setPrefix(guild.getId(), null);

		if (responseCode != 200) {
			return apiFailMessage(responseCode);
		}

		guildMap.get(guild.getId()).setPrefix(DEFAULT_PREFIX);
		return defaultSettingsEmbed().setDescription("**Reset the server's prefix to:** " + DEFAULT_PREFIX);
	}

	/* Helper functions */
	public String displaySettings(JsonElement jsonSettings, String settingName) {
		if (higherDepth(jsonSettings, settingName) != null) {
			if (settingName.equals("applyReqs")) {
				JsonArray reqs = higherDepth(jsonSettings, settingName).getAsJsonArray();

				if (reqs.size() == 0) {
					return "None";
				}

				StringBuilder reqsString = new StringBuilder("\n");
				for (int i = 0; i < reqs.size(); i++) {
					JsonElement req = reqs.get(i);
					String slayerReq = higherDepth(req, "slayerReq").getAsString();
					String skillsReq = higherDepth(req, "skillsReq").getAsString();
					String cataReq = higherDepth(req, "catacombsReq").getAsString();
					String weightReq = higherDepth(req, "weightReq").getAsString();

					reqsString
						.append("`")
						.append(i + 1)
						.append(")` ")
						.append(slayerReq)
						.append(" slayer and ")
						.append(skillsReq)
						.append(" skill average and ")
						.append(cataReq)
						.append(" cata and ")
						.append(weightReq)
						.append(" weight\n");
				}

				return reqsString.toString();
			} else if (settingName.equals("verifiedRoles")) {
				JsonArray roles = higherDepth(jsonSettings, settingName).getAsJsonArray();
				StringBuilder ebStr = new StringBuilder();
				for (JsonElement role : roles) {
					ebStr.append("<@&").append(role.getAsString()).append(">").append(" ");
				}

				if (ebStr.length() == 0) {
					ebStr = new StringBuilder("None");
				}

				return ebStr.toString();
			}

			String currentSettingValue = higherDepth(jsonSettings, settingName).getAsString();
			if (currentSettingValue.length() > 0) {
				switch (settingName) {
					case "messageTextChannelId":
					case "waitingChannelId":
					case "messageStaffChannelId":
						try {
							return "<#" + currentSettingValue + ">";
						} catch (PermissionException e) {
							if (e.getMessage().contains("Missing permission")) {
								return ("Missing permission: " + e.getMessage().split("Missing permission: ")[1]);
							}
						}
						break;
					case "staffPingRoleId":
						return currentSettingValue.equals("none") ? "None" : "<@&" + currentSettingValue + ">";
					case "roleId":
						return "<@&" + currentSettingValue + ">";
					case "newChannelCategory":
						try {
							return ("<#" + guild.getCategoryById(currentSettingValue).getId() + ">");
						} catch (PermissionException e) {
							if (e.getMessage().contains("Missing permission")) {
								return ("Missing permission: " + e.getMessage().split("Missing permission: ")[1]);
							}
						}
						break;
					case "enable":
						return currentSettingValue.equals("true") ? "• Enabled" : "• Disabled";
					case "guildId":
						try {
							HypixelResponse guildJson = getGuildFromId(currentSettingValue);
							return guildJson.get("name").getAsString();
						} catch (Exception e) {
							return ("Error finding guild associated with " + currentSettingValue + " id");
						}
					case "enableGuildRole":
						return currentSettingValue.equals("true") ? "• Guild role enabled" : "• Guild role disabled";
					case "enableGuildRanks":
						return currentSettingValue.equals("true") ? "• Guild ranks enabled" : "• Guild ranks disabled";
				}
				return currentSettingValue;
			}
		}
		return "None";
	}

	public EmbedBuilder apiFailMessage(int responseCode) {
		return invalidEmbed("API returned response code " + responseCode + ". Please report this to the developer.");
	}

	public EmbedBuilder defaultSettingsEmbed() {
		return defaultEmbed("Settings");
	}
}
