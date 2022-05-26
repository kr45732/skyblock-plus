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

package com.skyblockplus.features.apply;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.getNameHistory;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.*;
import com.skyblockplus.features.apply.log.ApplyLog;
import com.skyblockplus.features.apply.log.LogMessage;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class ApplyUser implements Serializable {

	public String applyingUserId;
	public String currentSettingsString;
	public String guildId;
	public final Map<String, String> profileEmojiToName = new LinkedHashMap<>();
	public String applicationChannelId;
	public String reactMessageId;
	public int state = 0;
	public String staffChannelId;
	public boolean logApplication = false;
	public final List<LogMessage> logs = new ArrayList<>();
	public String applySubmitedMessageId;
	// Embed
	public String playerSlayer;
	public String playerSkills;
	public String playerCatacombs;
	public String playerWeight;
	public String playerUsername;
	public String nameHistory = "";
	public String playerCoins;
	public String ironmanSymbol = "";
	public String playerProfileName;
	public String failCause;

	public ApplyUser(ButtonInteractionEvent event, JsonElement currentSettings, String playerUsername) {
		try {
			logCommand(event.getGuild(), event.getUser(), "apply " + playerUsername);

			currentSettings.getAsJsonObject().remove("applyUsersCache");
			this.applyingUserId = event.getUser().getId();
			this.currentSettingsString = gson.toJson(currentSettings);
			this.guildId = event.getGuild().getId();
			this.playerUsername = playerUsername;
			try {
				this.logApplication = jda.getTextChannelById(higherDepth(currentSettings, "applyLogChannel").getAsString()) != null;
			} catch (Exception ignored) {}
			Category applyCategory = event.getGuild().getCategoryById(higherDepth(currentSettings, "applyCategory").getAsString());

			if (applyCategory.getChannels().size() == 50) {
				failCause =
					client.getError() +
					" Unable to create a new application since the application category has reached 50/50 channels. Please report this to the server's staff.";
				return;
			}

			ChannelAction<TextChannel> applicationChannelAction = applyCategory
				.createTextChannel("apply-" + playerUsername)
				.syncPermissionOverrides()
				.addPermissionOverride(event.getGuild().getSelfMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
				.addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
				.addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
			try {
				for (JsonElement staffPingRole : higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray()) {
					applicationChannelAction =
						applicationChannelAction.addPermissionOverride(
							event.getGuild().getRoleById(staffPingRole.getAsString()),
							EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
							null
						);
				}
			} catch (Exception ignored) {}

			TextChannel applicationChannel;
			try {
				applicationChannel = applicationChannelAction.complete();
			} catch (PermissionException e) {
				failCause = client.getError() + " Missing permission: " + e.getPermission().getName();
				return;
			}
			this.applicationChannelId = applicationChannel.getId();

			Player player = new Player(playerUsername);
			String[] profileNames = player.getAllProfileNames(Player.Gamemode.of(higherDepth(currentSettings, "applyGamemode", "all")));

			getNameHistory(player.getUuid()).forEach(i -> nameHistory += "\n• " + fixUsername(i));
			if (profileNames.length == 1) {
				applicationChannel
					.sendMessage(
						event.getUser().getAsMention() +
						" this is your application for " +
						capitalizeString(higherDepth(currentSettings, "guildName").getAsString().replace("_", " "))
					)
					.complete();
				caseOne(profileNames[0], currentSettings, applicationChannel);
			} else {
				EmbedBuilder welcomeEb =
					this.defaultPlayerEmbed()
						.setDescription(
							"Please react with the emoji that corresponds to the profile you want to apply with or react with " +
							client.getError() +
							" to cancel the application.\n"
						);

				for (String profileName : profileNames) {
					String profileEmoji = profileNameToEmoji(profileName);
					this.profileEmojiToName.put(profileEmoji, profileName);
					profileEmoji = profileEmoji.contains(":") ? "<:" + profileEmoji + ">" : profileEmoji;
					welcomeEb.appendDescription(
						"\n" +
						profileEmoji +
						" - [" +
						capitalizeString(profileName) +
						"](" +
						skyblockStatsLink(player.getUsername(), profileName) +
						")"
					);
				}
				welcomeEb.appendDescription(
					"\n↩️ - [Last played profile (" +
					player.getProfileName() +
					")](" +
					skyblockStatsLink(player.getUsername(), player.getProfileName()) +
					")"
				);
				profileEmojiToName.put("↩️", player.getProfileName());

				Message reactMessage = applicationChannel
					.sendMessage(
						event.getUser().getAsMention() +
						" this is your application for " +
						capitalizeString(higherDepth(currentSettings, "guildName").getAsString().replace("_", " "))
					)
					.setEmbeds(welcomeEb.build())
					.complete();
				this.reactMessageId = reactMessage.getId();

				for (String profileEmoji : profileEmojiToName.keySet()) {
					reactMessage.addReaction(profileEmoji).complete();
				}

				reactMessage.addReaction(client.getError().replaceAll("[<>]", "")).queue();
			}
		} catch (Exception e) {
			AutomaticGuild.getLogger().error(guildId, e);
			failCause = e.getMessage();
		}
	}

	public boolean onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getMessageId().equals(reactMessageId)) {
			return false;
		}

		TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
		Message reactMessage = applicationChannel.retrieveMessageById(reactMessageId).complete();
		JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

		if (!event.getUser().getId().equals(applyingUserId) && !guildMap.get(guildId).isAdmin(event.getMember())) {
			JsonArray staffPingRoles = higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray();
			boolean hasStaffRole = false;
			if (staffPingRoles.size() != 0) {
				for (JsonElement staffPingRole : staffPingRoles) {
					if (event.getMember().getRoles().contains(event.getGuild().getRoleById(staffPingRole.getAsString()))) {
						hasStaffRole = true;
						break;
					}
				}
			}

			if (!hasStaffRole) {
				return false;
			}
		}

		if (state == 0) {
			reactMessage.clearReactions().queue();
			if (event.getReactionEmote().getAsReactionCode().equals(client.getError())) {
				event.getChannel().sendMessageEmbeds(defaultEmbed("Closing channel").build()).queue();
				event
					.getGuild()
					.getTextChannelById(event.getChannel().getId())
					.delete()
					.reason("Application canceled")
					.queueAfter(10, TimeUnit.SECONDS);
				return true;
			} else if (profileEmojiToName.containsKey(event.getReactionEmote().getAsReactionCode())) {
				caseOne(profileEmojiToName.get(event.getReactionEmote().getAsReactionCode()), currentSettings, applicationChannel);
			}
		}

		return false;
	}

	public void caseOne(String profile, JsonElement currentSettings, TextChannel applicationChannel) {
		Player player = new Player(playerUsername, profile);

		JsonArray currentReqs = higherDepth(currentSettings, "applyReqs").getAsJsonArray();

		boolean meetReqs = false;
		StringBuilder missingReqsStr = new StringBuilder();
		if (currentReqs.size() == 0) {
			meetReqs = true;
		} else {
			for (JsonElement req : currentReqs) {
				int slayerReq = higherDepth(req, "slayerReq", 0);
				int skillsReq = higherDepth(req, "skillsReq", 0);
				int cataReq = higherDepth(req, "catacombsReq", 0);
				int weightReq = higherDepth(req, "weightReq", 0);

				if (
					player.getTotalSlayer() >= slayerReq &&
					player.getSkillAverage() >= skillsReq &&
					player.getCatacombs().getProgressLevel() >= cataReq &&
					player.getWeight() >= weightReq
				) {
					meetReqs = true;
					break;
				} else {
					boolean isFirst = true;
					if (slayerReq > 0) {
						missingReqsStr.append("• " + "Slayer - ").append(formatNumber(slayerReq));
						isFirst = false;
					}
					if (skillsReq > 0) {
						missingReqsStr.append(isFirst ? "• " : " | ").append("Skill Average - ").append(formatNumber(skillsReq));
						isFirst = false;
					}
					if (cataReq > 0) {
						missingReqsStr.append(isFirst ? "• " : " | ").append("Catacombs - ").append(formatNumber(cataReq));
						isFirst = false;
					}
					if (weightReq > 0) {
						missingReqsStr.append(isFirst ? "• " : " | ").append("Weight - ").append(formatNumber(weightReq));
					}

					missingReqsStr.append("\n");
				}
			}
		}

		Message reactMessage;
		if (!meetReqs) {
			EmbedBuilder reqEmbed = defaultEmbed("Does not meet requirements");
			reqEmbed.setDescription(
				"**Your statistics:**\n• Slayer - " +
				formatNumber(player.getTotalSlayer()) +
				" | Skill Average - " +
				(player.getSkillAverage() == -1 ? "API disabled" : roundAndFormat(player.getSkillAverage())) +
				" | Catacombs - " +
				roundAndFormat(player.getCatacombs().getProgressLevel()) +
				" | Weight - " +
				roundAndFormat(player.getWeight())
			);
			reqEmbed.appendDescription("\n\n**You do not meet any of the following requirements:**\n" + missingReqsStr);
			reqEmbed.appendDescription(
				"\nIf you any of these value seem incorrect, then make sure all your APIs are enabled and/or try relinking"
			);

			playerSlayer = formatNumber(player.getTotalSlayer());
			playerSkills = roundAndFormat(player.getSkillAverage());
			playerSkills = playerSkills.equals("-1") ? "API disabled" : playerSkills;
			playerCatacombs = roundAndFormat(player.getCatacombs().getProgressLevel());
			playerWeight = roundAndFormat(player.getWeight());

			reactMessage =
				applicationChannel
					.sendMessageEmbeds(reqEmbed.build())
					.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
					.complete();
			this.reactMessageId = reactMessage.getId();
			state = 3;
		} else {
			try {
				playerSlayer = formatNumber(player.getTotalSlayer());
			} catch (Exception e) {
				playerSlayer = "0";
			}

			try {
				playerSkills = roundAndFormat(player.getSkillAverage());
			} catch (Exception e) {
				playerSkills = "API disabled";
			}

			playerSkills = playerSkills.equals("-1") ? "API disabled" : playerSkills;

			try {
				playerCatacombs = roundAndFormat(player.getCatacombs().getProgressLevel());
			} catch (Exception e) {
				playerCatacombs = "0";
			}

			try {
				playerWeight = roundAndFormat(player.getWeight());
			} catch (Exception e) {
				playerWeight = "API disabled";
			}
			playerUsername = player.getUsername();

			ironmanSymbol = player.getSymbol(" ");
			playerProfileName = player.getProfileName();
			double bankCoins = player.getBankBalance();
			playerCoins = (bankCoins != -1 ? simplifyNumber(bankCoins) : "API disabled") + " + " + simplifyNumber(player.getPurseCoins());

			EmbedBuilder statsEmbed = player.defaultPlayerEmbed();
			statsEmbed.addField("Weight", playerWeight, true);
			statsEmbed.addField("Total slayer", playerSlayer, true);
			statsEmbed.addField("Progress skill level", playerSkills, true);
			statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);
			statsEmbed.addField("Bank & purse coins", playerCoins, true);

			List<Button> buttons = new ArrayList<>();
			buttons.add(Button.success("apply_user_submit", "Submit"));
			if (!profileEmojiToName.isEmpty()) {
				buttons.add(Button.primary("apply_user_retry", "Retry"));
			}
			buttons.add(Button.danger("apply_user_cancel", "Cancel"));

			reactMessage = applicationChannel.sendMessageEmbeds(statsEmbed.build()).setActionRow(buttons).complete();
			this.reactMessageId = reactMessage.getId();
			state = 1;
		}
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultEmbed(fixUsername(playerUsername) + ironmanSymbol, skyblockStatsLink(playerUsername, playerProfileName));
	}

	public boolean onButtonClick(ButtonInteractionEvent event, ApplyGuild parent, boolean isWait) {
		JsonElement currentSettings = JsonParser.parseString(currentSettingsString);
		if (!event.getUser().getId().equals(applyingUserId) && !guildMap.get(guildId).isAdmin(event.getMember())) {
			JsonArray staffPingRoles = higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray();
			boolean hasStaffRole = false;
			if (staffPingRoles.size() != 0) {
				for (JsonElement staffPingRole : staffPingRoles) {
					if (event.getMember().getRoles().contains(event.getGuild().getRoleById(staffPingRole.getAsString()))) {
						hasStaffRole = true;
						break;
					}
				}
			}

			if (!hasStaffRole) {
				return false;
			}
		}

		switch (state) {
			case 1:
				switch (event.getButton().getId()) {
					case "apply_user_submit" -> {
						event.getMessage().editMessageComponents().queue();
						EmbedBuilder finishApplyEmbed = defaultEmbed("Application Sent");
						finishApplyEmbed.setDescription("You will be notified once staff review your application");
						event
							.getHook()
							.editOriginalEmbeds(finishApplyEmbed.build())
							.setActionRow(Button.danger("apply_user_cancel", "Cancel Application"))
							.queue(m -> applySubmitedMessageId = m.getId());
						state = 2;
						TextChannel staffChannel = jda.getTextChannelById(higherDepth(currentSettings, "applyStaffChannel").getAsString());
						staffChannelId = staffChannel.getId();
						EmbedBuilder applyPlayerStats = defaultPlayerEmbed();
						applyPlayerStats.addField("Weight", playerWeight, true);
						applyPlayerStats.addField("Total slayer", playerSlayer, true);
						applyPlayerStats.addField("Progress skill level", playerSkills, true);
						applyPlayerStats.addField("Catacombs level", playerCatacombs, true);
						applyPlayerStats.addField("Bank & purse coins", playerCoins, true);
						double playerNetworth = NetworthExecute.getTotalNetworth(playerUsername, playerProfileName);
						applyPlayerStats.addField(
							"Networth",
							playerNetworth == -1 ? "Inventory API disabled" : roundAndFormat(playerNetworth),
							true
						);
						if (!nameHistory.isEmpty()) {
							applyPlayerStats.addField("Name history", nameHistory, true);
						}
						applyPlayerStats.setThumbnail("https://cravatar.eu/helmavatar/" + playerUsername + "/64.png");
						String waitlistMsg = higherDepth(currentSettings, "applyWaitlistMessage", null);
						List<Button> row = new ArrayList<>();
						row.add(Button.success("apply_user_accept", "Accept"));
						if (waitlistMsg != null && waitlistMsg.length() > 0 && !waitlistMsg.equals("none")) {
							row.add(Button.primary("apply_user_waitlist", "Waitlist"));
						}
						row.add(Button.danger("apply_user_deny", "Deny"));
						String staffPingMentions = streamJsonArray(higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray())
							.map(r -> "<@&" + r.getAsString() + ">")
							.collect(Collectors.joining(" "));
						Message reactMessage = staffPingMentions.isEmpty()
							? staffChannel.sendMessageEmbeds(applyPlayerStats.build()).setActionRow(row).complete()
							: staffChannel.sendMessage(staffPingMentions).setEmbeds(applyPlayerStats.build()).setActionRow(row).complete();
						reactMessageId = reactMessage.getId();
						return true;
					}
					case "apply_user_retry" -> {
						EmbedBuilder retryEmbed = defaultPlayerEmbed();
						retryEmbed.setDescription(
							"Please react with the emoji that corresponds to the profile you want to apply with or react with " +
							client.getError() +
							" to cancel the application."
						);
						for (Map.Entry<String, String> profileEntry : profileEmojiToName.entrySet()) {
							String profileEmoji = profileEntry.getKey().contains(":")
								? "<:" + profileEntry.getKey() + ">"
								: profileEntry.getKey();
							if (profileEntry.getKey().equals("↩️")) {
								String lastPlayedProfile = profileEmojiToName.get("↩️");
								retryEmbed.appendDescription(
									"\n" +
									profileEmoji +
									" - [Last played profile (" +
									lastPlayedProfile +
									")](" +
									skyblockStatsLink(playerUsername, lastPlayedProfile) +
									")"
								);
							} else {
								retryEmbed.appendDescription(
									"\n" +
									profileEmoji +
									" - [" +
									capitalizeString(profileEntry.getValue()) +
									"](" +
									skyblockStatsLink(playerUsername, profileEntry.getValue()) +
									")"
								);
							}
						}
						event.getMessage().editMessageComponents().complete();
						Message reactMessage = event.getHook().editOriginalEmbeds(retryEmbed.build()).complete();
						this.reactMessageId = reactMessage.getId();
						for (String profileEmoji : profileEmojiToName.keySet()) {
							reactMessage.addReaction(profileEmoji).complete();
						}
						reactMessage.addReaction(client.getError().replaceAll("[<>]", "")).queue();
						state = 0;
						return true;
					}
					case "apply_user_cancel" -> {
						event.getMessage().editMessageComponents().queue();
						event.getHook().editOriginalEmbeds(defaultEmbed("Canceling application & closing channel").build()).complete();
						event
							.getGuild()
							.getTextChannelById(event.getChannel().getId())
							.delete()
							.reason("Application canceled")
							.queueAfter(10, TimeUnit.SECONDS);
						parent.applyUserList.remove(this);
						return true;
					}
				}
				break;
			case 2:
				TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
				Message reactMessage = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
				switch (event.getButton().getId()) {
					case "apply_user_accept":
						event.getMessage().editMessageComponents().queue();
						reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
						try {
							applicationChannel.editMessageComponentsById(applySubmitedMessageId).queue();
						}catch(Exception ignored){}

						event
							.getHook()
							.editOriginal(
								fixUsername(playerUsername) +
								" (<@" +
								applyingUserId +
								">) was accepted by " +
								event.getUser().getAsMention()
							)
							.queue();

						TextChannel waitInviteChannel = null;
						try {
							waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "applyWaitingChannel").getAsString());
						} catch (Exception ignored) {}

						MessageAction action = applicationChannel
							.sendMessage("<@" + applyingUserId + ">")
							.setEmbeds(
								defaultEmbed("Application Accepted")
									.setDescription(higherDepth(currentSettings, "applyAcceptMessage").getAsString())
									.build()
							);
						if (waitInviteChannel == null) {
							action = action.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"));
							try {
								event
									.getGuild()
									.addRoleToMember(
										UserSnowflake.fromId(applyingUserId),
										jda.getRoleById(higherDepth(currentSettings, "guildMemberRole").getAsString())
									)
									.queue();
							} catch (Exception ignored) {}
						} else {
							action =
								action.setActionRow(
									Button.danger(
										"apply_user_cancel_" +
										waitInviteChannel.getId() +
										"_" +
										waitInviteChannel
											.sendMessageEmbeds(
												defaultEmbed("Waiting for invite").setDescription("`" + playerUsername + "`").build()
											)
											.setActionRow(
												Button.success(
													"apply_user_wait_" +
													higherDepth(currentSettings, "guildName").getAsString() +
													"_" +
													applicationChannelId +
													"_" +
													applyingUserId +
													"_" +
													higherDepth(currentSettings, "guildMemberRole", "null"),
													"Invited"
												)
											)
											.complete()
											.getId(),
										"Cancel Application"
									)
								);
						}

						state = 3;
						this.reactMessageId = action.complete().getId();
						return true;
					case "apply_user_waitlist":
						if (
							!higherDepth(currentSettings, "applyWaitlistMessage", "").isEmpty() &&
							!higherDepth(currentSettings, "applyWaitlistMessage", "").equals("none")
						) {
							event.getMessage().editMessageComponents().queue();
							reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
							try {
								applicationChannel.editMessageComponentsById(applySubmitedMessageId).queue();
							}catch(Exception ignored){}

							event
								.getHook()
								.editOriginal(
									fixUsername(playerUsername) +
									" (<@" +
									applyingUserId +
									">) was waitlisted by " +
									event.getUser().getAsMention()
								)
								.queue();

							waitInviteChannel = null;
							try {
								waitInviteChannel =
									jda.getTextChannelById(higherDepth(currentSettings, "applyWaitingChannel").getAsString());
							} catch (Exception ignored) {}

							action =
								applicationChannel
									.sendMessage("<@" + applyingUserId + ">")
									.setEmbeds(
										defaultEmbed("Application Waitlisted")
											.setDescription(higherDepth(currentSettings, "applyWaitlistMessage").getAsString())
											.build()
									);
							if (waitInviteChannel == null) {
								action = action.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"));
								try {
									event
										.getGuild()
										.addRoleToMember(
											UserSnowflake.fromId(applyingUserId),
											jda.getRoleById(higherDepth(currentSettings, "guildMemberRole").getAsString())
										)
										.queue();
								} catch (Exception ignored) {}
							} else {
								action =
									action.setActionRow(
										Button.danger(
											"apply_user_cancel_" +
											waitInviteChannel.getId() +
											"_" +
											waitInviteChannel
												.sendMessageEmbeds(
													defaultEmbed("Waiting for invite").setDescription("`" + playerUsername + "`").build()
												)
												.setActionRow(
													Button.success(
														"apply_user_wait_" +
														higherDepth(currentSettings, "guildName").getAsString() +
														"_" +
														applicationChannelId +
														"_" +
														applyingUserId +
														"_" +
														higherDepth(currentSettings, "guildMemberRole", "null"),
														"Invited"
													)
												)
												.complete()
												.getId(),
											"Cancel Application"
										)
									);
							}

							state = 3;
							this.reactMessageId = action.complete().getId();
						}
						return true;
					case "apply_user_deny":
						event.getMessage().editMessageComponents().queue();
						reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);
						try {
							applicationChannel.editMessageComponentsById(applySubmitedMessageId).queue();
						}catch(Exception ignored){}

						try {
							event
								.getHook()
								.editOriginal(
									playerUsername + " (<@" + applyingUserId + ">) was denied by " + event.getUser().getAsMention()
								)
								.queue();
						} catch (Exception e) {
							event.getHook().editOriginal(playerUsername + " was denied by " + event.getUser().getAsMention()).queue();
						}

						state = 3;
						this.reactMessageId =
							applicationChannel
								.sendMessage("<@" + applyingUserId + ">")
								.setEmbeds(
									defaultEmbed("Application Not Accepted")
										.setDescription(higherDepth(currentSettings, "applyDenyMessage").getAsString())
										.build()
								)
								.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
								.complete()
								.getId();
						return true;
				}
				break;
			case 3:
				if (event.getComponentId().startsWith("apply_user_cancel_")) {
					parent.applyUserList.remove(this);
					event.getMessage().editMessageComponents().queue();
					event.getHook().editOriginalEmbeds(defaultEmbed("Canceling application & closing channel").build()).queue();
					event
						.getGuild()
						.getTextChannelById(event.getChannel().getId())
						.delete()
						.reason("Application canceled")
						.queueAfter(10, TimeUnit.SECONDS);

					String[] channelMessageSplit = event.getComponentId().split("apply_user_cancel_")[1].split("_");
					event.getGuild().getTextChannelById(channelMessageSplit[0]).deleteMessageById(channelMessageSplit[1]).queue();
					return true;
				}

				TextChannel appChannel = jda.getTextChannelById(applicationChannelId);
				if (!isWait) {
					event.getMessage().editMessageComponents().queue();
					event.getHook().editOriginalEmbeds(defaultEmbed("Closing Channel").build()).complete();
				} else {
					appChannel.sendMessageEmbeds(defaultEmbed("Closing Channel").build()).complete();
				}
				appChannel.delete().reason("Application closed").queueAfter(10, TimeUnit.SECONDS);
				parent.applyUserList.remove(this);
				if (logApplication) {
					try {
						File logFile = new File(
							"src/main/java/com/skyblockplus/json/application_transcripts/" + applicationChannelId + ".json"
						);
						try (Writer writer = new FileWriter(logFile)) {
							formattedGson.toJson(logs, writer);
							writer.flush();
						}
						event
							.getGuild()
							.getTextChannelById(higherDepth(currentSettings, "applyLogChannel").getAsString())
							.sendFile(logFile, playerUsername + ".json")
							.queue(m ->
								m
									.editMessageEmbeds(
										defaultEmbed("Application Log")
											.addField("Applicant", playerUsername, true)
											.addField(
												"Guild Name",
												capitalizeString(higherDepth(currentSettings, "guildName").getAsString().replace("_", " ")),
												true
											)
											.addField(
												"Direct Transcript",
												"[Link](https://skyblock-plus-logs.vercel.app/logs?url=" +
												m.getAttachments().get(0).getUrl() +
												")",
												true
											)
											.addField(
												"Users in Transcript",
												String.join(
													"\n",
													logs.stream().map(logM -> logM.getUser().getName()).collect(Collectors.toSet())
												),
												true
											)
											.build()
									)
									.queue()
							);

						logFile.delete();
					} catch (Exception e) {
						AutomaticGuild.getLogger().error(guildId, e);
					}
				}
				return true;
		}

		return false;
	}

	public boolean onGuildMessageReceived(MessageReceivedEvent event) {
		if (!event.getChannel().getId().equals(applicationChannelId)) {
			return false;
		}

		if (!logApplication) {
			return true;
		}

		logs.add(ApplyLog.toLog(event.getMessage()));
		return true;
	}

	public boolean onGuildMessageUpdate(MessageUpdateEvent event) {
		if (!event.getChannel().getId().equals(applicationChannelId)) {
			return false;
		}

		if (!logApplication) {
			return true;
		}

		for (int i = 0; i < logs.size(); i++) {
			if (logs.get(i).getId().equals(event.getMessage().getId())) {
				logs.set(i, ApplyLog.toLog(event.getMessage()));
				break;
			}
		}
		return true;
	}

	public boolean onGuildMessageDelete(MessageDeleteEvent event) {
		if (!event.getChannel().getId().equals(applicationChannelId)) {
			return false;
		}

		if (!logApplication) {
			return true;
		}

		logs.removeIf(m -> m.getId().equals(event.getMessageId()));
		return true;
	}
}
