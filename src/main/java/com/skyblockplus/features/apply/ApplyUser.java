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

package com.skyblockplus.features.apply;

import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.Player;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class ApplyUser {

	private final List<String> profileNames = new ArrayList<>();
	public String applyingUserId;
	public String applicationChannelId;
	public String reactMessageId;
	// 0 = waiting for user to select profile
	// 1 = waiting for user to submit application
	// 2 = waiting for staff to accept or deny application
	// 3 = user doesn't meet requirements, or was accepted/waitlisted/denied
	public int state = 0;
	public String staffChannelId;
	private String applySubmitedMessageId;
	// Embed
	public final String playerUsername;
	private final String playerUuid;
	private String playerSlayer;
	private String playerSkills;
	private String playerCatacombs;
	private String playerWeight;
	private String playerLilyWeight;
	private String playerLevel;
	private String playerCoins;
	private String ironmanSymbol = "";
	private String playerProfileName;

	public ApplyUser(ButtonInteractionEvent event, Player.Profile player, ApplyGuild parent) {
		logCommand(event.getGuild(), event.getUser(), "apply " + player.getUsername());

		this.applyingUserId = event.getUser().getId();
		this.playerUsername = player.getUsername();
		this.playerUuid = player.getUuid();
		JsonElement currentSettings = parent.currentSettings;

		Category applyCategory = null;
		try {
			applyCategory = event.getGuild().getCategoryById(higherDepth(currentSettings, "applyCategory", null));
		} catch (Exception ignored) {}
		if (applyCategory == null) {
			event
				.getHook()
				.editOriginal(client.getError() + " Invalid application category. Please report this to the server's staff.")
				.queue();
			return;
		}
		if (applyCategory.getChannels().size() == 50) {
			event
				.getHook()
				.editOriginal(
					client.getError() +
					" Unable to create a new application since the application category has reached 50/50 channels. Please report this to the server's staff."
				)
				.queue();
			return;
		}

		try {
			staffChannelId = event.getGuild().getTextChannelById(higherDepth(currentSettings, "applyStaffChannel").getAsString()).getId();
		} catch (Exception e) {
			event.getHook().editOriginal(client.getError() + " Invalid staff channel. Please report this to the server's staff.").queue();
			return;
		}

		ChannelAction<TextChannel> channelAction;
		try {
			channelAction =
				applyCategory
					.createTextChannel("apply-" + playerUsername)
					.syncPermissionOverrides()
					.addPermissionOverride(
						event.getGuild().getSelfMember(),
						EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
						null
					)
					.addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
					.addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
			try {
				for (JsonElement staffPingRole : higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray()) {
					channelAction =
						channelAction.addPermissionOverride(
							event.getGuild().getRoleById(staffPingRole.getAsString()),
							EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
							null
						);
				}
			} catch (Exception ignored) {}
		} catch (PermissionException e) {
			event.getHook().editOriginal(client.getError() + " Missing permission: " + e.getPermission().getName()).queue();
			return;
		}

		channelAction.queue(
			applicationChannel -> {
				try {
					this.applicationChannelId = applicationChannel.getId();

					List<String> profileNames = player.getMatchingProfileNames(
						Player.Gamemode.of(higherDepth(currentSettings, "applyGamemode", "all"))
					);

					if (profileNames.size() == 1) {
						applicationChannel
							.sendMessage(
								event.getUser().getAsMention() +
								" this is your application for " +
								capitalizeString(higherDepth(currentSettings, "guildName").getAsString().replace("_", " "))
							)
							.queue(ignored -> {
								parent.applyUserList.add(this);
								stateZero(profileNames.get(0), currentSettings, applicationChannel, null);
							});
						return;
					}

					EmbedBuilder eb = this.defaultPlayerEmbed().setDescription("Please select the profile you want to apply with.\n");

					StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("apply_user_profile");
					eb.appendDescription(
						"\n↩️ [Last Played Profile (" +
						player.getProfileName() +
						")](" +
						skyblockStatsLink(player.getUuid(), player.getProfileName()) +
						")"
					);
					menuBuilder.addOption("Last Played Profile", player.getProfileName(), Emoji.fromFormatted("↩️"));
					this.profileNames.add(player.getProfileName());
					for (String profileName : profileNames) {
						String profileEmoji = profileNameToEmoji(profileName);
						eb.appendDescription(
							"\n" +
							profileEmoji +
							" [" +
							capitalizeString(profileName) +
							"](" +
							skyblockStatsLink(player.getUuid(), profileName) +
							")"
						);
						menuBuilder.addOption(capitalizeString(profileName), profileName, Emoji.fromFormatted(profileEmoji));
						this.profileNames.add(profileName);
					}
					menuBuilder.addOption("Cancel Application", "cancel_application", Emoji.fromFormatted(client.getError()));

					applicationChannel
						.sendMessage(
							event.getUser().getAsMention() +
							" this is your application for " +
							capitalizeString(higherDepth(currentSettings, "guildName").getAsString().replace("_", " "))
						)
						.setEmbeds(eb.build())
						.setActionRow(menuBuilder.build())
						.queue(m -> {
							reactMessageId = m.getId();
							parent.applyUserList.add(this);
							event
								.getHook()
								.editOriginal(
									client.getSuccess() + " A new application was created in " + applicationChannel.getAsMention()
								)
								.queue();
						});
				} catch (Exception e) {
					AutomaticGuild.getLog().error(event.getGuild().getId(), e);
					event.getHook().editOriginal(client.getError() + " " + e.getMessage()).queue();
				}
			},
			e -> {
				if (e instanceof ErrorResponseException ex) {
					event.getHook().editOriginal(client.getError() + " Error when creating channel: " + ex.getMeaning()).queue();
				} else {
					event.getHook().editOriginal(client.getError() + " " + e.getMessage()).queue();
				}
			}
		);
	}

	public void onStringSelectInteraction(StringSelectInteractionEvent event, ApplyGuild parent) {
		if (!event.getMessageId().equals(reactMessageId)) {
			return;
		}

		if (state != 0) {
			return;
		}

		if (!event.getUser().getId().equals(applyingUserId) && !parent.isApplyAdmin(event.getMember())) {
			return;
		}

		// Edit original message
		event.getMessage().editMessageComponents().queue();

		String selectedOption = event.getSelectedOptions().get(0).getValue();
		if (selectedOption.equals("cancel_application")) {
			parent.applyUserList.remove(this);
			event
				.replyEmbeds(defaultEmbed("Closing Channel").build())
				.queue(m -> {
					m.editOriginalComponents().queue();
					event.getGuildChannel().delete().reason("Application canceled").queueAfter(10, TimeUnit.SECONDS);
				});
		} else {
			event.deferReply().queue(hook -> stateZero(selectedOption, parent.currentSettings, null, hook));
		}
	}

	private void stateZero(String profile, JsonElement currentSettings, TextChannel applicationChannel, InteractionHook hook) {
		Player.Profile player = Player.create(playerUsername, profile);
		JsonArray currentReqs = higherDepth(currentSettings, "applyReqs").getAsJsonArray();

		if (!currentReqs.isEmpty()) {
			boolean meetReqsOr = currentReqs.isEmpty();
			StringBuilder missingReqsStr = new StringBuilder();

			for (JsonElement req : currentReqs) {
				boolean meetsReqAnd = true;
				List<String> reqsMissingFmt = new ArrayList<>();

				for (Map.Entry<String, JsonElement> reqEntry : higherDepth(req, "requirements").getAsJsonObject().entrySet()) {
					long playerAmount = (long) switch (reqEntry.getKey()) {
						case "slayer" -> player.getTotalSlayer();
						case "skills" -> player.getSkillAverage();
						case "catacombs" -> player.getCatacombs().getProgressLevel();
						case "weight" -> player.getWeight();
						case "lily_weight" -> player.getLilyWeight();
						case "level" -> player.getLevel();
						case "networth" -> player.getNetworth();
						case "farming_weight" -> player.getWeight("farming");
						default -> throw new IllegalStateException("Unexpected value: " + reqEntry.getKey());
					};
					long reqAmount = reqEntry.getValue().getAsLong();

					reqsMissingFmt.add(capitalizeString(reqEntry.getKey().replace("_", " ")) + " - " + formatNumber(reqAmount));
					if (reqAmount > playerAmount) {
						meetsReqAnd = false;
					}
				}

				if (meetsReqAnd) {
					meetReqsOr = true;
					break;
				} else {
					missingReqsStr.append("• ").append(String.join(" | ", reqsMissingFmt)).append("\n");
				}
			}

			if (!meetReqsOr) {
				EmbedBuilder eb = defaultEmbed("Does not meet requirements")
					.setDescription(
						"**Your statistics:**\n• Slayer - " +
						formatNumber(player.getTotalSlayer()) +
						" | Skill Average - " +
						(player.isSkillsApiEnabled() ? roundAndFormat((long) player.getSkillAverage()) : "API disabled") +
						" | Catacombs - " +
						roundAndFormat((long) player.getCatacombs().getProgressLevel()) +
						" | Weight - " +
						roundAndFormat((long) player.getWeight()) +
						" | Lily Weight - " +
						roundAndFormat((long) player.getLilyWeight()) +
						" | Level - " +
						roundAndFormat((long) player.getLevel()) +
						" | Networth - " +
						roundAndFormat((long) player.getNetworth())
					)
					.appendDescription("\n\n**You do not meet any of the following requirements:**\n" + missingReqsStr)
					.appendDescription("\nIf any of these value seem incorrect, then make sure all your APIs are enabled");

				if (applicationChannel != null) {
					applicationChannel
						.sendMessageEmbeds(eb.build())
						.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
						.queue(m -> {
							reactMessageId = m.getId();
							state = 3;
						});
				} else {
					hook
						.editOriginalEmbeds(eb.build())
						.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
						.queue(m -> {
							reactMessageId = m.getId();
							state = 3;
						});
				}
				return;
			}
		}

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

		try {
			playerLilyWeight = roundAndFormat(player.getLilyWeight());
		} catch (Exception e) {
			playerLilyWeight = "API disabled";
		}

		try {
			playerLevel = roundAndFormat(player.getLevel());
		} catch (Exception e) {
			playerLevel = "API disabled";
		}

		double bankCoins = player.getBankBalance();
		playerCoins = (bankCoins != -1 ? simplifyNumber(bankCoins) : "API disabled") + " + " + simplifyNumber(player.getPurseCoins());
		ironmanSymbol = player.getSymbol(" ");
		playerProfileName = player.getProfileName();

		EmbedBuilder eb = player
			.defaultPlayerEmbed()
			.addField("Total Slayer", playerSlayer, true)
			.addField("Skills", playerSkills, true)
			.addField("Catacombs", playerCatacombs, true)
			.addField("Weight", playerWeight, true)
			.addField("Lily Weight", playerLilyWeight, true)
			.addField("Skyblock Level", playerLevel, true)
			.addField("Bank & Purse", playerCoins, true);

		List<Button> buttons = new ArrayList<>();
		buttons.add(Button.success("apply_user_submit", "Submit"));
		if (!profileNames.isEmpty()) {
			buttons.add(Button.primary("apply_user_retry", "Retry"));
		}
		buttons.add(Button.danger("apply_user_cancel", "Cancel"));

		if (applicationChannel != null) {
			applicationChannel
				.sendMessageEmbeds(eb.build())
				.setActionRow(buttons)
				.queue(m -> {
					reactMessageId = m.getId();
					state = 1;
				});
		} else {
			hook
				.editOriginalEmbeds(eb.build())
				.setActionRow(buttons)
				.queue(m -> {
					reactMessageId = m.getId();
					state = 1;
				});
		}
	}

	public void onButtonClick(ButtonInteractionEvent event, ApplyGuild parent) {
		JsonElement currentSettings = parent.currentSettings;
		if (!event.getUser().getId().equals(applyingUserId) && !parent.isApplyAdmin(event.getMember())) {
			return;
		}

		switch (state) {
			case 1 -> {
				switch (event.getComponentId()) {
					case "apply_user_submit" -> {
						// Edit original message
						event.getMessage().editMessageComponents().queue();

						// Edit deferred message
						event
							.getHook()
							.editOriginalEmbeds(
								defaultEmbed("Application Sent")
									.setDescription("You will be notified once staff review your application")
									.build()
							)
							.setActionRow(Button.danger("apply_user_cancel", "Cancel Application"))
							.queue(m -> applySubmitedMessageId = m.getId());

						String waitlistMessage = higherDepth(currentSettings, "applyWaitlistMessage", null);
						String staffPingMentions = streamJsonArray(higherDepth(currentSettings, "applyStaffRoles"))
							.map(r -> "<@&" + r.getAsString() + ">")
							.collect(Collectors.joining(" "));
						double playerNetworth = NetworthExecute.getNetworth(playerUuid, playerProfileName);

						EmbedBuilder applyPlayerStats = defaultPlayerEmbed()
							.addField("Total Slayer", playerSlayer, true)
							.addField("Skills", playerSkills, true)
							.addField("Catacombs", playerCatacombs, true)
							.addField("Weight", playerWeight, true)
							.addField("Lily Weight", playerLilyWeight, true)
							.addField("Skyblock Level", playerLevel, true)
							.addField("Bank & Purse", playerCoins, true)
							.addField("Networth", playerNetworth == -1 ? "Inventory API disabled" : roundAndFormat(playerNetworth), true);

						List<Button> actionRow = new ArrayList<>();
						actionRow.add(Button.success("apply_user_accept", "Accept"));
						if (waitlistMessage != null && !waitlistMessage.isEmpty() && !waitlistMessage.equals("none")) {
							actionRow.add(Button.primary("apply_user_waitlist", "Waitlist"));
						}
						actionRow.add(Button.danger("apply_user_deny", "Deny"));

						MessageCreateAction messageAction = jda
							.getTextChannelById(staffChannelId)
							.sendMessageEmbeds(applyPlayerStats.build())
							.setActionRow(actionRow);
						if (!staffPingMentions.isEmpty()) {
							messageAction.setContent(staffPingMentions);
						}

						messageAction.queue(m -> {
							reactMessageId = m.getId();
							state = 2;
						});
					}
					case "apply_user_retry" -> {
						// Edit original message
						event.getMessage().editMessageComponents().queue();

						EmbedBuilder retryEmbed = defaultPlayerEmbed()
							.setDescription("Please select the profile you want to apply with.\n");

						StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("apply_user_profile");
						for (int i = 0; i < profileNames.size(); i++) {
							String profileName = profileNames.get(i);
							if (i == 0) {
								retryEmbed.appendDescription(
									"\n↩️ [Last Played Profile (" +
									capitalizeString(profileName) +
									")](" +
									skyblockStatsLink(playerUuid, profileName) +
									")"
								);
								menuBuilder.addOption("Last Played Profile", profileName, Emoji.fromFormatted("↩️"));
							} else {
								String profileEmoji = profileNameToEmoji(profileName);
								retryEmbed.appendDescription(
									"\n" +
									profileEmoji +
									" [" +
									capitalizeString(profileName) +
									"](" +
									skyblockStatsLink(playerUuid, profileName) +
									")"
								);
								menuBuilder.addOption(capitalizeString(profileName), profileName, Emoji.fromFormatted(profileEmoji));
							}
						}
						menuBuilder.addOption("Cancel Application", "cancel_application", Emoji.fromFormatted(client.getError()));

						// Edit deferred message
						event
							.getHook()
							.editOriginalEmbeds(retryEmbed.build())
							.setActionRow(menuBuilder.build())
							.queue(m -> {
								reactMessageId = m.getId();
								state = 0;
							});
					}
					case "apply_user_cancel" -> {
						// Edit original message
						event.getMessage().editMessageComponents().queue();

						// Edit deferred message
						event
							.getHook()
							.editOriginalEmbeds(defaultEmbed("Canceling Application").build())
							.queue(ignored ->
								event.getGuildChannel().delete().reason("Application canceled").queueAfter(10, TimeUnit.SECONDS)
							);

						parent.applyUserList.remove(this);
					}
				}
			}
			case 2 -> {
				TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
				if (
					event.getComponentId().equals("apply_user_accept") ||
					event.getComponentId().equals("apply_user_waitlist") ||
					event.getComponentId().equals("apply_user_deny")
				) {
					if (event.getComponentId().equals("apply_user_waitlist")) {
						String waitlistMessage = higherDepth(currentSettings, "applyWaitlistMessage", "");
						if (waitlistMessage.isEmpty() || waitlistMessage.equals("none")) {
							return;
						}
					}

					// Edit application message (remove cancel button)
					try {
						applicationChannel.editMessageComponentsById(applySubmitedMessageId).queue();
					} catch (Exception ignored) {}

					// Edit staff message
					event
						.getHook()
						.editOriginal(
							escapeUsername(playerUsername) +
							" (<@" +
							applyingUserId +
							">) was " +
							switch (event.getComponentId()) {
								case "apply_user_accept" -> "accepted";
								case "apply_user_waitlist" -> "waitlisted";
								default -> "denied";
							} +
							" by " +
							event.getUser().getAsMention()
						)
						.setComponents()
						.setEmbeds()
						.queue();

					if (event.getComponentId().equals("apply_user_deny")) {
						applicationChannel
							.sendMessage("<@" + applyingUserId + ">")
							.setEmbeds(
								defaultEmbed("Application Not Accepted")
									.setDescription(higherDepth(currentSettings, "applyDenyMessage").getAsString())
									.build()
							)
							.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
							.queue(m -> {
								reactMessageId = m.getId();
								state = 3;
							});
						return;
					}

					boolean isAccept = event.getComponentId().equals("apply_user_accept");
					MessageCreateAction action = applicationChannel
						.sendMessage("<@" + applyingUserId + ">")
						.setEmbeds(
							defaultEmbed("Application " + (isAccept ? "Accepted" : "Waitlisted"))
								.setDescription(
									higherDepth(currentSettings, isAccept ? "applyAcceptMessage" : "applyWaitlistMessage").getAsString()
								)
								.build()
						);

					TextChannel waitInviteChannel = null;
					try {
						waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "applyWaitingChannel").getAsString());
					} catch (Exception ignored) {}
					if (waitInviteChannel == null) {
						action
							.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"))
							.queue(m -> {
								reactMessageId = m.getId();
								state = 3;
							});
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
						waitInviteChannel
							.sendMessageEmbeds(
								defaultEmbed("Waiting for invite")
									.setDescription(
										"**Player:** " + escapeUsername(playerUsername) + "\n**Discord:** <@" + applyingUserId + ">"
									)
									.build()
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
								),
								Button.link(nameMcLink(playerUuid), "NameMC"),
								Button.link(skyblockStatsLink(playerUuid, playerProfileName), "SkyCrypt")
							)
							.queue(waitingForInviteMessage ->
								action
									.setActionRow(
										Button.danger(
											"apply_user_cancel_" +
											higherDepth(currentSettings, "applyWaitingChannel").getAsString() +
											"_" +
											waitingForInviteMessage.getId(),
											"Cancel Application"
										)
									)
									.queue(m -> {
										reactMessageId = m.getId();
										state = 3;
									})
							);
					}
				}
			}
			case 3 -> {
				parent.applyUserList.remove(this);
				event.getMessage().editMessageComponents().queue();

				if (event.getComponentId().startsWith("apply_user_cancel_")) {
					event.getHook().editOriginalEmbeds(defaultEmbed("Canceling Application").build()).queue();
					event.getGuildChannel().delete().reason("Application canceled").queueAfter(10, TimeUnit.SECONDS, ignore, ignore);

					String[] channelMessageSplit = event.getComponentId().split("apply_user_cancel_")[1].split("_");
					event
						.getGuild()
						.getTextChannelById(channelMessageSplit[0])
						.deleteMessageById(channelMessageSplit[1])
						.queue(ignore, ignore);
				} else {
					event.getHook().editOriginalEmbeds(defaultEmbed("Closing Channel").build()).queue();
					event.getGuildChannel().delete().reason("Application closed").queueAfter(10, TimeUnit.SECONDS);
				}
			}
		}
	}

	private EmbedBuilder defaultPlayerEmbed() {
		return defaultEmbed(escapeUsername(playerUsername) + ironmanSymbol, skyblockStatsLink(playerUuid, playerProfileName))
			.setThumbnail(getAvatarUrl(playerUuid));
	}
}
