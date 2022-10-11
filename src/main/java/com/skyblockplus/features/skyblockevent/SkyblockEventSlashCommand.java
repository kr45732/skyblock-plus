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
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class SkyblockEventSlashCommand extends SlashCommand {

	public SkyblockEventSlashCommand() {
		this.name = "event";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		String subcommandName = event.getSubcommandName();
		if (
			(
				subcommandName.equals("create") ||
				subcommandName.equals("cancel") ||
				subcommandName.equals("end") ||
				subcommandName.equals("add")
			) &&
			!guildMap.get(event.getGuild().getId()).isAdmin(event.getMember())
		) {
			event.string(client.getError() + " You are missing the required permissions or roles to use this command");
			return;
		}

		switch (subcommandName) {
			case "create":
				event.paginate(createSkyblockEvent(event));
				break;
			case "current":
				event.embed(getCurrentSkyblockEvent(event.getGuild().getId()));
				break;
			case "cancel":
				event.embed(cancelSkyblockEvent(event.getGuild()));
				break;
			case "join":
				event.embed(joinSkyblockEvent(null, event.getOptionStr("profile"), event.getMember(), event.getGuild().getId()));
				break;
			case "add":
				event.embed(joinSkyblockEvent(event.getOptionStr("player"), event.getOptionStr("profile"), null, event.getGuild().getId()));
				break;
			case "leave":
				event.embed(leaveSkyblockEvent(event.getGuild().getId(), event.getUser().getId()));
				break;
			case "leaderboard":
			case "lb":
				event.paginate(getEventLeaderboard(event.getGuild(), event.getUser(), event, null));
				break;
			case "end":
				if (database.getSkyblockEventActive(event.getGuild().getId())) {
					event.embed(endSkyblockEvent(event.getGuild().getId()));
				} else {
					event.embed(defaultEmbed("No event running"));
				}
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Main event command")
			.addSubcommands(
				new SubcommandData("create", "Interactive message to create a Skyblock event"),
				new SubcommandData("end", "Force end the event"),
				new SubcommandData("current", "Get information about the current event"),
				new SubcommandData("join", "Join the current event").addOption(OptionType.STRING, "profile", "Profile name"),
				new SubcommandData("add", "Force add a player to the current event")
					.addOption(OptionType.STRING, "player", "Player username or mention", true, true)
					.addOption(OptionType.STRING, "profile", "Profile name"),
				new SubcommandData("leave", "Leave the current event"),
				new SubcommandData("cancel", "Cancel the event"),
				new SubcommandData("leaderboard", "Get the leaderboard for current event")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
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
					m.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event has ended").build()).setComponents().queue()
				);
		} catch (Exception ignored) {}

		CustomPaginator.Builder paginateBuilder = defaultPaginator()
			.setColumns(1)
			.setItemsPerPage(25)
			.updateExtras(extra -> extra.setEveryPageTitle("Event Leaderboard"))
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
					.updateExtras(extra -> extra.setEveryPageTitle("Prizes"))
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
		List<CompletableFuture<EventMember>> futuresList = new ArrayList<>();
		List<Player> players = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		String key = null;
		if (membersArr.size() > 40) {
			key = database.getServerHypixelApiKey(guildId);
			if (key == null || checkHypixelKey(key) != null) {
				return null;
			}
		}
		String hypixelKey = key;

		guildMap.get(guildId).setEventCurrentlyUpdating(true);
		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
			String guildMemberProfile = higherDepth(guildMember, "profileName").getAsString();

			try {
				if (hypixelKey != null ? keyCooldownMap.get(hypixelKey).isRateLimited() : remainingLimit.get() < 5) {
					System.out.println(
						"Sleeping for " +
						(hypixelKey != null ? keyCooldownMap.get(hypixelKey).getTimeTillReset() : timeTillReset) +
						" seconds"
					);
					TimeUnit.SECONDS.sleep(hypixelKey != null ? keyCooldownMap.get(hypixelKey).getTimeTillReset() : timeTillReset.get());
				}
			} catch (Exception ignored) {}

			futuresList.add(
				asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey != null ? hypixelKey : HYPIXEL_API_KEY)
					.thenApplyAsync(
						guildMemberProfileJsonResponse -> {
							Player guildMemberPlayer = new Player(
								guildMemberUuid,
								usernameToUuid(guildMemberUuid).username(),
								guildMemberProfile,
								guildMemberProfileJsonResponse,
								false
							);

							if (guildMemberPlayer.isValid()) {
								players.add(guildMemberPlayer);

								switch (eventType) {
									case "slayer" -> {
										return new EventMember(
											guildMemberPlayer.getUsername(),
											guildMemberUuid,
											"" +
											(guildMemberPlayer.getTotalSlayer() - higherDepth(guildMember, "startingAmount").getAsDouble()),
											higherDepth(guildMember, "profileName").getAsString()
										);
									}
									case "catacombs" -> {
										return new EventMember(
											guildMemberPlayer.getUsername(),
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
											guildMemberPlayer.getUsername(),
											guildMemberUuid,
											"" + (guildMemberPlayer.getWeight() - higherDepth(guildMember, "startingAmount").getAsDouble()),
											higherDepth(guildMember, "profileName").getAsString()
										);
									}
									default -> {
										if (eventType.startsWith("collection.")) {
											return new EventMember(
												guildMemberPlayer.getUsername(),
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
													guildMemberPlayer.getUsername(),
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
													guildMemberPlayer.getUsername(),
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
						},
						executor
					)
			);
		}

		for (CompletableFuture<EventMember> future : futuresList) {
			try {
				EventMember playerFutureResponse = future.get();
				if (playerFutureResponse != null) {
					guildMemberPlayersList.add(playerFutureResponse);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		leaderboardDatabase.insertIntoLeaderboard(players);

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.getStartingAmount())));

		guildMap.get(guildId).setEventCurrentlyUpdating(false);
		return guildMemberPlayersList;
	}

	public static EmbedBuilder getEventLeaderboard(
		Guild guild,
		User user,
		SlashCommandEvent slashCommandEvent,
		ButtonInteractionEvent buttonEvent
	) {
		String guildId = guild.getId();
		if (!database.getSkyblockEventActive(guildId)) {
			return defaultEmbed("No event running");
		}

		AutomaticGuild currentGuild = guildMap.get(guildId);

		CustomPaginator.Builder paginateBuilder = defaultPaginator(user).setColumns(1).setItemsPerPage(25);

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
				if (slashCommandEvent != null) {
					slashCommandEvent.paginate(
						paginateBuilder.updateExtras(extra ->
							extra
								.setEveryPageTitle("Event Leaderboard")
								.setEveryPageText(
									"**Last Updated <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>**\n"
								)
						)
					);
				} else {
					paginateBuilder
						.updateExtras(extra ->
							extra
								.setEveryPageTitle("Event Leaderboard")
								.setEveryPageText(
									"**Last Updated:** <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>\n"
								)
						)
						.build()
						.paginate(buttonEvent.getHook(), 0);
				}
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}

		if (currentGuild.eventCurrentlyUpdating) {
			return invalidEmbed("The leaderboard is currently updating, please try again in a couple of seconds");
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

		paginateBuilder.getExtras().setEveryPageTitle("Event Leaderboard");

		guildMap.get(guildId).setEventMemberList(guildMemberPlayersList);
		guildMap.get(guildId).setEventMemberListLastUpdated(Instant.now());

		if (paginateBuilder.size() > 0) {
			if (slashCommandEvent != null) {
				slashCommandEvent.paginate(paginateBuilder);
			} else {
				paginateBuilder.build().paginate(buttonEvent.getHook(), 0);
			}
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

	public static EmbedBuilder joinSkyblockEvent(String username, String profile, Member member, String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			String uuid;
			if (member != null) {
				LinkedAccount linkedAccount = database.getByDiscord(member.getId());
				if (linkedAccount == null) {
					return invalidEmbed("You must be linked to run this command. Use `/link <player>` to link");
				}

				uuid = linkedAccount.uuid();
				username = linkedAccount.username();
			} else {
				UsernameUuidStruct uuidStruct = usernameToUuid(username);
				if (!uuidStruct.isValid()) {
					return invalidEmbed(uuidStruct.failCause());
				}

				uuid = usernameToUuid(username).uuid();
				username = uuidStruct.username();
			}

			if (database.eventHasMemberByUuid(guildId, uuid)) {
				return invalidEmbed(
					member != null
						? "You are already in the event! If you want to leave or change profile use `/event leave`"
						: "Player is already in the event"
				);
			}

			JsonElement eventSettings = database.getSkyblockEventSettings(guildId);

			if (member != null) {
				if (!higherDepth(eventSettings, "eventGuildId", "").isEmpty()) {
					HypixelResponse guildJson = getGuildFromPlayer(uuid);
					if (!guildJson.isValid()) {
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
			}

			Player player = profile != null ? new Player(username, profile) : new Player(username);
			if (player.isValid()) {
				try {
					double startingAmount = 0;
					String startingAmountFormatted = "";

					String eventType = higherDepth(eventSettings, "eventType").getAsString();

					if ((eventType.startsWith("skills") || eventType.startsWith("weight")) && !player.isSkillsApiEnabled()) {
						return invalidEmbed(
							member != null ? "Please enable your skills API before joining" : "Player's skills API is disabled"
						);
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
									formatNumber(startingAmount) + " " + (skillType.equals("all") ? "total skills" : skillType) + "  xp";
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
								(member != null ? "You" : "Player") +
								" must have at least " +
								formatNumber(minAmt) +
								" " +
								getEventTypeFormatted(eventType) +
								" to join"
							);
						}
					} catch (Exception ignored) {}

					try {
						int maxAmt = Integer.parseInt(higherDepth(eventSettings, "maxAmount").getAsString());
						if (maxAmt != -1 && startingAmount > maxAmt) {
							return invalidEmbed(
								(member != null ? "You" : "Player") +
								" must have no more than " +
								formatNumber(maxAmt) +
								" " +
								getEventTypeFormatted(eventType) +
								" to join"
							);
						}
					} catch (Exception ignored) {}

					int code = database.addMemberToSkyblockEvent(
						guildId,
						new EventMember(player.getUsername(), player.getUuid(), "" + startingAmount, player.getProfileName())
					);

					if (code == 200) {
						return defaultEmbed(member != null ? "Joined event" : "Added player to event")
							.setDescription(
								"**Username:** " +
								player.getUsername() +
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
			return invalidEmbed("No event running");
		}
	}

	public static EmbedBuilder getCurrentSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
			EmbedBuilder eb = defaultEmbed("Current Event");

			if (!higherDepth(currentSettings, "eventGuildId", "").isEmpty()) {
				HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
				if (!guildJson.isValid()) {
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
					m.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event has ended").build()).setComponents().queue()
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

	public static EmbedBuilder createSkyblockEvent(SlashCommandEvent event) {
		boolean sbEventActive = database.getSkyblockEventActive(event.getGuild().getId());
		if (sbEventActive) {
			return invalidEmbed("Event already running");
		} else if (guildMap.containsKey(event.getGuild().getId())) {
			AutomaticGuild automaticGuild = guildMap.get(event.getGuild().getId());
			if (automaticGuild.skyblockEventHandler == null || automaticGuild.skyblockEventHandler.hasTimedOut()) {
				automaticGuild.setSkyblockEventHandler(new SkyblockEventHandler(event));
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
}
