package com.skyblockplus.features.apply;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.*;
import com.skyblockplus.utils.Player;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class ApplyUser implements Serializable {

	public final String applyingUserId;
	public final String applicationChannelId;
	public final String currentSettingsString;
	public final String guildId;
	public final Map<String, String> profileEmojiToName = new LinkedHashMap<>();
	public String reactMessageId;
	public int state = 0;
	public String staffChannelId;
	public boolean shouldDeleteChannel = false;
	public String playerSlayer;
	public String playerSkills;
	public String playerCatacombs;
	public String playerWeight;
	public String playerUsername;
	public String ironmanSymbol;
	public String playerProfileName;

	public ApplyUser(ButtonClickEvent event, JsonElement currentSettings, String playerUsername) {
		User applyingUser = event.getUser();
		logCommand(event.getGuild(), applyingUser, "apply " + applyingUser.getName());

		JsonObject currentSettingsObj = currentSettings.getAsJsonObject();
		currentSettingsObj.remove("applyUsersCache");
		currentSettings = currentSettingsObj.getAsJsonObject();

		this.applyingUserId = applyingUser.getId();
		this.currentSettingsString = new Gson().toJson(currentSettings);
		this.guildId = event.getGuild().getId();
		this.playerUsername = playerUsername;

		Category applyCategory = event.getGuild().getCategoryById(higherDepth(currentSettings, "newChannelCategory").getAsString());
		TextChannel applicationChannel = applyCategory
			.createTextChannel("apply-" + playerUsername)
			.addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
			.addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
			.complete();
		this.applicationChannelId = applicationChannel.getId();

		applicationChannel.sendMessage("Welcome " + applyingUser.getAsMention() + "!").complete();

		boolean isIronman = false;
		try {
			isIronman = higherDepth(currentSettings, "ironmanOnly").getAsBoolean();
		} catch (Exception ignored) {}

		Player player = new Player(playerUsername);
		String[] profileNames = player.getAllProfileNames(isIronman);

		if (profileNames.length == 1) {
			caseOne(profileNames[0], currentSettings, applicationChannel);
		} else {
			EmbedBuilder welcomeEb = defaultEmbed("Application for " + player.getUsername());
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

			Message reactMessage = applicationChannel.sendMessageEmbeds(welcomeEb.build()).complete();
			this.reactMessageId = reactMessage.getId();

			for (String profileEmoji : profileEmojiToName.keySet()) {
				reactMessage.addReaction(profileEmoji).complete();
			}

			reactMessage.addReaction("❌").queue();
		}
	}

	public boolean onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event != null) if (event.getUser().isBot()) {
			return false;
		}

		if (state == 4) {
			return onMessageReactionAddStaff(event);
		}

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
					event
						.getMember()
						.getRoles()
						.contains(event.getGuild().getRoleById(higherDepth(currentSettings, "staffPingRoleId").getAsString())) ||
					event.getMember().hasPermission(Permission.ADMINISTRATOR)
				)
			) {
				reactMessage.removeReaction(event.getReactionEmote().getAsReactionCode(), event.getUser()).queue();
				return false;
			}
		}

		if (event.getReactionEmote().getAsReactionCode().equals("❌")) {
			state = 3;
		} else if (event.getReactionEmote().getAsReactionCode().equals("\uD83D\uDD04") && state == 1) {
			state = 2;
		} else if (
			!(
				(profileEmojiToName.containsKey(event.getReactionEmote().getAsReactionCode()) && (state == 0)) ||
				(event.getReactionEmote().getAsReactionCode().equals("✅") && (state == 1 || state == 5))
			)
		) {
			reactMessage.clearReactions(event.getReactionEmote().getAsReactionCode()).queue();
			return false;
		}

		reactMessage.clearReactions().queue();

		switch (state) {
			case 0:
				caseOne(profileEmojiToName.get(event.getReactionEmote().getAsReactionCode()), currentSettings, applicationChannel);
				break;
			case 1:
				EmbedBuilder finishApplyEmbed = defaultEmbed("Thank you for applying!");
				finishApplyEmbed.setDescription(
					"**Your stats have been submitted to staff**\nYou will be notified once staff review your stats"
				);
				applicationChannel.sendMessageEmbeds(finishApplyEmbed.build()).queue();

				state = 4;
				staffCaseConstructor();
				break;
			case 2:
				EmbedBuilder retryEmbed = defaultEmbed("Application for " + playerUsername);
				retryEmbed.setDescription(
					"Please react with the emoji that corresponds to the profile you want to apply with or react with ❌ to cancel the application.\n"
				);

				for (Map.Entry<String, String> profileEntry : profileEmojiToName.entrySet()) {
					String profileEmoji = profileEntry.getKey().contains(":") ? "<:" + profileEntry.getKey() + ">" : profileEntry.getKey();
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

				reactMessage = applicationChannel.sendMessageEmbeds(retryEmbed.build()).complete();
				this.reactMessageId = reactMessage.getId();

				for (String profileEmoji : profileEmojiToName.keySet()) {
					reactMessage.addReaction(profileEmoji).complete();
				}

				reactMessage.addReaction("❌").queue();
				state = 0;
				break;
			case 3:
				EmbedBuilder cancelEmbed = defaultEmbed("Canceling application");
				cancelEmbed.setDescription("Channel closing");
				applicationChannel.sendMessageEmbeds(cancelEmbed.build()).queue();
				event
					.getGuild()
					.getTextChannelById(event.getChannel().getId())
					.delete()
					.reason("Application canceled")
					.queueAfter(5, TimeUnit.SECONDS);
				return true;
			case 5:
				EmbedBuilder closeChannelEmbed = defaultEmbed("Channel closing");
				applicationChannel.sendMessageEmbeds(closeChannelEmbed.build()).queue();
				event
					.getGuild()
					.getTextChannelById(event.getChannel().getId())
					.delete()
					.reason("Application closed")
					.queueAfter(5, TimeUnit.SECONDS);
				return true;
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
				int slayerReq = higherDepth(req, "slayerReq").getAsInt();
				int skillsReq = higherDepth(req, "skillsReq").getAsInt();
				int cataReq = higherDepth(req, "catacombsReq").getAsInt();
				int weightReq = higherDepth(req, "weightReq").getAsInt();

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
			reqEmbed.appendDescription("\n• React with ✅ to close the channel");

			reactMessage = applicationChannel.sendMessageEmbeds(reqEmbed.build()).complete();
			reactMessage.addReaction("✅").queue();
			this.reactMessageId = reactMessage.getId();
			state = 5;
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
			statsEmbed.setDescription("**Skyblock weight:** " + playerWeight);
			statsEmbed.addField("Total slayer", playerSlayer, true);
			statsEmbed.addField("Progress skill level", playerSkills, true);
			statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);
			statsEmbed.addField("Are the above stats correct?", "React with ✅ for yes, 🔄 to retry, and ❌ to cancel", false);

			reactMessage = applicationChannel.sendMessageEmbeds(statsEmbed.build()).complete();
			reactMessage.addReaction("✅").queue();
			reactMessage.addReaction("\uD83D\uDD04").queue();
			reactMessage.addReaction("❌").queue();
			this.reactMessageId = reactMessage.getId();
			state = 1;
		}
	}

	private boolean onMessageReactionAddStaff(MessageReactionAddEvent event) {
		if (event.getUser().isBot()) {
			return false;
		}

		if (!event.getMessageId().equals(reactMessageId)) {
			return false;
		}

		TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
		try {
			if (shouldDeleteChannel && (event.getMessageId().equals(reactMessageId))) {
				if (event.getReactionEmote().getName().equals("✅")) {
					event.getReaction().clearReactions().queue();
					EmbedBuilder eb = defaultEmbed("Channel closing");
					applicationChannel.sendMessageEmbeds(eb.build()).queue();
					applicationChannel.delete().reason("Applicant read final message").queueAfter(10, TimeUnit.SECONDS);
					return true;
				} else {
					event.getReaction().removeReaction(event.getUser()).queue();
				}
				return false;
			}
		} catch (Exception ignored) {}

		JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

		TextChannel staffChannel = jda.getTextChannelById(staffChannelId);
		User applyingUser = jda.retrieveUserById(applyingUserId).complete();
		Message reactMessage = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
		if (event.getReactionEmote().getEmoji().equals("❌")) {
			reactMessage.clearReactions().queue();
			reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

			try {
				staffChannel
					.sendMessage(playerUsername + " (" + applyingUser.getAsMention() + ") was denied by " + event.getUser().getAsMention())
					.queue();
				applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
			} catch (Exception e) {
				staffChannel.sendMessage(playerUsername + " was denied by " + event.getUser().getAsMention()).queue();
			}

			EmbedBuilder eb = defaultEmbed("Application Not Accepted");
			eb.setDescription(higherDepth(currentSettings, "denyMessageText").getAsString() + "\n**React with ✅ to close the channel**");

			reactMessage = applicationChannel.sendMessageEmbeds(eb.build()).complete();
			reactMessage.addReaction("✅").queue();
			this.reactMessageId = reactMessage.getId();
			shouldDeleteChannel = true;
		} else if (event.getReactionEmote().getEmoji().equals("✅")) {
			reactMessage.clearReactions().queue();
			reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

			try {
				staffChannel
					.sendMessage(
						playerUsername + " (" + applyingUser.getAsMention() + ") was accepted by " + event.getUser().getAsMention()
					)
					.queue();
				applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
			} catch (Exception e) {
				staffChannel.sendMessage(playerUsername + " was accepted by " + event.getUser().getAsMention()).queue();
			}

			EmbedBuilder eb = defaultEmbed("Application Accepted");
			eb.setDescription(higherDepth(currentSettings, "acceptMessageText").getAsString() + "\n**React with ✅ to close the channel**");

			reactMessage = applicationChannel.sendMessageEmbeds(eb.build()).complete();
			reactMessage.addReaction("✅").queue();

			try {
				TextChannel waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
				waitInviteChannel
					.sendMessageEmbeds(
						defaultEmbed("Waiting for invite")
							.setDescription("`" + playerUsername + "`\n\n" + "**React with ✅ to delete this message**")
							.build()
					)
					.complete()
					.addReaction("✅")
					.queue();
			} catch (Exception ignored) {}

			this.reactMessageId = reactMessage.getId();
			shouldDeleteChannel = true;
		} else if (event.getReactionEmote().getEmoji().equals("\uD83D\uDD50")) {
			if (
				higherDepth(currentSettings, "waitlistedMessageText") != null &&
				higherDepth(currentSettings, "waitlistedMessageText").getAsString().length() > 0 &&
				!higherDepth(currentSettings, "waitlistedMessageText").getAsString().equals("none")
			) {
				reactMessage.clearReactions().queue();
				reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

				try {
					staffChannel
						.sendMessage(
							playerUsername + " (" + applyingUser.getAsMention() + ") was waitlisted by " + event.getUser().getAsMention()
						)
						.queue();
					applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
				} catch (Exception e) {
					staffChannel.sendMessage(playerUsername + " was waitlisted by " + event.getUser().getAsMention()).queue();
				}

				EmbedBuilder eb = defaultEmbed("Application waitlisted");
				eb.setDescription(
					higherDepth(currentSettings, "waitlistedMessageText").getAsString() + "\n**React with ✅ to close the channel**"
				);

				reactMessage = applicationChannel.sendMessageEmbeds(eb.build()).complete();
				reactMessage.addReaction("✅").queue();

				try {
					TextChannel waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
					waitInviteChannel
						.sendMessageEmbeds(
							defaultEmbed("Waiting for invite")
								.setDescription("`" + playerUsername + "`\n\n" + "**React with ✅ to delete this message**")
								.build()
						)
						.complete()
						.addReaction("✅")
						.queue();
				} catch (Exception ignored) {}

				this.reactMessageId = reactMessage.getId();
				shouldDeleteChannel = true;
			}
		}
		return false;
	}

	private void staffCaseConstructor() {
		JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

		TextChannel staffChannel = jda.getTextChannelById(higherDepth(currentSettings, "messageStaffChannelId").getAsString());
		staffChannelId = staffChannel.getId();

		EmbedBuilder applyPlayerStats = defaultPlayerEmbed();
		applyPlayerStats.setDescription("**Skyblock weight:** " + playerWeight);
		applyPlayerStats.addField("Total slayer", playerSlayer, true);
		applyPlayerStats.addField("Progress average skill level", playerSkills, true);
		applyPlayerStats.addField("Catacombs level", "" + playerCatacombs, true);
		applyPlayerStats.addField("To accept the application,", "React with ✅", true);
		JsonElement waitlistMsg = higherDepth(currentSettings, "waitlistedMessageText");
		if (waitlistMsg != null && waitlistMsg.getAsString().length() > 0 && !waitlistMsg.getAsString().equals("none")) {
			applyPlayerStats.addField("To waitlist the application,", "React with \uD83D\uDD50", true);
		}
		applyPlayerStats.addField("To deny the application,", "React with ❌", true);
		staffChannel.sendMessage("<@&" + higherDepth(currentSettings, "staffPingRoleId").getAsString() + ">").complete();
		Message reactMessage = staffChannel.sendMessageEmbeds(applyPlayerStats.build()).complete();
		reactMessage.addReaction("✅").queue();
		if (waitlistMsg != null && waitlistMsg.getAsString().length() > 0 && !waitlistMsg.getAsString().equals("none")) {
			reactMessage.addReaction("\uD83D\uDD50").queue();
		}
		reactMessage.addReaction("❌").queue();
		reactMessageId = reactMessage.getId();
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultEmbed(fixUsername(playerUsername) + ironmanSymbol, skyblockStatsLink(playerUsername, playerProfileName));
	}
}
