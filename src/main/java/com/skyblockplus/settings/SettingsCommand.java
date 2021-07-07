package com.skyblockplus.settings;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.eventlisteners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.eventlisteners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedapplication.ApplyRequirements;
import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRank;
import com.skyblockplus.api.discordserversettings.automatedguildroles.GuildRole;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleObject;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.vdurmont.emoji.EmojiParser;
import java.util.*;
import java.util.Map.Entry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class SettingsCommand extends Command {

	private CommandEvent event;

	public SettingsCommand() {
		this.name = "settings";
		this.cooldown = globalCooldown + 1;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				this.event = event;
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");
				eb = null;

				if (!content.contains("hypixel_key")) {
					logCommand(event.getGuild(), event.getAuthor(), content);
				}

				if (!database.serverByServerIdExists(event.getGuild().getId())) {
					database.addNewServerSettings(
						event.getGuild().getId(),
						new ServerSettingsModel(event.getGuild().getName(), event.getGuild().getId())
					);
				}
				JsonElement currentSettings = database.getServerSettings(event.getGuild().getId());

				if (content.split(" ", 4).length == 4 && args[1].equals("set")) {
					if (args[2].equals("hypixel_key")) {
						eb = setHypixelKey(args[3]);
					} else if (args[2].equals("prefix")) {
						eb = setPrefix(content.split(" ", 4)[3]);
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
							if (database.deleteServerSettings(event.getGuild().getId()) == 200) {
								eb = defaultEmbed("Success").setDescription("Server settings deleted");
							} else {
								eb = defaultEmbed("Error").setDescription("Error deleting server settings");
							}
							break;
						case "hypixel_key":
							eb = deleteHypixelKey();
							break;
						case "prefix":
							eb = deletePrefix();
							break;
					}

					if (eb == null) {
						eb = errorEmbed("settings delete");
					}
				} else if (args.length == 1) {
					eb = defaultEmbed("Settings");
					eb.addField(
						"Verify Settings",
						"Use `" + getGuildPrefix(event.getGuild().getId()) + "settings verify` to see the current settings",
						false
					);
					eb.addField(
						"Apply Settings",
						"Use `" + getGuildPrefix(event.getGuild().getId()) + "settings apply` to see the current settings",
						false
					);
					eb.addField(
						"Roles Settings",
						"Use `" + getGuildPrefix(event.getGuild().getId()) + "settings roles` to see the current settings",
						false
					);
					eb.addField(
						"Guild Role/Ranks Settings",
						"Use `" + getGuildPrefix(event.getGuild().getId()) + "settings guild` to see the current settings",
						false
					);
					eb.addField(
						"Mee6 Roles Settings",
						"Use `" + getGuildPrefix(event.getGuild().getId()) + "settings mee6` to see the current settings",
						false
					);
				} else if (args.length >= 2 && args[1].equals("roles")) {
					if (args.length == 2) {
						if (higherDepth(currentSettings, "automatedRoles") != null) {
							ebMessage.delete().queue();
							getCurrentRolesSettings(higherDepth(currentSettings, "automatedRoles")).build().paginate(event.getChannel(), 0);
							return;
						} else {
							eb = defaultEmbed("Settings").addField("Roles Settings", "Error! Data not found", false);
						}
					} else if (args.length == 3) {
						if (args[2].equals("enable")) {
							if (allowRolesEnable()) {
								eb = setRolesEnable("true");
							} else {
								eb = defaultEmbed("Error").setDescription("No roles enabled");
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
						if (args[2].equals("stackable") && args[4].equals("true")) {
							eb = setRoleStackable(args[3], "true");
						} else if (args[2].equals("stackable") && args[4].equals("false")) {
							eb = setRoleStackable(args[3], "false");
						} else if (args[2].equals("remove")) {
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
									getGuildPrefix(event.getGuild().getId()) +
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
									getGuildPrefix(event.getGuild().getId()) +
									"settings apply " +
									higherDepth(currentSettings, "automatedApplicationTwo.name").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
					} else if (args.length == 3) {
						JsonElement applySettings = database.getApplySettings(event.getGuild().getId(), args[2]);
						if (applySettings != null && !applySettings.isJsonNull()) {
							eb = getCurrentApplySettings(applySettings);
						}
					} else if (args.length == 4) {
						if (args[2].equals("create")) {
							eb = createApplyGuild(args[3]);
						} else if (args[2].equals("delete")) {
							eb = deleteApplyGuild(args[3]);
						} else {
							JsonElement applySettings = database.getApplySettings(event.getGuild().getId(), args[2]);
							if (applySettings != null && !applySettings.isJsonNull()) {
								if (args[3].equals("enable")) {
									if (allowApplyEnable(args[2])) {
										eb = setApplyEnable(args[2], "true");
									} else {
										eb =
											defaultEmbed("Error")
												.setDescription("All other apply settings must be set before " + "enabling apply!");
									}
								} else if (args[3].equals("disable")) {
									eb = setApplyEnable(args[2], "false");
								}
							}
						}
					} else if (args.length == 5) {
						JsonElement applySettings = database.getApplySettings(event.getGuild().getId(), args[2]);
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
								case "prefix":
									eb = setApplyNewChannelPrefix(args[2], args[4]);
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
							eb.addField(
								"Verify Settings",
								getCurrentVerifySettings(higherDepth(currentSettings, "automatedVerify")),
								false
							);
						} else {
							eb.addField("Verify Settings", "Error! Data not found", false);
						}
					} else if (args.length == 3) {
						if (args[2].equals("enable")) {
							if (allowVerifyEnable()) {
								eb = setVerifyEnable("true");
							} else {
								eb =
									defaultEmbed("Error")
										.setDescription("All other verify settings must be set before " + "enabling verify!");
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
									eb = defaultEmbed("Error").setDescription("Invalid setting");
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
									getGuildPrefix(event.getGuild().getId()) +
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
									getGuildPrefix(event.getGuild().getId()) +
									"settings guild " +
									higherDepth(currentSettings, "automaticGuildRolesTwo.name").getAsString() +
									"`" +
									""
									: "Not setup"
							),
							false
						);
					} else if (args.length == 3) {
						eb = defaultEmbed("Settings");
						JsonElement guildRoleSettings = database.getGuildRoleSettings(event.getGuild().getId(), args[2]);
						if (guildRoleSettings != null && !guildRoleSettings.isJsonNull()) {
							eb.addField("Guild Role Settings", getCurrentGuildRoleSettings(guildRoleSettings), false);
						} else {
							eb.addField("Guild Role Settings", "Error! Data not found", false);
						}
					} else if (args.length == 4) {
						if (args[2].equals("create")) {
							eb = createGuildRoles(args[3]);
						}
					} else if (args.length == 5) {
						JsonElement guildRoleSettings = database.getGuildRoleSettings(event.getGuild().getId(), args[2]);
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
											eb = setGuildRoleEnable(args[2], "true");
											break;
										case "rank":
											eb = setGuildRankEnable(args[2], "true");
											break;
										case "counter":
											eb = setGuildCounterEnable(args[2], "true");
											break;
									}
									break;
								case "disable":
									switch (args[4]) {
										case "role":
											eb = setGuildRoleEnable(args[4], "false");
											break;
										case "rank":
											eb = setGuildRankEnable(args[4], "false");
											break;
										case "counter":
											eb = setGuildCounterEnable(args[2], "false");
											break;
									}
									break;
								case "remove":
									eb = removeGuildRank(args[2], args[4]);
									break;
							}
						}
					} else if (args.length == 6) {
						JsonElement guildRoleSettings = database.getGuildRoleSettings(event.getGuild().getId(), args[2]);
						if (guildRoleSettings == null || guildRoleSettings.isJsonNull()) {
							eb = defaultEmbed("Error").setDescription("Invalid name");
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

				ebMessage.editMessageEmbeds(eb.build()).queue();
			}
		);
	}

	private EmbedBuilder setHypixelKey(String newKey) {
		try {
			higherDepth(getJson("https://api.hypixel.net/key?key=" + newKey), "record.key").getAsString();
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("Invalid API key");
		}

		event.getMessage().delete().queue();

		int responseCode = database.setServerHypixelApiKey(event.getGuild().getId(), newKey);

		if (responseCode == 200) {
			return defaultEmbed("Settings")
				.setDescription(
					"Set the Hypixel API key. Note that you cannot view the set API key cannot be viewed for the privacy of the key owner."
				);
		}

		return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
	}

	private EmbedBuilder deleteHypixelKey() {
		int responseCode = database.setServerHypixelApiKey(event.getGuild().getId(), "");

		if (responseCode == 200) {
			return defaultEmbed("Settings").setDescription("Deleted the set Hypixel API key");
		}

		return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
	}

	private EmbedBuilder deleteApplyGuild(String name) {
		if (database.getApplySettings(event.getGuild().getId(), name) != null) {
			int responseCode = database.removeApplySettings(event.getGuild().getId(), name);
			if (responseCode == 200) {
				return defaultEmbed("Settings").setDescription("Apply settings with name `" + name + "` was deleted");
			}

			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		return defaultEmbed("Error").setDescription("Invalid Name");
	}

	private EmbedBuilder removeVerifyRole(String roleMention) {
		Role verifyRole;
		try {
			verifyRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((verifyRole.isPublicRole() || verifyRole.isManaged())) {
				return defaultEmbed("Error").setDescription("Invalid role");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonArray currentVerifyRoles = higherDepth(database.getVerifySettings(event.getGuild().getId()), "verifiedRoles").getAsJsonArray();

		for (int i = currentVerifyRoles.size() - 1; i >= 0; i--) {
			if (currentVerifyRoles.get(i).getAsString().equals(verifyRole.getId())) {
				currentVerifyRoles.remove(i);
			}
		}

		int responseCode = database.setVerifyRolesSettings(event.getGuild().getId(), currentVerifyRoles);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		if (currentVerifyRoles.size() == 0) {
			updateVerifySettings("enable", "false");
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("**Removed verify role:** " + verifyRole.getAsMention());
	}

	private EmbedBuilder addVerifyRole(String roleMention) {
		Role verifyRole;
		try {
			verifyRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((verifyRole.isPublicRole() || verifyRole.isManaged())) {
				return defaultEmbed("Error").setDescription("Role cannot be managed or @everyone");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonArray currentVerifyRoles = higherDepth(database.getVerifySettings(event.getGuild().getId()), "verifiedRoles").getAsJsonArray();
		if (currentVerifyRoles.size() >= 3) {
			return defaultEmbed("You have reached the max number of verify roles (3/3)");
		}

		currentVerifyRoles.add(verifyRole.getId());
		int responseCode = database.setVerifyRolesSettings(event.getGuild().getId(), currentVerifyRoles);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		return eb.setDescription("**Verify role added:** " + verifyRole.getAsMention());
	}

	/* Guild Role Settings */
	private String getCurrentGuildRoleSettings(JsonElement currentSettings) {
		String ebFieldString = "";
		ebFieldString += "**" + displaySettings(currentSettings, "enableGuildRole") + "**";
		ebFieldString += "\n**• Guild Name:** " + displaySettings(currentSettings, "guildId");
		ebFieldString += "\n**• Guild Role:** " + displaySettings(currentSettings, "roleId");
		ebFieldString += "\n\n**" + displaySettings(currentSettings, "enableGuildRanks") + "**";

		StringBuilder guildRanksString = new StringBuilder();
		try {
			for (JsonElement guildRank : higherDepth(currentSettings, "guildRanks").getAsJsonArray()) {
				guildRanksString
					.append("\n• ")
					.append(higherDepth(guildRank, "minecraftRoleName").getAsString())
					.append(" - ")
					.append("<@&")
					.append(higherDepth(guildRank, "discordRoleId").getAsString())
					.append(">");
			}
		} catch (Exception ignored) {}

		ebFieldString += guildRanksString.length() > 0 ? guildRanksString.toString() : "\n• No guild ranks set";

		return ebFieldString;
	}

	private EmbedBuilder setGuildRoleEnable(String name, String enable) {
		JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();
		if ((higherDepth(currentSettings, "guildId") == null) || (higherDepth(currentSettings, "roleId") == null)) {
			return defaultEmbed("Guild name and role must be set before enabling");
		}

		currentSettings.remove("enableGuildRole");
		currentSettings.addProperty("enableGuildRole", enable);
		int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Guild role " + (enable.equals("true") ? "enabled" : "disabled"));
		return eb;
	}

	private EmbedBuilder setGuildRankEnable(String name, String enable) {
		JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();

		if (
			(higherDepth(currentSettings, "guildId") == null) || (higherDepth(currentSettings, "guildRanks").getAsJsonArray().size() == 0)
		) {
			return defaultEmbed("Error").setDescription("The guild name and a guild rank must be set");
		}

		currentSettings.remove("enableGuildRanks");
		currentSettings.addProperty("enableGuildRanks", enable);
		int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Guild ranks " + (enable.equals("true") ? "enabled" : "disabled"));
		return eb;
	}

	private EmbedBuilder setGuildCounterEnable(String name, String enable) {
		JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();

		if (
			currentSettings.get("enableGuildUserCount") == null || !currentSettings.get("enableGuildUserCount").getAsString().equals(enable)
		) {
			currentSettings.remove("enableGuildUserCount");
			currentSettings.addProperty("enableGuildUserCount", enable);
			if (enable.equals("true")) {
				if ((higherDepth(currentSettings, "guildId") == null) || (higherDepth(currentSettings, "roleId") == null)) {
					return defaultEmbed("Guild name must be set before enabling");
				}

				JsonElement guildJson = getJson(
					"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + higherDepth(currentSettings, "guildId").getAsString()
				);

				if (higherDepth(guildJson, "guild") == null || higherDepth(guildJson, "guild").isJsonNull()) {
					return defaultEmbed("Current guild name is invalid");
				}

				VoiceChannel guildMemberCounterChannel = event
					.getGuild()
					.createVoiceChannel(
						higherDepth(guildJson, "guild.name").getAsString() +
						" Members: " +
						higherDepth(guildJson, "guild.members").getAsJsonArray().size() +
						"/125"
					)
					.addPermissionOverride(
						event.getGuild().getPublicRole(),
						EnumSet.of(Permission.VIEW_CHANNEL),
						EnumSet.of(Permission.VOICE_CONNECT)
					)
					.addMemberPermissionOverride(event.getSelfMember().getIdLong(), EnumSet.of(Permission.VOICE_CONNECT), null)
					.complete();
				currentSettings.remove("guildUserChannelId");
				currentSettings.addProperty("guildUserChannelId", guildMemberCounterChannel.getId());
			} else {
				try {
					event.getGuild().getVoiceChannelById(currentSettings.get("guildUserChannelId").getAsString()).delete().complete();
				} catch (Exception ignored) {}
			}

			int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Guild member counter " + (enable.equals("true") ? "enabled" : "disabled"));
		return eb;
	}

	private EmbedBuilder addGuildRank(String name, String rankName, String roleMention) {
		Role guildRankRole;
		try {
			guildRankRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if ((guildRankRole.isPublicRole() || guildRankRole.isManaged())) {
				return defaultEmbed("Error").setDescription("Role cannot be managed or @everyone");
			}
		} catch (Exception e) {
			return defaultEmbed("Invalid Role");
		}

		JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();

		if (higherDepth(currentSettings, "guildId") == null) {
			return defaultEmbed("Guild name must first be set");
		}

		String guildId = higherDepth(currentSettings, "guildId").getAsString();

		JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);

		if (higherDepth(guildJson, "guild") == null || higherDepth(guildJson, "guild").isJsonNull()) {
			return defaultEmbed("Current guild name is invalid");
		}

		JsonArray guildRanks = higherDepth(guildJson, "guild.ranks").getAsJsonArray();

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

				Gson gson = new Gson();
				currentGuildRanks.add(gson.toJsonTree(new GuildRank(rankName.toLowerCase(), guildRankRole.getId())));

				currentSettings.remove("guildRanks");
				currentSettings.add("guildRanks", currentGuildRanks);

				int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);
				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription(
					"**Guild rank added:** " + higherDepth(guildRank, "name").getAsString() + " - " + guildRankRole.getAsMention()
				);
				return eb;
			}
		}

		return defaultEmbed("Invalid guild rank")
			.setDescription((guildRanksString.length() > 0 ? "Valid guild ranks are: " + guildRanksString : "No guild ranks found"));
	}

	private EmbedBuilder removeGuildRank(String name, String rankName) {
		JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();
		JsonArray currentGuildRanks = currentSettings.get("guildRanks").getAsJsonArray();

		for (JsonElement guildRank : currentGuildRanks) {
			if (higherDepth(guildRank, "minecraftRoleName").getAsString().equalsIgnoreCase(rankName)) {
				JsonArray currentGuildRanksTemp = currentSettings.get("guildRanks").getAsJsonArray();
				currentGuildRanksTemp.remove(guildRank);

				if (currentGuildRanksTemp.size() == 0) {
					currentSettings.remove("enableGuildRanks");
					currentSettings.addProperty("enableGuildRanks", "false");
				}

				currentSettings.remove("guildRanks");
				currentSettings.add("guildRanks", currentGuildRanksTemp);

				int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);
				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Guild rank removed:** " + rankName);
				return eb;
			}
		}

		return defaultEmbed("Error").setDescription("Invalid rank name");
	}

	private EmbedBuilder setGuildRoleId(String name, String guildName) {
		try {
			JsonElement guildJson = getJson(
				"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name=" + guildName.replace("_", "%20")
			);
			String guildId = higherDepth(guildJson, "guild._id").getAsString();
			JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();
			currentSettings.remove("guildId");
			currentSettings.addProperty("guildId", guildId);
			int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Guild set to:** " + higherDepth(guildJson, "guild.name").getAsString());
			return eb;
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("Invalid guild name");
		}
	}

	private EmbedBuilder setGuildRoleName(String name, String roleMention) {
		try {
			Role verifyGuildRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
				JsonObject currentSettings = database.getGuildRoleSettings(event.getGuild().getId(), name).getAsJsonObject();
				currentSettings.remove("roleId");
				currentSettings.addProperty("roleId", verifyGuildRole.getId());
				int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), currentSettings);

				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Guild role set to:** " + verifyGuildRole.getAsMention());
				return eb;
			}
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Role");
	}

	private EmbedBuilder createGuildRoles(String name) {
		if (name.length() > 25) {
			return defaultEmbed("Error").setDescription("Name cannot be more than 25 letters");
		}

		List<GuildRole> currentGuildRoles = database.getAllGuildRoles(event.getGuild().getId());
		currentGuildRoles.removeIf(o1 -> o1.getGuildId() == null);

		if (currentGuildRoles.size() == 2) {
			return defaultEmbed("Error").setDescription("You can reached the max amount of apply guilds (2/2)");
		}

		for (GuildRole currentGuildRole : currentGuildRoles) {
			if (currentGuildRole.getName().equalsIgnoreCase(name)) {
				return defaultEmbed("Error").setDescription(name + " name is taken");
			}
		}

		GuildRole newGuildRole = new GuildRole(name);

		int responseCode = database.setGuildRoleSettings(event.getGuild().getId(), newGuildRole);
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Created new guild role with name `" + name + "`");
		return eb;
	}

	/* Roles Settings */
	private EmbedBuilder getCurrentRoleSettings(String roleName) {
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
			CustomPaginator.Builder currentRoleSettings = getCurrentRolesSettings(database.getRolesSettings(event.getGuild().getId()));
			currentRoleSettings.build().paginate(event.getChannel(), rolePageMap.get(roleName));
			return null;
		} else {
			try {
				if (rolePageMap.containsValue(Integer.parseInt(roleName))) {
					CustomPaginator.Builder currentRoleSettings = getCurrentRolesSettings(
						database.getRolesSettings(event.getGuild().getId())
					);
					currentRoleSettings.build().paginate(event.getChannel(), rolePageMap.get(roleName));
					return null;
				}
			} catch (Exception ignored) {}
		}

		return defaultEmbed("Error").setDescription("Invalid role name");
	}

	private CustomPaginator.Builder getCurrentRolesSettings(JsonElement rolesSettings) {
		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

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
				database.setRoleSettings(event.getGuild().getId(), roleName, new Gson().toJsonTree(new RoleModel()));
				currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName);
			}

			if (higherDepth(currentRoleSettings, "stackable") == null) {
				database.setRoleSettings(event.getGuild().getId(), roleName, new Gson().toJsonTree(new RoleModel()));
				currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName);
			}

			switch (roleName) {
				case "guild_member":
					{
						ebFieldString
							.append("**Member role for Hypixel guilds**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add guild_member skyblock_forceful @sbf guild member`\n");
						break;
					}
				case "sven":
					{
						ebFieldString
							.append("**A player's sven packmaster slayer xp**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add sven 1000000 @sven 9`\n");
						break;
					}
				case "rev":
					{
						ebFieldString
							.append("**A player's revenant horror xp slayer**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add rev 400000 @rev 8`\n");
						break;
					}
				case "tara":
					{
						ebFieldString
							.append("**A player's tarantula broodfather slayer xp**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add tara 100000 @tara 7`\n");
						break;
					}
				case "bank_coins":
					{
						ebFieldString
							.append("**Coins in a player's bank**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
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
							.append(getGuildPrefix(event.getGuild().getId()))
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
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add fairy_souls 50 @50 souls collected`\n");
						break;
					}
				case "slot_collector":
					{
						ebFieldString
							.append("**Number of minion slots excluding upgrades (__not fully working__)**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add slot_collector 24 @maxed minion slots`\n");
						break;
					}
				case "pet_enthusiast":
					{
						ebFieldString
							.append("**Having a level 100 epic or legendary pet that is not an enchanting or alchemy pet**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles set pet_enthusiast @level 100 pet`\n");
						break;
					}
				case "doom_slayer":
					{
						ebFieldString
							.append("**Having at least one level nine slayer**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles set doom_slayer @level nine slayer`\n");
						break;
					}
				case "guild_ranks":
					{
						ebFieldString
							.append("**If a player is in the guild set in `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append(
								"settings guild`, they will be given the corresponding rank role set there**\nNote: this role can only be enabled, disabled, and linked here. To modify guild ranks use `"
							)
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings guild [name]`");
						break;
					}
				case "all_slayer_nine":
					{
						ebFieldString
							.append("**Having all level nine slayers**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles set all_slayer_nine @role`\n");
						break;
					}
				case "dungeon_secrets":
					{
						ebFieldString
							.append("**A player's dungeon secrets count**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add dungeon_secrets 25000 @secret sweat`\n");
						break;
					}
				case "enderman":
					{
						ebFieldString
							.append("**A player's voidgloom seraph slayer xp**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add enderman 100000 @enderman 7`\n");
						break;
					}
				case "weight":
					{
						ebFieldString
							.append("**A player's weight**\nExample: `")
							.append(getGuildPrefix(event.getGuild().getId()))
							.append("settings roles add weight 5000 @5k weight`\n");
						break;
					}
			}

			ebFieldString.append("\nCurrent Settings:\n");

			ebFieldString.append(higherDepth(currentRoleSettings, "enable").getAsString().equals("true") ? "• Enabled" : "• Disabled");
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
							.append(getGuildPrefix(event.getGuild().getId()))
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
				ebFieldString.append(
					higherDepth(currentRoleSettings, "stackable").getAsString().equals("true") ? "\n• Stackable" : "\n• Not stackable"
				);

				if (roleName.equals("guild_member")) {
					for (JsonElement roleLevel : higherDepth(currentRoleSettings, "levels").getAsJsonArray()) {
						String guildId = higherDepth(roleLevel, "value").getAsString();
						JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);
						ebFieldString
							.append("\n• ")
							.append(higherDepth(guildJson, "guild.name").getAsString())
							.append(" - ")
							.append("<@&")
							.append(higherDepth(roleLevel, "roleId").getAsString())
							.append(">");
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

	private boolean allowRolesEnable() {
		JsonObject currentSettings = database.getRolesSettings(event.getGuild().getId()).getAsJsonObject();
		currentSettings.remove("enable");
		for (String role : getJsonKeys(currentSettings)) {
			if (higherDepth(currentSettings, role + ".enable").getAsBoolean()) {
				return true;
			}
		}
		return false;
	}

	private EmbedBuilder setRolesEnable(String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			JsonObject newRolesJson = database.getRolesSettings(event.getGuild().getId()).getAsJsonObject();
			newRolesJson.remove("enable");
			newRolesJson.addProperty("enable", enable);
			int responseCode = database.setRolesSettings(event.getGuild().getId(), newRolesJson);
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Roles:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setRoleEnable(String roleName, String enable) {
		JsonObject currentRoleSettings = null;
		try {
			currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
		} catch (Exception ignored) {}

		if (currentRoleSettings == null) {
			return defaultEmbed("Error").setDescription("Invalid role name");
		}

		if (currentRoleSettings.get("levels").getAsJsonArray().size() != 0) {
			currentRoleSettings.remove("enable");
			currentRoleSettings.addProperty("enable", enable);
			int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**" + roleName + " role:** " + (enable.equalsIgnoreCase("true") ? "enabled" : "disabled"));
			return eb;
		} else {
			currentRoleSettings.remove("enable");
			currentRoleSettings.addProperty("enable", "false");
			database.setRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
		}
		EmbedBuilder eb = defaultEmbed("Error");
		eb.setDescription("Specified role must have at least one configuration!");
		return eb;
	}

	private EmbedBuilder addRoleLevel(String roleName, String roleValue, String roleMention) {
		String guildName = "";
		if (roleName.equals("guild_member")) {
			try {
				JsonElement guildJson = getJson(
					"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name=" + roleValue.replace("_", "%20")
				);
				roleValue = higherDepth(guildJson, "guild._id").getAsString();
				guildName = higherDepth(guildJson, "guild.name").getAsString();
			} catch (Exception e) {
				return defaultEmbed("Error").setDescription("Invalid username");
			}
		} else if (roleName.equals("guild_ranks")) {
			JsonObject newRoleSettings = database.getRoleSettings(event.getGuild().getId(), "guild_ranks").getAsJsonObject();
			JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

			if (currentLevels.size() >= 2) {
				return defaultEmbed("Error").setDescription("This role has reached the max limit of levels (2/2)");
			}
			JsonElement guildRoleSettings = database.getGuildRoleSettings(event.getGuild().getId(), roleValue);
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

				Gson gson = new Gson();
				currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, roleValue)));

				newRoleSettings.remove("levels");
				newRoleSettings.add("levels", currentLevels);

				int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, newRoleSettings);
				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				return defaultEmbed("Settings").setDescription("Added guild ranks for guild roles with name `" + roleValue + "`");
			}

			return defaultEmbed("Error").setDescription("Invalid guild role name or guild ranks not enabled");
		} else if (isOneLevelRole(roleName)) {
			return defaultEmbed(
				"These roles do not support levels. Use `" +
				getGuildPrefix(event.getGuild().getId()) +
				"settings roles set [roleName] [@role]` instead"
			);
		} else {
			try {
				Integer.parseInt(roleValue);
			} catch (Exception e) {
				return defaultEmbed("Error").setDescription("Role value must be an integer");
			}
		}

		Role role = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
		if (role == null) {
			return defaultEmbed("Error").setDescription("Invalid role mention");
		}

		if (role.isPublicRole() || role.isManaged()) {
			return defaultEmbed("Error").setDescription("Role cannot be managed or @everyone!");
		}
		JsonObject newRoleSettings;
		try {
			newRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("Invalid role");
		}

		int totalRoleCount = 0;
		JsonObject allRoleSettings = database.getRolesSettings(event.getGuild().getId()).getAsJsonObject();

		for (Entry<String, JsonElement> i : allRoleSettings.entrySet()) {
			try {
				totalRoleCount += higherDepth(i.getValue(), "levels").getAsJsonArray().size();
			} catch (Exception ignored) {}
		}

		JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

		if (totalRoleCount >= 120) {
			return defaultEmbed("Error").setDescription("You have reached the max amount of total levels (120/120)");
		}

		for (JsonElement level : currentLevels) {
			if (higherDepth(level, "value").getAsString().equals(roleValue)) {
				currentLevels.remove(level);
				break;
			}
		}

		Gson gson = new Gson();
		currentLevels.add(gson.toJsonTree(new RoleObject(roleValue, role.getId())));

		if (!roleName.equals("guild_member")) {
			RoleObject[] temp = gson.fromJson(currentLevels, new TypeToken<RoleObject[]>() {}.getType());
			Arrays.sort(temp, Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
			currentLevels = gson.toJsonTree(temp).getAsJsonArray();
		}

		newRoleSettings.remove("levels");
		newRoleSettings.add("levels", currentLevels);

		int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, newRoleSettings);
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		if (roleName.equals("guild_member")) {
			roleValue = guildName;
		}

		return defaultEmbed("Settings").setDescription(roleName + " " + roleValue + " set to " + role.getAsMention());
	}

	private EmbedBuilder removeRoleLevel(String roleName, String value) {
		if (isOneLevelRole(roleName)) {
			return defaultEmbed(
				"These roles do not support levels. Use `" +
				getGuildPrefix(event.getGuild().getId()) +
				"settings roles set [roleName] [@role]` instead"
			);
		}

		JsonObject currentRoleSettings;
		try {
			currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("Invalid role name");
		}

		JsonArray currentLevels = currentRoleSettings.get("levels").getAsJsonArray();
		for (JsonElement level : currentLevels) {
			String currentValue = higherDepth(level, "value").getAsString();
			if (roleName.equals("guild_member")) {
				JsonElement guildJson = getJson(
					"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + higherDepth(level, "value").getAsString()
				);
				currentValue = higherDepth(guildJson, "guild.name").getAsString();
			}

			if (currentValue.equalsIgnoreCase(value.replace("_", " "))) {
				currentLevels.remove(level);
				currentRoleSettings.remove("levels");
				currentRoleSettings.add("levels", currentLevels);
				int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();

				if (currentRoleSettings.get("levels").getAsJsonArray().size() == 0) {
					setRoleEnable(roleName, "false");
				}

				if (!allowRolesEnable()) {
					setRolesEnable("false");
				}

				return defaultEmbed("Settings").setDescription(roleName + " " + value + " removed");
			}
		}
		return defaultEmbed("Error").setDescription("Invalid role value");
	}

	private EmbedBuilder setRoleStackable(String roleName, String stackable) {
		if (isOneLevelRole(roleName) || roleName.equals("guild_ranks")) {
			return defaultEmbed("Error").setDescription("This role does not support stacking");
		}
		JsonObject currentRoleSettings;
		try {
			currentRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("Invalid Role");
		}

		currentRoleSettings.remove("stackable");
		currentRoleSettings.addProperty("stackable", stackable);
		int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, currentRoleSettings);
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("**" + roleName + " role:** " + (stackable.equalsIgnoreCase("true") ? "stackable" : "not stackable"));
		return eb;
	}

	private EmbedBuilder setOneLevelRole(String roleName, String roleMention) {
		if (!isOneLevelRole(roleName)) {
			return defaultEmbed("Error")
				.setDescription(
					"This role does is not a one level role. Use `" +
					getGuildPrefix(event.getGuild().getId()) +
					"settings roles add [roleName] [value] [@role]` instead"
				);
		}

		Role role = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
		if (role == null) {
			return defaultEmbed("Error").setDescription("Invalid role mention");
		}
		if (role.isPublicRole() || role.isManaged()) {
			return defaultEmbed("Error").setDescription("Role cannot be managed or @everyone!");
		}

		JsonObject newRoleSettings;
		try {
			newRoleSettings = database.getRoleSettings(event.getGuild().getId(), roleName).getAsJsonObject();
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("Invalid role");
		}

		JsonArray currentLevels = newRoleSettings.get("levels").getAsJsonArray();

		Gson gson = new Gson();
		currentLevels.add(gson.toJsonTree(new RoleObject("default", role.getId())));

		RoleObject[] temp = gson.fromJson(currentLevels, new TypeToken<RoleObject[]>() {}.getType());
		Arrays.sort(temp, Comparator.comparing(RoleObject::getValue));
		currentLevels = gson.toJsonTree(temp).getAsJsonArray();

		newRoleSettings.remove("levels");
		newRoleSettings.add("levels", currentLevels);

		int responseCode = database.setRoleSettings(event.getGuild().getId(), roleName, newRoleSettings);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		return defaultEmbed("Settings").setDescription(roleName + " set to " + role.getAsMention());
	}

	private boolean isOneLevelRole(String roleName) {
		return (roleName.equals("pet_enthusiast") || roleName.equals("doom_slayer") || roleName.equals("all_slayer_nine"));
	}

	/* Verify Settings */
	private String getCurrentVerifySettings(JsonElement verifySettings) {
		String ebFieldString = "";
		ebFieldString += "**" + displaySettings(verifySettings, "enable") + "**";
		ebFieldString += "\n**• React Message Text:** " + displaySettings(verifySettings, "messageText");
		ebFieldString += "\n**• React Message Channel:** " + displaySettings(verifySettings, "messageTextChannelId");
		ebFieldString += "\n**• Verified Role(s):** " + displaySettings(verifySettings, "verifiedRoles");
		ebFieldString += "\n**• Nickname Template:** " + displaySettings(verifySettings, "verifiedNickname");
		return ebFieldString;
	}

	private boolean allowVerifyEnable() {
		JsonObject currentSettings = database.getVerifySettings(event.getGuild().getId()).getAsJsonObject();
		currentSettings.remove("previousMessageId");

		try {
			for (Entry<String, JsonElement> key : currentSettings.entrySet()) {
				if (key.getValue().getAsString().length() == 0) {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private EmbedBuilder setVerifyEnable(String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			int responseCode = updateVerifySettings("enable", enable);
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Verify:** " +
				(enable.equalsIgnoreCase("true") ? "enabled" : "disabled") +
				"\nRun `" +
				getGuildPrefix(event.getGuild().getId()) +
				"reload` to reload the settings"
			);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setVerifyMessageText(String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return defaultEmbed("Error").setDescription("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateVerifySettings("messageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Verify message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setVerifyMessageTextChannelId(String textChannel) {
		try {
			TextChannel verifyMessageTextChannel = event.getGuild().getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			int responseCode = updateVerifySettings("messageTextChannelId", verifyMessageTextChannel.getId());
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Verify text channel set to:** " + verifyMessageTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	private EmbedBuilder setVerifyNickname(String nickname) {
		if (!nickname.contains("[IGN]")) {
			if (nickname.equalsIgnoreCase("none")) {
				int responseCode = updateVerifySettings("verifiedNickname", "none");

				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Verify nickname disabled**");
				return eb;
			}
			return defaultEmbed("Error").setDescription("Nickname must contain [IGN] parameter");
		}

		if (nickname.contains("[GUILD_RANK]")) {
			List<GuildRole> guildRoleSettings = database.getAllGuildRoles(event.getGuild().getId());
			guildRoleSettings.removeIf(
				o1 -> {
					try {
						return !o1.getEnableGuildRanks().equalsIgnoreCase("true");
					} catch (Exception e) {
						return true;
					}
				}
			);
			if (guildRoleSettings.size() == 0) {
				return defaultEmbed("Error")
					.setDescription(
						"At least one guild ranks must be enabled in " +
						getGuildPrefix(event.getGuild().getId()) +
						"`settings guild [name]` to use the [GUILD_RANK] prefix"
					);
			}
		}

		if (nickname.replace("[IGN]", "").length() > 15) {
			return defaultEmbed("Error").setDescription("Nickname prefix and/or postfix must be less than or equal to 15 letters");
		}

		int responseCode = updateVerifySettings("verifiedNickname", nickname);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("**Verify nickname set to:** " + nickname);
		return eb;
	}

	private int updateVerifySettings(String key, String newValue) {
		JsonObject newVerifySettings = database.getVerifySettings(event.getGuild().getId()).getAsJsonObject();
		newVerifySettings.remove(key);
		newVerifySettings.addProperty(key, newValue);
		return database.setVerifySettings(event.getGuild().getId(), newVerifySettings);
	}

	/* Apply Settings */
	private EmbedBuilder getCurrentApplySettings(JsonElement applySettings) {
		EmbedBuilder eb = defaultEmbed("Apply Settings");
		eb.setDescription("**" + displaySettings(applySettings, "enable").replace("•", "").trim() + "**");
		eb.addField("React Message Channel", displaySettings(applySettings, "messageTextChannelId"), true);
		eb.addField("Staff Message Channel", displaySettings(applySettings, "messageStaffChannelId"), true);
		eb.addField("Waiting For Invite Channel", displaySettings(applySettings, "waitingChannelId"), true);
		eb.addField("Staff Ping Role", displaySettings(applySettings, "staffPingRoleId"), true);
		eb.addField("New Channel Prefix", displaySettings(applySettings, "newChannelPrefix"), true);
		eb.addField("New Channel Category", displaySettings(applySettings, "newChannelCategory"), true);
		eb.addField("Ironman only", displaySettings(applySettings, "ironmanOnly"), true);
		eb.addField("React Message Text", displaySettings(applySettings, "messageText"), true);
		eb.addField("Accepted Message", displaySettings(applySettings, "acceptMessageText"), true);
		eb.addField("Waitlisted Message", displaySettings(applySettings, "waitlistedMessageText"), true);
		eb.addField("Denied Message", displaySettings(applySettings, "denyMessageText"), true);
		eb.addField("Requirements", displaySettings(applySettings, "applyReqs"), true);
		return eb;
	}

	private EmbedBuilder setWaitingChannel(String name, String textChannel) {
		try {
			TextChannel applyMessageTextChannel = event.getGuild().getTextChannelById(textChannel.replaceAll("[<#>]", ""));

			int responseCode = updateApplySettings(name, "waitingChannelId", applyMessageTextChannel.getId());
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply waiting for invite channel set to:** " + applyMessageTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	private EmbedBuilder createApplyGuild(String name) {
		if (name.length() > 25) {
			return defaultEmbed("Error").setDescription("Name cannot be more than 25 letters");
		}

		List<AutomatedApplication> currentApplications = database.getAllApplySettings(event.getGuild().getId());
		currentApplications.removeIf(o1 -> o1.getName() == null);

		if (currentApplications.size() == 2) {
			return defaultEmbed("Error").setDescription("You can reached the max amount of apply guilds (2/2)");
		}

		for (AutomatedApplication currentApply : currentApplications) {
			if (currentApply.getName().equalsIgnoreCase(name)) {
				return defaultEmbed("Error").setDescription(name + " name is taken");
			}
		}

		AutomatedApplication newApply = new AutomatedApplication(name);

		int responseCode = database.setApplySettings(event.getGuild().getId(), newApply);
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		EmbedBuilder eb = defaultEmbed("Settings");
		eb.setDescription("Created new apply guild with name `" + name + "`");
		return eb;
	}

	private boolean allowApplyEnable(String name) {
		JsonObject currentSettings = database.getApplySettings(event.getGuild().getId(), name).getAsJsonObject();
		currentSettings.remove("previousMessageId");
		currentSettings.remove("applyUsersCache");
		currentSettings.remove("waitlistedMessageText");
		currentSettings.remove("applyReqs");
		currentSettings.remove("ironmanOnly");
		currentSettings.remove("waitingChannelId");

		for (String key : getJsonKeys(currentSettings)) {
			if (higherDepth(currentSettings, key).getAsString().length() == 0) {
				return false;
			}
		}
		return true;
	}

	private EmbedBuilder setApplyEnable(String name, String enable) {
		if (enable.equalsIgnoreCase("true") || enable.equalsIgnoreCase("false")) {
			int responseCode = updateApplySettings(name, "enable", enable);
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Apply:** " +
				(enable.equalsIgnoreCase("true") ? "enabled" : "disabled") +
				"\nRun `" +
				getGuildPrefix(event.getGuild().getId()) +
				"reload` to reload the settings"
			);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setIsIronman(String name, String isIronman) {
		if (isIronman.equalsIgnoreCase("true") || isIronman.equalsIgnoreCase("false")) {
			int responseCode = updateApplySettings(name, "ironmanOnly", isIronman.toLowerCase());
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Ironman only:** " + (isIronman.equalsIgnoreCase("true") ? "enabled" : "disabled"));
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setApplyMessageTextChannelId(String name, String textChannel) {
		try {
			TextChannel applyMessageTextChannel = event.getGuild().getTextChannelById(textChannel.replaceAll("[<#>]", ""));

			int responseCode = updateApplySettings(name, "messageTextChannelId", applyMessageTextChannel.getId());
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply text channel set to:** " + applyMessageTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	private EmbedBuilder setApplyMessageStaffChannelId(String name, String textChannel) {
		try {
			TextChannel staffTextChannel = event.getGuild().getTextChannelById(textChannel.replaceAll("[<#>]", ""));
			int responseCode = updateApplySettings(name, "messageStaffChannelId", staffTextChannel.getId());
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply staff channel set to:** " + staffTextChannel.getAsMention());
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Text Channel");
	}

	private EmbedBuilder setApplyNewChannelPrefix(String name, String channelPrefix) {
		if (channelPrefix.length() > 0) {
			if (EmojiParser.parseToAliases(channelPrefix).length() > 25) {
				return defaultEmbed("Error").setDescription("Prefix cannot be longer than 25 letters!");
			}
			int responseCode = updateApplySettings(name, "newChannelPrefix", EmojiParser.parseToAliases(channelPrefix));
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply new channel prefix set to:** " + channelPrefix);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setApplyMessageText(String name, String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return defaultEmbed("Error").setDescription("Text cannot be longer than 1500 letters!");
			}
			int responseCode = updateApplySettings(name, "messageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setApplyAcceptMessageText(String name, String verifyText) {
		if (verifyText.length() > 0) {
			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return defaultEmbed("Error").setDescription("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateApplySettings(name, "acceptMessageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply accept message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setApplyWaitListMessageText(String name, String verifyText) {
		if (verifyText.length() > 0) {
			if (verifyText.equalsIgnoreCase("none")) {
				int responseCode = updateVerifySettings("waitlistedMessageText", "none");

				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Waitlist message disabled**");
				return eb;
			}

			if (EmojiParser.parseToAliases(verifyText).length() > 1500) {
				return defaultEmbed("Error").setDescription("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateApplySettings(name, "waitlistedMessageText", EmojiParser.parseToAliases(verifyText));
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Waitlisted message set to:** " + verifyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setApplyDenyMessageText(String name, String denyText) {
		if (denyText.length() > 0) {
			if (EmojiParser.parseToAliases(denyText).length() > 1500) {
				return defaultEmbed("Error").setDescription("Text cannot be longer than 1500 letters!");
			}

			int responseCode = updateApplySettings(name, "denyMessageText", EmojiParser.parseToAliases(denyText));
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply deny message set to:** " + denyText);
			return eb;
		}
		return defaultEmbed("Invalid Input");
	}

	private EmbedBuilder setApplyStaffPingRoleId(String name, String staffPingRoleMention) {
		try {
			Role verifyGuildRole = event.getGuild().getRoleById(staffPingRoleMention.replaceAll("[<@&>]", ""));
			if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
				int responseCode = updateApplySettings(name, "staffPingRoleId", verifyGuildRole.getId());
				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Apply staff ping role set to:** " + verifyGuildRole.getAsMention());
				return eb;
			}
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Role");
	}

	private EmbedBuilder setApplyNewChannelCategory(String name, String messageCategory) {
		try {
			net.dv8tion.jda.api.entities.Category applyCategory = event.getGuild().getCategoryById(messageCategory.replaceAll("[<#>]", ""));
			int responseCode = updateApplySettings(name, "newChannelCategory", applyCategory.getId());
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription("**Apply new channel category set to:** <#" + applyCategory.getId() + ">");
			return eb;
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Guild Category");
	}

	private EmbedBuilder removeApplyRequirement(String name, String reqNumber) {
		JsonArray currentReqs;
		try {
			currentReqs = database.getApplyReqs(event.getGuild().getId(), name).getAsJsonArray();
		} catch (Exception ignored) {
			return defaultEmbed("Error").setDescription("Unable to get current settings");
		}

		try {
			JsonElement req = currentReqs.get(Integer.parseInt(reqNumber) - 1);
			currentReqs.remove(Integer.parseInt(reqNumber) - 1);

			int responseCode = database.setApplyReqs(event.getGuild().getId(), name, currentReqs);

			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			EmbedBuilder eb = defaultEmbed("Settings");
			eb.setDescription(
				"**Removed application requirement of:**\n• Slayer - " +
				higherDepth(req, "slayerReq").getAsInt() +
				"\n• Skills - " +
				higherDepth(req, "skillsReq").getAsInt() +
				"\n• Catacombs - " +
				higherDepth(req, "catacombsReq").getAsInt() +
				"\n• Weight - " +
				higherDepth(req, "weightReq").getAsInt()
			);
			return eb;
		} catch (Exception ignored) {
			return defaultEmbed("Error")
				.setDescription(
					"Invalid requirement number. Run `" +
					getGuildPrefix(event.getGuild().getId()) +
					"settings apply` to see the current apply requirements"
				);
		}
	}

	private EmbedBuilder addApplyRequirement(String name, String reqArgs) {
		JsonArray currentReqs;
		try {
			currentReqs = database.getApplyReqs(event.getGuild().getId(), name).getAsJsonArray();
		} catch (Exception ignored) {
			return defaultEmbed("Error").setDescription("Unable to get current settings");
		}

		if (currentReqs.size() >= 3) {
			return defaultEmbed("Error").setDescription("You can only have up to 3 requirements");
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

		currentReqs.add(new Gson().toJsonTree(toAddReq));

		int responseCode = database.setApplyReqs(event.getGuild().getId(), name, currentReqs);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
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

	private int updateApplySettings(String name, String key, String newValue) {
		JsonObject newApplyJson = database.getApplySettings(event.getGuild().getId(), name).getAsJsonObject();
		newApplyJson.remove(key);
		newApplyJson.addProperty(key, newValue);
		return database.setApplySettings(event.getGuild().getId(), newApplyJson);
	}

	/* Mee6 */
	private EmbedBuilder getMee6DataSettings() {
		JsonElement curSettings = database.getMee6Settings(event.getGuild().getId());
		EmbedBuilder eb = defaultEmbed("Settings");
		eb.appendDescription(
			"**• Enable:** " + (higherDepth(curSettings, "enable") != null ? higherDepth(curSettings, "enable").getAsBoolean() : "false")
		);
		JsonArray curRoles = higherDepth(curSettings, "mee6Ranks").getAsJsonArray();
		if (curRoles.size() == 0) {
			eb.appendDescription("\n**• Leveling roles:** None");
		} else {
			for (JsonElement curRole : curRoles) {
				eb.appendDescription(
					"\n• Level " +
					higherDepth(curRole, "value").getAsString() +
					" - <@&" +
					higherDepth(curRole, "roleId").getAsString() +
					">"
				);
			}
		}

		return eb;
	}

	private EmbedBuilder setMee6Enable(boolean enable) {
		if (!enable) {
			int responseCode = updateMee6Enable("false");
			if (responseCode != 200) {
				return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
			}

			return defaultEmbed("Settings").setDescription("Disabled Mee6 roles");
		}

		JsonElement curSettings = database.getMee6Settings(event.getGuild().getId());

		if (higherDepth(curSettings, "mee6Ranks").getAsJsonArray().size() == 0) {
			return defaultEmbed("Error").setDescription("There must be at least one leveling role set");
		}

		try {
			if (
				higherDepth(getJson("https://mee6.xyz/api/plugins/levels/leaderboard/" + event.getGuild().getId()), "players")
					.getAsJsonArray()
					.size() ==
				0
			) {
				return defaultEmbed("Error").setDescription("The Mee6 isn't public for this server");
			}
		} catch (Exception e) {
			return defaultEmbed("Error").setDescription("The Mee6 isn't public for this server");
		}

		int responseCode = updateMee6Enable("true");
		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		return defaultEmbed("Settings").setDescription("Enabled Mee6 roles");
	}

	private EmbedBuilder addMee6Role(String level, String roleMention) {
		try {
			Role verifyGuildRole = event.getGuild().getRoleById(roleMention.replaceAll("[<@&>]", ""));
			if (!(verifyGuildRole.isPublicRole() || verifyGuildRole.isManaged())) {
				int intLevel;
				try {
					intLevel = Integer.parseInt(level);
				} catch (Exception e) {
					return defaultEmbed("Error").setDescription("The level must be an integer");
				}

				if (intLevel <= 0 || intLevel >= 250) {
					return defaultEmbed("Error").setDescription("The level must be more than 0 and less than 250");
				}

				JsonObject curSettings = database.getMee6Settings(event.getGuild().getId()).getAsJsonObject();
				JsonArray curRanks = curSettings.get("mee6Ranks").getAsJsonArray();

				if (curRanks.size() >= 10) {
					return defaultEmbed("You have reached the max amount of Mee6 roles (10/10)");
				}

				for (JsonElement rank : curRanks) {
					if (higherDepth(rank, "value").getAsInt() == intLevel) {
						curRanks.remove(rank);
						break;
					}
				}

				curRanks.add(new Gson().toJsonTree(new RoleObject("" + intLevel, verifyGuildRole.getId())));
				curSettings.remove("mee6Ranks");
				curSettings.add("mee6Ranks", curRanks);

				int responseCode = database.setMee6Settings(event.getGuild().getId(), curSettings);
				if (responseCode != 200) {
					return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
				}

				EmbedBuilder eb = defaultEmbed("Settings");
				eb.setDescription("**Added Mee6 Role:** rank " + intLevel + " - " + verifyGuildRole.getAsMention());
				return eb;
			}
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid Role");
	}

	private EmbedBuilder removeMee6Role(String level) {
		try {
			int intLevel;
			try {
				intLevel = Integer.parseInt(level);
			} catch (Exception e) {
				return defaultEmbed("Invalid level");
			}

			if (intLevel <= 0 || intLevel >= 250) {
				return defaultEmbed("Invalid level");
			}

			JsonObject curSettings = database.getMee6Settings(event.getGuild().getId()).getAsJsonObject();
			JsonArray curRanks = curSettings.get("mee6Ranks").getAsJsonArray();

			for (JsonElement rank : curRanks) {
				if (higherDepth(rank, "value").getAsInt() == intLevel) {
					curRanks.remove(rank);
					if (curRanks.size() == 0) {
						curSettings.remove("enable");
						curSettings.addProperty("enable", "false");
					}
					curSettings.remove("mee6Ranks");
					curSettings.add("mee6Ranks", curRanks);

					int responseCode = database.setMee6Settings(event.getGuild().getId(), curSettings);
					if (responseCode != 200) {
						return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
					}

					EmbedBuilder eb = defaultEmbed("Settings");
					eb.setDescription("**Removed Mee6 Role:** rank " + intLevel);
					return eb;
				}
			}
		} catch (Exception ignored) {}
		return defaultEmbed("Invalid level");
	}

	private int updateMee6Enable(String newValue) {
		JsonObject newApplyJson = database.getMee6Settings(event.getGuild().getId()).getAsJsonObject();
		newApplyJson.remove("enable");
		newApplyJson.addProperty("enable", newValue);
		return database.setMee6Settings(event.getGuild().getId(), newApplyJson);
	}

	/* Misc */
	private String displaySettings(JsonElement jsonSettings, String settingName) {
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
					case "roleId":
						try {
							return "<@&" + currentSettingValue + ">";
						} catch (PermissionException e) {
							if (e.getMessage().contains("Missing permission")) {
								return ("Missing permission: " + e.getMessage().split("Missing permission: ")[1]);
							}
						}
						break;
					case "newChannelCategory":
						try {
							return ("<#" + event.getGuild().getCategoryById(currentSettingValue).getId() + ">");
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
							JsonElement guildJson = getJson(
								"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + currentSettingValue
							);
							return higherDepth(guildJson, "guild.name").getAsString();
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

	private EmbedBuilder setPrefix(String prefix) {
		if (prefix.length() == 0 || prefix.length() > 5) {
			return defaultEmbed("Error").setDescription("The prefix must be a least on character and no more than 5");
		}

		int responseCode = database.setPrefix(event.getGuild().getId(), prefix);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		guildMap.get(event.getGuild().getId()).setPrefix(prefix);

		return defaultEmbed("Settings").setDescription("**Set prefix to:** " + prefix);
	}

	private EmbedBuilder deletePrefix() {
		int responseCode = database.setPrefix(event.getGuild().getId(), null);

		if (responseCode != 200) {
			return defaultEmbed("Error").setDescription("API returned response code " + responseCode);
		}

		guildMap.get(event.getGuild().getId()).setPrefix(DEFAULT_PREFIX);

		return defaultEmbed("Settings").setDescription("**Reset the prefix to:** " + DEFAULT_PREFIX);
	}
}
