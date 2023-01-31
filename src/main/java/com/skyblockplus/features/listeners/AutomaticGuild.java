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
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
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
import com.skyblockplus.features.skyblockevent.SkyblockEventHandler;
import com.skyblockplus.features.skyblockevent.SkyblockEventSlashCommand;
import com.skyblockplus.features.verify.VerifyGuild;
import com.skyblockplus.general.LinkSlashCommand;
import com.skyblockplus.miscellaneous.MayorSlashCommand;
import com.skyblockplus.miscellaneous.RolesSlashCommand;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.HypixelPlayer;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticGuild {

	private static final Logger log = LoggerFactory.getLogger(AutomaticGuild.class);
	private static final ScheduledFuture<?> logFuture = scheduler.scheduleWithFixedDelay(
		() -> guildMap.values().forEach(g -> g.logAction(null, null)),
		5,
		5,
		TimeUnit.MINUTES
	);

	/* Apply */
	public final List<ApplyGuild> applyGuild = new ArrayList<>();
	private Role applyGuestRole = null;
	/* Verify */
	public VerifyGuild verifyGuild;
	private final Set<String> updatedMembers = new HashSet<>();
	/* Skyblock event */
	public SkyblockEventHandler skyblockEventHandler = null;
	public List<EventMember> eventMemberList = new ArrayList<>();
	public Instant eventMemberListLastUpdated = null;
	public boolean eventCurrentlyUpdating = false;
	private ScheduledFuture<?> sbEventFuture;
	/* Event */
	public final EventGuild eventGuild;
	/* Fetchur */
	private TextChannel fetchurChannel = null;
	private Role fetchurPing = null;
	/* Mayor */
	private TextChannel mayorChannel = null;
	private Role mayorPing = null;
	public Message lastMayorElectionOpenMessage = null;
	public Message lastMayorElectedMessage = null;
	/* Party */
	public final List<Party> partyList = new ArrayList<>();
	/* Jacob */
	public final JacobGuild jacobGuild;
	/* Miscellaneous */
	private final List<String> botManagerRoles = new ArrayList<>();
	public final String guildId;
	private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
	private TextChannel logChannel = null;
	private final List<MessageEmbed> logQueue = new ArrayList<>();
	private JsonArray blacklist = new JsonArray();
	private List<String> isUsing = new ArrayList<>();

	/* Constructor */
	public AutomaticGuild(GenericGuildEvent event) {
		guildId = event.getGuild().getId();

		if (isMainBot() && guildId.equals("796790757947867156")) {
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
					botStatusWebhook.send(client.getSuccess() + " Restarted in " + seconds + " seconds");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		JsonElement serverSettings = allServerSettings.get(guildId);
		applyConstructor(event, serverSettings);
		verifyConstructor(event, higherDepth(serverSettings, "automatedVerify"));
		int eventDelay = (int) (Math.random() * 60 + 5);
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateGuild, eventDelay, 60, TimeUnit.MINUTES));
		scheduleSbEventFuture(higherDepth(serverSettings, "sbEvent"));
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
			try {
				// Election closed
				if (
					Instant.now().getEpochSecond() >
					Long.parseLong(
						lastMayorElectionOpenMessage.getEmbeds().get(0).getDescription().split("Closes:\\*\\* <t:")[1].split(":R>")[0]
					)
				) {
					lastMayorElectionOpenMessage = null;
				}
			} catch (Exception ignored) {}
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
			logChannel = event.getGuild().getTextChannelById(higherDepth(serverSettings, "logChannel", null));
		} catch (Exception ignored) {}
		if (cacheDatabase.partyCaches.containsKey(guildId)) {
			partyList.addAll(cacheDatabase.partyCaches.get(guildId));
		}
	}

	/* Apply Methods */
	public void applyConstructor(GenericGuildEvent event, JsonElement serverSettings) {
		List<AutomatedGuild> currentSettings;
		try {
			currentSettings =
				gson.fromJson(higherDepth(serverSettings, "automatedGuilds"), new TypeToken<List<AutomatedGuild>>() {}.getType());
		} catch (Exception e) {
			return;
		}
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
			if (!higherDepth(currentSettings, "enable", false)) {
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
						reactMessage
							.editMessage(higherDepth(currentSettings, "messageText").getAsString())
							.setAttachments()
							.setActionRow(Button.primary("verify_button", "Verify"), Button.primary("verify_help_button", "Help"))
							.queue();

						verifyGuild = new VerifyGuild(reactChannel, reactMessage, currentSettings);
						return;
					}
				} catch (Exception ignored) {}

				MessageCreateAction action = reactChannel
					.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
					.setActionRow(Button.primary("verify_button", "Verify"), Button.primary("verify_help_button", "Help"));
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
						reactMessage
							.editMessage(higherDepth(currentSettings, "messageText").getAsString())
							.setAttachments()
							.setActionRow(Button.primary("verify_button", "Verify"), Button.primary("verify_help_button", "Help"))
							.queue();

						verifyGuild = new VerifyGuild(reactChannel, reactMessage, currentSettings);
						return "Reloaded";
					}
				} catch (Exception ignored) {}

				verifyGuild = new VerifyGuild(); // Prevent the old settings from deleting the new message

				MessageCreateAction action = reactChannel
					.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
					.setActionRow(Button.primary("verify_button", "Verify"), Button.primary("verify_help_button", "Help"));
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

			JsonElement serverSettings = database.getServerSettings(guild.getId());
			List<AutomatedGuild> guildSettings = database.getAllGuildSettings(guild.getId());

			// Should only happens if the server settings don't exist
			if (serverSettings == null || guildSettings == null) {
				return;
			}

			boolean verifyEnabled = higherDepth(serverSettings, "automatedVerify.enableAutomaticSync", false);
			boolean rolesEnabled = higherDepth(serverSettings, "automatedVerify.enableAutomaticRolesSync", false);

			JsonElement rolesSettings = higherDepth(serverSettings, "automatedRoles");
			guildSettings.removeIf(g -> g == null || g.getGuildName() == null);

			// Filtered for role, rank, or member count enabled
			List<AutomatedGuild> filteredGuildSettings = new ArrayList<>();
			boolean roleOrRankEnabled = false;
			for (AutomatedGuild curSettings : guildSettings) {
				boolean curRoleOrRankEnabled =
					Objects.equals(curSettings.getGuildMemberRoleEnable(), "true") ||
					Objects.equals(curSettings.getGuildRanksEnable(), "true");
				roleOrRankEnabled = roleOrRankEnabled || curRoleOrRankEnabled;
				if (curRoleOrRankEnabled || Objects.equals(curSettings.getGuildCounterEnable(), "true")) {
					filteredGuildSettings.add(curSettings);
				}
			}

			if (filteredGuildSettings.isEmpty() && !verifyEnabled && !rolesEnabled) {
				return;
			}

			List<String> blacklist = streamJsonArray(guildMap.get(guildId).getBlacklist())
				.map(u -> higherDepth(u, "uuid").getAsString())
				.toList();

			List<Member> inGuildUsers = new ArrayList<>();
			Map<String, LinkedAccount> discordToUuid = new HashMap<>();
			int counterUpdate = 0;
			if (roleOrRankEnabled || verifyEnabled || rolesEnabled) {
				discordToUuid.putAll(
					database.getAllLinkedAccounts().stream().collect(Collectors.toMap(LinkedAccount::discord, Function.identity()))
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

			Map<String, HypixelResponse> guildResponses = null;
			Map<Member, RoleModifyRecord> memberToRoleChanges = new HashMap<>();
			if (verifyEnabled || rolesEnabled) {
				List<Role> verifyRolesAdd = new ArrayList<>();
				List<Role> verifyRolesRemove = new ArrayList<>();
				if (verifyEnabled) {
					verifyRolesAdd.addAll(
						streamJsonArray(higherDepth(serverSettings, "automatedVerify.verifiedRoles").getAsJsonArray())
							.map(e -> guild.getRoleById(e.getAsString()))
							.filter(Objects::nonNull)
							.toList()
					);
					try {
						verifyRolesRemove.add(
							guild.getRoleById(higherDepth(serverSettings, "automatedVerify.verifiedRemoveRole").getAsString())
						);
					} catch (Exception ignored) {}
				}

				String key = database.getServerHypixelApiKey(guild.getId());
				key = checkHypixelKey(key, false) == null ? key : null;
				int numUpdated = 0;

				int updateLimit = rolesEnabled && key != null ? 30 : 120;

				if (inGuildUsers.stream().filter(m -> !updatedMembers.contains(m.getId())).limit(updateLimit).count() < updateLimit) {
					inGuildUsers.sort(Comparator.comparing(m -> updatedMembers.contains(m.getId())));
					updatedMembers.clear();
				}

				for (Member linkedMember : inGuildUsers) {
					// updatedMembers.add returns true if ele not in set
					if (numUpdated < updateLimit && updatedMembers.add(linkedMember.getId())) {
						if (!guild.getSelfMember().canInteract(linkedMember)) {
							continue;
						}

						LinkedAccount linkedAccount = discordToUuid.get(linkedMember.getId());
						if (blacklist.contains(linkedAccount.uuid())) {
							continue;
						}

						numUpdated++;

						List<Role> toAddRoles = new ArrayList<>(verifyRolesAdd);
						List<Role> toRemoveRoles = new ArrayList<>(verifyRolesRemove);
						Player player = null;

						if (verifyEnabled) {
							String nicknameTemplate = higherDepth(serverSettings, "automatedVerify.verifiedNickname").getAsString();
							if (nicknameTemplate.contains("[IGN]")) {
								nicknameTemplate = nicknameTemplate.replace("[IGN]", linkedAccount.username());

								Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
								HypixelPlayer hypixelPlayer = null;
								while (matcher.find()) {
									String category = matcher.group(1).toUpperCase();
									String type = matcher.group(2).toUpperCase();
									String extra = matcher.group(3) == null ? "" : matcher.group(3);

									if (
										category.equals("GUILD") &&
										(type.equals("NAME") || type.equals("TAG") || type.equals("RANK")) &&
										!guildSettings.isEmpty()
									) {
										if (guildResponses == null) {
											guildResponses =
												guildSettings
													.stream()
													.collect(
														Collectors.toMap(AutomatedGuild::getGuildId, g -> getGuildFromId(g.getGuildId()))
													);
										}

										HypixelResponse guildResponse = guildResponses
											.values()
											.stream()
											.filter(g ->
												streamJsonArray(g.get("members").getAsJsonArray())
													.anyMatch(m -> higherDepth(m, "uuid", "").equals(linkedAccount.uuid()))
											)
											.findFirst()
											.orElse(null);

										if (guildResponse != null && guildResponse.isValid()) {
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
											type.equals("CLASS") ||
											type.equals("LEVEL")
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
															case "SKILLS" -> formatNumber((int) player.getSkillAverage());
															case "SLAYER" -> simplifyNumber(player.getTotalSlayer());
															case "WEIGHT" -> formatNumber((int) player.getWeight());
															case "CLASS" -> player.getSelectedDungeonClass().equals("none")
																? ""
																: "" + player.getSelectedDungeonClass().toUpperCase().charAt(0);
															case "LEVEL" -> formatNumber((int) player.getLevel());
															default -> formatNumber((int) player.getCatacombs().getProgressLevel());
														} +
														extra
													);
											}
										}
									} else if (category.equals("HYPIXEL") && type.equals("RANK")) {
										if (key != null) {
											if (hypixelPlayer == null) {
												numUpdated++; // Requires another request
												HypixelResponse response = playerFromUuid(linkedAccount.uuid());
												hypixelPlayer =
													response.isValid()
														? new HypixelPlayer(
															linkedAccount.uuid(),
															linkedAccount.username(),
															response.response()
														)
														: new HypixelPlayer();
											}

											if (hypixelPlayer.isValid()) {
												nicknameTemplate =
													nicknameTemplate.replace(matcher.group(0), hypixelPlayer.getRank() + extra);
											}
										}
									}

									nicknameTemplate = nicknameTemplate.replace(matcher.group(0), "");
								}

								if ((player != null && !player.isValid()) || (hypixelPlayer != null && !hypixelPlayer.isValid())) {
									continue;
								}

								linkedMember.modifyNickname(nicknameTemplate).queue(ignore, ignore);
							}
						}

						if (rolesEnabled && key != null) {
							if (player == null) {
								HypixelResponse response = skyblockProfilesFromUuid(linkedAccount.uuid(), key);
								player =
									!response.isValid()
										? new Player()
										: new Player(linkedAccount.uuid(), linkedAccount.username(), response.response());
							}

							if (player.isValid()) {
								try {
									Object[] out = (Object[]) RolesSlashCommand.updateRoles(player, linkedMember, rolesSettings, true);
									toAddRoles.addAll((List<Role>) out[1]);
									toRemoveRoles.addAll((List<Role>) out[2]);
								} catch (Exception ignored) {}
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
			}

			if (!filteredGuildSettings.isEmpty()) {
				Set<String> inGuild = new HashSet<>();
				for (AutomatedGuild currentSetting : filteredGuildSettings) {
					HypixelResponse response = guildResponses != null && guildResponses.containsKey(currentSetting.getGuildId())
						? guildResponses.get(currentSetting.getGuildId())
						: getGuildFromId(currentSetting.getGuildId());
					if (!response.isValid()) {
						continue;
					}

					JsonArray guildMembers = response.get("members").getAsJsonArray();
					boolean enableGuildRole = Objects.equals(currentSetting.getGuildMemberRoleEnable(), "true");
					boolean enableGuildRanks = Objects.equals(currentSetting.getGuildRanksEnable(), "true");
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
						}
					}

					if (Objects.equals(currentSetting.getGuildCounterEnable(), "true")) {
						try {
							VoiceChannel curVc = guild.getVoiceChannelById(currentSetting.getGuildCounterChannel());

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
						} catch (Exception e) {
							currentSetting.setGuildCounterEnable("false");
							database.setGuildSettings(guild.getId(), gson.toJsonTree(currentSetting));
						}
					}
				}
			}

			for (Map.Entry<Member, RoleModifyRecord> entry : memberToRoleChanges.entrySet()) {
				if (guild.getSelfMember().canInteract(entry.getKey()) && !blacklist.contains(entry.getValue().uuid())) {
					try {
						guild.modifyMemberRoles(entry.getKey(), entry.getValue().add(), entry.getValue().remove()).queue();
					} catch (Exception ignored) {}
				}
			}

			logCommand(
				guild,
				"Update Guild | Time (" +
				roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) +
				"s)" +
				(!memberToRoleChanges.isEmpty() ? " | Users (" + memberToRoleChanges.size() + ")" : "") +
				(counterUpdate > 0 ? " | Counters (" + counterUpdate + ")" : "")
			);
			logAction(
				defaultEmbed("Automatic Guild Update")
					.setDescription(
						"• Updated " +
						formatNumber(memberToRoleChanges.size()) +
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
			JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(currentSettings, "eventType", "").isEmpty()) {
				Instant endingTime = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());
				if (Instant.now().isAfter(endingTime)) {
					SkyblockEventSlashCommand.EndSubcommand.endSkyblockEvent(jda.getGuildById(guildId), false);
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

	public boolean onMayorElection(MessageEmbed embed, File mayorGraphFile, int year) {
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
					MessageEditAction action = mayorChannel.editMessageEmbedsById(lastMayorElectionOpenMessage.getId(), embed);
					(mayorGraphFile != null ? action.setFiles(FileUpload.fromData(mayorGraphFile)) : action).queue(
							m -> lastMayorElectionOpenMessage = m,
							e -> {
								if (e instanceof ErrorResponseException ex && ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
									lastMayorElectionOpenMessage = null;
								}
							}
						);
				} else {
					MessageCreateAction action = mayorChannel.sendMessageEmbeds(embed);
					(mayorGraphFile != null ? action.setFiles(FileUpload.fromData(mayorGraphFile)) : action).queue(
							m -> lastMayorElectionOpenMessage = m,
							ignore
						);
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

				MessageCreateAction action = mayorChannel.sendMessageEmbeds(embed);
				if (button != null) {
					action = action.setActionRow(button);
				}
				if (mayorPing != null) {
					action = action.setContent(mayorPing.getAsMention());
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
		verifyGuild.onGuildMessageReceived(event);
	}

	public void onTextChannelDelete(ChannelDeleteEvent event) {
		applyGuild.forEach(o1 -> o1.onTextChannelDelete(event));
	}

	public void onModalInteraction(ModalInteractionEvent event) {
		if (event.getModalId().equalsIgnoreCase("verify_modal")) {
			try {
				event.deferReply(true).complete();
			} catch (ErrorResponseException ignored) {
				return;
			}
			Object ebOrMb = LinkSlashCommand.linkAccount(event.getValues().get(0).getAsString(), event.getMember(), event.getGuild());
			if (ebOrMb instanceof EmbedBuilder eb) {
				event.getHook().editOriginalEmbeds(eb.build()).queue(ignore, ignore);
			} else if (ebOrMb instanceof MessageEditBuilder mb) {
				event.getHook().editOriginal(mb.build()).queue(ignore, ignore);
			}
		} else if (event.getModalId().startsWith("nw_")) {
			event.deferReply(true).complete();

			// 0 = uuid, 1 = profile name, 2 = last action, 3 = optional verbose json link
			String[] split = event.getModalId().split("nw_")[1].split("_");
			if (split.length < 4) {
				NetworthExecute calc = new NetworthExecute().setVerbose(true);
				calc.getPlayerNetworth(split[0], split[1], null);
				split =
					new String[] {
						split[0],
						split[1],
						split[2],
						makeHastePost(formattedGson.toJson(calc.getVerboseJson())).split(getHasteUrl())[1],
					};
			}
			split[2] = "" + Instant.now().toEpochMilli();

			List<ActionRow> newRows = new ArrayList<>();
			for (ActionRow actionRow : event.getMessage().getActionRows()) {
				List<ItemComponent> newComponents = new ArrayList<>();
				for (ItemComponent component : actionRow) {
					if (component instanceof Button button && button.getId() != null && button.getId().startsWith("nw_")) {
						component = button.withId("nw_" + String.join("_", split));
					}
					newComponents.add(component);
				}
				newRows.add(ActionRow.of(newComponents));
			}
			event.getMessage().editMessageComponents(newRows).queue();

			String[] finalSplit = split;
			getNetworthBugReportChannel()
				.sendMessageEmbeds(
					defaultEmbed("Networth Bug Report")
						.addField(
							"Information",
							"**User:** " +
							event.getUser().getAsMention() +
							"\n**Username:** [" +
							uuidToUsername(split[0]).username() +
							"](" +
							skyblockStatsLink(split[0], split[1]) +
							")\n**Profile name:** " +
							split[1],
							false
						)
						.setDescription(event.getValue("items").getAsString() + "\n\n" + event.getValue("prices").getAsString())
						.build()
				)
				.queue(m ->
					m
						.editMessageComponents(
							ActionRow.of(
								Button.link(getHasteUrl() + finalSplit[3], "Verbose Link"),
								Button.primary("nw_run_" + finalSplit[0] + "_" + finalSplit[1], "Run Networth"),
								Button.success("nw_resolved_" + event.getUser().getId() + "_" + m.getId(), "Resolved")
							)
						)
						.queue()
				);

			event.getHook().editOriginal(client.getSuccess() + " Bug report sent").queue();
		}
	}

	public void onButtonClick(ButtonInteractionEvent event) {
		if (event.getComponentId().equals("verify_button")) {
			verifyGuild.onButtonClick(event);
		} else if (event.getComponentId().equals("verify_help_button")) {
			event
				.replyFiles(FileUpload.fromData(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4")))
				.setEphemeral(true)
				.queue();
		} else if (event.getComponentId().equals("mayor_special_button")) {
			event.replyEmbeds(MayorSlashCommand.getSpecialMayors().build()).setEphemeral(true).queue();
		} else if (event.getComponentId().equals("mayor_current_election_button")) {
			Message msg = guildMap.get("796790757947867156").lastMayorElectionOpenMessage;
			event
				.reply(
					(
						msg != null
							? new MessageCreateBuilder().applyMessage(msg)
							: new MessageCreateBuilder().setEmbeds(invalidEmbed("Election is not open").build())
					).build()
				)
				.setEphemeral(true)
				.queue();
		} else if (event.getComponentId().equals("mayor_jerry_button")) {
			event.replyEmbeds(jerryEmbed).setEphemeral(true).queue();
		} else if (event.getComponentId().startsWith("nw_resolved_")) {
			if (event.getUser().getId().equals(client.getOwnerId())) {
				event
					.editComponents(
						ActionRow.of(event.getMessage().getButtons().stream().filter(b -> b.getStyle() == ButtonStyle.LINK).toList())
					)
					.queue();
				// 0 = user id, 1 = message id
				String[] split = event.getComponentId().split("nw_resolved_")[1].split("_");
				getNetworthBugReportChannel()
					.retrieveMessageById(split[1])
					.queue(
						m ->
							jda
								.retrieveUserById(split[0])
								.queue(
									u ->
										u
											.openPrivateChannel()
											.queue(
												c ->
													c
														.sendMessageEmbeds(
															defaultEmbed(null).setDescription(m.getEmbeds().get(0).getDescription()).build()
														)
														.setContent(
															client.getSuccess() +
															" Your networth bug report has been resolved by " +
															event.getUser().getAsMention()
														)
														.queue(ignore, ignore),
												ignore
											),
									ignore
								),
						ignore
					);
			}
		} else if (event.getComponentId().startsWith("nw_run_")) {
			event.deferReply(true).complete();
			// 0 = uuid, 1 = profile name
			String[] split = event.getComponentId().split("nw_run_")[1].split("_");
			EmbedBuilder eb = new NetworthExecute().setVerbose(true).getPlayerNetworth(split[0], split[1], event);
			if (eb != null) {
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		} else if (event.getComponentId().startsWith("nw_")) {
			long seconds = Duration
				.between(Instant.now(), Instant.ofEpochMilli(Long.parseLong(event.getComponentId().split("nw_")[1].split("_")[2])))
				.abs()
				.toSeconds();
			if (seconds < 30) {
				event
					.reply(client.getError() + " That is on cooldown for " + (30 - seconds) + " more second" + (seconds == 1 ? "" : "s"))
					.setEphemeral(true)
					.queue();
			} else {
				event
					.replyModal(
						Modal
							.create(event.getComponentId(), "Networth Bug Report")
							.addActionRows(
								ActionRow.of(
									TextInput
										.create("items", "Items Calculated Incorrectly", TextInputStyle.PARAGRAPH)
										.setPlaceholder("Description of the items calculated incorrectly")
										.build()
								),
								ActionRow.of(
									TextInput
										.create("prices", "Expected Calculations", TextInputStyle.PARAGRAPH)
										.setPlaceholder("What you expected the price to be and why")
										.build()
								)
							)
							.build()
					)
					.queue();
			}
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
							WebhookMessageEditAction<Message> action = event.getHook().editOriginalEmbeds(eb);
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
		} else if (event.getComponentId().startsWith("event_message_")) {
			event
				.deferReply(true)
				.queue(ignored -> {
					if (event.getComponentId().equals("event_message_join")) {
						event
							.getHook()
							.editOriginalEmbeds(
								SkyblockEventSlashCommand.JoinSubcommand
									.joinSkyblockEvent(null, null, event.getMember(), event.getGuild().getId())
									.build()
							)
							.queue();
					} else {
						EmbedBuilder eb = SkyblockEventSlashCommand.LeaderboardSubcommand.getEventLeaderboard(
							event.getGuild(),
							event.getUser(),
							null,
							event
						);
						if (eb != null) {
							event.getHook().editOriginalEmbeds(eb.build()).queue();
						}
					}
				});
		} else if (event.getComponentId().startsWith("setup_command_")) {
			if (!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())) {
				event
					.reply(client.getError() + " You are missing the required permissions or roles to use this")
					.setEphemeral(true)
					.queue();
			} else {
				new SetupCommandHandler(event, event.getComponentId().split("setup_command_")[1]);
			}
		} else if (event.getComponentId().startsWith("party_finder_channel_close_")) {
			if (event.getUser().getId().equals(event.getComponentId().split("party_finder_channel_close_")[1])) {
				event
					.reply(client.getSuccess() + " Archiving thread")
					.queue(ignored -> ((ThreadChannel) event.getChannel()).getManager().setArchived(true).queueAfter(3, TimeUnit.SECONDS));
			} else {
				event.reply(client.getError() + " Only the party leader can archive the thread").setEphemeral(true).queue();
			}
		} else if (
			!event.getComponentId().startsWith("paginator_") &&
			!event.getComponentId().startsWith("reactive_") &&
			!event.getComponentId().startsWith("inv_paginator_") &&
			!event.getComponentId().startsWith("inv_list_paginator_") &&
			!event.getComponentId().startsWith("leaderboard_paginator_")
		) {
			event
				.deferReply(!event.getComponentId().startsWith("apply_user_") || event.getComponentId().startsWith("apply_user_wait_"))
				.complete();

			for (ApplyGuild applyG : applyGuild) {
				String buttonClickReply = applyG.onButtonClick(event);
				if (buttonClickReply != null) {
					if (buttonClickReply.startsWith("SBZ_SCAMMER_CHECK_")) {
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
					} else if (!buttonClickReply.equals("IGNORE_INTERNAL")) {
						event.getHook().editOriginal(buttonClickReply).queue();
					}
					return;
				}
			}
		}
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
	public void scheduleSbEventFuture(JsonElement sbEventSettings) {
		if (!higherDepth(sbEventSettings, "eventType", "").isEmpty()) {
			if (sbEventFuture != null) {
				sbEventFuture.cancel(true);
			}

			long secondsTillEnd = Math.max(
				0,
				Duration.between(Instant.now(), Instant.ofEpochSecond(higherDepth(sbEventSettings, "timeEndingSeconds", 0L))).toSeconds()
			);
			sbEventFuture =
				scheduler.schedule(
					() -> SkyblockEventSlashCommand.EndSubcommand.endSkyblockEvent(jda.getGuildById(guildId), false),
					secondsTillEnd,
					TimeUnit.SECONDS
				);
			scheduledFutures.add(sbEventFuture);
		}
	}

	public void cancelSbEventFuture() {
		if (sbEventFuture != null) {
			scheduledFutures.remove(sbEventFuture);
			sbEventFuture.cancel(true);
		}
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

	public void setIsUsing(JsonArray arr) {
		isUsing = streamJsonArray(arr).map(JsonElement::getAsString).toList();
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
