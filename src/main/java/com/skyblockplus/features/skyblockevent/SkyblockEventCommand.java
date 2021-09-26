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

import static com.skyblockplus.Main.*;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyblockEventCommand extends Command {

	private static final Logger log = LoggerFactory.getLogger(SkyblockEventCommand.class);

	public SkyblockEventCommand() {
		this.name = "event";
		this.cooldown = globalCooldown + 3;
		this.botPermissions = defaultPerms();
	}

	public static void endSkyblockEvent(String guildId) {
		JsonElement runningEventSettings = database.getRunningEventSettings(guildId);
		TextChannel announcementChannel = jda.getTextChannelById(higherDepth(runningEventSettings, "announcementId").getAsString());

		List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningEventSettings);

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, null)
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

		if (paginateBuilder.getItemsSize() > 0) {
			paginateBuilder.build().paginate(announcementChannel, 0);
		} else {
			announcementChannel
				.sendMessageEmbeds(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
				.complete();
		}

		try {
			paginateBuilder =
				defaultPaginator(waiter, null)
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

			if (paginateBuilder.getItemsSize() > 0) {
				paginateBuilder.build().paginate(announcementChannel, 0);
				database.setSkyblockEventSettings(guildId, new EventSettings());
				return;
			}
		} catch (Exception ignored) {}
		announcementChannel.sendMessageEmbeds(defaultEmbed("Prizes").setDescription("None").build()).complete();
		database.setSkyblockEventSettings(guildId, new EventSettings());
	}

	private static List<EventMember> getEventLeaderboardList(JsonElement runningSettings) {
		List<EventMember> guildMemberPlayersList = new ArrayList<>();
		List<CompletableFuture<CompletableFuture<EventMember>>> futuresList = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
			String guildMemberProfile = higherDepth(guildMember, "profileName").getAsString();

			CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);
			futuresList.add(
				guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
					try {
						if (remainingLimit.get() < 5) {
							log.info("Sleeping for " + timeTillReset + " seconds");
							TimeUnit.SECONDS.sleep(timeTillReset.get());
						}
					} catch (Exception ignored) {}

					CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(guildMemberUuid, HYPIXEL_API_KEY);

					return guildMemberProfileJson.thenApply(guildMemberProfileJsonResponse -> {
						Player guildMemberPlayer = new Player(
							guildMemberUuid,
							guildMemberUsernameResponse,
							guildMemberProfile,
							guildMemberProfileJsonResponse
						);

						if (guildMemberPlayer.isValid()) {
							switch (eventType) {
								case "slayer":
									{
										return new EventMember(
											guildMemberUsernameResponse,
											guildMemberUuid,
											"" +
											(guildMemberPlayer.getTotalSlayer() - higherDepth(guildMember, "startingAmount").getAsDouble()),
											higherDepth(guildMember, "profileName").getAsString()
										);
									}
								case "catacombs":
									{
										return new EventMember(
											guildMemberUsernameResponse,
											guildMemberUuid,
											"" +
											(
												guildMemberPlayer.getCatacombsSkill().totalSkillExp -
												higherDepth(guildMember, "startingAmount").getAsDouble()
											),
											higherDepth(guildMember, "profileName").getAsString()
										);
									}
								case "weight":
									{
										return new EventMember(
											guildMemberUsernameResponse,
											guildMemberUuid,
											"" + (guildMemberPlayer.getWeight() - higherDepth(guildMember, "startingAmount").getAsDouble()),
											higherDepth(guildMember, "profileName").getAsString()
										);
									}
								default:
									{
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
		return guildMemberPlayersList;
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
						!event.getMember().hasPermission(Permission.ADMINISTRATOR)
					) {
						ebMessage.delete().complete();
						event.getChannel().sendMessage("❌ You must have the Administrator permission in this Guild to use that!").queue();
						return;
					}

					switch (args[1]) {
						case "create":
							paginate(createSkyblockEvent(event.getAuthor(), event.getGuild(), event.getChannel(), null));
							return;
						case "current":
							embed(getCurrentSkyblockEvent(event.getGuild().getId()));
							return;
						case "cancel":
							embed(cancelSkyblockEvent(event.getGuild().getId()));
							return;
						case "join":
							embed(joinSkyblockEvent(event.getGuild().getId(), event.getAuthor().getId(), args));
							return;
						case "leave":
							embed(leaveSkyblockEvent(event.getGuild().getId(), event.getAuthor().getId()));
							return;
						case "leaderboard":
						case "lb":
							paginate(getEventLeaderboard(event.getGuild().getId(), event.getAuthor(), event.getChannel(), null));
							return;
						case "end":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								endSkyblockEvent(event.getGuild().getId());
								embed(defaultEmbed("Success").setDescription("Event Ended"));
							} else {
								embed(defaultEmbed("No event running"));
							}
							return;
					}
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	public static EmbedBuilder getEventLeaderboard(String guildId, User user, MessageChannel channel, InteractionHook hook) {
		if (!database.getSkyblockEventActive(guildId)) {
			return defaultEmbed("No event running");
		}

		if (!guildMap.containsKey(guildId)) {
			return defaultEmbed("No guild found");
		}

		AutomaticGuild currentGuild = guildMap.get(guildId);

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(1).setItemsPerPage(25);

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

			if (paginateBuilder.getItemsSize() > 0) {
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
						.setEveryPageText("**Last updated " + minutesSinceUpdateString + " ago**\n")
				);
				if (channel != null) {
					paginateBuilder.build().paginate(channel, 0);
				} else {
					paginateBuilder.build().paginate(hook, 0);
				}
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}

		JsonElement runningSettings = database.getRunningEventSettings(guildId);
		List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningSettings);

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

		if (paginateBuilder.getItemsSize() > 0) {
			if (channel != null) {
				paginateBuilder.build().paginate(channel, 0);
			} else {
				paginateBuilder.build().paginate(hook, 0);
			}
			return null;
		}

		return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
	}

	public static EmbedBuilder leaveSkyblockEvent(String guildId, String userId) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement linkedAccount = database.getLinkedUserByDiscordId(userId);
			if (linkedAccount != null) {
				String uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();
				int code = database.removeEventMemberFromRunningEvent(guildId, uuid);

				if (code == 200) {
					return defaultEmbed("Success").setDescription("You left the event");
				} else {
					return invalidEmbed("An error occurred when leaving the event");
				}
			} else {
				return defaultEmbed("You must be linked to run this command. Use `" + getGuildPrefix(guildId) + "link [IGN]` to link");
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	public static EmbedBuilder joinSkyblockEvent(String guildId, String userId, String[] args) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement linkedAccount = database.getLinkedUserByDiscordId(userId);
			if (linkedAccount != null) {
				String uuid;
				String username;
				try {
					uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();
					username = higherDepth(linkedAccount, "minecraftUsername").getAsString();
				} catch (Exception e) {
					return defaultEmbed("You must be linked to run this command. Use `" + getGuildPrefix(guildId) + "link [IGN]` to link");
				}

				if (database.eventHasMemberByUuid(guildId, uuid)) {
					return invalidEmbed(
						"You are already in the event! If you want to leave or change profile use `" +
						getGuildPrefix(guildId) +
						"event leave`"
					);
				}

				HypixelResponse guildJson = getGuildFromPlayer(uuid);

				if (guildJson.isNotValid()) {
					return invalidEmbed(guildJson.failCause);
				}

				if (!guildJson.get("_id").getAsString().equals(database.getSkyblockEventGuildId(guildId))) {
					return invalidEmbed("You must be in the guild to join the event");
				}
				Player player = args.length == 3 ? new Player(username, args[2]) : new Player(username);

				if (player.isValid()) {
					try {
						double startingAmount = 0;
						String startingAmountFormatted = "";
						JsonElement eventSettings = database.getRunningEventSettings(guildId);
						String eventType = higherDepth(eventSettings, "eventType").getAsString();

						switch (eventType) {
							case "slayer":
								{
									startingAmount = player.getTotalSlayer();
									startingAmountFormatted = formatNumber(startingAmount) + " total slayer xp";
									break;
								}
							case "skills":
								{
									startingAmount = player.getTotalSkillsXp();
									startingAmountFormatted = formatNumber(startingAmount) + " total skills xp";
									break;
								}
							case "catacombs":
								{
									startingAmount = player.getCatacombsSkill().totalSkillExp;
									startingAmountFormatted = formatNumber(startingAmount) + " total catacombs xp";
									break;
								}
							case "weight":
								{
									startingAmount = player.getWeight();
									if (player.getTotalSkillsXp() == -1) {
										startingAmount = -1;
									}
									startingAmountFormatted = formatNumber(startingAmount) + " weight";
									break;
								}
							default:
								{
									if (eventType.startsWith("collection.")) {
										startingAmount =
											higherDepth(player.profileJson(), eventType.split("-")[0]) != null
												? higherDepth(player.profileJson(), eventType.split("-")[0]).getAsDouble()
												: 0;
										startingAmountFormatted =
											formatNumber(startingAmount) + " " + eventType.split("-")[1] + " collection";
									} else if (eventType.startsWith("skills.")) {
										String skillType = eventType.split("skills.")[1];
										startingAmount = skillType.equals("all") ? player.getTotalSkillsXp() : player.getSkillXp(skillType);
										startingAmountFormatted =
											formatNumber(startingAmount) +
											" " +
											(skillType.equals("all") ? "total skills" : skillType) +
											"  xp";
										break;
									}
									break;
								}
						}

						if (startingAmount == -1) {
							return invalidEmbed("Please enable your skills API and try again");
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

						int code = database.addEventMemberToRunningEvent(
							guildId,
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

				return invalidEmbed(player.getFailCause());
			} else {
				return invalidEmbed("You must be linked to run this command. Use `" + getGuildPrefix(guildId) + "link [IGN]` to link");
			}
		} else {
			return invalidEmbed("No event running");
		}
	}

	public static EmbedBuilder getCurrentSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement currentSettings = database.getRunningEventSettings(guildId);
			EmbedBuilder eb = defaultEmbed("Current Event");

			HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
			if (guildJson.isNotValid()) {
				return invalidEmbed(guildJson.failCause);
			}
			eb.addField("Guild", guildJson.get("name").getAsString(), false);

			eb.addField(
				"Event Type",
				capitalizeString(getEventTypeFormatted(higherDepth(currentSettings, "eventType").getAsString())),
				false
			);

			Instant eventInstantEnding = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());

			eb.addField("End Date", "Ends in <t:" + eventInstantEnding.getEpochSecond() + ":R>", false);

			ArrayList<String> prizesKeys = getJsonKeys(higherDepth(currentSettings, "prizeMap"));
			StringBuilder ebString = new StringBuilder();
			for (String prizePlace : prizesKeys) {
				ebString
					.append("• ")
					.append(prizePlace)
					.append(") - ")
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

	public static EmbedBuilder cancelSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			int code = database.setSkyblockEventSettings(guildId, new EventSettings());

			if (code == 200) {
				return defaultEmbed("Event canceled");
			} else {
				return defaultEmbed("API returned code " + code);
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	public static EmbedBuilder createSkyblockEvent(User user, Guild guild, MessageChannel channel, InteractionHook hook) {
		boolean sbEventActive = database.getSkyblockEventActive(guild.getId());
		if (guildMap.containsKey(guild.getId()) && !sbEventActive) {
			guildMap.get(guild.getId()).createSkyblockEvent(user, guild, channel, hook);
			return null;
		} else if (sbEventActive) {
			return invalidEmbed("Event already running");
		}

		return invalidEmbed("Cannot find server");
	}

	public static String getEventTypeFormatted(String eventType) {
		if (eventType.startsWith("collection.")) {
			return eventType.split("-")[1] + " collection";
		} else if (eventType.startsWith("skills.")) {
			return eventType.split("skills.")[1].equals("all") ? "skills" : eventType.split("skills.")[1];
		}

		return eventType;
	}
}
