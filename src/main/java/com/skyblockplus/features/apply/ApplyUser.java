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

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.ApiHandler.getNameHistory;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class ApplyUser implements Serializable {

	public final String applyingUserId;
	public final String currentSettingsString;
	public final String guildId;
	public final Map<String, String> profileEmojiToName = new LinkedHashMap<>();
	public String applicationChannelId;
	public String reactMessageId;
	public int state = 0;
	public String staffChannelId;
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

	public ApplyUser(ButtonClickEvent event, JsonElement currentSettings, String playerUsername) {
		User applyingUser = event.getUser();
		logCommand(event.getGuild(), applyingUser, "apply " + applyingUser.getName());

		JsonObject currentSettingsObj = currentSettings.getAsJsonObject();
		currentSettingsObj.remove("applyUsersCache");
		currentSettings = currentSettingsObj.getAsJsonObject();

		this.applyingUserId = applyingUser.getId();
		this.currentSettingsString = gson.toJson(currentSettings);
		this.guildId = event.getGuild().getId();
		this.playerUsername = playerUsername;

		Category applyCategory = event.getGuild().getCategoryById(higherDepth(currentSettings, "newChannelCategory").getAsString());
		if (applyCategory.getChannels().size() == 50) {
			failCause =
				"Unable to create a new application due to the application category reaching 50/50 channels. Please report this to the server's staff.";
			return;
		}

		ChannelAction<TextChannel> applicationChannelAction = applyCategory
			.createTextChannel("apply-" + playerUsername)
			.addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
			.addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

		try {
			for (JsonElement staffPingRole : higherDepth(currentSettings, "staffPingRoles").getAsJsonArray()) {
				applicationChannelAction =
						applicationChannelAction.addPermissionOverride(
								event.getGuild().getRoleById(staffPingRole.getAsString()),
								EnumSet.of(Permission.VIEW_CHANNEL),
								null
						);
			}
		} catch (Exception ignored) {}

		TextChannel applicationChannel = applicationChannelAction.complete();

		this.applicationChannelId = applicationChannel.getId();

		boolean isIronman = false;
		try {
			isIronman = higherDepth(currentSettings, "ironmanOnly").getAsBoolean();
		} catch (Exception ignored) {}

		Player player = new Player(playerUsername);
		String[] profileNames = player.getAllProfileNames(isIronman);

		getNameHistory(player.getUuid()).forEach(i -> nameHistory += "\n• " + fixUsername(i));
		if (profileNames.length == 1) {
			applicationChannel.sendMessage(applyingUser.getAsMention()).complete();
			caseOne(profileNames[0], currentSettings, applicationChannel);
		} else {
			EmbedBuilder welcomeEb = this.defaultPlayerEmbed();
			welcomeEb.setDescription(
				"Please react with the emoji that corresponds to the profile you want to apply with or react with ❌ to cancel the application.\n"
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

			Message reactMessage = applicationChannel.sendMessage(applyingUser.getAsMention()).setEmbeds(welcomeEb.build()).complete();
			this.reactMessageId = reactMessage.getId();

			for (String profileEmoji : profileEmojiToName.keySet()) {
				reactMessage.addReaction(profileEmoji).complete();
			}

			reactMessage.addReaction("❌").queue();
		}
	}

	public boolean onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getMessageId().equals(reactMessageId)) {
			return false;
		}

		User applyingUser = jda.retrieveUserById(applyingUserId).complete();
		TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
		Message reactMessage = applicationChannel.retrieveMessageById(reactMessageId).complete();
		JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

		if (!event.getUser().getId().equals(applyingUserId) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
			JsonArray staffPingRoles = higherDepth(currentSettings, "staffPingRoles").getAsJsonArray();
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
			if (event.getReactionEmote().getAsReactionCode().equals("❌")) {
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
					missingReqsStr
						.append("• Slayer - ")
						.append(formatNumber(slayerReq))
						.append(" | Skill Average - ")
						.append(formatNumber(skillsReq))
						.append(" | Catacombs - ")
						.append(formatNumber(cataReq))
						.append(" | Weight - ")
						.append(formatNumber(weightReq))
						.append("\n");
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
				(player.getSkillAverage() == -1 ? "API disabled" : formatNumber(player.getSkillAverage())) +
				" | Catacombs - " +
				formatNumber(player.getCatacombs().getProgressLevel()) +
				" | Weight - " +
				formatNumber(player.getWeight())
			);
			reqEmbed.appendDescription("\n\n**You do not meet any of the following requirements:**\n" + missingReqsStr);
			reqEmbed.appendDescription(
				"\nIf you think these values are incorrect make sure all your APIs are enabled and/or try relinking"
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

			ironmanSymbol = player.isIronman() ? " ♻️" : "";
			playerProfileName = player.getProfileName();
			double bankCoins = player.getBankBalance();
			playerCoins = (bankCoins != -1 ? simplifyNumber(bankCoins) : "API disabled") + " + " + simplifyNumber(player.getPurseCoins());

			EmbedBuilder statsEmbed = player.defaultPlayerEmbed();
			statsEmbed.addField("Weight", playerWeight, true);
			statsEmbed.addField("Total slayer", playerSlayer, true);
			statsEmbed.addField("Progress skill level", playerSkills, true);
			statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);
			statsEmbed.addField("Bank & purse coins", playerCoins, true);

			reactMessage =
				applicationChannel
					.sendMessageEmbeds(statsEmbed.build())
					.setActionRow(
						Button.success("apply_user_submit", "Submit"),
						Button.primary("apply_user_retry", "Retry"),
						Button.danger("apply_user_cancel", "Cancel")
					)
					.complete();
			this.reactMessageId = reactMessage.getId();
			state = 1;
		}
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultEmbed(fixUsername(playerUsername) + ironmanSymbol, skyblockStatsLink(playerUsername, playerProfileName));
	}

	public boolean onButtonClick(ButtonClickEvent event, ApplyGuild parent) {
		JsonElement currentSettings = JsonParser.parseString(currentSettingsString);
		if (!event.getUser().getId().equals(applyingUserId) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
			JsonArray staffPingRoles = higherDepth(currentSettings, "staffPingRoles").getAsJsonArray();
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
					case "apply_user_submit":
						event.getMessage().editMessageComponents().queue();

						EmbedBuilder finishApplyEmbed = defaultEmbed("Thank you for applying!");
						finishApplyEmbed.setDescription("You will be notified once staff review your application");

						event.getHook().editOriginalEmbeds(finishApplyEmbed.build()).queue();

						state = 2;

						TextChannel staffChannel = jda.getTextChannelById(
							higherDepth(currentSettings, "messageStaffChannelId").getAsString()
						);
						staffChannelId = staffChannel.getId();

						EmbedBuilder applyPlayerStats = defaultPlayerEmbed();
						applyPlayerStats.addField("Weight", playerWeight, true);
						applyPlayerStats.addField("Total slayer", playerSlayer, true);
						applyPlayerStats.addField("Progress average skill level", playerSkills, true);
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
						String waitlistMsg = higherDepth(currentSettings, "waitlistedMessageText", null);

						List<Button> row = new ArrayList<>();
						row.add(Button.success("apply_user_accept", "Accept"));
						if (waitlistMsg != null && waitlistMsg.length() > 0 && !waitlistMsg.equals("none")) {
							row.add(Button.primary("apply_user_waitlist", "Waitlist"));
						}
						row.add(Button.danger("apply_user_deny", "Deny"));
						String staffPingMentions = streamJsonArray(higherDepth(currentSettings, "staffPingRoles").getAsJsonArray()).map(r -> "<@&" + r.getAsString() + ">").collect(Collectors.joining(" "));
						Message reactMessage = staffPingMentions.isEmpty()
							? staffChannel.sendMessageEmbeds(applyPlayerStats.build()).complete()
							: staffChannel
								.sendMessage(staffPingMentions)
								.setEmbeds(applyPlayerStats.build())
								.setActionRow(row)
								.complete();

						reactMessageId = reactMessage.getId();
						return true;
					case "apply_user_retry":
						EmbedBuilder retryEmbed = defaultPlayerEmbed();
						retryEmbed.setDescription(
							"Please react with the emoji that corresponds to the profile you want to apply with or react with ❌ to cancel the application."
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
						reactMessage = event.getHook().editOriginalEmbeds(retryEmbed.build()).complete();
						this.reactMessageId = reactMessage.getId();

						for (String profileEmoji : profileEmojiToName.keySet()) {
							reactMessage.addReaction(profileEmoji).complete();
						}

						reactMessage.addReaction("❌").queue();

						state = 0;
						return true;
					case "apply_user_cancel":
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
				break;
			case 2:
				TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
				User applyingUser = jda.retrieveUserById(applyingUserId).complete();
				Message reactMessage = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
				switch (event.getButton().getId()) {
					case "apply_user_accept":
						event.getMessage().editMessageComponents().queue();
						reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

						try {
							event
								.getHook()
								.editOriginal(
									fixUsername(playerUsername) +
									" (" +
									applyingUser.getAsMention() +
									") was accepted by " +
									event.getUser().getAsMention()
								)
								.queue();
						} catch (Exception e) {
							event
								.getHook()
								.editOriginal(fixUsername(playerUsername) + " was accepted by " + event.getUser().getAsMention())
								.queue();
						}

						TextChannel waitInviteChannel = null;
						try {
							waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
						} catch (Exception ignored) {}

						EmbedBuilder eb = defaultEmbed("Application Accepted");
						eb.setDescription(higherDepth(currentSettings, "acceptMessageText").getAsString());
						MessageAction action = applicationChannel.sendMessage(applyingUser.getAsMention()).setEmbeds(eb.build());
						if (waitInviteChannel == null) {
							action = action.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"));
						}

						reactMessage = action.complete();

						state = 3;
						if (waitInviteChannel != null) {
							waitInviteChannel
								.sendMessageEmbeds(defaultEmbed("Waiting for invite").setDescription("`" + playerUsername + "`").build())
								.setActionRow(
									Button.success(
										"apply_user_wait_" +
										higherDepth(currentSettings, "name").getAsString() +
										"_" +
										applicationChannelId,
										"Invited"
									)
								)
								.queue();
						}

						this.reactMessageId = reactMessage.getId();
						return true;
					case "apply_user_waitlist":
						if (
							higherDepth(currentSettings, "waitlistedMessageText") != null &&
							higherDepth(currentSettings, "waitlistedMessageText").getAsString().length() > 0 &&
							!higherDepth(currentSettings, "waitlistedMessageText").getAsString().equals("none")
						) {
							event.getMessage().editMessageComponents().queue();
							reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

							try {
								event
									.getHook()
									.editOriginal(
										fixUsername(playerUsername) +
										" (" +
										applyingUser.getAsMention() +
										") was waitlisted by " +
										event.getUser().getAsMention()
									)
									.queue();
							} catch (Exception e) {
								event
									.getHook()
									.editOriginal(fixUsername(playerUsername) + " was waitlisted by " + event.getUser().getAsMention())
									.queue();
							}

							waitInviteChannel = null;
							try {
								waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
							} catch (Exception ignored) {}
							eb = defaultEmbed("Application waitlisted");
							eb.setDescription(higherDepth(currentSettings, "waitlistedMessageText").getAsString());

							action = applicationChannel.sendMessage(applyingUser.getAsMention()).setEmbeds(eb.build());

							if (waitInviteChannel == null) {
								action = action.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"));
							}

							reactMessage = action.complete();

							state = 3;
							if (waitInviteChannel != null) {
								waitInviteChannel
									.sendMessageEmbeds(
										defaultEmbed("Waiting for invite").setDescription("`" + playerUsername + "`").build()
									)
									.setActionRow(
										Button.success(
											"apply_user_wait_" +
											higherDepth(currentSettings, "name").getAsString() +
											"_" +
											applicationChannelId,
											"Invited"
										)
									)
									.queue();
							}

							this.reactMessageId = reactMessage.getId();
						}
						return true;
					case "apply_user_deny":
						event.getMessage().editMessageComponents().queue();
						reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

						try {
							event
								.getHook()
								.editOriginal(
									playerUsername +
									" (" +
									applyingUser.getAsMention() +
									") was denied by " +
									event.getUser().getAsMention()
								)
								.queue();
						} catch (Exception e) {
							event.getHook().editOriginal(playerUsername + " was denied by " + event.getUser().getAsMention()).queue();
						}

						eb = defaultEmbed("Application Not Accepted");
						eb.setDescription(higherDepth(currentSettings, "denyMessageText").getAsString());

						reactMessage =
							applicationChannel
								.sendMessage(applyingUser.getAsMention())
								.setEmbeds(eb.build())
								.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
								.complete();
						state = 3;
						this.reactMessageId = reactMessage.getId();
						return true;
				}
				break;
			case 3:
				event.getMessage().editMessageComponents().queue();
				event.getHook().editOriginalEmbeds(defaultEmbed("Closing Channel").build()).queue();
				event.getTextChannel().delete().reason("Application closed").queueAfter(10, TimeUnit.SECONDS);
				parent.applyUserList.remove(this);
				return true;
		}

		return false;
	}
}
