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
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Constants.ALL_SKILL_NAMES;
import static com.skyblockplus.utils.Hypixel.getGuildFromName;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyblockEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SkyblockEventHandler.class);

	public final boolean enable;
	public EmbedBuilder eb;
	public TextChannel announcementChannel;
	public Map<Integer, String> prizeListMap;
	public int state = 0;
	public JsonElement guildJson;
	public String eventType;
	public int eventDuration;
	public long epochSecondEndingTime;
	public Instant lastMessageSentTime;
	public ScheduledFuture<?> scheduledFuture;
	public int attemptsLeft = 3;
	public MessageChannel channel;
	public User user;
	public Guild guild;
	public int minAmount = -1;
	private int maxAmount = -1;

	public SkyblockEventHandler() {
		this.enable = false;
	}

	public SkyblockEventHandler(User user, Guild guild, MessageChannel channel, InteractionHook hook) {
		this.user = user;
		this.guild = guild;
		this.enable = true;
		this.eb =
			defaultEmbed("Skyblock competition")
				.setFooter("Type 'cancel' to stop the process")
				.setDescription("What is the name of the guild I should track?");

		if (channel != null) {
			this.channel = channel;
			sendEmbedMessage(eb);
		} else {
			this.channel = hook.getInteraction().getMessageChannel();
			hook.editOriginalEmbeds(eb.build()).queue();
		}

		lastMessageSentTime = Instant.now();
		scheduledFuture = scheduler.scheduleWithFixedDelay(this::checkForTimeout, 0, 1, TimeUnit.MINUTES);
	}

	private void checkForTimeout() {
		try {
			Duration res = Duration.between(lastMessageSentTime, Instant.now());
			if (res.toMinutes() >= 3) {
				resetSkyblockEvent(defaultEmbed("Timeout"));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}

	private void sendEmbedMessage(EmbedBuilder eb) {
		channel.sendMessageEmbeds(eb.build()).complete();
	}

	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!enable) {
			return;
		}

		if (!channel.equals(event.getChannel())) {
			return;
		}

		if (!user.equals(event.getAuthor())) {
			return;
		}

		lastMessageSentTime = Instant.now();

		String replyMessage = event.getMessage().getContentRaw();

		if (replyMessage.equalsIgnoreCase("cancel")) {
			resetSkyblockEvent(defaultEmbed("Skyblock competition").setDescription("Canceled event creation"));
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
						eventType = "catacombs";
						break;
					case "slayer":
						eb.addField("Event Type", "Slayer", false);
						eventType = "slayer";
						break;
					case "weight":
						eb.addField("Event Type", "Weight", false);
						eventType = "weight";
						break;
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
				}
				if (eventType != null) {
					eb.setDescription(
						"Reply with the minimum and/or maximum amount a player can have when joining the event or 'none'. Follow the format in the example below (type:value):\nmin:5000\nmax:8000"
					);
					state++;
				} else {
					eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
			case 2:
				String[] minMax = replyMessage.toLowerCase().split("\n");
				boolean failed = false;
				for (String i : minMax) {
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
					eventDuration = Integer.parseInt(replyMessage);
					if (eventDuration <= 0 || eventDuration > 336) {
						eb.setDescription("The event must be at least an hour and at most 2 weeks (336 hours). Please try again.");
						attemptsLeft--;
					} else {
						Instant endsAt = Instant.now().plus(eventDuration, ChronoUnit.HOURS);
						eb.addField("End Date", "Ends in <t:" + endsAt.getEpochSecond() + ":R>", false);
						eb.setDescription(
							"If there are no prizes, type 'none'. Otherwise, type the prizes in one message following the format in the example below (place number:prize):\n1:15 mil coins\n2:10 mil\n3:500k"
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
					prizeListMap = null;
					eb.setDescription("Please mention the channel the announcement should be in.");
					state++;
				} else {
					String[] prizeList = replyMessage.split("\n");
					prizeListMap = new TreeMap<>();
					for (String prizeLevel : prizeList) {
						try {
							String[] prizeLevelArr = prizeLevel.split(":");
							prizeListMap.put(Integer.parseInt(prizeLevelArr[0].trim()), prizeLevelArr[1].trim());
						} catch (Exception ignored) {}
					}

					if (prizeListMap.size() == 0) {
						prizeListMap = null;
						eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
						attemptsLeft--;
					} else {
						StringBuilder ebString = new StringBuilder();
						for (Map.Entry<Integer, String> prize : prizeListMap.entrySet()) {
							ebString.append("`").append(prize.getKey()).append(")` ").append(prize.getValue()).append("\n");
						}
						eb.addField("Prizes", ebString.toString(), false);
						eb.setDescription("Please mention the channel the announcement should be in.");
						state++;
					}
				}
				sendEmbedMessage(eb);
				break;
			case 5:
				try {
					announcementChannel = event.getGuild().getTextChannelById(replyMessage.toLowerCase().replaceAll("[<#>]", ""));
					eb.addField("Announcement Channel", announcementChannel.getAsMention(), false);
					eb.setDescription("To start the event reply with 'start'. Reply with anything else to cancel.");
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
					String eventTypeFormatted = eventType;
					if (eventType.startsWith("collection.")) {
						eventTypeFormatted = eventType.split("-")[1] + " collection";
					} else if (eventType.startsWith("skills.")) {
						eventTypeFormatted = eventType.split("skills.")[1].equals("all") ? "skills" : eventType.split("skills.")[1];
					}

					announcementEb.setDescription(
						"A new " +
						eventTypeFormatted.toLowerCase() +
						" Skyblock competition has been created! Please see below for more information."
					);
					announcementEb.addField("Guild Name", higherDepth(guildJson, "name").getAsString(), false);

					Instant endsAt = Instant.now().plus(eventDuration, ChronoUnit.HOURS);
					epochSecondEndingTime = endsAt.getEpochSecond();
					announcementEb.addField("End Date", "Ends in <t:" + endsAt.getEpochSecond() + ":R>", false);

					StringBuilder ebString = new StringBuilder();
					if (prizeListMap != null) {
						for (Map.Entry<Integer, String> prize : prizeListMap.entrySet()) {
							ebString.append("`").append(prize.getKey()).append(")` ").append(prize.getValue()).append("\n");
						}
					} else {
						ebString = new StringBuilder("None");
					}
					announcementEb.addField("Prizes", ebString.toString(), false);
					announcementEb.addField(
						"How to join",
						"Run `" +
						getGuildPrefix(event.getGuild().getId()) +
						"event join` to join!\nYou must be linked to the bot and in the guild.",
						false
					);

					if (setSkyblockEventInDatabase()) {
						announcementChannel.sendMessageEmbeds(announcementEb.build()).complete();
						resetSkyblockEvent(
							defaultEmbed("Skyblock competition")
								.setDescription("Event successfully started in " + announcementChannel.getAsMention())
						);
					} else {
						resetSkyblockEvent(defaultEmbed("Skyblock competition").setDescription("Error starting event"));
					}
				} else {
					resetSkyblockEvent(defaultEmbed("Skyblock competition").setDescription("Canceled event creation"));
				}
				return;
			case 7:
				Map<String, String> collections = new HashMap<>();
				for (Map.Entry<String, JsonElement> collection : getCollectionsJson().entrySet()) {
					String collectionName = higherDepth(collection.getValue(), "name").getAsString();
					if (collectionName.equalsIgnoreCase(replyMessage)) {
						eb.addField("Event Type", capitalizeString(collectionName) + " collection", false);
						eventType = "collection." + collection.getKey() + "-" + collectionName.toLowerCase();
						eb.setDescription(
							"Reply with the minimum and/or maximum amount a player can have when joining the event or 'none'. Follow the format in the example below (type:value):\nmin:5000\nmax:8000"
						);
						state = 2;
						sendEmbedMessage(eb);
						return;
					}
					collections.put(collectionName, collection.getKey());
				}

				String closestMatch = getClosestMatch(replyMessage, new ArrayList<>(collections.keySet()));
				eb.setDescription("`" + replyMessage + "` is invalid. Did you mean `" + closestMatch.toLowerCase() + "`?");
				attemptsLeft--;
				sendEmbedMessage(eb);
				break;
			case 8:
				if (replyMessage.equalsIgnoreCase("all") || ALL_SKILL_NAMES.contains(replyMessage.toLowerCase())) {
					eb.addField("Event Type", capitalizeString(replyMessage.equalsIgnoreCase("all") ? "skills" : replyMessage), false);
					eventType = "skills." + replyMessage.toLowerCase();
					eb.setDescription(
						"Reply with the minimum and/or maximum amount a player can have when joining the event or 'none'. Follow the format in the example below (type:value):\nmin:5000\nmax:8000"
					);
					state = 2;
				} else {
					String closestSkill = getClosestMatch(replyMessage, ALL_SKILL_NAMES);
					eb.setDescription("`" + replyMessage + "` is invalid. Did you mean `" + closestSkill.toLowerCase() + "`?");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
		}

		if (attemptsLeft == 0) {
			resetSkyblockEvent(defaultEmbed("Canceled (3/3 failed attempts)"));
		}
	}

	private boolean setSkyblockEventInDatabase() {
		if (!database.serverByServerIdExists(guild.getId())) {
			database.addNewServerSettings(guild.getId(), new ServerSettingsModel(guild.getName(), guild.getId()));
		}

		EventSettings sbEvent = new EventSettings(
			eventType,
			announcementChannel.getId(),
			"" + epochSecondEndingTime,
			prizeListMap,
			new ArrayList<>(),
			higherDepth(guildJson, "_id").getAsString(),
			"" + minAmount,
			"" + maxAmount
		);

		return (database.setSkyblockEventSettings(guild.getId(), sbEvent) == 200);
	}

	public void resetSkyblockEvent(EmbedBuilder eb) {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}

		if (eb != null) {
			sendEmbedMessage(eb);
		}
		guildMap.get(guild.getId()).setSkyblockEvent(new SkyblockEventHandler());
	}
}
