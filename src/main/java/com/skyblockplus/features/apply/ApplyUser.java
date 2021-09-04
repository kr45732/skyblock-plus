package com.skyblockplus.features.apply;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.*;
import com.skyblockplus.utils.Player;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class ApplyUser implements Serializable {

	public final String applyingUserId;
	public final String applicationChannelId;
	public final String currentSettingsString;
	public final String guildId;
	public final Map<String, String> profileEmojiToName = new LinkedHashMap<>();
	public String reactMessageId;
	public int state = 0;
	public String staffChannelId;
	public String playerSlayer;
	public String playerSkills;
	public String playerCatacombs;
	public String playerWeight;
	public String playerUsername;
	public String ironmanSymbol = "";
	public String playerProfileName;

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
		TextChannel applicationChannel = applyCategory
			.createTextChannel("apply-" + playerUsername)
			.addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
			.addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
			.complete();
		this.applicationChannelId = applicationChannel.getId();

		boolean isIronman = false;
		try {
			isIronman = higherDepth(currentSettings, "ironmanOnly").getAsBoolean();
		} catch (Exception ignored) {}

		Player player = new Player(playerUsername);
		String[] profileNames = player.getAllProfileNames(isIronman);

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

		if (!event.getUser().equals(applyingUser)) {
			if (
				!(
					(
						!higherDepth(currentSettings, "staffPingRoleId").getAsString().equals("none") &&
						event
							.getMember()
							.getRoles()
							.contains(event.getGuild().getRoleById(higherDepth(currentSettings, "staffPingRoleId").getAsString()))
					) ||
					event.getMember().hasPermission(Permission.ADMINISTRATOR)
				)
			) {
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
					player.getCatacombsLevel() >= cataReq &&
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
			reqEmbed.setDescription("You do not meet any of the following requirements:\n" + missingReqsStr);
			reqEmbed.appendDescription(
				"\n\n• If you think these values are incorrect make sure all your APIs are enabled and/or try relinking"
			);

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
				playerCatacombs = roundAndFormat(player.getCatacombsSkill().skillLevel + player.getCatacombsSkill().progressToNext);
			} catch (Exception e) {
				playerCatacombs = "0";
			}

			try {
				playerWeight = roundAndFormat(player.getWeight());
			} catch (Exception e) {
				playerWeight = "API disabled";
			}

			playerUsername = player.getUsername();
			ironmanSymbol = higherDepth(player.getOuterProfileJson(), "game_mode") != null ? " ♻️" : "";
			playerProfileName = player.getProfileName();

			EmbedBuilder statsEmbed = player.defaultPlayerEmbed();
			statsEmbed.setDescription("**Weight:** " + playerWeight);
			statsEmbed.addField("Total slayer", playerSlayer, true);
			statsEmbed.addField("Progress skill level", playerSkills, true);
			statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);

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
		if (!event.getMessage().getId().equals(reactMessageId)) {
			return false;
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

						JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

						TextChannel staffChannel = jda.getTextChannelById(
							higherDepth(currentSettings, "messageStaffChannelId").getAsString()
						);
						staffChannelId = staffChannel.getId();

						EmbedBuilder applyPlayerStats = defaultPlayerEmbed();
						applyPlayerStats.setDescription("**Weight:** " + playerWeight);
						applyPlayerStats.addField("Total slayer", playerSlayer, true);
						applyPlayerStats.addField("Progress average skill level", playerSkills, true);
						applyPlayerStats.addField("Catacombs level", playerCatacombs, true);
						applyPlayerStats.setThumbnail("https://cravatar.eu/helmavatar/" + playerUsername + "/64.png");
						String waitlistMsg = higherDepth(currentSettings, "waitlistedMessageText", null);

						List<Button> row = new ArrayList<>();
						row.add(Button.success("apply_user_accept", "Accept"));
						if (waitlistMsg != null && waitlistMsg.length() > 0 && !waitlistMsg.equals("none")) {
							row.add(Button.primary("apply_user_waitlist", "Waitlist"));
						}
						row.add(Button.danger("apply_user_deny", "Deny"));
						Message reactMessage = higherDepth(currentSettings, "staffPingRoleId").getAsString().equals("none")
							? staffChannel.sendMessageEmbeds(applyPlayerStats.build()).complete()
							: staffChannel
								.sendMessage("<@&" + higherDepth(currentSettings, "staffPingRoleId").getAsString() + ">")
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
				JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

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
							action.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"));
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
								action.setActionRow(Button.success("apply_user_delete_channel", "Close Channel"));
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
