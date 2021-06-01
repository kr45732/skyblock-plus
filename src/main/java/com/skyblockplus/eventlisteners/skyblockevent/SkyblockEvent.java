package com.skyblockplus.eventlisteners.skyblockevent;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.HYPIXEL_API_KEY;
import static com.skyblockplus.utils.Utils.capitalizeString;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.getJson;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.api.discordserversettings.skyblockevent.RunningEvent;
import com.skyblockplus.api.discordserversettings.skyblockevent.SbEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SkyblockEvent {

  public final DateTimeFormatter formatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withZone(ZoneId.of("UTC"));
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
  public boolean timeout = false;
  public ScheduledExecutorService scheduler;

  public SkyblockEvent() {
    this.enable = false;
  }

  public SkyblockEvent(CommandEvent commandEvent) {
    this.enable = true;
    this.commandEvent = commandEvent;
    this.eb =
      defaultEmbed("Create a Skyblock competition")
        .setFooter("Type `exit` to cancel");
    lastMessageSentTime = Instant.now();
    scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(
      this::checkForTimeout,
      0,
      1,
      TimeUnit.MINUTES
    );
  }

  public ScheduledExecutorService getScheduler() {
    return scheduler;
  }

  private void checkForTimeout() {
    try {
      Duration res = Duration.between(lastMessageSentTime, Instant.now());
      System.out.println("Timeout Event: " + res.toMinutes());
      if (res.toMinutes() >= 1) {
        timeout = true;
        sendEmbedMessage(defaultEmbed("Timeout"));
      }
    } catch (Exception e) {
      System.out.println("== Stack Trace (checkForTimeout) ==");
      e.printStackTrace();
    }
  }

  public boolean isEnable() {
    return enable;
  }

  private Message sendEmbedMessage(EmbedBuilder eb) {
    return commandEvent.getChannel().sendMessage(eb.build()).complete();
  }

  public String onGuildMessageReceived(GuildMessageReceivedEvent event) {
    if (!enable) {
      return "false";
    }

    if (timeout) {
      return "delete";
    }

    if (!commandEvent.getChannel().equals(event.getChannel())) {
      return "false";
    }

    if (!commandEvent.getAuthor().equals(event.getAuthor())) {
      return "false";
    }

    lastMessageSentTime = Instant.now();

    if (event.getMessage().getContentRaw().equalsIgnoreCase("exit")) {
      sendEmbedMessage(
        defaultEmbed("Create a Skyblock competition").setDescription("Canceled")
      );
      return "delete";
    }

    switch (state) {
      case 0:
        try {
          guildJson =
            getJson(
              "https://api.hypixel.net/guild?key=" +
              HYPIXEL_API_KEY +
              "&name=" +
              event.getMessage().getContentRaw().replace(" ", "%20")
            );
          eb
            .addField(
              "Guild",
              "Name: " +
              higherDepth(guildJson, "guild.name").getAsString() +
              "\nMembers: " +
              higherDepth(guildJson, "guild.members").getAsJsonArray().size(),
              false
            )
            .setDescription(
              "Is this a __catacombs__, __slayer__, or __skills__ event?"
            );
          state++;
        } catch (Exception e) {
          eb.setDescription(
            "Invalid guild name: " + event.getMessage().getContentRaw()
          );
        }
        sendEmbedMessage(eb);
        break;
      case 1:
        String replyMessage = event.getMessage().getContentRaw().toLowerCase();
        if (
          replyMessage.equals("catacombs") ||
          replyMessage.equals("slayer") ||
          replyMessage.equals("skills")
        ) {
          if (replyMessage.equals("catacombs")) {
            eb.addField("Event Type", "Catacombs", false);
            eventType = "catacombs";
          } else if (replyMessage.equals("slayer")) {
            eb.addField("Event Type", "Slayer", false);
            eventType = "slayer";
          } else {
            eb.addField("Event Type", "Skills", false);
            eventType = "skills";
          }
          eb.setDescription(
            "Please enter the number of __hours__ the event should last"
          );
          state++;
        } else {
          eb.setDescription(
            replyMessage +
            " is an invalid option\nPlease choose from catacombs, slayer, or skills"
          );
        }
        sendEmbedMessage(eb);
        break;
      case 2:
        replyMessage = event.getMessage().getContentRaw().toLowerCase();
        try {
          eventDuration = Integer.parseInt(replyMessage);
          if (eventDuration <= 0) {
            eb.setDescription("Event must be longer than 0 hours");
          } else if (eventDuration > 336) {
            eb.setDescription("Event must be at most 2 weeks (336 hours)");
          } else {
            eb.addField(
              "End Date",
              formatter.format(
                Instant.now().plus(eventDuration, ChronoUnit.HOURS)
              ) +
              " UTC",
              false
            );
            eb.setDescription(
              "If there are no prizes please type \"none\", else please enter the prizes in one message following the format in the example below (place number : prize):\n1 : 15 mil coins\n2 : 10 mil\n3 : 500k"
            );
            state++;
          }
        } catch (Exception e) {
          eb.setDescription("Invalid hours value: " + replyMessage);
        }
        sendEmbedMessage(eb);
        break;
      case 3:
        replyMessage = event.getMessage().getContentRaw().toLowerCase();
        if (replyMessage.equals("none")) {
          eb.addField("Prizes", "None", false);
          prizeListMap = null;
        } else {
          String[] prizeList = replyMessage.split("\n");
          prizeListMap = new TreeMap<>();
          for (String prizeLevel : prizeList) {
            try {
              String[] prizeLevelArr = prizeLevel.split(":");
              prizeListMap.put(
                Integer.parseInt(prizeLevelArr[0].trim()),
                prizeLevelArr[1].trim()
              );
            } catch (Exception ignored) {}
          }

          StringBuilder ebString = new StringBuilder();
          for (Map.Entry<Integer, String> prize : prizeListMap.entrySet()) {
            ebString
              .append("• ")
              .append(prize.getKey())
              .append(") - ")
              .append(prize.getValue())
              .append("\n");
          }

          eb.addField("Prizes", ebString.toString(), false);
        }
        eb.setDescription("Please enter the channel for the announcement");
        state++;
        sendEmbedMessage(eb);
        break;
      case 4:
        replyMessage = event.getMessage().getContentRaw().toLowerCase();
        try {
          announcementChannel =
            event
              .getGuild()
              .getTextChannelById(replyMessage.replaceAll("[<#>]", ""));
          eb.addField(
            "Announcement Channel",
            announcementChannel.getAsMention(),
            false
          );
          eb.setDescription(
            "Please confirm the event by replying with \"yes\" or anything else to cancel"
          );
          state++;
        } catch (Exception e) {
          eb.setDescription(
            "Invalid channel. Please make sure the bot can see the channel and it is a valid channel!"
          );
        }
        sendEmbedMessage(eb);

        break;
      case 5:
        if (event.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
          Message temp = sendEmbedMessage(
            defaultEmbed("Create a Skyblock competition")
              .setDescription("Event starting...")
          );
          EmbedBuilder announcementEb = defaultEmbed(
            capitalizeString(eventType) + " event started!"
          );
          announcementEb.setDescription(
            "A new " +
            eventType +
            " event has been created! Please see below for more information."
          );
          announcementEb.addField(
            "Guild Name",
            higherDepth(guildJson, "guild.name").getAsString(),
            false
          );

          epochSecondEndingTime =
            Instant
              .now()
              .plus(eventDuration, ChronoUnit.HOURS)
              .getEpochSecond();
          announcementEb.addField(
            "End Date",
            formatter.format(
              Instant.now().plus(eventDuration, ChronoUnit.HOURS)
            ) +
            " UTC",
            false
          );

          StringBuilder ebString = new StringBuilder();
          if (prizeListMap != null) {
            for (Map.Entry<Integer, String> prize : prizeListMap.entrySet()) {
              ebString
                .append("• ")
                .append(prize.getKey())
                .append(") - ")
                .append(prize.getValue())
                .append("\n");
            }
          } else {
            ebString = new StringBuilder("None");
          }
          announcementEb.addField("Prizes", ebString.toString(), false);
          announcementEb.addField(
            "How To Join",
            "Run `" +
            BOT_PREFIX +
            "event join` to join!\n**You must be linked and in the guild**",
            false
          );

          if (setSkyblockEventInDatabase()) {
            announcementChannel.sendMessage(announcementEb.build()).complete();

            temp
              .editMessage(
                defaultEmbed("Create a Skyblock competition")
                  .setDescription("Event started")
                  .build()
              )
              .queueAfter(3, TimeUnit.SECONDS);
          } else {
            temp
              .editMessage(
                defaultEmbed("Create a Skyblock competition")
                  .setDescription("**Error starting event**")
                  .build()
              )
              .queueAfter(3, TimeUnit.SECONDS);
          }
        } else {
          sendEmbedMessage(
            defaultEmbed("Create a Skyblock competition")
              .setDescription("Canceled")
          );
        }
        return "delete";
    }
    return "true";
  }

  private boolean setSkyblockEventInDatabase() {
    if (!database.serverByServerIdExists(commandEvent.getGuild().getId())) {
      database.addNewServerSettings(
        commandEvent.getGuild().getId(),
        new ServerSettingsModel(
          commandEvent.getGuild().getName(),
          commandEvent.getGuild().getId()
        )
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

    return (
      database.updateSkyblockEventSettings(
        commandEvent.getGuild().getId(),
        newSkyblockEventSettings
      ) ==
      200
    );
  }
}
