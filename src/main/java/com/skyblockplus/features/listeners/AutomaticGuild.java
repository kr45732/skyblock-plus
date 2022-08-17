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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.features.mayor.MayorHandler.jerryEmbed;
import static com.skyblockplus.features.mayor.MayorHandler.votesEmbed;
import static com.skyblockplus.features.skyblockevent.SkyblockEventCommand.endSkyblockEvent;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.event.EventGuild;
import com.skyblockplus.features.jacob.JacobGuild;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.features.setup.SetupCommandHandler;
import com.skyblockplus.features.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventHandler;
import com.skyblockplus.features.verify.VerifyGuild;
import com.skyblockplus.miscellaneous.MayorCommand;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.RoleModifyRecord;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
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
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticGuild {

	private static final Logger log = LoggerFactory.getLogger(AutomaticGuild.class);
	public static final ScheduledFuture<?> logFuture = scheduler.scheduleWithFixedDelay(
		() -> guildMap.values().forEach(g -> g.logAction(null, null)),
		5,
		5,
		TimeUnit.MINUTES
	);

	/* Apply */
	public final List<ApplyGuild> applyGuild = new ArrayList<>();
	public Role applyGuestRole = null;
	/* Verify */
	public VerifyGuild verifyGuild;
	public final List<String> updatedMembers = new ArrayList<>();
	/* Skyblock event */
	public SkyblockEventHandler skyblockEventHandler = null;
	public List<EventMember> eventMemberList = new ArrayList<>();
	public Instant eventMemberListLastUpdated = null;
	public boolean eventCurrentlyUpdating = false;
	/* Event */
	public final EventGuild eventGuild;
	/* Fetchur */
	public TextChannel fetchurChannel = null;
	public Role fetchurPing = null;
	/* Mayor */
	public TextChannel mayorChannel = null;
	public Role mayorPing = null;
	public Message lastMayorElectionOpenMessage = null;
	public Message lastMayorElectedMessage = null;
	/* Party */
	public final List<Party> partyList = new ArrayList<>();
	/* Jacob */
	public final JacobGuild jacobGuild;
	/* Miscellaneous */
	public final List<String> botManagerRoles = new ArrayList<>();
	public final List<String> channelBlacklist = new ArrayList<>();
	public final String guildId;
	public final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
	public String prefix;
	public TextChannel logChannel = null;
	public final List<MessageEmbed> logQueue = new ArrayList<>();
	public JsonArray blacklist = new JsonArray();
	public List<String> isUsing = new ArrayList<>();

	/* Constructor */
	public AutomaticGuild(GenericGuildEvent event) {
		guildId = event.getGuild().getId();

		if (guildId.equals("796790757947867156") && isMainBot()) {
			try {
				long seconds = Duration
						.between(
								((GuildMessageChannel) jda.getGuildChannelById("957658797155975208")).getHistory()
										.retrievePast(1)
										.complete()
										.get(0)
										.getTimeCreated()
										.toInstant(),
								Instant.now()
						)
						.toSeconds();
				if (seconds < 900) {
					botStatusWebhook.send(
							client.getSuccess() +
									" Restarted in " + seconds
									+
									" seconds"
					);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		JsonElement serverSettings = allServerSettings.get(guildId);
		applyConstructor(event, serverSettings);
		verifyConstructor(event, higherDepth(serverSettings, "automatedVerify"));
		schedulerConstructor();
		prefix = higherDepth(serverSettings, "prefix", "");
		prefix = (prefix.length() > 0 && prefix.length() <= 5) ? prefix : DEFAULT_PREFIX;
		jacobGuild = new JacobGuild(higherDepth(serverSettings, "jacobSettings"), this);
		eventGuild = new EventGuild(higherDepth(serverSettings, "eventNotif"), this);
		try {
			blacklist = higherDepth(serverSettings, "blacklist.blacklist").getAsJsonArray();
			setIsUsing(higherDepth(blacklist, "isUsing").getAsJsonArray());
		} catch (Exception ignored) {}
		try {
			fetchurChannel = event.getGuild().getTextChannelById(higherDepth(serverSettings, "fetchurChannel", null));
		} catch (Exception ignored) {}
		try {
			applyGuestRole = event.getGuild().getRoleById(higherDepth(serverSettings, "applyGuestRole", null));
		} catch (Exception ignored) {}
		try {
			fetchurPing = event.getGuild().getRoleById(higherDepth(serverSettings, "fetchurRole", null));
		} catch (Exception ignored) {}
		try {
			mayorChannel = event.getGuild().getTextChannelById(higherDepth(serverSettings, "mayorChannel", null));
			lastMayorElectionOpenMessage =
				mayorChannel
					.getIterableHistory()
					.takeAsync(15)
					.get()
					.stream()
					.filter(m ->
						m.getAuthor().getId().equals(selfUserId) &&
						!m.getEmbeds().isEmpty() &&
						m.getEmbeds().get(0).getTitle() != null &&
						m.getEmbeds().get(0).getTitle().startsWith("Mayor Election Open | Year ")
					)
					.findFirst()
					.orElse(null);
			lastMayorElectedMessage =
				mayorChannel
					.getIterableHistory()
					.takeAsync(15)
					.get()
					.stream()
					.filter(m ->
						m.getAuthor().getId().equals(selfUserId) &&
						!m.getEmbeds().isEmpty() &&
						m.getEmbeds().get(0).getTitle() != null &&
						m.getEmbeds().get(0).getTitle().startsWith("Mayor Elected | Year ")
					)
					.findFirst()
					.orElse(null);
		} catch (Exception ignored) {}
		try {
			mayorPing = event.getGuild().getRoleById(higherDepth(serverSettings, "mayorRole", null));
		} catch (Exception ignored) {}
		try {
			botManagerRoles.addAll(
				streamJsonArray(higherDepth(serverSettings, "botManagerRoles").getAsJsonArray()).map(JsonElement::getAsString).toList()
			);
		} catch (Exception ignored) {}
		try {
			channelBlacklist.addAll(
				streamJsonArray(higherDepth(serverSettings, "channelBlacklist").getAsJsonArray()).map(JsonElement::getAsString).toList()
			);
		} catch (Exception ignored) {}
		try {
			logChannel = event.getGuild().getTextChannelById(higherDepth(serverSettings, "logChannel", null));
		} catch (Exception ignored) {}
		if (cacheDatabase.partyCaches.containsKey(guildId)) {
			this.partyList.addAll(cacheDatabase.partyCaches.get(guildId));
		}
	}

	public static String getGuildPrefix(String guildId) {
		AutomaticGuild automaticGuild = guildMap.getOrDefault(guildId, null);
		return automaticGuild != null ? automaticGuild.prefix : DEFAULT_PREFIX;
	}

	/* Apply Methods */
	public void applyConstructor(GenericGuildEvent event, JsonElement serverSettings) {
		List<AutomatedGuild> currentSettings;
		try {
			currentSettings =
				List.of(
					gson.fromJson(higherDepth(serverSettings, "automatedGuildOne"), AutomatedGuild.class),
					gson.fromJson(higherDepth(serverSettings, "automatedGuildTwo"), AutomatedGuild.class)
				);
		} catch (Exception e) {
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
				if (e instanceof PermissionException ex) {
					applyStr
						.append("• Error reloading `")
						.append(currentSetting.getGuildName())
						.append("` - missing permission: ")
						.append(ex.getPermission().getName())
						.append("\n");
				} else {
					applyStr.append("• Error Reloading for `").append(currentSetting.getGuildName()).append("`\n");
				}
			}
		}
		return applyStr.length() > 0 ? applyStr.toString() : "• Error reloading";
	}

	public void setBlacklist(JsonArray blacklist) {
		this.blacklist = blacklist;
	}

	/* Verify Methods */
	public void verifyConstructor(GenericGuildEvent event, JsonElement currentSettings) {
		verifyGuild = new VerifyGuild();
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

						verifyGuild = new VerifyGuild(reactChannel, reactMessage, currentSettings);
						return;
					}
				} catch (Exception ignored) {}

				MessageAction action = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString());
				if (higherDepth(currentSettings, "enableVerifyVideo", true)) {
					action = action.addFile(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4"));
				}
				Message reactMessage = action.complete();

				JsonObject newSettings = currentSettings.getAsJsonObject();
				newSettings.addProperty("previousMessageId", reactMessage.getId());
				database.setVerifySettings(event.getGuild().getId(), newSettings);

				verifyGuild = new VerifyGuild(reactChannel, reactMessage, newSettings);
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

						verifyGuild = new VerifyGuild(reactChannel, reactMessage, currentSettings);
						return "Reloaded";
					}
				} catch (Exception ignored) {}

				verifyGuild = new VerifyGuild(); // Prevent the old settings from deleting the new message

				MessageAction action = reactChannel.sendMessage(higherDepth(currentSettings, "messageText").getAsString());
				if (higherDepth(currentSettings, "enableVerifyVideo", true)) {
					action = action.addFile(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4"));
				}
				Message reactMessage = action.complete();

				JsonObject newSettings = currentSettings.getAsJsonObject();
				newSettings.addProperty("previousMessageId", reactMessage.getId());
				database.setVerifySettings(guild.getId(), newSettings);

				verifyGuild = new VerifyGuild(reactChannel, reactMessage, newSettings);
				return "Reloaded";
			} else {
				verifyGuild = new VerifyGuild();
				return "Not enabled";
			}
		} catch (Exception e) {
			log.error("Reload verify constructor error - " + guildId, e);
			if (e instanceof PermissionException ex) {
				return ("Error Reloading\nMissing permission: " + ex.getPermission().getName());
			}
		}
		return "Error Reloading";
	}

	/* Automated Guild Methods */
	public void updateGuild() {
		try {
			long startTime = System.currentTimeMillis();

			Guild guild = jda.getGuildById(guildId);

			JsonElement verifySettings = database.getVerifySettings(guild.getId());
			boolean verifyEnabled = higherDepth(verifySettings, "enableAutomaticSync", false);
			List<AutomatedGuild> guildSettings = database.getAllGuildSettings(guild.getId());

			if (guildSettings == null && !verifyEnabled) {
				return;
			}

			final boolean[] roleOrRankEnabled = { false };
			List<AutomatedGuild> filteredGuildSettings = null;
			if (guildSettings != null) {
				guildSettings.removeIf(g -> g == null || g.getGuildName() == null);
				filteredGuildSettings =
					guildSettings
						.stream()
						.filter(curSettings ->
							curSettings.getGuildMemberRoleEnable().equals("true") || curSettings.getGuildRanksEnable().equals("true")
								? (roleOrRankEnabled[0] = true)
								: (curSettings.getGuildCounterEnable() != null && !curSettings.getGuildCounterEnable().equals("false"))
						)
						.collect(Collectors.toList());

				if (filteredGuildSettings.isEmpty()) {
					return;
				}
			}

			JsonArray blacklist = guildMap.get(guildId).getBlacklist();

			List<Member> inGuildUsers = new ArrayList<>();
			Map<String, LinkedAccount> discordToUuid = new HashMap<>();
			int counterUpdate = 0;
			if (roleOrRankEnabled[0] || verifyEnabled) {
				discordToUuid.putAll(
					database.getLinkedAccounts().stream().collect(Collectors.toMap(LinkedAccount::discord, Function.identity()))
				);

				CountDownLatch latch = new CountDownLatch(1);
				guild
					.findMembers(member -> discordToUuid.containsKey(member.getId()))
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

			Map<Member, RoleModifyRecord> memberToRoleChanges = new HashMap<>();
			if (verifyEnabled) {
				List<Role> toAddRoles = streamJsonArray(higherDepth(verifySettings, "verifiedRoles").getAsJsonArray())
					.map(e -> guild.getRoleById(e.getAsString()))
					.filter(Objects::nonNull)
					.collect(Collectors.toCollection(ArrayList::new));
				List<Role> toRemoveRoles = new ArrayList<>();
				try {
					toRemoveRoles.add(guild.getRoleById(higherDepth(verifySettings, "verifiedRemoveRole").getAsString()));
				} catch (Exception ignored) {}

				List<HypixelResponse> guildResponses = null;
				String key = database.getServerHypixelApiKey(guild.getId());
				key = checkHypixelKey(key, false) == null ? key : null;
				int numUpdated = 0;

				List<Member> notUpdatedMembers = inGuildUsers.stream().filter(m -> !updatedMembers.contains(m.getId())).toList();
				if (notUpdatedMembers.size() < 120) {
					updatedMembers.clear();
					inGuildUsers.sort(Comparator.comparing(m -> !notUpdatedMembers.contains(m)));
				}

				for (Member linkedMember : inGuildUsers) {
					if (!guild.getSelfMember().canInteract(linkedMember)) {
						continue;
					}

					if (numUpdated < 120 && !updatedMembers.contains(linkedMember.getId())) {
						updatedMembers.add(linkedMember.getId());
						numUpdated++;

						String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname").getAsString();
						if (nicknameTemplate.contains("[IGN]")) {
							LinkedAccount linkedAccount = discordToUuid.get(linkedMember.getId());
							nicknameTemplate = nicknameTemplate.replace("[IGN]", linkedAccount.username());

							Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
							Player player = null;
							while (matcher.find()) {
								String category = matcher.group(1).toUpperCase();
								String type = matcher.group(2).toUpperCase();
								String extra = matcher.group(3) == null ? "" : matcher.group(3);

								if (
									category.equals("GUILD") &&
									(type.equals("NAME") || type.equals("TAG") || type.equals("RANK")) &&
									guildSettings != null &&
									!guildSettings.isEmpty()
								) {
									if (guildResponses == null) {
										guildResponses =
											guildSettings.stream().map(g -> getGuildFromId(g.getGuildId())).collect(Collectors.toList());
									}
									HypixelResponse guildResponse = guildResponses
										.stream()
										.filter(g ->
											streamJsonArray(g.get("members").getAsJsonArray())
												.anyMatch(m -> higherDepth(m, "uuid", "").equals(linkedAccount.uuid()))
										)
										.findFirst()
										.orElse(null);

									if (guildResponse != null) {
										nicknameTemplate =
											nicknameTemplate.replace(
												matcher.group(0),
												switch (type) {
													case "NAME" -> guildResponse.get("name").getAsString();
													case "RANK" -> higherDepth(
														streamJsonArray(guildResponse.get("members").getAsJsonArray())
															.filter(g -> higherDepth(g, "uuid", "").equals(linkedAccount.uuid()))
															.findFirst()
															.orElse(null),
														"rank",
														""
													);
													default -> guildResponse.get("tag").getAsString();
												} +
												extra
											);
									}
								} else if (
									category.equals("PLAYER") &&
									(
										type.equals("SKILLS") ||
										type.equals("CATACOMBS") ||
										type.equals("SLAYER") ||
										type.equals("WEIGHT") ||
										type.equals("CLASS")
									)
								) {
									if (key != null) {
										if (player == null) {
											HypixelResponse response = skyblockProfilesFromUuid(linkedAccount.uuid(), key);
											player =
												!response.isValid()
													? new Player()
													: new Player(linkedAccount.uuid(), linkedAccount.username(), response.response());
										}

										if (player.isValid()) {
											nicknameTemplate =
												nicknameTemplate.replace(
													matcher.group(0),
													switch (type) {
														case "SKILLS" -> roundAndFormat((int) player.getSkillAverage());
														case "SLAYER" -> simplifyNumber(player.getTotalSlayer());
														case "WEIGHT" -> roundAndFormat((int) player.getWeight());
														case "CLASS" -> player.getSelectedDungeonClass().equals("none")
															? ""
															: "" + player.getSelectedDungeonClass().toUpperCase().charAt(0);
														default -> roundAndFormat((int) player.getCatacombs().getProgressLevel());
													} +
													extra
												);
										}
									}
								}

								nicknameTemplate = nicknameTemplate.replace(matcher.group(0), "");
							}

							if (
								streamJsonArray(blacklist).noneMatch(u -> higherDepth(u, "uuid").getAsString().equals(linkedAccount.uuid()))
							) {
								linkedMember.modifyNickname(nicknameTemplate).queue(ignore, ignore);
							}
						}
					}

					if (!toAddRoles.isEmpty() || !toRemoveRoles.isEmpty()) {
						memberToRoleChanges.put(
							linkedMember,
							memberToRoleChanges
								.getOrDefault(linkedMember, new RoleModifyRecord(discordToUuid.get(linkedMember.getId()).uuid()))
								.update(toAddRoles, toRemoveRoles)
						);
					}
				}
			}

			Set<String> memberCountList = new HashSet<>();
			if (filteredGuildSettings != null) {
				Set<String> inGuild = new HashSet<>();
				for (AutomatedGuild currentSetting : filteredGuildSettings) {
					HypixelResponse response = getGuildFromId(currentSetting.getGuildId());
					if (!response.isValid()) {
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
						Role applyGuestRole = guildMap.get(guildId).applyGuestRole;
						for (Member linkedMember : inGuildUsers) {
							if (!guild.getSelfMember().canInteract(linkedMember)) {
								continue;
							}

							List<Role> rolesToAdd = new ArrayList<>();
							List<Role> rolesToRemove = new ArrayList<>();

							if (enableGuildRole) {
								if (uuidToRankMap.containsKey(discordToUuid.get(linkedMember.getId()).uuid())) {
									rolesToAdd.add(guildMemberRole);
									if (applyGuestRole != null && !inGuild.contains(linkedMember.getId())) {
										inGuild.add(linkedMember.getId());
										rolesToRemove.add(applyGuestRole);
									}
								} else {
									rolesToRemove.add(guildMemberRole);
									if (applyGuestRole != null && !inGuild.contains(linkedMember.getId())) {
										rolesToAdd.add(applyGuestRole);
									}
								}
							}

							if (enableGuildRanks) {
								List<RoleObject> guildRanksArr = currentSetting.getGuildRanks();
								if (!uuidToRankMap.containsKey(discordToUuid.get(linkedMember.getId()).uuid())) {
									for (RoleObject guildRank : guildRanksArr) {
										rolesToRemove.add(guild.getRoleById(guildRank.getRoleId()));
									}
								} else {
									String currentRank = uuidToRankMap.get(discordToUuid.get(linkedMember.getId()).uuid());
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

							if (!rolesToAdd.isEmpty() || !rolesToRemove.isEmpty()) {
								memberToRoleChanges.put(
									linkedMember,
									memberToRoleChanges
										.getOrDefault(linkedMember, new RoleModifyRecord(discordToUuid.get(linkedMember.getId()).uuid()))
										.update(rolesToAdd, rolesToRemove)
								);
							}
							memberCountList.add(linkedMember.getId());
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

						curVc
							.getManager()
							.setName(
								curVc.getName().split(":").length == 2
									? curVc.getName().split(":")[0].trim() + ": " + guildMembers.size() + "/125"
									: response.get("name").getAsString() + " Members: " + guildMembers.size() + "/125"
							)
							.queue();

						counterUpdate++;
					}
				}
			}

			for (Map.Entry<Member, RoleModifyRecord> entry : memberToRoleChanges.entrySet()) {
				if (streamJsonArray(blacklist).noneMatch(u -> higherDepth(u, "uuid").getAsString().equals(entry.getValue().uuid()))) {
					try {
						guild.modifyMemberRoles(entry.getKey(), entry.getValue().add(), entry.getValue().remove()).queue();
					} catch (Exception ignored) {}
				}
			}

			logCommand(
				guild,
				"Update Guild | Time (" +
				((System.currentTimeMillis() - startTime) / 1000) +
				"s)" +
				(!memberCountList.isEmpty() ? " | Users (" + memberCountList.size() + ")" : "") +
				(counterUpdate > 0 ? " | Counters (" + counterUpdate + ")" : "")
			);
			logAction(
				defaultEmbed("Automatic Guild Update")
					.setDescription(
						"• Checked " +
						formatNumber(memberCountList.size()) +
						" linked members" +
						(counterUpdate > 0 ? "\n• Updated " + counterUpdate + " counter" + (counterUpdate > 1 ? "s" : "") : "")
					)
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
		} catch (Exception ignored) {}
	}

	public void setEventMemberListLastUpdated(Instant eventMemberListLastUpdated) {
		this.eventMemberListLastUpdated = eventMemberListLastUpdated;
	}

	public void setSkyblockEventHandler(SkyblockEventHandler skyblockEventHandler) {
		this.skyblockEventHandler = skyblockEventHandler;
	}

	public void setEventCurrentlyUpdating(boolean eventCurrentlyUpdating) {
		this.eventCurrentlyUpdating = eventCurrentlyUpdating;
	}

	/* Fetchur */
	public void setFetchurChannel(TextChannel channel) {
		this.fetchurChannel = channel;
	}

	public void setFetchurPing(Role role) {
		this.fetchurPing = role;
	}

	public boolean onFetchur(MessageEmbed embed) {
		try {
			if (fetchurChannel != null) {
				if (!fetchurChannel.canTalk()) {
					logAction(
						defaultEmbed("Fetchur Notifications")
							.setDescription("Missing permissions to view or send messages in " + fetchurChannel.getAsMention())
					);
					return false;
				}

				if (fetchurPing == null) {
					fetchurChannel.sendMessageEmbeds(embed).queue();
				} else {
					fetchurChannel.sendMessage(fetchurPing.getAsMention()).setEmbeds(embed).queue();
				}
				return true;
			}
		} catch (Exception e) {
			log.error(guildId, e);
		}
		return false;
	}

	/* Mayor */
	public void setMayorChannel(TextChannel channel) {
		this.mayorChannel = channel;
	}

	public void setMayorPing(Role role) {
		this.mayorPing = role;
	}

	public boolean onMayorElection(MessageEmbed embed, Button button, int year) {
		try {
			if (mayorChannel != null) {
				if (!mayorChannel.canTalk()) {
					logAction(
						defaultEmbed("Mayor Notifications")
							.setDescription("Missing permissions to view or send messages in " + mayorChannel.getAsMention())
					);
					return false;
				}

				if (
					lastMayorElectionOpenMessage != null &&
					Integer.parseInt(lastMayorElectionOpenMessage.getEmbeds().get(0).getTitle().split("Year ")[1]) != year
				) {
					lastMayorElectionOpenMessage = null;
				}

				if (lastMayorElectionOpenMessage != null) {
					mayorChannel
						.editMessageEmbedsById(lastMayorElectionOpenMessage.getId(), embed)
						.setActionRow(button)
						.queue(
							m -> lastMayorElectionOpenMessage = m,
							e -> {
								if (e instanceof ErrorResponseException ex && ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
									lastMayorElectionOpenMessage = null;
								}
							}
						);
				} else {
					mayorChannel.sendMessageEmbeds(embed).setActionRow(button).queue(m -> lastMayorElectionOpenMessage = m);
				}
				return true;
			}
		} catch (Exception e) {
			log.error(guildId, e);
		}
		return false;
	}

	public boolean onMayorElected(MessageEmbed embed, Button button) {
		try {
			if (mayorChannel != null) {
				if (lastMayorElectionOpenMessage != null) {
					lastMayorElectionOpenMessage.editMessageComponents().queue(ignore, ignore);
					lastMayorElectionOpenMessage = null;
				}
				if (lastMayorElectedMessage != null) {
					lastMayorElectedMessage.editMessageComponents().queue(ignore, ignore);
					lastMayorElectedMessage = null;
				}

				if (!mayorChannel.canTalk()) {
					logAction(
						defaultEmbed("Mayor Notifications")
							.setDescription("Missing permissions to view or send messages in " + mayorChannel.getAsMention())
					);
					return false;
				}

				MessageAction action = mayorChannel.sendMessageEmbeds(embed);
				if (button != null) {
					action = action.setActionRow(button);
				}
				if (mayorPing != null) {
					action = action.content(mayorPing.getAsMention());
				}
				action.queue(m -> lastMayorElectedMessage = m);

				return true;
			}
		} catch (Exception e) {
			log.error(guildId, e);
		}
		return false;
	}

	/* Jacob */
	public boolean onFarmingContest(List<String> crops, MessageEmbed embed) {
		return jacobGuild.onFarmingContest(crops, embed);
	}

	/* Events */
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		applyGuild.forEach(o1 -> o1.onMessageReactionAdd(event));
	}

	public void onGuildMessageReceived(MessageReceivedEvent event) {
		if (verifyGuild.onGuildMessageReceived(event)) {
			return;
		}

		if (event.getAuthor().isBot() && !event.getAuthor().getId().equals(selfUserId)) {
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
		if (
			event.getComponentId().startsWith("paginator_") ||
			event.getComponentId().startsWith("reactive_") ||
			event.getComponentId().startsWith("inv_paginator_") ||
			event.getComponentId().startsWith("inv_list_paginator_") ||
			event.getComponentId().startsWith("leaderboard_paginator_")
		) {
			return;
		} else if (event.getComponentId().equals("mayor_graph_button")) {
			event.replyEmbeds(votesEmbed).setEphemeral(true).queue();
			return;
		} else if (event.getComponentId().equals("mayor_special_button")) {
			event.replyEmbeds(MayorCommand.getSpecialMayors().build()).setEphemeral(true).queue();
			return;
		} else if (event.getComponentId().equals("mayor_current_election_button")) {
			Message msg = guildMap.get("796790757947867156").lastMayorElectionOpenMessage;
			event
				.reply(msg != null ? msg : new MessageBuilder().setEmbeds(invalidEmbed("Election is not open").build()).build())
				.setEphemeral(true)
				.queue();
			return;
		} else if (event.getComponentId().equals("mayor_jerry_button")) {
			event.replyEmbeds(jerryEmbed).setEphemeral(true).queue();
			return;
		} else if (event.getComponentId().startsWith("bingo_")) {
			StringBuilder card = new StringBuilder();
			String[] split = event.getComponentId().split("bingo_")[1].split("");
			for (int i = 0; i < split.length; i++) {
				card
					.append(i % 5 == 0 ? "\n" : "")
					.append(
						switch (split[i]) {
							case "C" -> getEmoji("EMERALD_BLOCK", "e");
							case "c" -> getEmoji("IRON_BLOCK", "e");
							case "S" -> getEmoji("INK_SACK:10", "e");
							default -> getEmoji("PAPER", "e");
						}
					);
			}
			event.reply(card.toString()).setEphemeral(true).queue();
			return;
		} else if (event.getComponentId().startsWith("track_auctions_")) {
			String[] discordUuidSplit = event.getComponentId().split("track_auctions_")[1].split("_", 2)[1].split("_", 2);
			if (event.getUser().getId().equals(discordUuidSplit[0])) {
				event
					.deferReply(true)
					.queue(ignored -> {
						if (event.getComponentId().startsWith("track_auctions_start_")) {
							MessageEmbed eb = AuctionTracker.trackAuctions(discordUuidSplit[1], event.getUser().getId()).build();
							event.getHook().editOriginalEmbeds(eb).queue();
							if (!eb.getTitle().equals("Error")) {
								ActionRow updatedButton = ActionRow.of(
									Button.primary(
										"track_auctions_stop_" + event.getUser().getId() + "_" + discordUuidSplit[1],
										"Stop Tracking Auctions"
									)
								);
								(
									event.getMessage().getActionRows().size() == 1
										? event.getMessage().editMessageComponents(updatedButton)
										: event.getMessage().editMessageComponents(event.getMessage().getActionRows().get(0), updatedButton)
								).queue();
							}
						} else if (event.getComponentId().startsWith("track_auctions_stop_")) {
							MessageEmbed eb = AuctionTracker.stopTrackingAuctions(event.getUser().getId()).build();
							WebhookMessageUpdateAction<Message> action = event.getHook().editOriginalEmbeds(eb);
							if (!eb.getTitle().equals("Error")) {
								ActionRow updatedButton = ActionRow.of(
									Button.primary(
										"track_auctions_start_" + event.getUser().getId() + "_" + discordUuidSplit[1],
										"Track Auctions"
									)
								);
								(
									event.getMessage().getActionRows().size() == 1
										? event.getMessage().editMessageComponents(updatedButton)
										: event.getMessage().editMessageComponents(event.getMessage().getActionRows().get(0), updatedButton)
								).queue();
							}

							action.queue();
						}
					});
			}
			return;
		} else if (event.getComponentId().startsWith("event_message_")) {
			event
				.deferReply(true)
				.queue(ignored -> {
					if (event.getComponentId().equals("event_message_join")) {
						event
							.getHook()
							.editOriginalEmbeds(
								SkyblockEventCommand.joinSkyblockEvent(null, null, event.getMember(), event.getGuild().getId()).build()
							)
							.queue();
					} else {
						EmbedBuilder eb = SkyblockEventCommand.getEventLeaderboard(event.getGuild(), event.getUser(), null, event);
						if (eb != null) {
							event.getHook().editOriginalEmbeds(eb.build()).queue();
						}
					}
				});
			return;
		} else if (event.getComponentId().startsWith("setup_command_")) {
			if (!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())) {
				event
					.reply(client.getError() + " You must have the administrator permission in this guild to use that!")
					.setEphemeral(true)
					.queue();
				return;
			}

			event.deferReply().complete();
			SetupCommandHandler handler = new SetupCommandHandler(event, event.getComponentId().split("setup_command_")[1]);
			if (handler.isValid()) {
				return;
			}
		} else if (event.getComponentId().startsWith("party_finder_channel_close_")) {
			if (event.getUser().getId().equals(event.getComponentId().split("party_finder_channel_close_")[1])) {
				event.replyEmbeds(defaultEmbed("Party Finder").setDescription("Archiving thread...").build()).queue();
				((ThreadChannel) event.getChannel()).getManager().setArchived(true).queueAfter(3, TimeUnit.SECONDS);
			} else {
				event.replyEmbeds(invalidEmbed("Only the party leader can close the channel").build()).setEphemeral(true).queue();
			}
			return;
		} else {
			if (event.getComponentId().startsWith("apply_user_") && !event.getComponentId().startsWith("apply_user_wait_")) {
				event.deferReply().complete();
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
		}

		event.editButton(event.getButton().asDisabled().withLabel("Disabled").withStyle(ButtonStyle.DANGER)).queue();
		event.getHook().editOriginal(client.getError() + " This button has been disabled").queue();
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

	/* Miscellaneous */
	public void schedulerConstructor() {
		int eventDelay = (int) (Math.random() * 60 + 5);
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateGuild, eventDelay, 180, TimeUnit.MINUTES));
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateSkyblockEvent, eventDelay, 60, TimeUnit.MINUTES));
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setLogChannel(TextChannel channel) {
		this.logChannel = channel;
	}

	public void setApplyGuestRole(Role role) {
		this.applyGuestRole = role;
	}

	public void setBotManagerRoles(List<String> botManagerRoles) {
		this.botManagerRoles.clear();
		this.botManagerRoles.addAll(botManagerRoles);
	}

	public boolean isAdmin(Member member) {
		if (!member.hasPermission(Permission.ADMINISTRATOR)) {
			List<String> playerRoles = member.getRoles().stream().map(ISnowflake::getId).toList();
			return botManagerRoles.stream().anyMatch(playerRoles::contains);
		}
		return true;
	}

	public static Logger getLogger() {
		return log;
	}

	public void logAction(EmbedBuilder eb) {
		logAction(eb, jda.getGuildById(guildId).getSelfMember());
	}

	public void logAction(EmbedBuilder eb, Member member) {
		try {
			if (logChannel == null) {
				return;
			}

			if (eb != null && member != null) {
				eb.setAuthor(member.getEffectiveName() + " (" + member.getId() + ")", null, member.getEffectiveAvatarUrl());
				eb.setTimestamp(Instant.now());
				logQueue.add(eb.build());
			}

			if (logQueue.size() == 5 || logQueue.stream().anyMatch(l -> l.getTimestamp().isBefore(OffsetDateTime.now().minusMinutes(5)))) {
				logChannel.sendMessageEmbeds(logQueue).queue();
				logQueue.clear();
			}
		} catch (Exception e) {
			log.error(guildId, e);
		}
	}

	public void setChannelBlacklist(List<String> channelBlacklist) {
		this.channelBlacklist.clear();
		this.channelBlacklist.addAll(channelBlacklist);
	}

	public void setIsUsing(JsonArray arr) {
		isUsing = streamJsonArray(arr).map(JsonElement::getAsString).collect(Collectors.toList());
	}

	public JsonArray getBlacklist() {
		JsonArray currentBlacklist = new JsonArray();
		currentBlacklist.addAll(blacklist);
		for (String g : isUsing) {
			currentBlacklist.addAll(guildMap.get(g).blacklist);
		}
		return currentBlacklist;
	}

	public void onEventNotif(Map<String, MessageEmbed> ebs) {
		eventGuild.onEventNotif(ebs);
	}
}
