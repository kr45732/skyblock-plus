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

package com.skyblockplus.features.listeners;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.features.mayor.MayorHandler.jerryEmbed;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.DUNGEON_CLASS_NAMES;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.event.EventGuild;
import com.skyblockplus.features.jacob.JacobGuild;
import com.skyblockplus.features.party.Party;
import com.skyblockplus.features.skyblockevent.SkyblockEventHandler;
import com.skyblockplus.features.skyblockevent.SkyblockEventSlashCommand;
import com.skyblockplus.features.verify.VerifyGuild;
import com.skyblockplus.general.LinkSlashCommand;
import com.skyblockplus.general.help.HelpData;
import com.skyblockplus.general.help.HelpSlashCommand;
import com.skyblockplus.miscellaneous.MayorSlashCommand;
import com.skyblockplus.miscellaneous.RolesSlashCommand;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.ModifyMemberRecord;
import groovy.lang.Tuple3;
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
import lombok.Getter;
import lombok.Setter;
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
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticGuild {

	@Getter
	private static final Logger log = LoggerFactory.getLogger(AutomaticGuild.class);

	private static final ScheduledFuture<?> logFuture = scheduler.scheduleWithFixedDelay(
		() -> guildMap.values().forEach(g -> g.logAction("automatic", null)),
		5,
		5,
		TimeUnit.MINUTES
	);
	private static final ScheduledFuture<?> updateGuildFuture = scheduler.scheduleAtFixedRate(
		() ->
			guildMap
				.values()
				.forEach(g -> {
					try {
						TimeUnit.SECONDS.sleep(1);
						g.updateGuild();
					} catch (Exception ignored) {}
				}),
		5,
		60,
		TimeUnit.MINUTES
	);

	/* Apply */
	public final List<ApplyGuild> applyGuilds = new ArrayList<>();
	/* Event */
	public final EventGuild eventGuild;
	/* Party */
	public final List<Party> partyList = new ArrayList<>();
	/* Jacob */
	public final JacobGuild jacobGuild;
	/* Miscellaneous */
	private final List<String> botManagerRoles = new ArrayList<>();
	private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
	private final List<MessageEmbed> logQueue = new ArrayList<>();
	public final String guildId;
	/* Verify */
	public VerifyGuild verifyGuild;

	/* Skyblock event */
	@Setter
	public SkyblockEventHandler skyblockEventHandler = null;

	@Setter
	public List<EventMember> eventMembers = new ArrayList<>();

	@Setter
	public Instant eventLastUpdated = null;

	@Setter
	public boolean eventCurrentlyUpdating = false;

	public Message lastMayorElectionOpenMessage = null;
	public Message lastMayorElectedMessage = null;

	@Setter
	private Role applyGuestRole = null;

	private ScheduledFuture<?> sbEventFuture;

	/* Fetchur */
	@Setter
	private TextChannel fetchurChannel = null;

	@Setter
	private Role fetchurPing = null;

	/* Mayor */
	@Setter
	private TextChannel mayorChannel = null;

	@Setter
	private Role mayorPing = null;

	@Setter
	private TextChannel logChannel = null;

	private final List<String> logEvents = new ArrayList<>();

	@Setter
	private JsonArray blacklist = new JsonArray();

	private List<String> isUsing = new ArrayList<>();

	/* Constructor */
	public AutomaticGuild(GenericGuildEvent event) {
		guildId = event.getGuild().getId();

		if (isMainBot() && guildId.equals("796790757947867156")) {
			try {
				((GuildMessageChannel) jda.getGuildChannelById("957658797155975208")).getHistory()
					.retrievePast(1)
					.queue(m -> {
						long seconds = Duration.between(m.get(0).getTimeCreated().toInstant(), Instant.now()).toSeconds();
						if (seconds < 900) {
							botStatusWebhook.send(client.getSuccess() + " Restarted in " + seconds + " seconds");
						}
					});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		JsonElement serverSettings = gson.toJsonTree(allServerSettings.remove(guildId));
		reloadVerifyGuild(event.getGuild(), higherDepth(serverSettings, "automatedVerify"), true);
		reloadApplyGuilds(event.getGuild(), higherDepth(serverSettings, "automatedGuilds"), true);
		scheduleSbEventFuture(higherDepth(serverSettings, "sbEvent"));
		jacobGuild = new JacobGuild(higherDepth(serverSettings, "jacobSettings"), this);
		eventGuild = new EventGuild(higherDepth(serverSettings, "eventNotif"), this);
		try {
			blacklist = higherDepth(serverSettings, "blacklist.blacklist").getAsJsonArray();
			setIsUsing(higherDepth(serverSettings, "blacklist.isUsing").getAsJsonArray());
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
				streamJsonArray(higherDepth(serverSettings, "botManagerRoles"))
					.map(JsonElement::getAsString)
					.collect(Collectors.toCollection(ArrayList::new))
			);
		} catch (Exception ignored) {}
		try {
			logChannel = event.getGuild().getTextChannelById(higherDepth(serverSettings, "logChannel", null));
		} catch (Exception ignored) {}
		try {
			logEvents.addAll(
				streamJsonArray(higherDepth(serverSettings, "logEvents"))
					.map(JsonElement::getAsString)
					.collect(Collectors.toCollection(ArrayList::new))
			);
		} catch (Exception ignored) {}
		if (cacheDatabase.partyCaches.containsKey(guildId)) {
			partyList.addAll(cacheDatabase.partyCaches.get(guildId));
		}
	}

	/* Apply Methods */
	public String reloadApplyGuilds(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return client.getError() + " Invalid guild";
		}

		JsonElement currentSettings = gson.toJsonTree(database.getAllGuildSettings(guildId));
		return reloadApplyGuilds(guild, currentSettings, false);
	}

	public String reloadApplyGuilds(Guild guild, JsonElement applySettings, boolean isStartup) {
		if (applySettings == null || applySettings.isJsonNull()) {
			return client.getError() + " No enabled automated applications";
		}

		StringBuilder applyStr = new StringBuilder();
		for (JsonElement currentSettings : applySettings.getAsJsonArray()) {
			String guildName = higherDepth(currentSettings, "guildName", null);
			if (guildName == null) {
				continue;
			}

			try {
				if (higherDepth(currentSettings, "applyEnable", false)) {
					boolean closed = higherDepth(currentSettings, "applyClosed", false);
					TextChannel reactChannel = guild.getTextChannelById(higherDepth(currentSettings, "applyMessageChannel").getAsString());

					EmbedBuilder eb = defaultEmbed("Apply For Guild");
					eb.setDescription(higherDepth(currentSettings, "applyMessage").getAsString());

					List<ApplyUser> curApplyUsers = null;
					if (!isStartup) {
						curApplyUsers = new ArrayList<>();
						for (Iterator<ApplyGuild> iterator = applyGuilds.iterator(); iterator.hasNext();) {
							ApplyGuild applyG = iterator.next();

							if (higherDepth(applyG.currentSettings, "guildName").getAsString().equals(guildName)) {
								curApplyUsers.addAll(applyG.applyUserList);
								iterator.remove();
								break;
							}
						}
					}

					try {
						Message reactMessage = reactChannel
							.editMessageEmbedsById(higherDepth(currentSettings, "applyPrevMessage").getAsString(), eb.build())
							.setActionRow(
								Button
									.primary("apply_user_create_" + guildName, closed ? "Applications Closed" : "Create Application")
									.withDisabled(closed)
							)
							.complete();

						applyGuilds.add(new ApplyGuild(reactMessage, currentSettings, curApplyUsers));
						applyStr.append(client.getSuccess()).append(" Reloaded `").append(guildName).append("`\n");
					} catch (Exception e) {
						Message reactMessage = reactChannel
							.sendMessageEmbeds(eb.build())
							.setActionRow(
								Button
									.primary("apply_user_create_" + guildName, closed ? "Applications Closed" : "Create Application")
									.withDisabled(closed)
							)
							.complete();

						currentSettings.getAsJsonObject().addProperty("applyPrevMessage", reactMessage.getId());
						database.setGuildSettings(guild.getId(), currentSettings);

						applyGuilds.add(new ApplyGuild(reactMessage, currentSettings, curApplyUsers));
						applyStr.append(client.getSuccess()).append(" Reloaded `").append(guildName).append("`\n");
					}
				} else {
					applyGuilds.removeIf(o1 -> higherDepth(o1.currentSettings, "guildName").getAsString().equals(guildName));
					applyStr.append(client.getSuccess()).append(" `").append(guildName).append("` is disabled\n");
				}
			} catch (Exception e) {
				log.error("Reload apply guild error - guildId={" + guildId + "}, name={" + guildName + "}", e);
				if (e instanceof PermissionException ex) {
					applyStr
						.append(client.getError())
						.append(" Error reloading `")
						.append(guildName)
						.append("` - missing permission: ")
						.append(ex.getPermission().getName())
						.append("\n");
				} else {
					applyStr.append(client.getError()).append(" Error reloading `").append(guildName).append("`\n");
				}
			}
		}

		return applyStr.isEmpty() ? client.getError() + " No enabled automated applications" : applyStr.toString();
	}

	/* Verify Methods */
	public String reloadVerifyGuild(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return client.getError() + " Invalid guild";
		}

		JsonElement currentSettings = database.getVerifySettings(guild.getId());
		return reloadVerifyGuild(guild, currentSettings, false);
	}

	public String reloadVerifyGuild(Guild guild, JsonElement verifySettings, boolean isStartup) {
		verifyGuild = new VerifyGuild();
		if (verifySettings == null) {
			return client.getError() + " No verify settings";
		}

		try {
			if (higherDepth(verifySettings, "enable", false)) {
				TextChannel reactChannel = guild.getTextChannelById(higherDepth(verifySettings, "messageTextChannelId").getAsString());
				try {
					reactChannel
						.editMessageById(
							higherDepth(verifySettings, "previousMessageId").getAsString(),
							higherDepth(verifySettings, "messageText").getAsString()
						)
						.setActionRow(Button.primary("verify_button", "Verify"), Button.primary("verify_help_button", "Help"))
						.complete();

					verifyGuild = new VerifyGuild(verifySettings);
					return client.getSuccess() + " Reloaded";
				} catch (Exception e) {
					Message reactMessage = reactChannel
						.sendMessage(higherDepth(verifySettings, "messageText").getAsString())
						.setActionRow(Button.primary("verify_button", "Verify"), Button.primary("verify_help_button", "Help"))
						.complete();

					verifySettings.getAsJsonObject().addProperty("previousMessageId", reactMessage.getId());
					database.setVerifySettings(guild.getId(), verifySettings);

					verifyGuild = new VerifyGuild(verifySettings);
					return client.getSuccess() + " Reloaded";
				}
			} else {
				return client.getSuccess() + " Not enabled";
			}
		} catch (Exception e) {
			log.error("Reload verify guild error - guildId={" + guildId + "}", e);
			if (e instanceof PermissionException ex) {
				return client.getError() + " Error reloading - missing permission: " + ex.getPermission().getName();
			} else {
				return client.getError() + " Error reloading";
			}
		}
	}

	/* Automated Guild Methods */
	public void updateGuild() {
		try {
			long startTime = System.currentTimeMillis();

			Guild guild = jda.getGuildById(guildId);
			if (guild == null) {
				return;
			}

			JsonElement serverSettings = database.getServerSettings(guild.getId());
			List<AutomatedGuild> guildSettings = database.getAllGuildSettings(guild.getId());

			// Should only happen if the server settings don't exist
			if (serverSettings == null || guildSettings == null) {
				return;
			}

			boolean verifyEnabled = higherDepth(serverSettings, "automatedVerify.enableAutomaticSync", false);
			boolean rolesEnabled = higherDepth(serverSettings, "automatedRoles.enableAutomaticSync", false);

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
				.collect(Collectors.toCollection(ArrayList::new));

			List<Member> inGuildUsers = new ArrayList<>();
			Map<String, LinkedAccount> discordToUuid = new HashMap<>();
			int counterUpdate = 0;
			if (roleOrRankEnabled || verifyEnabled || rolesEnabled) {
				discordToUuid.putAll(
					database
						.getAllLinkedAccounts()
						.stream()
						.filter(o -> !blacklist.contains(o.uuid()))
						.collect(Collectors.toMap(LinkedAccount::discord, Function.identity()))
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
			Map<Member, ModifyMemberRecord> memberToRoleChanges = new HashMap<>();
			if (verifyEnabled || rolesEnabled) {
				List<Role> verifyRolesAdd = new ArrayList<>();
				List<Role> verifyRolesRemove = new ArrayList<>();
				if (verifyEnabled) {
					streamJsonArray(higherDepth(serverSettings, "automatedVerify.verifiedRoles"))
						.map(e -> guild.getRoleById(e.getAsString()))
						.filter(Objects::nonNull)
						.forEach(verifyRolesAdd::add);
					try {
						verifyRolesRemove.add(
							guild.getRoleById(higherDepth(serverSettings, "automatedVerify.verifiedRemoveRole").getAsString())
						);
					} catch (Exception ignored) {}
				}

				boolean canModifyNicknames = guild.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE);
				boolean checkVerify = false;
				if (verifyEnabled) {
					String nicknameTemplate = higherDepth(serverSettings, "automatedVerify.verifiedNickname").getAsString();
					if (nicknameTemplate.contains("[IGN]") && canModifyNicknames) {
						Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
						while (matcher.find()) {
							String category = matcher.group(1).toUpperCase();
							String type = matcher.group(2).toUpperCase();

							if (
								category.equals("PLAYER") &&
								(type.equals("SKILLS") ||
									type.equals("CATACOMBS") ||
									type.equals("SLAYER") ||
									type.equals("WEIGHT") ||
									type.equals("CLASS") ||
									type.equals("LEVEL"))
							) {
								checkVerify = true;
								break;
							}
						}
					}
				}
				List<String> uuidsToRequest = new ArrayList<>();
				for (Member linkedMember : inGuildUsers) {
					LinkedAccount linkedAccount = discordToUuid.get(linkedMember.getId());
					if ((checkVerify && guild.getSelfMember().canInteract(linkedMember)) || rolesEnabled) {
						uuidsToRequest.add(linkedAccount.uuid());
					}
				}
				Map<String, DataObject> uuidToPlayer = leaderboardDatabase
					.getCachedPlayers(LinkSlashCommand.getLbTypes(rolesEnabled), Player.Gamemode.SELECTED, uuidsToRequest)
					.stream()
					.collect(Collectors.toMap(e -> e.getString("uuid"), e -> e));

				for (Member linkedMember : inGuildUsers) {
					// updatedMembers.add returns true if ele not in set
					LinkedAccount linkedAccount = discordToUuid.get(linkedMember.getId());
					List<Role> toAddRoles = new ArrayList<>();
					List<Role> toRemoveRoles = new ArrayList<>();
					String nickname = null;
					DataObject player = uuidToPlayer.getOrDefault(linkedAccount.uuid(), null);

					if (verifyEnabled) {
						toAddRoles.addAll(verifyRolesAdd);
						toRemoveRoles.addAll(verifyRolesRemove);

						String nicknameTemplate = higherDepth(serverSettings, "automatedVerify.verifiedNickname").getAsString();
						if (nicknameTemplate.contains("[IGN]") && canModifyNicknames && guild.getSelfMember().canInteract(linkedMember)) {
							nicknameTemplate = nicknameTemplate.replace("[IGN]", linkedAccount.username());

							Matcher matcher = nicknameTemplatePattern.matcher(nicknameTemplate);
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
												.collect(Collectors.toMap(AutomatedGuild::getGuildId, g -> getGuildFromId(g.getGuildId())));
									}

									HypixelResponse guildResponse = guildResponses
										.values()
										.stream()
										.filter(g ->
											streamJsonArray(g.get("members"))
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
														streamJsonArray(guildResponse.get("members"))
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
									} else {
										nicknameTemplate = nicknameTemplate.replace(matcher.group(0), "");
									}
								} else if (
									category.equals("PLAYER") &&
									(type.equals("SKILLS") ||
										type.equals("CATACOMBS") ||
										type.equals("SLAYER") ||
										type.equals("WEIGHT") ||
										type.equals("CLASS") ||
										type.equals("LEVEL"))
								) {
									if (player != null) {
										nicknameTemplate =
											nicknameTemplate.replace(
												matcher.group(0),
												switch (type) {
													case "SKILLS", "WEIGHT", "CATACOMBS", "LEVEL" -> formatNumber(
														(int) player.getDouble(type.toLowerCase())
													);
													case "SLAYER" -> simplifyNumber((long) player.getDouble("slayer"));
													case "CLASS" -> player.getDouble("selected_class") == -1
														? ""
														: "" +
														DUNGEON_CLASS_NAMES
															.get((int) player.getDouble("selected_class"))
															.toUpperCase()
															.charAt(0);
													default -> throw new IllegalStateException("Unexpected value: " + type);
												} +
												extra
											);
									}
								}
							}

							// Player wasn't requested or they were requested successfully
							if (!uuidsToRequest.contains(linkedAccount.uuid()) || uuidToPlayer.containsKey(linkedAccount.uuid())) {
								nickname = nicknameTemplate;
							}
						}
					}

					if (rolesEnabled && player != null) {
						try {
							Tuple3<EmbedBuilder, List<Role>, List<Role>> out = RolesSlashCommand.ClaimSubcommand.updateRoles(
								null,
								player,
								linkedMember,
								rolesSettings,
								true
							);
							toAddRoles.addAll(out.getV2());
							toRemoveRoles.addAll(out.getV3());
						} catch (Exception ignored) {}
					}

					if (!toAddRoles.isEmpty() || !toRemoveRoles.isEmpty() || nickname != null) {
						String finalNickname = nickname;
						memberToRoleChanges.compute(
							linkedMember,
							(k, v) ->
								(v == null ? new ModifyMemberRecord() : v).update(
										guild.getSelfMember(),
										toAddRoles,
										toRemoveRoles,
										finalNickname
									)
						);
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
					if (enableGuildRole || enableGuildRanks) {
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
								memberToRoleChanges.compute(
									linkedMember,
									(k, v) ->
										(v == null ? new ModifyMemberRecord() : v).update(guild.getSelfMember(), rolesToAdd, rolesToRemove)
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

			int updateCount = 0;
			int updateLimit = rolesEnabled ? 45 : 160;
			for (Map.Entry<Member, ModifyMemberRecord> entry : memberToRoleChanges.entrySet()) {
				if (updateCount >= updateLimit) {
					break;
				}

				if (entry.getValue().queue(entry.getKey())) {
					updateCount++;
				}
			}

			System.out.println(
				"Update Guild | " +
				guild.getId() +
				" | Time (" +
				roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) +
				"s)" +
				(!memberToRoleChanges.isEmpty()
						? " | Users (" + updateCount + "/" + updateLimit + "/" + memberToRoleChanges.size() + ")"
						: "") +
				(counterUpdate > 0 ? " | Counters (" + counterUpdate + ")" : "")
			);
			logAction(
				"guild_sync",
				defaultEmbed("Automatic Guild Update")
					.setDescription(
						(verifyEnabled ? client.getSuccess() : client.getError()) +
						" Verification sync " +
						(verifyEnabled ? "enabled" : "disabled") +
						"\n" +
						(rolesEnabled ? client.getSuccess() : client.getError()) +
						" Roles claim sync " +
						(rolesEnabled ? "enabled" : "disabled") +
						"\n" +
						(!filteredGuildSettings.isEmpty() ? client.getSuccess() : client.getError()) +
						" Guild member/ranks/counter sync " +
						(!filteredGuildSettings.isEmpty() ? "enabled" : "disabled") +
						"\n\n• Checked " +
						formatNumber(memberToRoleChanges.size()) +
						" linked members\n• " +
						(updateCount == 0 ? "All linked members updated" : "Updated " + formatNumber(updateCount) + " linked members") +
						(counterUpdate > 0 ? "\n• Updated " + counterUpdate + " counter" + (counterUpdate > 1 ? "s" : "") : "")
					)
			);
		} catch (Exception e) {
			log.error("updateGuild - " + guildId, e);
		}
	}

	/* Fetchur */
	public boolean onFetchur(MessageEmbed embed) {
		try {
			if (fetchurChannel != null) {
				if (!fetchurChannel.canTalk()) {
					logAction(
						"bot_permission_error",
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
	public boolean onMayorElection(MessageEmbed embed, File mayorGraphFile, int year) {
		try {
			if (mayorChannel != null) {
				if (!mayorChannel.canTalk()) {
					logAction(
						"bot_permission_error",
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
					if (mayorChannel.canTalk()) {
						lastMayorElectedMessage.editMessageComponents().queue(ignore, ignore);
					}
					lastMayorElectedMessage = null;
				}

				if (!mayorChannel.canTalk()) {
					logAction(
						"bot_permission_error",
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
	public boolean onStringSelectInteraction(StringSelectInteractionEvent event) {
		return applyGuilds.stream().anyMatch(e -> e.onStringSelectInteraction(event));
	}

	public void onTextChannelDelete(ChannelDeleteEvent event) {
		applyGuilds.forEach(o1 -> o1.onTextChannelDelete(event));
	}

	public void onModalInteraction(ModalInteractionEvent event) {
		if (event.getModalId().equalsIgnoreCase("verify_modal")) {
			event
				.deferReply(true)
				.queue(hook -> {
					Object ebOrMb = LinkSlashCommand.linkAccount(
						event.getValues().get(0).getAsString(),
						event.getMember(),
						event.getGuild()
					);
					if (ebOrMb instanceof EmbedBuilder eb) {
						hook.editOriginalEmbeds(eb.build()).queue(ignore, ignore);
					} else if (ebOrMb instanceof MessageEditBuilder mb) {
						hook.editOriginal(mb.build()).queue(ignore, ignore);
					}
				});
		} else if (event.getModalId().startsWith("nw_reply_")) {
			if (event.getUser().getId().equals(client.getOwnerId())) {
				event
					.editComponents(
						ActionRow.of(
							event
								.getMessage()
								.getButtons()
								.stream()
								.filter(b -> b.getStyle() == ButtonStyle.LINK)
								.collect(Collectors.toCollection(ArrayList::new))
						)
					)
					.queue();

				String replyMessage = event.getValue("value").getAsString();
				String userId = event.getModalId().split("nw_reply_")[1].split("_")[0];
				jda
					.retrieveUserById(userId)
					.queue(
						u ->
							u
								.openPrivateChannel()
								.queue(
									c ->
										c
											.sendMessageEmbeds(
												defaultEmbed(null)
													.setDescription(event.getMessage().getEmbeds().get(0).getDescription())
													.build()
											)
											.setContent(
												client.getSuccess() +
												" Your networth bug report has been resolved by " +
												event.getUser().getAsMention() +
												" (replies to this message will not be seen)\n" +
												"**Comment:** " +
												replyMessage
											)
											.queue(ignore, ignore),
									ignore
								),
						ignore
					);
			}
		} else if (event.getModalId().startsWith("nw_")) {
			event
				.deferReply(true)
				.queue(hook -> {
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
						.setComponents(
							ActionRow.of(
								Button.link(getHasteUrl() + finalSplit[3], "Verbose Link"),
								Button.primary("nw_run_" + finalSplit[0] + "_" + finalSplit[1], "Run Networth"),
								Button.success("nw_resolved_" + event.getUser().getId(), "Resolved"),
								Button.success("nw_reply_" + event.getUser().getId(), "Reply")
							)
						)
						.queue();

					hook.editOriginal(client.getSuccess() + " Bug report sent").queue();
				});
		}
	}

	public void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getComponentId().equals("verify_button")) {
			verifyGuild.onButtonClick(event);
		} else if (event.getComponentId().equals("verify_help_button")) {
			event
				.replyFiles(FileUpload.fromData(new File("src/main/java/com/skyblockplus/json/media/link_discord.mp4")))
				.setEphemeral(true)
				.queue();
		} else if (event.getComponentId().startsWith("enable_api_help_button")) {
			event
				.replyFiles(FileUpload.fromData(new File("src/main/java/com/skyblockplus/json/media/enable_api.mp4")))
				.setEphemeral(true)
				.queue();
		} else if (event.getComponentId().startsWith("s_help_")) {
			// settings help button
			String[] cmdSplit = event.getComponentId().split("s_help_")[1].split("\\s+", 2);
			HelpData matchCmd = HelpSlashCommand.helpDataList.stream().filter(cmd -> cmd.matchTo(cmdSplit[0])).findFirst().orElse(null);
			event.replyEmbeds(matchCmd.getHelp(cmdSplit.length == 2 ? cmdSplit[1] : null).build()).setEphemeral(true).queue();
		} else if (event.getComponentId().equals("mayor_special_button")) {
			event.replyEmbeds(MayorSlashCommand.getSpecialMayors().build()).setEphemeral(true).queue();
		} else if (event.getComponentId().equals("mayor_current_election_button")) {
			Message msg = guildMap.get("796790757947867156").lastMayorElectionOpenMessage;
			event
				.reply(
					(msg != null
							? new MessageCreateBuilder().applyMessage(msg)
							: new MessageCreateBuilder().setEmbeds(errorEmbed("Election is not open").build())).build()
				)
				.setEphemeral(true)
				.queue();
		} else if (event.getComponentId().equals("mayor_jerry_button")) {
			event.replyEmbeds(jerryEmbed).setEphemeral(true).queue();
		} else if (event.getComponentId().startsWith("nw_reply_")) {
			if (event.getUser().getId().equals(client.getOwnerId())) {
				event
					.replyModal(
						Modal
							.create(event.getComponentId(), "Networth Bug Report Reply")
							.addActionRow(TextInput.create("value", "Repy", TextInputStyle.PARAGRAPH).build())
							.build()
					)
					.queue();
			}
		} else if (event.getComponentId().startsWith("nw_resolved_")) {
			if (event.getUser().getId().equals(client.getOwnerId())) {
				event
					.editComponents(
						ActionRow.of(
							event
								.getMessage()
								.getButtons()
								.stream()
								.filter(b -> b.getStyle() == ButtonStyle.LINK)
								.collect(Collectors.toCollection(ArrayList::new))
						)
					)
					.queue();

				String userId = event.getComponentId().split("nw_resolved_")[1].split("_")[0];
				jda
					.retrieveUserById(userId)
					.queue(
						u ->
							u
								.openPrivateChannel()
								.queue(
									c ->
										c
											.sendMessageEmbeds(
												defaultEmbed(null)
													.setDescription(event.getMessage().getEmbeds().get(0).getDescription())
													.build()
											)
											.setContent(
												client.getSuccess() +
												" Your networth bug report has been resolved by " +
												event.getUser().getAsMention() +
												" (replies to this message will not be seen)"
											)
											.queue(ignore, ignore),
									ignore
								),
						ignore
					);
			}
		} else if (event.getComponentId().startsWith("nw_run_")) {
			event
				.deferReply(true)
				.queue(hook -> {
					// 0 = uuid, 1 = profile name
					String[] split = event.getComponentId().split("nw_run_")[1].split("_");
					MessageEditBuilder mb = new NetworthExecute().setVerbose(true).getPlayerNetworth(split[0], split[1], event);
					if (mb != null) {
						hook.editOriginal(mb.build()).queue();
					}
				});
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
							.addComponents(
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
							case "C" -> getEmojiWithName("EMERALD_BLOCK", "e");
							case "c" -> getEmojiWithName("IRON_BLOCK", "e");
							case "S" -> getEmojiWithName("INK_SACK:10", "e");
							default -> getEmojiWithName("PAPER", "e");
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
								(event.getMessage().getActionRows().size() == 1
										? event.getMessage().editMessageComponents(updatedButton)
										: event
											.getMessage()
											.editMessageComponents(event.getMessage().getActionRows().get(0), updatedButton)).queue();
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
								(event.getMessage().getActionRows().size() == 1
										? event.getMessage().editMessageComponents(updatedButton)
										: event
											.getMessage()
											.editMessageComponents(event.getMessage().getActionRows().get(0), updatedButton)).queue();
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
		} else if (event.getComponentId().startsWith("party_finder_channel_close_")) {
			if (event.getUser().getId().equals(event.getComponentId().split("party_finder_channel_close_")[1])) {
				event
					.reply(client.getSuccess() + " Archiving thread")
					.queue(ignored -> ((ThreadChannel) event.getChannel()).getManager().setArchived(true).queueAfter(3, TimeUnit.SECONDS));
			} else {
				event.reply(client.getError() + " Only the party leader can archive the thread").setEphemeral(true).queue();
			}
		} else if (event.getComponentId().startsWith("apply_user_")) {
			InteractionCallbackAction<?> action;
			if (
				event.getComponentId().equals("apply_user_accept") ||
				event.getComponentId().equals("apply_user_waitlist") ||
				event.getComponentId().equals("apply_user_deny")
			) {
				// Edit the staff message which had the accept/waitlist/deny buttons
				action = event.deferEdit();
			} else {
				action =
					event.deferReply(
						event.getComponentId().startsWith("apply_user_create_") || event.getComponentId().startsWith("apply_user_wait_")
					);
			}

			action.queue(ignored -> {
				for (ApplyGuild applyGuild : applyGuilds) {
					if (applyGuild.onButtonClick(event)) {
						return;
					}
				}
			});
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
				60,
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

	public void cancelEvent() {
		if (sbEventFuture != null) {
			scheduledFutures.remove(sbEventFuture);
			sbEventFuture.cancel(true);
		}

		eventMembers = null;
		eventLastUpdated = null;
		eventCurrentlyUpdating = false;
	}

	public void setBotManagerRoles(List<String> botManagerRoles) {
		this.botManagerRoles.clear();
		this.botManagerRoles.addAll(botManagerRoles);
	}

	public boolean isAdmin(Member member) {
		if (!member.hasPermission(Permission.ADMINISTRATOR)) {
			List<String> playerRoles = member.getRoles().stream().map(ISnowflake::getId).collect(Collectors.toCollection(ArrayList::new));
			return botManagerRoles.stream().anyMatch(playerRoles::contains);
		}
		return true;
	}

	public void setLogEvents(List<String> logEvents) {
		this.logEvents.clear();
		this.logEvents.addAll(logEvents);
	}

	public void logAction(String eventType, EmbedBuilder eb) {
		logAction(eventType, eb, jda.getGuildById(guildId).getSelfMember());
	}

	public void logAction(String eventType, EmbedBuilder eb, Member member) {
		try {
			if (logChannel == null || !logChannel.canTalk()) {
				return;
			}

			if (!eventType.equals("automatic") && !logEvents.contains(eventType)) {
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
		isUsing = streamJsonArray(arr).map(JsonElement::getAsString).collect(Collectors.toCollection(ArrayList::new));
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
