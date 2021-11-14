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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.features.skyblockevent.SkyblockEventCommand.getEventTypeFormatted;
import static com.skyblockplus.utils.ApiHandler.getGuildFromName;
import static com.skyblockplus.utils.Constants.ALL_SKILL_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;

public class SkyblockEventHandler {

	private final PaginatorEvent paginatorEvent;
	private final EventSettings eventSettings;
	private final EmbedBuilder eb;
	private int state = 0;
	private int attemptsLeft = 3;
	public TextChannel announcementChannel;
	public JsonElement guildJson;

	public SkyblockEventHandler(PaginatorEvent paginatorEvent) {
		this.paginatorEvent = paginatorEvent;
		this.eb =
			defaultEmbed("Skyblock Event")
				.setFooter("Type 'cancel' to stop the process")
				.setDescription("Reply with the name of the guild I should track.");

		if (paginatorEvent.isSlashCommand()) {
			paginatorEvent.getSlashCommand().getHook().editOriginalEmbeds(eb.build()).queue();
		} else {
			paginatorEvent.getChannel().sendMessageEmbeds(eb.build()).queue();
		}

		this.eventSettings = new EventSettings();
		waiter.waitForEvent(
			GuildMessageReceivedEvent.class,
			this::condition,
			this::action,
			3,
			TimeUnit.MINUTES,
			() -> sendEmbedMessage(defaultEmbed("Skyblock Event").setDescription("Event creation timed out"), false)
		);
	}

	private boolean condition(GuildMessageReceivedEvent event) {
		return (
			paginatorEvent.getChannel().getId().equals(event.getChannel().getId()) &&
			paginatorEvent.getUser().getId().equals(event.getAuthor().getId())
		);
	}

