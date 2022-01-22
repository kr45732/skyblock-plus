/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.getNameHistory;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.Main;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.apply.log.ApplyLog;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class FixApplicationCommand extends Command {

	public FixApplicationCommand() {
		this.name = "fix-application";
		this.cooldown = globalCooldown + 1;
		this.botPermissions = defaultPerms();
		this.aliases = new String[] { "fix" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3) {
					TextChannel channel;
					try {
						channel = getGuild().getTextChannelById(args[1].replaceAll("[<#>]", ""));
						channel.getId();
					} catch (Exception e) {
						embed(invalidEmbed("Invalid channel provided"));
						return;
					}

					int state = -1;
					try {
						state = Integer.parseInt(args[2]);
					} catch (Exception ignored) {}
					if (state < 0 || state > 3) {
						embed(invalidEmbed("State must be 0, 1, 2, or 3"));
						return;
					}

					embed(getFixApplicationEmbed(channel, state, event.getGuild()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.setAdminCommand(true)
			.queue();
	}

	public static EmbedBuilder getFixApplicationEmbed(TextChannel channel, int state, Guild guild) {
		try {
			JsonObject applicationJson = new JsonObject();
			List<Message> messages = channel.getIterableHistory().takeAsync(251).get();
			if (messages.size() == 251) {
				return invalidEmbed("Cannot retrieve an application with more than 250 messages");
			}

			String botId = jda.getSelfUser().getId();
			Message firstMessage = messages
				.stream()
				.filter(m -> m.getAuthor().getId().equals(botId) && m.getContentRaw().contains(" this is your application for "))
				.findFirst()
				.orElse(null);
			if (firstMessage == null) {
				return invalidEmbed("Unable to find initial message in application");
			}

			User applicant = firstMessage.getMentionedUsers().get(0);
			String guildName = firstMessage.getContentRaw().split(" this is your application for ")[1].replace(" ", "_").toLowerCase();
			JsonObject settings = database.getGuildSettings(guild.getId(), guildName).getAsJsonObject();
			settings.remove("applyUsersCache");
			boolean logApplication = false;
			try {
				logApplication = guild.getTextChannelById(higherDepth(settings, "applyLogChannel").getAsString()) != null;
			} catch (Exception ignored) {}

			LinkedAccount linkedAcc = database.getByDiscord(applicant.getId());
			String username = linkedAcc.username();
			StringBuilder nameHistory = new StringBuilder();
			for (String i : getNameHistory(linkedAcc.uuid())) {
				nameHistory.append("\n• ").append(fixUsername(i));
			}

			JsonObject emojiJson = new JsonObject();
			try {
				for (String emojiLine : firstMessage.getEmbeds().get(0).getDescription().split("\n\n")[1].split("\n")) {
					String[] emojiLineSplit = emojiLine.split(" - ");
					String name = emojiLineSplit[1];
					name = name.substring(name.lastIndexOf("/") + 1, name.length() - 1);
					emojiJson.addProperty(emojiLineSplit[0].replaceAll("<:|>", ""), name);
				}
			} catch (Exception ignored) {}

			applicationJson.addProperty("applyingUserId", applicant.getId());
			applicationJson.addProperty("currentSettingsString", settings.toString());
			applicationJson.addProperty("guildId", guild.getId());
			applicationJson.add("profileEmojiToName", emojiJson);
			applicationJson.addProperty("applicationChannelId", channel.getId());
			applicationJson.addProperty("reactMessageId", firstMessage.getId());
			applicationJson.addProperty("state", state);
			applicationJson.addProperty("logApplication", logApplication);
			applicationJson.addProperty("playerUsername", username);
			applicationJson.addProperty("nameHistory", nameHistory.toString());
			applicationJson.addProperty("ironmanSymbol", "");

			if (state >= 1) {
				Message submitAppMessage = messages
					.stream()
					.filter(m -> {
						try {
							return (
								m.getAuthor().getId().equals(botId) && m.getEmbeds().get(0).getFields().get(0).getName().equals("Weight")
							);
						} catch (Exception e) {
							return false;
						}
					})
					.findFirst()
					.orElse(null);
				if (submitAppMessage == null) {
					return invalidEmbed("Unable to find submit application confirm message");
				}

				for (MessageEmbed.Field field : submitAppMessage.getEmbeds().get(0).getFields()) {
					String jsonName =
						switch (field.getName()) {
							case "Weight" -> "playerWeight";
							case "Total slayer" -> "playerSlayer";
							case "Progress skill level" -> "playerSkills";
							case "Catacombs level" -> "playerCatacombs";
							case "Bank & purse coins" -> "playerCoins";
							default -> throw new IllegalStateException("Unexpected value: " + field.getName());
						};
					applicationJson.addProperty(jsonName, field.getValue());
				}
				applicationJson.addProperty("reactMessageId", submitAppMessage.getId());
				String url = submitAppMessage.getEmbeds().get(0).getUrl();
				applicationJson.addProperty("playerProfileName", url.substring(url.lastIndexOf("/") + 1));
				applicationJson.addProperty("ironmanSymbol", submitAppMessage.getEmbeds().get(0).getTitle().endsWith(" ♻️") ? " ♻️" : "");

				List<Message> toDeleteSubmitAppMessages = messages
					.stream()
					.filter(m -> {
						if (m.getAuthor().getId().equals(botId) && m.getContentRaw().equals("❌ This button has been disabled")) {
							Message repliedMessage = m.getReferencedMessage();
							return repliedMessage != null && repliedMessage.getId().equals(submitAppMessage.getId());
						}
						return false;
					})
					.limit(100)
					.collect(Collectors.toList());
				channel.purgeMessages(toDeleteSubmitAppMessages);

				submitAppMessage
					.editMessageComponents(
						submitAppMessage
							.getActionRows()
							.stream()
							.map(r ->
								ActionRow
									.of(
										r
											.getButtons()
											.stream()
											.map(b ->
												switch (b.getId()) {
													case "apply_user_submit" -> b.withLabel("Submit").withStyle(ButtonStyle.SUCCESS);
													case "apply_user_retry" -> b.withLabel("Retry").withStyle(ButtonStyle.PRIMARY);
													case "apply_user_cancel" -> b.withLabel("Cancel").withStyle(ButtonStyle.DANGER);
													default -> throw new IllegalStateException("Unexpected value: " + b.getId());
												}
											)
											.collect(Collectors.toList())
									)
									.asEnabled()
							)
							.collect(Collectors.toList())
					)
					.queue();
			}

			if (state >= 2) {
				TextChannel staffChannel = guild.getTextChannelById(higherDepth(settings, "applyStaffChannel").getAsString());
				applicationJson.addProperty("staffChannelId", staffChannel.getId());

				List<Message> staffMessages = staffChannel.getIterableHistory().takeAsync(100).get();
				Message staffMessage = staffMessages
					.stream()
					.filter(m -> {
						try {
							return (
								m.getAuthor().getId().equals(botId) && m.getEmbeds().get(0).getTitle().replace(" ♻️", "").equals(username)
							);
						} catch (Exception e) {
							return false;
						}
					})
					.findFirst()
					.orElse(null);
				if (staffMessage == null) {
					return invalidEmbed("Unable to find staff accept/waitlist/deny message in the past 100 messages");
				}

				List<Message> toDeleteStaffMsg = staffMessages
					.stream()
					.filter(m -> {
						if (m.getAuthor().getId().equals(botId) && m.getContentRaw().equals("❌ This button has been disabled")) {
							Message repliedMessage = m.getReferencedMessage();
							return repliedMessage != null && repliedMessage.getId().equals(staffMessage.getId());
						}
						return false;
					})
					.limit(100)
					.collect(Collectors.toList());
				staffChannel.purgeMessages(toDeleteStaffMsg);

				applicationJson.addProperty("reactMessageId", staffMessage.getId());
				staffMessage
					.editMessageComponents(
						staffMessage
							.getActionRows()
							.stream()
							.map(r ->
								ActionRow
									.of(
										r
											.getButtons()
											.stream()
											.map(b ->
												switch (b.getId()) {
													case "apply_user_accept" -> b.withLabel("Accept").withStyle(ButtonStyle.SUCCESS);
													case "apply_user_waitlist" -> b.withLabel("Waitlist").withStyle(ButtonStyle.PRIMARY);
													case "apply_user_deny" -> b.withLabel("Deny").withStyle(ButtonStyle.DANGER);
													default -> throw new IllegalStateException("Unexpected value: " + b.getId());
												}
											)
											.collect(Collectors.toList())
									)
									.asEnabled()
							)
							.collect(Collectors.toList())
					)
					.queue();
			}

			Collections.reverse(messages);
			applicationJson.add("logs", gson.toJsonTree(messages.stream().map(ApplyLog::toLog).collect(Collectors.toList())));

			guildMap
				.get(guild.getId())
				.applyGuild.stream()
				.filter(g -> higherDepth(g.currentSettings, "guildName").getAsString().equals(guildName))
				.findFirst()
				.get()
				.applyUserList.add(gson.fromJson(applicationJson, ApplyUser.class));
			return defaultEmbed("Success").setDescription("Fixed & retrieved application successfully: " + makeHastePost(applicationJson));
		} catch (Exception e) {
			Main.log.error("Error when retrieving application", e);
			return invalidEmbed("Error when fixing application, please report this to the developer:\n```" + e.getMessage() + "```");
		}
	}
}
