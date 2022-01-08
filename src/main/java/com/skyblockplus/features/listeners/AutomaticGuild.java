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

package com.skyblockplus.features.listeners;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.features.skyblockevent.SkyblockEventCommand.endSkyblockEvent;
import static com.skyblockplus.utils.ApiHandler.getGuildFromId;
import static com.skyblockplus.utils.Constants.NUMBER_TO_RARITY_MAP;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.jacob.FarmingContest;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.features.setup.SetupCommandHandler;
import com.skyblockplus.features.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventHandler;
import com.skyblockplus.features.verify.VerifyGuild;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticGuild {

	private static final Logger log = LoggerFactory.getLogger(AutomaticGuild.class);

	/* Automated Apply */
	public final List<ApplyGuild> applyGuild = new ArrayList<>();
	/* Miscellaneous */
	public final String guildId;
	public final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
	public final FarmingContest farmingContest;
	/* Automated Verify */
	public VerifyGuild verifyGuild;
	/* Skyblock event */
	public SkyblockEventHandler skyblockEventHandler = null;
	public List<EventMember> eventMemberList = new ArrayList<>();
	public Instant eventMemberListLastUpdated = null;
	/* Party */
	public List<Party> partyList = new ArrayList<>();
	public String prefix;
	public TextChannel fetchurChannel = null;
	private Role applyGuestRole = null;
	private Role fetchurPing = null;

	/* Constructor */
	public AutomaticGuild(GenericGuildEvent event) {
		guildId = event.getGuild().getId();
		JsonElement serverSettings = database.getServerSettings(guildId);
		applyConstructor(event);
		verifyConstructor(event, higherDepth(serverSettings, "automatedVerify"));
		schedulerConstructor();
		prefix = database.getPrefix(guildId);
		farmingContest = new FarmingContest(guildId, higherDepth(serverSettings, "jacobSettings"));
		try {
			fetchurChannel = event.getGuild().getTextChannelById(higherDepth(serverSettings, "fetchurChannel", null));
		} catch (Exception ignored) {}
		try {
			applyGuestRole = event.getGuild().getRoleById(higherDepth(serverSettings, "applyGuestRole", null));
		} catch (Exception ignored) {}
		try {
			fetchurPing = event.getGuild().getRoleById(higherDepth(serverSettings, "fetchurRole", null));
		} catch (Exception ignored) {}
	}

	public static String getGuildPrefix(String guildId) {
		AutomaticGuild automaticGuild = guildMap.getOrDefault(guildId, null);
		return automaticGuild != null ? automaticGuild.prefix : DEFAULT_PREFIX;
	}

	/* Automated Apply Methods */
	public void applyConstructor(GenericGuildEvent event) {
		List<AutomatedGuild> currentSettings = database.getAllGuildSettings(event.getGuild().getId());
		if (currentSettings == null) {
			return;
		}

		for (AutomatedGuild currentSetting : currentSettings) {
			try {
				if (currentSetting.getApplyEnable() == null || currentSetting.getApplyEnable().equalsIgnoreCase("false")) {
					continue;
				}

				TextChannel reactChannel = event.getGuild().getTextChannelById(currentSetting.getApplyMessageChannel());

				EmbedBuilder eb = defaultEmbed("Apply For Guild");
				eb.setDescription(currentSetting.getApplyMessage());

				try {
					Message reactMessage = reactChannel.retrieveMessageById(currentSetting.getApplyPrevMessage()).complete();
					reactMessage
						.editMessageEmbeds(eb.build())
						.setActionRow(Button.primary("create_application_button_" + currentSetting.getGuildName(), "Apply Here"))
						.queue();

					applyGuild.removeIf(o1 ->
						higherDepth(o1.currentSettings, "guildName").getAsString().equals(currentSetting.getGuildName())
					);
					applyGuild.add(new ApplyGuild(reactMessage, gson.toJsonTree(currentSetting)));
				} catch (Exception e) {
					Message reactMessage = reactChannel
						.sendMessageEmbeds(eb.build())
						.setActionRow(Button.primary("create_application_button_" + currentSetting.getGuildName(), "Apply Here"))
						.complete();

					currentSetting.setApplyPrevMessage(reactMessage.getId());
					database.setGuildSettings(event.getGuild().getId(), gson.toJsonTree(currentSetting));

					applyGuild.removeIf(o1 ->
						higherDepth(o1.currentSettings, "guildName").getAsString().equals(currentSetting.getGuildName())
					);
					applyGuild.add(new ApplyGuild(reactMessage, gson.toJsonTree(currentSetting)));
				}
			} catch (Exception e) {
				log.error("Apply constructor error - " + event.getGuild().getId(), e);
			}
		}
	}

	public String reloadApplyConstructor(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return "Invalid guild";
		}

		List<AutomatedGuild> currentSettings = database.getAllGuildSettings(guildId);
		currentSettings.removeIf(o1 -> o1 == null || o1.getGuildName() == null);

		if (currentSettings.size() == 0) {
			return "No enabled apply settings";
		}

		StringBuilder applyStr = new StringBuilder();
		for (AutomatedGuild currentSetting : currentSettings) {
			try {
				if (currentSetting.getApplyEnable().equalsIgnoreCase("true")) {
					TextChannel reactChannel = guild.getTextChannelById(currentSetting.getApplyMessageChannel());

					EmbedBuilder eb = defaultEmbed("Apply For Guild");
					eb.setDescription(currentSetting.getApplyMessage());

					List<ApplyUser> curApplyUsers = new ArrayList<>();
					for (Iterator<ApplyGuild> iterator = applyGuild.iterator(); iterator.hasNext();) {
						ApplyGuild applyG = iterator.next();

						if (higherDepth(applyG.currentSettings, "guildName").getAsString().equals(currentSetting.getGuildName())) {
							curApplyUsers.addAll(applyG.applyUserList);
							iterator.remove();
							break;
						}
					}

					try {
						Message reactMessage = reactChannel.retrieveMessageById(currentSetting.getApplyPrevMessage()).complete();
						reactMessage
							.editMessageEmbeds(eb.build())
							.setActionRow(Button.primary("create_application_button_" + currentSetting.getGuildName(), "Apply Here"))
							.queue();

						applyGuild.add(new ApplyGuild(reactMessage, gson.toJsonTree(currentSetting), curApplyUsers));
						applyStr.append("• Reloaded `").append(currentSetting.getGuildName()).append("`\n");
					} catch (Exception e) {
						Message reactMessage = reactChannel
							.sendMessageEmbeds(eb.build())
							.setActionRow(Button.primary("create_application_button_" + currentSetting.getGuildName(), "Apply Here"))
							.complete();

						currentSetting.setApplyPrevMessage(reactMessage.getId());
						database.setGuildSettings(guild.getId(), gson.toJsonTree(currentSetting));

						applyGuild.add(new ApplyGuild(reactMessage, gson.toJsonTree(currentSetting), curApplyUsers));
						applyStr.append("• Reloaded `").append(currentSetting.getGuildName()).append("`\n");
					}
				} else {
					applyGuild.removeIf(o1 ->
						higherDepth(o1.currentSettings, "guildName").getAsString().equals(currentSetting.getGuildName())
					);
					applyStr.append("• `").append(currentSetting.getGuildName()).append("` is disabled\n");
				}
			} catch (Exception e) {
				log.error("Reload apply constructor error - " + guildId, e);
				if (e.getMessage() != null && e.getMessage().contains("Missing permission")) {
					applyStr
						.append("• Error Reloading for `")
						.append(currentSetting.getGuildName())
						.append("` - missing permission(s): ")
						.append(e.getMessage().split("Missing permission: ")[1])
						.append("\n");
				} else {
					applyStr.append("• Error Reloading for `").append(currentSetting.getGuildName()).append("`\n");
				}
			}
		}
		return applyStr.length() > 0 ? applyStr.toString() : "• Error reloading";
	}

	/* Automated Verify Methods */
	public void verifyConstructor(GenericGuildEvent event, JsonElement currentSettings) {
		verifyGuild = new VerifyGuild(guildId);
		if (currentSettings == null) {
			return;
		}

		try {
			if (
				higherDepth(currentSettings, "enable") == null ||
				(higherDepth(currentSettings, "enable") != null && !higherDepth(currentSettings, "enable").getAsBoolean())
			) {
				return;
			}

			if (higherDepth(currentSettings, "enable").getAsBoolean()) {
				TextChannel reactChannel = event
					.getGuild()
					.getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
				try {
					Message reactMessage = reactChannel
						.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
						.complete();
					if (reactMessage != null) {
						reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

						verifyGuild = new VerifyGuild(reactChannel, reactMessage, currentSettings, guildId);
						return;
					}
				} catch (Exception ignored) {}

				Message reactMessage = reactChannel
					.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
					.addFile(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4"))
					.complete();

				JsonObject newSettings = currentSettings.getAsJsonObject();
				newSettings.addProperty("previousMessageId", reactMessage.getId());
				database.setVerifySettings(event.getGuild().getId(), newSettings);

				verifyGuild = new VerifyGuild(reactChannel, reactMessage, newSettings, guildId);
			}
		} catch (Exception e) {
			log.error("Verify constructor error - " + event.getGuild().getId(), e);
		}
	}

	public String reloadVerifyConstructor(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return "Invalid guild";
		}

		JsonElement currentSettings = database.getVerifySettings(guild.getId());
		if (currentSettings == null) {
			return "No settings found";
		}

		try {
			if (higherDepth(currentSettings, "enable").getAsBoolean()) {
				TextChannel reactChannel = guild.getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
				try {
					Message reactMessage = reactChannel
						.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
						.complete();
					if (reactMessage != null) {
						reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

						verifyGuild = new VerifyGuild(reactChannel, reactMessage, currentSettings, guildId);
						return "Reloaded";
					}
				} catch (Exception ignored) {}

				Message reactMessage = reactChannel
					.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
					.addFile(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4"))
					.complete();

				JsonObject newSettings = currentSettings.getAsJsonObject();
				newSettings.addProperty("previousMessageId", reactMessage.getId());
				database.setVerifySettings(guild.getId(), newSettings);

				verifyGuild = new VerifyGuild(reactChannel, reactMessage, newSettings, guildId);
				return "Reloaded";
			} else {
				verifyGuild = new VerifyGuild(guildId);
				return "Not enabled";
			}
		} catch (Exception e) {
			log.error("Reload verify constructor error - " + guildId, e);
			if (e.getMessage().contains("Missing permission")) {
				return ("Error Reloading\nMissing permission: " + e.getMessage().split("Missing permission: ")[1]);
			}
		}
		return "Error Reloading";
	}

	/* Automated Guild Methods */
	public void updateGuild() {
		try {
			long startTime = System.currentTimeMillis();

			Guild guild = jda.getGuildById(guildId);
			List<AutomatedGuild> currentSettings = database.getAllGuildSettings(guild.getId());

			if (currentSettings == null) {
				return;
			}

			boolean anyGuildRoleRankEnable = false;
			for (int i = currentSettings.size() - 1; i >= 0; i--) {
				AutomatedGuild curSettings = currentSettings.get(i);
				if (curSettings.getGuildName() == null) {
					currentSettings.remove(i);
				} else if (
					curSettings.getGuildMemberRoleEnable().equalsIgnoreCase("true") ||
					curSettings.getGuildRanksEnable().equalsIgnoreCase("true")
				) {
					anyGuildRoleRankEnable = true;
				} else if (curSettings.getGuildCounterEnable() == null || curSettings.getGuildCounterEnable().equalsIgnoreCase("false")) {
					currentSettings.remove(i);
				}
			}

			if (currentSettings.size() == 0) {
				return;
			}

			Set<String> memberCountList = new HashSet<>();
			List<Member> inGuildUsers = new ArrayList<>();
			Map<String, String> discordIdToUuid = new HashMap<>();
			int counterUpdate = 0;
			if (anyGuildRoleRankEnable) {
				database
					.getLinkedUsers()
					.forEach(linkedUser -> discordIdToUuid.put(linkedUser.getDiscordId(), linkedUser.getMinecraftUuid()));

				CountDownLatch latch = new CountDownLatch(1);
				guild
					.findMembers(member -> discordIdToUuid.containsKey(member.getId()))
					.onSuccess(members -> {
						inGuildUsers.addAll(members);
						latch.countDown();
					})
					.onError(error -> latch.countDown());

				try {
					latch.await(15, TimeUnit.SECONDS);
				} catch (Exception e) {
					log.error("updateGuild latch - " + guildId, e);
				}
			}

			Set<String> inGuild = new HashSet<>();
			for (AutomatedGuild currentSetting : currentSettings) {
				HypixelResponse response = getGuildFromId(currentSetting.getGuildId());
				if (response.isNotValid()) {
					continue;
				}

				JsonArray guildMembers = response.get("members").getAsJsonArray();

				boolean enableGuildRole = currentSetting.getGuildMemberRoleEnable().equalsIgnoreCase("true");
				boolean enableGuildRanks = currentSetting.getGuildRanksEnable().equalsIgnoreCase("true");
				if (enableGuildRanks || enableGuildRole) {
					Map<String, String> uuidToRankMap = new HashMap<>();
					for (JsonElement guildMember : guildMembers) {
						uuidToRankMap.put(
							higherDepth(guildMember, "uuid").getAsString(),
							higherDepth(guildMember, "rank").getAsString().replace(" ", "_")
						);
					}

					Role guildMemberRole = enableGuildRole ? guild.getRoleById(currentSetting.getGuildMemberRole()) : null;
					for (Member linkedUser : inGuildUsers) {
						List<Role> rolesToAdd = new ArrayList<>();
						List<Role> rolesToRemove = new ArrayList<>();

						if (enableGuildRole) {
							if (uuidToRankMap.containsKey(discordIdToUuid.get(linkedUser.getId()))) {
								rolesToAdd.add(guildMemberRole);
								Role applyGuestRole = guildMap.get(guildId).applyGuestRole;
								if (applyGuestRole != null && !inGuild.contains(linkedUser.getId())) {
									inGuild.add(linkedUser.getId());
									rolesToRemove.add(applyGuestRole);
								}
							} else {
								rolesToRemove.add(guildMemberRole);
								Role applyGuestRole = guildMap.get(guildId).applyGuestRole;
								if (applyGuestRole != null && !inGuild.contains(linkedUser.getId())) {
									rolesToAdd.add(applyGuestRole);
								}
							}
						}

						if (enableGuildRanks) {
							List<RoleObject> guildRanksArr = currentSetting.getGuildRanks();
							if (!uuidToRankMap.containsKey(discordIdToUuid.get(linkedUser.getId()))) {
								for (RoleObject guildRank : guildRanksArr) {
									rolesToRemove.add(guild.getRoleById(guildRank.getRoleId()));
								}
							} else {
								String currentRank = uuidToRankMap.get(discordIdToUuid.get(linkedUser.getId()));
								for (RoleObject guildRank : guildRanksArr) {
									Role currentRankRole = guild.getRoleById(guildRank.getRoleId());
									if (guildRank.getValue().equalsIgnoreCase(currentRank)) {
										rolesToAdd.add(currentRankRole);
									} else {
										rolesToRemove.add(currentRankRole);
									}
								}
							}
						}

						try {
							guild.modifyMemberRoles(linkedUser, rolesToAdd, rolesToRemove).complete();
						} catch (Exception ignored) {}

						memberCountList.add(linkedUser.getId());
					}
				}

				if (currentSetting.getGuildCounterEnable() != null && currentSetting.getGuildCounterEnable().equals("true")) {
					VoiceChannel curVc = null;
					try {
						curVc = guild.getVoiceChannelById(currentSetting.getGuildCounterChannel());
					} catch (Exception ignored) {}

					if (curVc == null) {
						currentSetting.setGuildCounterEnable("false");
						database.setGuildSettings(guild.getId(), gson.toJsonTree(currentSetting));
						continue;
					}

					if (curVc.getName().contains(guildMembers.size() + "/125")) {
						continue;
					}

					if (curVc.getName().split(":").length == 2) {
						curVc.getManager().setName(curVc.getName().split(":")[0].trim() + ": " + guildMembers.size() + "/125").complete();
					} else {
						curVc
							.getManager()
							.setName(response.get("name").getAsString() + " Members: " + guildMembers.size() + "/125")
							.complete();
					}

					counterUpdate++;
				}
			}

			logCommand(
				guild,
				"Guild Role | Users (" +
				memberCountList.size() +
				") | Time (" +
				((System.currentTimeMillis() - startTime) / 1000) +
				"s)" +
				(counterUpdate > 0 ? " | Counters (" + counterUpdate + ")" : "")
			);
		} catch (Exception e) {
			log.error("updateGuild - " + guildId, e);
		}
	}

	/* Skyblock Event Methods */
	public void setEventMemberList(List<EventMember> eventMemberList) {
		this.eventMemberList = eventMemberList;
	}

	public void updateSkyblockEvent() {
		try {
			if (database.getSkyblockEventActive(guildId)) {
				JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
				Instant endingTime = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());
				if (Duration.between(Instant.now(), endingTime).toMinutes() <= 5) {
					endSkyblockEvent(guildId);
				}
			}
		} catch (Exception e) {
			log.error("updateSkyblockEvent - " + guildId, e);
		}
	}

	public void setEventMemberListLastUpdated(Instant eventMemberListLastUpdated) {
		this.eventMemberListLastUpdated = eventMemberListLastUpdated;
	}

	public void setSkyblockEventHandler(SkyblockEventHandler skyblockEventHandler) {
		this.skyblockEventHandler = skyblockEventHandler;
	}


	/* Events */
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		applyGuild.forEach(o1 -> o1.onMessageReactionAdd(event));
	}

	public void onGuildMessageReceived(MessageReceivedEvent event) {
		if (event.getGuild().getId().equals("796790757947867156") && event.getChannel().getId().equals("869278025018114108")) {
			if (
				event.getMessage().getEmbeds().size() > 0 &&
				event.getMessage().getEmbeds().get(0).getDescription() != null &&
				event
					.getMessage()
					.getEmbeds()
					.get(0)
					.getDescription()
					.contains("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/commit/") &&
				isMainBot()
			) {
				updateItemMappings();
				internalJsonMappings = null;
				getInternalJsonMappings();
				refreshPriceOverrideJson();
			}
			return;
		}

		if (verifyGuild.onGuildMessageReceived(event)) {
			return;
		}

		if (event.getAuthor().isBot() && !event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			return;
		}

		for (ApplyGuild guild : applyGuild) {
			if (guild.onGuildMessageReceived(event)) {
				return;
			}
		}
	}

	public void onTextChannelDelete(ChannelDeleteEvent event) {
		applyGuild.forEach(o1 -> o1.onTextChannelDelete(event));
	}

	public void onButtonClick(ButtonInteractionEvent event) {
		if (event.getComponentId().startsWith("paginator_") || event.getComponentId().startsWith("inv_paginator_")) {
			return;
		} else if (event.getComponentId().startsWith("event_message_")) {
			event.deferReply(true).complete();

			if (event.getComponentId().equals("event_message_join")) {
				event
					.getHook()
					.editOriginalEmbeds(
						SkyblockEventCommand.joinSkyblockEvent(event.getGuild().getId(), event.getUser().getId(), new String[0]).build()
					)
					.queue();
			} else {
				executor.submit(() -> {
					EmbedBuilder eb = SkyblockEventCommand.getEventLeaderboard(event);
					if (eb != null) {
						event.getHook().editOriginalEmbeds(eb.build()).queue();
					}
				});
			}
			return;
		} else if (event.getComponentId().startsWith("setup_command_")) {
			if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				event.reply("❌ You must have the administrator permission in this guild to use that!").setEphemeral(true).queue();
				return;
			}

			event.deferReply().complete();
			SetupCommandHandler handler = new SetupCommandHandler(event, event.getComponentId().split("setup_command_")[1]);
			if (handler.isValid()) {
				return;
			}
		} else if (event.getButton().getId().startsWith("apply_user_") && !event.getButton().getId().startsWith("apply_user_wait_")) {
			event.deferReply().complete();
		} else if (event.getButton().getId().startsWith("party_finder_channel_close_")) {
			if (event.getUser().getId().equals(event.getButton().getId().split("party_finder_channel_close_")[1])) {
				event.replyEmbeds(defaultEmbed("Party Finder").setDescription("Archiving thread...").build()).queue();
				((ThreadChannel) event.getChannel()).getManager().setArchived(true).queueAfter(3, TimeUnit.SECONDS);
			} else {
				event.replyEmbeds(invalidEmbed("Only the party leader can close the channel").build()).setEphemeral(true).queue();
			}
			return;
		} else {
			event.deferReply(true).complete();
		}

		for (ApplyGuild o1 : applyGuild) {
			String buttonClickReply = o1.onButtonClick(event);
			if (buttonClickReply != null) {
				if (buttonClickReply.equals("IGNORE_INTERNAL")) {
					return;
				} else if (buttonClickReply.startsWith("SBZ_SCAMMER_CHECK_")) {
					event
						.getHook()
						.editOriginalEmbeds(
							defaultEmbed("Error")
								.setDescription(
									"You have been marked as a scammer with reason `" +
									buttonClickReply.split("SBZ_SCAMMER_CHECK_")[1] +
									"`"
								)
								.setFooter("Scammer check powered by SkyBlockZ (discord.gg/skyblock)")
								.build()
						)
						.queue();
					return;
				}

				event.getHook().editOriginal(buttonClickReply).queue();
				return;
			}
		}

		event.editButton(event.getButton().asDisabled().withLabel("Disabled").withStyle(ButtonStyle.DANGER)).queue();
		event.getHook().editOriginal("❌ This button has been disabled").queue();
	}

	public void onGuildLeave() {
		for (ScheduledFuture<?> scheduledFuture : scheduledFutures) {
			scheduledFuture.cancel(true);
		}
	}

	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		verifyGuild.onGuildMemberJoin(event);
		if (applyGuestRole != null) {
			event.getGuild().addRoleToMember(event.getMember(), applyGuestRole).queue();
		}
	}

	/* Miscellaneous */
	public void schedulerConstructor() {
		int eventDelay = (int) (Math.random() * 60 + 5);
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateGuild, eventDelay, 180, TimeUnit.MINUTES));
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateSkyblockEvent, eventDelay, 60, TimeUnit.MINUTES));
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void updateItemMappings() {
		try {
			File neuDir = new File("src/main/java/com/skyblockplus/json/neu");
			if (neuDir.exists()) {
				FileUtils.deleteDirectory(neuDir);
			}
			neuDir.mkdir();

			File skyblockPlusDir = new File("src/main/java/com/skyblockplus/json/skyblock_plus");
			if (skyblockPlusDir.exists()) {
				FileUtils.deleteDirectory(skyblockPlusDir);
			}
			skyblockPlusDir.mkdir();

			Git neuRepo = Git
				.cloneRepository()
				.setURI("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO.git")
				.setDirectory(neuDir)
				.call();

			Git skyblockPlusDataRepo = Git
				.cloneRepository()
				.setURI("https://github.com/kr45732/skyblock-plus-data.git")
				.setDirectory(skyblockPlusDir)
				.call();

			JsonElement currentPriceOverrides = JsonParser.parseReader(
				new FileReader("src/main/java/com/skyblockplus/json/skyblock_plus/PriceOverrides.json")
			);
			try (Writer writer = new FileWriter("src/main/java/com/skyblockplus/json/skyblock_plus/PriceOverrides.json")) {
				formattedGson.toJson(getUpdatedPriceOverridesJson(currentPriceOverrides), writer);
				writer.flush();
			}

			try (Writer writer = new FileWriter("src/main/java/com/skyblockplus/json/skyblock_plus/InternalNameMappings.json")) {
				formattedGson.toJson(getUpdatedItemMappingsJson(), writer);
				writer.flush();
			}

			skyblockPlusDataRepo.add().addFilepattern("InternalNameMappings.json").addFilepattern("PriceOverrides.json").call();
			skyblockPlusDataRepo
				.commit()
				.setAuthor("kr45632", "52721908+kr45732@users.noreply.github.com")
				.setCommitter("kr45632", "52721908+kr45732@users.noreply.github.com")
				.setMessage("Automatic update (" + neuRepo.log().setMaxCount(1).call().iterator().next().getName() + ")")
				.call();
			skyblockPlusDataRepo.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(GITHUB_TOKEN, "")).call();

			FileUtils.deleteDirectory(neuDir);
			FileUtils.deleteDirectory(skyblockPlusDir);
		} catch (Exception e) {
			log.error("Exception while automatically updating item mappings", e);
		}
	}

	public JsonElement getUpdatedItemMappingsJson() {
		File dir = new File("src/main/java/com/skyblockplus/json/neu/items");
		JsonObject outputObj = new JsonObject();

		for (File child : Arrays.stream(dir.listFiles()).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList())) {
			try {
				JsonElement itemJson = JsonParser.parseReader(new FileReader(child));
				String itemName = parseMcCodes(higherDepth(itemJson, "displayname").getAsString()).replace("�", "");
				String itemId = higherDepth(itemJson, "internalname").getAsString();
				if (itemName.contains("(")) {
					continue;
				}

				if (itemName.startsWith("[Lvl")) {
					itemName = capitalizeString(NUMBER_TO_RARITY_MAP.get(itemId.split(";")[1])) + " " + itemName.split("] ")[1];
				}
				if (itemName.equals("Enchanted Book")) {
					itemName = parseMcCodes(higherDepth(itemJson, "lore.[0]").getAsString());
				}
				if (itemId.contains("-")) {
					itemId = itemId.replace("-", ":");
				}

				JsonObject toAdd = new JsonObject();
				toAdd.addProperty("name", itemName);
				if (itemId.contains("GENERATOR")) {
					toAdd.add("recipe", higherDepth(itemJson, "recipe"));
				}
				toAdd.add("wiki", higherDepth(itemJson, "infoType", "").equals("WIKI_URL") ? higherDepth(itemJson, "info.[0]") : null);

				outputObj.add(itemId, toAdd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return outputObj;
	}

	public JsonElement getUpdatedPriceOverridesJson(JsonElement currentPriceOverrides) {
		File dir = new File("src/main/java/com/skyblockplus/json/neu/items");
		JsonElement bazaarJson = higherDepth(getBazaarJson(), "products");
		JsonObject outputObject = new JsonObject();

		for (File child : dir.listFiles()) {
			try {
				JsonObject itemJson = JsonParser.parseReader(new FileReader(child)).getAsJsonObject();
				if (itemJson.has("vanilla")) {
					String id = itemJson.get("internalname").getAsString().replace("-", ":");
					if(higherDepth(bazaarJson, id + ".sell_summary.[0].pricePerUnit") == null) {
						outputObject.addProperty(id, Math.max(0, getNpcSellPrice(id)));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		JsonObject finalOutput = new JsonObject();
		finalOutput.add("manual", higherDepth(currentPriceOverrides, "manual"));
		finalOutput.add("automatic", outputObject);
		return finalOutput;
	}

	public void setApplyGuestRole(Role role) {
		this.applyGuestRole = role;
	}

	public void setPartyList(List<Party> partyList) {
		this.partyList = partyList;
	}

	public void setFetchurChannel(TextChannel channel) {
		this.fetchurChannel = channel;
	}

	public void setFetchurPing(Role role) {
		this.fetchurPing = role;
	}

	public void onFarmingContest(List<String> crops, MessageEmbed embed) {
		farmingContest.onFarmingContest(crops, embed);
	}

	public void onFetchur(MessageEmbed embed) {
		if (fetchurChannel != null) {
			if (fetchurPing == null) {
				fetchurChannel.sendMessageEmbeds(embed).queue();
			} else {
				fetchurChannel.sendMessage(fetchurPing.getAsMention()).setEmbeds(embed).queue();
			}
		}
	}

	public void onGuildMessageUpdate(MessageUpdateEvent event) {
		for (ApplyGuild guild : applyGuild) {
			if (guild.onGuildMessageUpdate(event)) {
				return;
			}
		}
	}

	public void onGuildMessageDelete(MessageDeleteEvent event) {
		for (ApplyGuild guild : applyGuild) {
			if (guild.onGuildMessageDelete(event)) {
				return;
			}
		}
	}
}