	private void action(GuildMessageReceivedEvent event) {
		String replyMessage = event.getMessage().getContentRaw();

		if (replyMessage.equalsIgnoreCase("cancel")) {
			sendEmbedMessage(defaultEmbed("Skyblock Event").setDescription("Canceled event creation"), false);
			return;
		}

		switch (state) {
			case 0:
				HypixelResponse response = getGuildFromName(replyMessage);
				if (response.isNotValid()) {
					eb.setDescription(response.getFailCause() + ". Please try again.");
					attemptsLeft--;
				} else {
					guildJson = response.getResponse();
					eventSettings.setEventGuildId(higherDepth(guildJson, "_id").getAsString());
					eb
						.addField(
							"Guild",
							"Name: " +
							higherDepth(guildJson, "name").getAsString() +
							"\nMembers: " +
							higherDepth(guildJson, "members").getAsJsonArray().size(),
							false
						)
						.setDescription("Is this a catacombs, slayer, skills, weight, or collections event?");
					state++;
				}
				sendEmbedMessage(eb);
				break;
			case 1:
				switch (replyMessage.toLowerCase()) {
					case "catacombs":
						eb.addField("Event Type", "Catacombs", false);
						eventSettings.setEventType("catacombs");
						break;
					case "slayer":
						eb.addField("Event Type", "Slayer", false);
						eventSettings.setEventType("slayer");
						break;
					case "weight":
						state = 9;
						eb.setDescription("Reply with the weight type this event should track or 'all' for total weight.");
						sendEmbedMessage(eb);
						return;
					case "skills":
						state = 8;
						eb.setDescription(
							"Reply with the skill this event should track or 'all' for all skills excluding cosmetic skills."
						);
						sendEmbedMessage(eb);
						return;
					case "collections":
						state = 7;
						eb.setDescription("Which collection should this event track?");
						sendEmbedMessage(eb);
						return;
					default:
						eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
						attemptsLeft--;
						sendEmbedMessage(eb);
						return;
				}
				eb.setDescription(
					"Reply with the minimum and/or maximum amount a player can have when joining the event. Follow the format in the example below (type:value):\nmin:5000\nmax:8000\n\nOptional and can be set to 'none'."
				);
				state++;
				sendEmbedMessage(eb);
				break;
			case 7:
				Map<String, String> collections = new HashMap<>();
				for (Map.Entry<String, JsonElement> collection : getCollectionsJson().entrySet()) {
					String collectionName = higherDepth(collection.getValue(), "name").getAsString();
					if (collectionName.equalsIgnoreCase(replyMessage)) {
						eb.addField("Event Type", capitalizeString(collectionName) + " collection", false);
						eventSettings.setEventType("collection." + collection.getKey() + "-" + collectionName.toLowerCase());
						eb.setDescription(
							"Reply with the minimum and/or maximum amount a player can have when joining the event. Follow the format in the example below (type:value):\nmin:5000\nmax:8000\n\nOptional and can be set to 'none'."
						);
						state = 2;
						sendEmbedMessage(eb);
						return;
					}
					collections.put(collectionName, collection.getKey());
				}

				eb.setDescription(
					"`" +
					replyMessage +
					"` is invalid. Did you mean `" +
					getClosestMatch(replyMessage, new ArrayList<>(collections.keySet())).toLowerCase() +
					"`?"
				);
				attemptsLeft--;
				sendEmbedMessage(eb);
				break;
			case 8:
				if (replyMessage.equalsIgnoreCase("all") || ALL_SKILL_NAMES.contains(replyMessage.toLowerCase())) {
					String eventType = "skills." + replyMessage.toLowerCase();
					eb.addField("Event Type", getEventTypeFormatted(eventType), false);
					eventSettings.setEventType(eventType);
					eb.setDescription(
						"Reply with the minimum and/or maximum amount a player can have when joining the event. Follow the format in the example below (type:value):\nmin:5000\nmax:8000\n\nOptional and can be set to 'none'."
					);
					state = 2;
				} else {
					String closestSkill = getClosestMatch(replyMessage, ALL_SKILL_NAMES);
					eb.setDescription("`" + replyMessage + "` is invalid. Did you mean `" + closestSkill.toLowerCase() + "`?");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
			case 9:
				if (
					replyMessage.equalsIgnoreCase("all") ||
					replyMessage.equalsIgnoreCase("skills") ||
					replyMessage.equalsIgnoreCase("dungeons") ||
					replyMessage.equalsIgnoreCase("slayer")
				) {
					String eventType = "weight." + replyMessage.toLowerCase();
					eb.addField("Event Type", getEventTypeFormatted(eventType), false);
					eventSettings.setEventType(eventType);
					eb.setDescription(
						"Reply with the minimum and/or maximum amount a player can have when joining the event. Follow the format in the example below (type:value):\nmin:5000\nmax:8000\n\nOptional and can be set to 'none'."
					);
					state = 2;
				} else {
					eb.setDescription("`" + replyMessage + "` is invalid. Please choose from all, skills, slayer, or dungeons");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
			case 2:
				boolean failed = false;
				int minAmount = -1;
				int maxAmount = -1;
				for (String i : replyMessage.toLowerCase().split("\n")) {
					if (i.startsWith("min:")) {
						try {
							minAmount = Integer.parseInt(i.split("min:")[1]);
							if (minAmount < 0) {
								failed = true;
								break;
							}
						} catch (Exception e) {
							failed = true;
							break;
						}
					} else if (i.startsWith("max:")) {
						try {
							maxAmount = Integer.parseInt(i.split("max:")[1]);
							if (maxAmount < 0) {
								failed = true;
								break;
							} else if (minAmount != -1 && minAmount > maxAmount) {
								failed = true;
								break;
							}
						} catch (Exception e) {
							failed = true;
							break;
						}
					}
				}

				if (failed) {
					attemptsLeft--;
					eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
				} else {
					eventSettings.setMinAmount("" + minAmount);
					eventSettings.setMaxAmount("" + maxAmount);
					if (minAmount != -1 || maxAmount != -1) {
						eb.addField(
							"Minimum & maximum amount",
							(minAmount != -1 ? "Minimum: " + formatNumber(minAmount) : "") +
							"\n" +
							(maxAmount != -1 ? "Maximum: " + formatNumber(maxAmount) : ""),
							false
						);
					}
					eb.setDescription("How many hours should the event last?");
					state++;
				}
				sendEmbedMessage(eb);
				break;
			case 3:
				try {
					int eventDuration = Integer.parseInt(replyMessage);
					if (eventDuration <= 0 || eventDuration > 336) {
						eb.setDescription("The event must be at least an hour and at most 2 weeks (336 hours). Please try again.");
						attemptsLeft--;
					} else {
						long endingTimeSeconds = Instant.now().plus(eventDuration, ChronoUnit.HOURS).getEpochSecond();
						eventSettings.setTimeEndingSeconds("" + endingTimeSeconds);
						eb.addField("End Date", "Ends <t:" + endingTimeSeconds + ":R>", false);
						eb.setDescription(
							"Reply with the prizes in one message following the format in the example below (place number:prize):\n1:15 mil coins\n2:10 mil\n3:500k\n\nOptional and can be set to 'none'."
						);
						state++;
					}
				} catch (Exception e) {
					eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
			case 4:
				if (replyMessage.equalsIgnoreCase("none")) {
					eb.addField("Prizes", "None", false);
					eb.setDescription("Reply with the channel the announcement and leaderboard should be in.");
					state++;
				} else {
					String[] prizeList = replyMessage.split("\n");
					Map<Integer, String> prizeListMap = new TreeMap<>();
					for (String prizeLevel : prizeList) {
						try {
							String[] prizeLevelArr = prizeLevel.split(":");
							prizeListMap.put(Integer.parseInt(prizeLevelArr[0].trim()), prizeLevelArr[1].trim());
						} catch (Exception ignored) {}
					}

					if (prizeListMap.size() == 0) {
						eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
						attemptsLeft--;
					} else {
						eb.addField(
							"Prizes",
							prizeListMap
								.entrySet()
								.stream()
								.map(prize -> "`" + prize.getKey() + ")` " + prize.getValue() + "\n")
								.collect(Collectors.joining()),
							false
						);
						eventSettings.setPrizeMap(prizeListMap);
						eb.setDescription("Reply with the channel the announcement and leaderboard should be in.");
						state++;
					}
				}
				sendEmbedMessage(eb);
				break;
			case 5:
				try {
					announcementChannel = event.getGuild().getTextChannelById(replyMessage.toLowerCase().replaceAll("[<#>]", ""));
					eb.addField("Announcement Channel", announcementChannel.getAsMention(), false);
					eventSettings.setAnnouncementId(announcementChannel.getId());
					eb.setDescription("Reply with 'start' to start the event or anything else to cancel.");
					state++;
				} catch (Exception e) {
					eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
			case 6:
				if (replyMessage.equalsIgnoreCase("start")) {
					EmbedBuilder announcementEb = defaultEmbed("Skyblock Event");
					String eventTypeFormatted = getEventTypeFormatted(eventSettings.getEventType());

					announcementEb.setDescription(
						"A new " +
						eventTypeFormatted.toLowerCase() +
						" Skyblock event has been created! Please see below for more information."
					);
					announcementEb.addField("Guild", higherDepth(guildJson, "name").getAsString(), false);
					announcementEb.addField("End Date", "Ends <t:" + eventSettings.getTimeEndingSeconds() + ":R>", false);

					StringBuilder ebString = new StringBuilder();
					if (eventSettings.getPrizeMap().size() > 0) {
						for (Map.Entry<Integer, String> prize : eventSettings.getPrizeMap().entrySet()) {
							ebString.append("`").append(prize.getKey()).append(")` ").append(prize.getValue()).append("\n");
						}
					} else {
						ebString.append("None");
					}
					announcementEb.addField("Prizes", ebString.toString(), false);
					announcementEb.addField(
						"Join the event",
						"Click the join button below or run `/event join` to join! You must be linked to the bot and in the guild.",
						false
					);
					announcementEb.addField(
						"Leaderboard",
						"Click the leaderboard button below or run `/event leaderboard`. to view the leaderboard.",
						false
					);

					Message announcementMessage = announcementChannel
						.sendMessageEmbeds(announcementEb.build())
						.setActionRow(
							Button.success("event_message_join", "Join Event"),
							Button.primary("event_message_leaderboard", "Event Leaderboard")
						)
						.complete();
					eventSettings.setAnnouncementMessageId(announcementMessage.getId());
					if (setSkyblockEventInDatabase()) {
						sendEmbedMessage(
							defaultEmbed("Skyblock Event")
								.setDescription("Event successfully started in " + announcementChannel.getAsMention()),
							false
						);
					} else {
						announcementMessage.delete().queue();
						sendEmbedMessage(defaultEmbed("Skyblock Event").setDescription("Error starting event"), false);
					}
				} else {
					sendEmbedMessage(defaultEmbed("Skyblock Event").setDescription("Canceled event creation"), false);
				}
				break;
		}
	}

	private void sendEmbedMessage(EmbedBuilder eb) {
		sendEmbedMessage(eb, true);
	}

	private void sendEmbedMessage(EmbedBuilder eb, boolean waitForReply) {
		paginatorEvent.getChannel().sendMessageEmbeds(eb.build()).queue();
		if (attemptsLeft == 0) {
			paginatorEvent
				.getChannel()
				.sendMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Canceled event creation (3/3 failed attempts)").build())
				.queue();
			guildMap.get(paginatorEvent.getGuild().getId()).setSkyblockEventHandler(null);
		} else if (waitForReply) {
			waiter.waitForEvent(
				GuildMessageReceivedEvent.class,
				this::condition,
				this::action,
				3,
				TimeUnit.MINUTES,
				() -> sendEmbedMessage(defaultEmbed("Skyblock Event").setDescription("Event creation timed out"), false)
			);
		} else {
			guildMap.get(paginatorEvent.getGuild().getId()).setSkyblockEventHandler(null);
		}
	}

	private boolean setSkyblockEventInDatabase() {
		if (!database.serverByServerIdExists(paginatorEvent.getGuild().getId())) {
			database.addNewServerSettings(
				paginatorEvent.getGuild().getId(),
				new ServerSettingsModel(paginatorEvent.getGuild().getName(), paginatorEvent.getGuild().getId())
			);
		}

		return database.setSkyblockEventSettings(paginatorEvent.getGuild().getId(), eventSettings) == 200;
	}
}
