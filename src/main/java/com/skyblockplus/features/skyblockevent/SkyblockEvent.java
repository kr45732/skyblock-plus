package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.skyblockevent.RunningEvent;
import com.skyblockplus.api.serversettings.skyblockevent.SbEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.skyblockplus.utils.Hypixel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SkyblockEvent {

	public final boolean enable;
	public EmbedBuilder eb;
	public TextChannel announcementChannel;
	public Map<Integer, String> prizeListMap;
	public CommandEvent commandEvent;
	public int state = 0;
	public JsonElement guildJson;
	public String eventType;
	public int eventDuration;
	public long epochSecondEndingTime;
	public Instant lastMessageSentTime;
	public ScheduledFuture<?> scheduledFuture;
	public int attemptsLeft = 3;

	public SkyblockEvent() {
		this.enable = false;
	}

	public SkyblockEvent(CommandEvent commandEvent) {
		this.enable = true;
		this.commandEvent = commandEvent;
		this.eb = defaultEmbed("Skyblock competition").setFooter("Type 'cancel' to stop the process");
		eb.setDescription("What is the name of the guild I should track?");
		sendEmbedMessage(eb);
		lastMessageSentTime = Instant.now();
		scheduledFuture = scheduler.scheduleAtFixedRate(this::checkForTimeout, 0, 1, TimeUnit.MINUTES);
	}

	private void checkForTimeout() {
		try {
			Duration res = Duration.between(lastMessageSentTime, Instant.now());
			if (res.toMinutes() >= 5) {
				resetSkyblockEvent(defaultEmbed("Timeout"));
			}
		} catch (Exception e) {
			System.out.println("== Stack Trace (checkForTimeout) ==");
			e.printStackTrace();
		}
	}

	private void sendEmbedMessage(EmbedBuilder eb) {
		commandEvent.getChannel().sendMessageEmbeds(eb.build()).complete();
	}

	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!enable) {
			return;
		}

		if (!commandEvent.getChannel().equals(event.getChannel())) {
			return;
		}

		if (!commandEvent.getAuthor().equals(event.getAuthor())) {
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
				try {
					guildJson = Hypixel.getGuildFromName(replyMessage, true);
					eb
						.addField(
							"Guild",
							"Name: " +
							higherDepth(guildJson, "guild.name").getAsString() +
							"\nMembers: " +
							higherDepth(guildJson, "guild.members").getAsJsonArray().size(),
							false
						)
						.setDescription("Is this a catacombs, slayer, skills, weight, or collections event?");
					state++;
				} catch (Exception e) {
					eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
					attemptsLeft--;
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
						eb.addField("Event Type", "Skills", false);
						eventType = "skills";
						break;
					case "collections":
						state = 6;
						eb.setDescription("Which collection should this event track?");
						sendEmbedMessage(eb);
						return;
				}
				if (eventType != null) {
					eb.setDescription("How many hours should the event last?");
					state++;
				} else {
					eb.setDescription("`" + replyMessage + "` is invalid. Please try again.");
					attemptsLeft--;
				}
				sendEmbedMessage(eb);
				break;
			case 2:
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
			case 3:
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
			case 4:
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
			case 5:
				if (replyMessage.equalsIgnoreCase("start")) {
					EmbedBuilder announcementEb = defaultEmbed("Skyblock Event");
					announcementEb.setDescription(
						"A new " +
						(eventType.startsWith("collection.") ? eventType.split("-")[1] + " collection" : eventType) +
						" Skyblock competition has been created! Please see below for more information."
					);
					announcementEb.addField("Guild Name", higherDepth(guildJson, "guild.name").getAsString(), false);

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
			case 6:
				Map<String, String> collections = new HashMap<>();
				for (Map.Entry<String, JsonElement> collection : getCollectionsJson().entrySet()) {
					String collectionName = higherDepth(collection.getValue(), "name").getAsString();
					if (collectionName.equalsIgnoreCase(replyMessage)) {
						eb.addField("Event Type", capitalizeString(collectionName) + " collection", false);
						eventType = "collection." + collection.getKey() + "-" + collectionName.toLowerCase();
						eb.setDescription("How many hours should the event last?");
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
		}

		if (attemptsLeft == 0) {
			resetSkyblockEvent(defaultEmbed("Canceled (3/3 failed attempts)"));
		}
	}

	private boolean setSkyblockEventInDatabase() {
		if (!database.serverByServerIdExists(commandEvent.getGuild().getId())) {
			database.addNewServerSettings(
				commandEvent.getGuild().getId(),
				new ServerSettingsModel(commandEvent.getGuild().getName(), commandEvent.getGuild().getId())
			);
		}

		RunningEvent newRunningEvent = new RunningEvent(
			eventType,
			announcementChannel.getId(),
			"" + epochSecondEndingTime,
			prizeListMap,
			new ArrayList<>(),
			higherDepth(guildJson, "guild._id").getAsString()
		);
		SbEvent newSkyblockEventSettings = new SbEvent(newRunningEvent, "true");

		return (database.setSkyblockEventSettings(commandEvent.getGuild().getId(), newSkyblockEventSettings) == 200);
	}

	public void resetSkyblockEvent(EmbedBuilder eb) {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}

		if (eb != null) {
			sendEmbedMessage(eb);
		}
		guildMap.get(commandEvent.getGuild().getId()).setSkyblockEvent(new SkyblockEvent());
	}
}
