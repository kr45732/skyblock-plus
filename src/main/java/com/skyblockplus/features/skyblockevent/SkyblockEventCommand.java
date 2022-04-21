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

package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SkyblockEventCommand extends Command {

	private static final Logger log = LoggerFactory.getLogger(SkyblockEventCommand.class);

	public SkyblockEventCommand() {
		this.name = "event";
		this.cooldown = globalCooldown + 3;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder endSkyblockEvent(String guildId) {
		JsonElement runningEventSettings = database.getSkyblockEventSettings(guildId);
		TextChannel announcementChannel = jda.getTextChannelById(higherDepth(runningEventSettings, "announcementId").getAsString());
		guildMap.get(guildId).setEventMemberListLastUpdated(null);
		List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningEventSettings, guildId);
		if (guildMemberPlayersList == null) {
			return invalidEmbed("A Hypixel API key must be set for events over 45 members so the leaderboard can be calculated");
		}
		guildMap.get(guildId).setEventMemberListLastUpdated(null);

		try {
			announcementChannel
				.retrieveMessageById(higherDepth(runningEventSettings, "announcementMessageId").getAsString())
				.queue(m ->
					m.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event has ended").build()).setActionRows().queue()
				);
		} catch (Exception ignored) {}

		CustomPaginator.Builder paginateBuilder = defaultPaginator()
			.setColumns(1)
			.setItemsPerPage(25)
			.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Event Leaderboard"))
			.setTimeout(24, TimeUnit.HOURS);

		for (int i = 0; i < guildMemberPlayersList.size(); i++) {
			EventMember eventMember = guildMemberPlayersList.get(i);
			paginateBuilder.addItems(
				"`" +
				(i + 1) +
				")` " +
				fixUsername(eventMember.getUsername()) +
				" | +" +
				formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
			);
		}

		if (paginateBuilder.size() > 0) {
			paginateBuilder.build().paginate(announcementChannel, 0);
		} else {
			announcementChannel
				.sendMessageEmbeds(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
				.complete();
		}

		try {
			paginateBuilder =
				defaultPaginator()
					.setColumns(1)
					.setItemsPerPage(25)
					.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Prizes"))
					.setTimeout(24, TimeUnit.HOURS);

			ArrayList<String> prizeListKeys = getJsonKeys(higherDepth(runningEventSettings, "prizeMap"));
			for (int i = 0; i < prizeListKeys.size(); i++) {
				try {
					paginateBuilder.addItems(
						"`" +
						(i + 1) +
						")` " +
						higherDepth(runningEventSettings, "prizeMap." + prizeListKeys.get(i)).getAsString() +
						" - " +
						fixUsername(guildMemberPlayersList.get(i).getUsername())
					);
				} catch (Exception ignored) {}
			}

			if (paginateBuilder.size() > 0) {
				paginateBuilder.build().paginate(announcementChannel, 0);
				database.setSkyblockEventSettings(guildId, new EventSettings());
				return defaultEmbed("Success").setDescription("Ended skyblock event");
			}
		} catch (Exception ignored) {}
		announcementChannel.sendMessageEmbeds(defaultEmbed("Prizes").setDescription("None").build()).complete();
		database.setSkyblockEventSettings(guildId, new EventSettings());
		return defaultEmbed("Success").setDescription("Ended skyblock event");
	}

	public static List<EventMember> getEventLeaderboardList(JsonElement runningSettings, String guildId) {
		List<EventMember> guildMemberPlayersList = new ArrayList<>();
		List<CompletableFuture<CompletableFuture<EventMember>>> futuresList = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		String key = null;
		if (membersArr.size() > 45) {
			key = database.getServerHypixelApiKey(guildId);
			if (key == null) {
				return null;
			}
		}
		String hypixelKey = key;

		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
			String guildMemberProfile = higherDepth(guildMember, "profileName").getAsString();

			CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);

			futuresList.add(
				guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
					try {
						if ((hypixelKey != null ? keyCooldownMap.get(hypixelKey).remainingLimit().get() : remainingLimit.get()) < 5) {
							log.info(
								"Sleeping for " +
								(hypixelKey != null ? keyCooldownMap.get(hypixelKey).timeTillReset() : timeTillReset) +
								" seconds"
							);
							TimeUnit.SECONDS.sleep(
								hypixelKey != null ? keyCooldownMap.get(hypixelKey).timeTillReset().get() : timeTillReset.get()
							);
						}
					} catch (Exception ignored) {}

					CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(
						guildMemberUuid,
						hypixelKey != null ? hypixelKey : HYPIXEL_API_KEY
					);

					return guildMemberProfileJson.thenApply(guildMemberProfileJsonResponse -> {
						Player guildMemberPlayer = new Player(
							guildMemberUuid,
							guildMemberUsernameResponse,
							guildMemberProfile,
							guildMemberProfileJsonResponse
						);

						if (guildMemberPlayer.isValid()) {
							switch (eventType) {
								case "slayer" -> {
									return new EventMember(
										guildMemberUsernameResponse,
										guildMemberUuid,
										"" +
										(guildMemberPlayer.getTotalSlayer() - higherDepth(guildMember, "startingAmount").getAsDouble()),
										higherDepth(guildMember, "profileName").getAsString()
									);
								}
								case "catacombs" -> {
									return new EventMember(
										guildMemberUsernameResponse,
										guildMemberUuid,
										"" +
										(
											guildMemberPlayer.getCatacombs().totalExp() -
											higherDepth(guildMember, "startingAmount").getAsDouble()
										),
										higherDepth(guildMember, "profileName").getAsString()
									);
								}
								case "weight" -> {
									return new EventMember(
										guildMemberUsernameResponse,
										guildMemberUuid,
										"" + (guildMemberPlayer.getWeight() - higherDepth(guildMember, "startingAmount").getAsDouble()),
										higherDepth(guildMember, "profileName").getAsString()
									);
								}
								default -> {
									if (eventType.startsWith("collection.")) {
										return new EventMember(
											guildMemberUsernameResponse,
											guildMemberUuid,
											"" +
											(
												(
													higherDepth(guildMemberPlayer.profileJson(), eventType.split("-")[0]) != null
														? higherDepth(guildMemberPlayer.profileJson(), eventType.split("-")[0])
															.getAsDouble()
														: 0
												) -
												higherDepth(guildMember, "startingAmount").getAsDouble()
											),
											higherDepth(guildMember, "profileName").getAsString()
										);
									} else if (eventType.startsWith("skills.")) {
										String skillType = eventType.split("skills.")[1];
										double skillXp = skillType.equals("all")
											? guildMemberPlayer.getTotalSkillsXp()
											: guildMemberPlayer.getSkillXp(skillType);

										if (skillXp != -1) {
											return new EventMember(
												guildMemberUsernameResponse,
												guildMemberUuid,
												"" + (skillXp - higherDepth(guildMember, "startingAmount").getAsDouble()),
												higherDepth(guildMember, "profileName").getAsString()
											);
										}
									} else if (eventType.startsWith("weight.")) {
										String weightTypes = eventType.split("weight.")[1];
										double skillXp = guildMemberPlayer.getWeight(weightTypes.split("-"));

										if (skillXp != -1) {
											return new EventMember(
												guildMemberUsernameResponse,
												guildMemberUuid,
												"" + (skillXp - higherDepth(guildMember, "startingAmount").getAsDouble()),
												higherDepth(guildMember, "profileName").getAsString()
											);
										}
									}
								}
							}
						}

						return null;
					});
				})
			);
		}

		for (CompletableFuture<CompletableFuture<EventMember>> future : futuresList) {
			try {
				EventMember playerFutureResponse = future.get().get();
				if (playerFutureResponse != null) {
					guildMemberPlayersList.add(playerFutureResponse);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.getStartingAmount())));

		if (guildId.equals("602137436490956820")) {
			executor.submit(() -> postJson("https://soopymc.my.to/api/soopyv2/lbdatathing", gson.toJsonTree(guildMemberPlayersList)));
		}
		return guildMemberPlayersList;
	}

	public static EmbedBuilder getEventLeaderboard(ButtonInteractionEvent event) {
		String guildId = event.getGuild().getId();
		if (!database.getSkyblockEventActive(guildId)) {
			return defaultEmbed("No event running");
		}

		if (!guildMap.containsKey(guildId)) {
			return defaultEmbed("No guild found");
		}

		AutomaticGuild currentGuild = guildMap.get(guildId);

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(25);

		if (
			(currentGuild.eventMemberList != null) &&
			(currentGuild.eventMemberListLastUpdated != null) &&
			(Duration.between(currentGuild.eventMemberListLastUpdated, Instant.now()).toMinutes() < 15)
		) {
			List<EventMember> eventMemberList = currentGuild.eventMemberList;
			for (int i = 0; i < eventMemberList.size(); i++) {
				EventMember eventMember = eventMemberList.get(i);
				paginateBuilder.addItems(
					"`" +
					(i + 1) +
					")` " +
					fixUsername(eventMember.getUsername()) +
					" | +" +
					formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
				);
			}

			if (paginateBuilder.size() > 0) {
				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle("Event Leaderboard")
						.setEveryPageText("**Last Updated:** <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>\n")
				);
				paginateBuilder.build().paginate(event.getHook(), 0);
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}

		JsonElement runningSettings = database.getSkyblockEventSettings(guildId);
		List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningSettings, guildId);
		if (guildMemberPlayersList == null) {
			return invalidEmbed("A Hypixel API key must be set for events with over 45 members");
		}

		for (int i = 0; i < guildMemberPlayersList.size(); i++) {
			EventMember eventMember = guildMemberPlayersList.get(i);
			paginateBuilder.addItems(
				"`" +
				(i + 1) +
				")` " +
				fixUsername(eventMember.getUsername()) +
				" | +" +
				formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
			);
		}

		paginateBuilder.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Event Leaderboard"));

		guildMap.get(guildId).setEventMemberList(guildMemberPlayersList);
		guildMap.get(guildId).setEventMemberListLastUpdated(Instant.now());

		if (paginateBuilder.size() > 0) {
			paginateBuilder.build().paginate(event.getHook(), 0);
			return null;
		}

		return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
	}

	public static EmbedBuilder getEventLeaderboard(PaginatorEvent event) {
		String guildId = event.getGuild().getId();
		if (!database.getSkyblockEventActive(guildId)) {
			return defaultEmbed("No event running");
		}

		AutomaticGuild currentGuild = guildMap.get(guildId);

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(25);

		if (
			(currentGuild.eventMemberList != null) &&
			(currentGuild.eventMemberListLastUpdated != null) &&
			(Duration.between(currentGuild.eventMemberListLastUpdated, Instant.now()).toMinutes() < 15)
		) {
			List<EventMember> eventMemberList = currentGuild.eventMemberList;
			for (int i = 0; i < eventMemberList.size(); i++) {
				EventMember eventMember = eventMemberList.get(i);
				paginateBuilder.addItems(
					"`" +
					(i + 1) +
					")` " +
					fixUsername(eventMember.getUsername()) +
					" | +" +
					formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
				);
			}

			if (paginateBuilder.size() > 0) {
				long minutesSinceUpdate = Duration.between(currentGuild.eventMemberListLastUpdated, Instant.now()).toMinutes();

				String minutesSinceUpdateString;
				if (minutesSinceUpdate == 0) {
					minutesSinceUpdateString = " less than a minute ";
				} else if (minutesSinceUpdate == 1) {
					minutesSinceUpdateString = " 1 minute ";
				} else {
					minutesSinceUpdateString = minutesSinceUpdate + " minutes ";
				}

				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle("Event Leaderboard")
						.setEveryPageText("**Last Updated " + minutesSinceUpdateString + " ago**\n")
				);
				event.paginate(paginateBuilder);
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}

		JsonElement runningSettings = database.getSkyblockEventSettings(guildId);
		List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningSettings, guildId);
		if (guildMemberPlayersList == null) {
			return invalidEmbed("A Hypixel API key must be set for events with over 45 members");
		}

		for (int i = 0; i < guildMemberPlayersList.size(); i++) {
			EventMember eventMember = guildMemberPlayersList.get(i);
			paginateBuilder.addItems(
				"`" +
				(i + 1) +
				")` " +
				fixUsername(eventMember.getUsername()) +
				" | +" +
				formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
			);
		}

		paginateBuilder.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Event Leaderboard"));

		guildMap.get(guildId).setEventMemberList(guildMemberPlayersList);
		guildMap.get(guildId).setEventMemberListLastUpdated(Instant.now());

		if (paginateBuilder.size() > 0) {
			event.paginate(paginateBuilder);
			return null;
		}

		return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
	}

	public static EmbedBuilder leaveSkyblockEvent(String guildId, String userId) {
		if (database.getSkyblockEventActive(guildId)) {
			LinkedAccount linkedAccount = database.getByDiscord(userId);
			if (linkedAccount != null) {
				int code = database.removeMemberFromSkyblockEvent(guildId, linkedAccount.uuid());

				if (code == 200) {
					return defaultEmbed("Success").setDescription("You left the event");
				} else {
					return invalidEmbed("An error occurred when leaving the event");
				}
			} else {
				return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	public static EmbedBuilder joinSkyblockEvent(String profile, Member member) {
		if (database.getSkyblockEventActive(member.getGuild().getId())) {
			LinkedAccount linkedAccount = database.getByDiscord(member.getId());
			if (linkedAccount != null) {
				String uuid = linkedAccount.uuid();
				String username = linkedAccount.username();

				if (database.eventHasMemberByUuid(member.getGuild().getId(), uuid)) {
					return invalidEmbed("You are already in the event! If you want to leave or change profile use `/event leave`");
				}

				JsonElement eventSettings = database.getSkyblockEventSettings(member.getGuild().getId());
				if (!higherDepth(eventSettings, "eventGuildId", "").isEmpty()) {
					HypixelResponse guildJson = getGuildFromPlayer(uuid);
					if (guildJson.isNotValid()) {
						return invalidEmbed(guildJson.failCause());
					}

					if (!guildJson.get("_id").getAsString().equals(higherDepth(eventSettings, "eventGuildId").getAsString())) {
						return invalidEmbed("You must be in the guild to join the event");
					}
				}
				String requiredRole = higherDepth(eventSettings, "whitelistRole", "");
				if (!requiredRole.isEmpty() && member.getRoles().stream().noneMatch(r -> r.getId().equals(requiredRole))) {
					return invalidEmbed("You must have the <@&" + requiredRole + "> role to join this event");
				}

				Player player = profile != null ? new Player(username, profile) : new Player(username);

				if (player.isValid()) {
					try {
						double startingAmount = 0;
						String startingAmountFormatted = "";

						String eventType = higherDepth(eventSettings, "eventType").getAsString();

						if ((eventType.startsWith("skills") || eventType.startsWith("weight")) && !player.isSkillsApiEnabled()) {
							return invalidEmbed("Please enable your skills API before joining");
						}

						switch (eventType) {
							case "slayer" -> {
								startingAmount = player.getTotalSlayer();
								startingAmountFormatted = formatNumber(startingAmount) + " total slayer xp";
							}
							case "skills" -> {
								startingAmount = player.getTotalSkillsXp();
								startingAmountFormatted = formatNumber(startingAmount) + " total skills xp";
							}
							case "catacombs" -> {
								startingAmount = player.getCatacombs().totalExp();
								startingAmountFormatted = formatNumber(startingAmount) + " total catacombs xp";
							}
							case "weight" -> {
								startingAmount = player.getWeight();
								startingAmountFormatted = formatNumber(startingAmount) + " weight";
							}
							default -> {
								if (eventType.startsWith("collection.")) {
									startingAmount =
										higherDepth(player.profileJson(), eventType.split("-")[0]) != null
											? higherDepth(player.profileJson(), eventType.split("-")[0]).getAsDouble()
											: 0;
									startingAmountFormatted = formatNumber(startingAmount) + " " + eventType.split("-")[1] + " collection";
								} else if (eventType.startsWith("skills.")) {
									String skillType = eventType.split("skills.")[1];
									startingAmount = skillType.equals("all") ? player.getTotalSkillsXp() : player.getSkillXp(skillType);
									startingAmountFormatted =
										formatNumber(startingAmount) +
										" " +
										(skillType.equals("all") ? "total skills" : skillType) +
										"  xp";
								} else if (eventType.startsWith("weight.")) {
									String weightTypes = eventType.split("weight.")[1];
									startingAmount = player.getWeight(weightTypes.split("-"));
									startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
								}
							}
						}

						try {
							int minAmt = Integer.parseInt(higherDepth(eventSettings, "minAmount").getAsString());
							if (minAmt != -1 && startingAmount < minAmt) {
								return invalidEmbed(
									"You must have at least " + formatNumber(minAmt) + " " + getEventTypeFormatted(eventType)
								);
							}
						} catch (Exception ignored) {}

						try {
							int maxAmt = Integer.parseInt(higherDepth(eventSettings, "maxAmount").getAsString());
							if (maxAmt != -1 && startingAmount > maxAmt) {
								return invalidEmbed(
									"You must have no more than " + formatNumber(maxAmt) + " " + getEventTypeFormatted(eventType)
								);
							}
						} catch (Exception ignored) {}

						int code = database.addMemberToSkyblockEvent(
							member.getGuild().getId(),
							new EventMember(username, uuid, "" + startingAmount, player.getProfileName())
						);

						if (code == 200) {
							return defaultEmbed("Joined event")
								.setDescription(
									"**Username:** " +
									username +
									"\n**Profile:** " +
									player.getProfileName() +
									"\n**Starting amount:** " +
									startingAmountFormatted
								);
						} else {
							return invalidEmbed("API returned code " + code);
						}
					} catch (Exception ignored) {}
				}

				return player.getFailEmbed();
			} else {
				return invalidEmbed("You must be linked to run this command. Use `/link <player>` to link");
			}
		} else {
			return invalidEmbed("No event running");
		}
	}

	public static EmbedBuilder getCurrentSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
			EmbedBuilder eb = defaultEmbed("Current Event");

			if (!higherDepth(currentSettings, "eventGuildId", "").isEmpty()) {
				HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
				if (guildJson.isNotValid()) {
					return invalidEmbed(guildJson.failCause());
				}
				eb.addField("Guild", guildJson.get("name").getAsString(), false);
			}

			eb.addField(
				"Event Type",
				capitalizeString(getEventTypeFormatted(higherDepth(currentSettings, "eventType").getAsString())),
				false
			);

			Instant eventInstantEnding = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());

			eb.addField("End Date", "Ends <t:" + eventInstantEnding.getEpochSecond() + ":R>", false);

			ArrayList<String> prizesKeys = getJsonKeys(higherDepth(currentSettings, "prizeMap"));
			StringBuilder ebString = new StringBuilder();
			for (String prizePlace : prizesKeys) {
				ebString
					.append("`")
					.append(prizePlace)
					.append(")` ")
					.append(higherDepth(currentSettings, "prizeMap." + prizePlace).getAsString())
					.append("\n");
			}

			if (ebString.length() == 0) {
				ebString = new StringBuilder("None");
			}

			eb.addField("Prizes", ebString.toString(), false);
			eb.addField("Members joined", "" + higherDepth(currentSettings, "membersList").getAsJsonArray().size(), false);

			return eb;
		} else {
			return defaultEmbed("No event running");
		}
	}

	public static EmbedBuilder cancelSkyblockEvent(Guild guild) {
		JsonElement settings = database.getSkyblockEventSettings(guild.getId());
		if (higherDepth(settings, "eventType", "").length() > 0) {
			guild
				.getTextChannelById(higherDepth(settings, "announcementId").getAsString())
				.retrieveMessageById(higherDepth(settings, "announcementMessageId").getAsString())
				.queue(m ->
					m.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event has ended").build()).setActionRows().queue()
				);
			guildMap.get(guild.getId()).setEventMemberListLastUpdated(null);
			int code = database.setSkyblockEventSettings(guild.getId(), new EventSettings());

			if (code == 200) {
				return defaultEmbed("Event canceled");
			} else {
				return defaultEmbed("API returned code " + code);
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	public static EmbedBuilder createSkyblockEvent(PaginatorEvent event) {
		boolean sbEventActive = database.getSkyblockEventActive(event.getGuild().getId());
		if (sbEventActive) {
			return invalidEmbed("Event already running");
		} else if (guildMap.containsKey(event.getGuild().getId())) {
			if (guildMap.get(event.getGuild().getId()).skyblockEventHandler == null) {
				guildMap.get(event.getGuild().getId()).setSkyblockEventHandler(new SkyblockEventHandler(event));
				return null;
			} else {
				return invalidEmbed("Someone is already creating an event in this server");
			}
		} else {
			return invalidEmbed("Cannot find server");
		}
	}

	public static String getEventTypeFormatted(String eventType) {
		if (eventType.startsWith("collection.")) {
			return eventType.split("-")[1] + " collection";
		} else if (eventType.startsWith("skills.")) {
			return eventType.split("skills.")[1].equals("all") ? "skills" : eventType.split("skills.")[1];
		} else if (eventType.startsWith("weight.")) {
			String[] types = eventType.split("weight.")[1].split("-");
			return types.length == 18 ? "weight" : String.join(", ", types) + " weight" + (types.length > 1 ? "s" : "");
		}

		return eventType;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 || args.length == 3) {
					if (
						(args[1].equals("create") || args[1].equals("cancel") || args[1].equals("end")) &&
						!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())
					) {
						ebMessage.delete().complete();
						event
							.getChannel()
							.sendMessage(client.getError() + " You must have the administrator permission in this guild to use that!")
							.queue();
						return;
					}

					switch (args[1]) {
						case "create" -> {
							paginate(createSkyblockEvent(new PaginatorEvent(event)));
							return;
						}
						case "current" -> {
							embed(getCurrentSkyblockEvent(event.getGuild().getId()));
							return;
						}
						case "cancel" -> {
							embed(cancelSkyblockEvent(event.getGuild()));
							return;
						}
						case "join" -> {
							embed(joinSkyblockEvent(args.length == 3 ? args[2] : null, event.getMember()));
							return;
						}
						case "leave" -> {
							embed(leaveSkyblockEvent(event.getGuild().getId(), event.getAuthor().getId()));
							return;
						}
						case "leaderboard", "lb" -> {
							paginate(getEventLeaderboard(new PaginatorEvent(event)));
							return;
						}
						case "end" -> {
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								embed(endSkyblockEvent(event.getGuild().getId()));
							} else {
								embed(defaultEmbed("No event running"));
							}
							return;
						}
					}
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
