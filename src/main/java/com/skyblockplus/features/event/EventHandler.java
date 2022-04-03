/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.features.event;

import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.features.mayor.MayorHandler.currentMayor;
import static com.skyblockplus.miscellaneous.TimeCommand.*;
import static com.skyblockplus.utils.Utils.*;

public class EventHandler {
    public static String messageId;

    public static void init(){
        scheduler.scheduleAtFixedRate(EventHandler::idk, 30, 30, TimeUnit.SECONDS);
        messageId = isMainBot() ? "959829803895255051" : "959888607676625016";
    }

    public static void idk() {
        try {
            String[] times = jda.getGuildById("796790757947867156").getTextChannelById("959829695686381658").retrieveMessageById(messageId).complete().getContentRaw().split("\n");
            // 0 - bingo start
            // 1 - bingo end
            // 2 - zoo early summer
            // 3 - zoo early winter
            // 4 - winter island open
            // 5 - dark auction open (5 min early)
            // 6 - new year celebration starts
            // 7 - spooky fishing starts
            // 8 - spooky event starts
            // 9 - fishing festival start
            // 10 - fallen star (5 min early)

            ZoneId z = ZoneId.of("America/New_York");
            LocalDate now = LocalDate.now(z);
            ZonedDateTime nowDateTime = ZonedDateTime.now(z);
            long nowEpoch = Instant.now().toEpochMilli();

            Instant startOfBingo = now.withDayOfMonth(1).atStartOfDay(z).toInstant();
            if (startOfBingo.toEpochMilli() <= nowEpoch && nowEpoch <= startOfBingo.plusSeconds(60).toEpochMilli() && Long.parseLong(times[0]) < startOfBingo.toEpochMilli()) {
                // start < now < now + 1 min
                sendMessage(times, 0, defaultEmbed("Bingo Start").setDescription("Bingo starts <t:" + startOfBingo.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > startOfBingo.plusSeconds(60).toEpochMilli() && Long.parseLong(times[0]) < startOfBingo.toEpochMilli()) {
                // missed
                sendMessage(times, 0, defaultEmbed("Bingo Start").setDescription("Bingo starts <t:" + startOfBingo.getEpochSecond() + ":R>").build());
            }

            Instant endOfBingo = now.withDayOfMonth(1).atStartOfDay(z).toInstant().plus(7, ChronoUnit.DAYS);
            if (endOfBingo.toEpochMilli() <= nowEpoch && nowEpoch <= endOfBingo.plusSeconds(60).toEpochMilli() && Long.parseLong(times[1]) < endOfBingo.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 1, defaultEmbed("Bingo End").setDescription("Bingo ends <t:" + endOfBingo.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > endOfBingo.plusSeconds(60).toEpochMilli() && Long.parseLong(times[1]) < endOfBingo.toEpochMilli()) {
                // missed
                sendMessage(times, 1, defaultEmbed("Bingo End").setDescription("Bingo ends <t:" + endOfBingo.getEpochSecond() + ":R>").build());
            }

            Instant zooEarlySummer = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 3 * MONTH_MS);
            if (zooEarlySummer.toEpochMilli() <= nowEpoch && nowEpoch <= zooEarlySummer.plusSeconds(60).toEpochMilli() && Long.parseLong(times[2]) < zooEarlySummer.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 2, defaultEmbed("Traveling Zoo").setDescription("Traveling zoo opens <t:" + zooEarlySummer.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > zooEarlySummer.plusSeconds(60).toEpochMilli() && Long.parseLong(times[2]) < zooEarlySummer.toEpochMilli()) {
                // missed
                sendMessage(times, 2, defaultEmbed("Traveling Zoo").setDescription("Traveling zoo opens <t:" + zooEarlySummer.getEpochSecond() + ":R>").build());
            }

            Instant zooEarlyWinter = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 9 * MONTH_MS);
            if (zooEarlyWinter.toEpochMilli() <= nowEpoch && nowEpoch <= zooEarlyWinter.plusSeconds(60).toEpochMilli() && Long.parseLong(times[3]) < zooEarlyWinter.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 3, defaultEmbed("Traveling Zoo").setDescription("Traveling zoo opens <t:" + zooEarlyWinter.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > zooEarlyWinter.plusSeconds(60).toEpochMilli() && Long.parseLong(times[3]) < zooEarlyWinter.toEpochMilli()) {
                // missed
                sendMessage(times, 3, defaultEmbed("Traveling Zoo").setDescription("Traveling zoo opens <t:" + zooEarlyWinter.getEpochSecond() + ":R>").build());
            }

            Instant jerryIslandOpen = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS);
            if (jerryIslandOpen.toEpochMilli() <= nowEpoch && nowEpoch <= jerryIslandOpen.plusSeconds(60).toEpochMilli() && Long.parseLong(times[4]) < jerryIslandOpen.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 4, defaultEmbed("Winter Island").setDescription("Winter island opens <t:" + jerryIslandOpen.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > jerryIslandOpen.plusSeconds(60).toEpochMilli() && Long.parseLong(times[4]) < jerryIslandOpen.toEpochMilli()) {
                // missed
                sendMessage(times, 4, defaultEmbed("Winter Island").setDescription("Winter island opens <t:" + jerryIslandOpen.getEpochSecond() + ":R>").build());
            }

            Instant darkAuctionOpen = nowDateTime.withMinute(50).toInstant();
            if (darkAuctionOpen.toEpochMilli() <= nowEpoch && nowEpoch <= darkAuctionOpen.plusSeconds(60).toEpochMilli() && Long.parseLong(times[5]) < darkAuctionOpen.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 5, defaultEmbed("Dark Auction").setDescription("Dark auction opens <t:" + darkAuctionOpen.plus(5, ChronoUnit.MINUTES).getEpochSecond() + ":R>").build());
            } else if (nowEpoch > darkAuctionOpen.plusSeconds(60).toEpochMilli() && Long.parseLong(times[5]) < darkAuctionOpen.toEpochMilli()) {
                // missed
                sendMessage(times, 5, defaultEmbed("Dark Auction").setDescription("Dark auction opens <t:" + darkAuctionOpen.plus(5, ChronoUnit.MINUTES).getEpochSecond() + ":R>").build());
            }

            Instant newYearEvent = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS + 28 * DAY_MS);
            if (newYearEvent.toEpochMilli() <= nowEpoch && nowEpoch <= newYearEvent.plusSeconds(60).toEpochMilli() && Long.parseLong(times[6]) < newYearEvent.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 6, defaultEmbed("New Year Celebration").setDescription("New year celebration starts <t:" + newYearEvent.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > newYearEvent.plusSeconds(60).toEpochMilli() && Long.parseLong(times[6]) < newYearEvent.toEpochMilli()) {
                // missed
                sendMessage(times, 6, defaultEmbed("New Year Celebration").setDescription("New year celebration starts <t:" + newYearEvent.getEpochSecond() + ":R>").build());
            }

            Instant spookyFishing = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS).minus(1, ChronoUnit.HOURS);
            if (spookyFishing.toEpochMilli() <= nowEpoch && nowEpoch <= spookyFishing.plusSeconds(60).toEpochMilli() && Long.parseLong(times[7]) < spookyFishing.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 7, defaultEmbed("Spooky Fishing").setDescription("Spooky fishing starts <t:" + spookyFishing.plus(1, ChronoUnit.HOURS).getEpochSecond() + ":R>").build());
            } else if (nowEpoch > spookyFishing.plusSeconds(60).toEpochMilli() && Long.parseLong(times[7]) < spookyFishing.toEpochMilli()) {
                // missed
                sendMessage(times, 7, defaultEmbed("Spooky Fishing").setDescription("Spooky fishing starts <t:" + spookyFishing.plus(1, ChronoUnit.HOURS).getEpochSecond() + ":R>").build());
            }

            Instant spookyEvent = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS);
            if (spookyEvent.toEpochMilli() <= nowEpoch && nowEpoch <= spookyEvent.plusSeconds(60).toEpochMilli() && Long.parseLong(times[8]) < spookyEvent.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 8, defaultEmbed("Spooky Event").setDescription("Spooky event starts <t:" + spookyEvent.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > spookyEvent.plusSeconds(60).toEpochMilli() && Long.parseLong(times[8]) < spookyEvent.toEpochMilli()) {
                // missed
                sendMessage(times, 8, defaultEmbed("Spooky Event").setDescription("Spooky event starts <t:" + spookyEvent.getEpochSecond() + ":R>").build());
            }

            if(currentMayor.equalsIgnoreCase("marina")) {
                Instant fishingFestival = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + Math.floorDiv((nowEpoch - YEAR_0) % YEAR_MS, MONTH_MS) * MONTH_MS);
                if (fishingFestival.toEpochMilli() <= nowEpoch && nowEpoch <= fishingFestival.plusSeconds(60).toEpochMilli() && Long.parseLong(times[9]) < fishingFestival.toEpochMilli()) {
                    // end < now < end + 1 min
                    sendMessage(times, 9, defaultEmbed("Fishing Festival").setDescription("Fishing festival starts <t:" + fishingFestival.getEpochSecond() + ":R>").build());
                } else if (nowEpoch > fishingFestival.plusSeconds(60).toEpochMilli() && Long.parseLong(times[9]) < fishingFestival.toEpochMilli()) {
                    // missed
                    sendMessage(times, 9, defaultEmbed("Fishing Festival").setDescription("Fishing festival starts <t:" + fishingFestival.getEpochSecond() + ":R>").build());
                }
            }

            long currentOffset = (nowEpoch - YEAR_0) % YEAR_MS;
            int currentMonth = (int) Math.floorDiv(currentOffset, MONTH_MS);
            int currentDay = (int) Math.floorDiv((currentOffset - (long) currentMonth * MONTH_MS) % MONTH_MS, DAY_MS);
            int out = 7;
            if(currentDay > 21){
                out = 28;
            }else if(currentDay > 14) {
                out = 21;
            }else if(currentDay > 7){
                out = 14;
            }
            Instant fallenStar = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + currentMonth * MONTH_MS + out * DAY_MS).minus(5, ChronoUnit.MINUTES);
            if (fallenStar.toEpochMilli() <= nowEpoch && nowEpoch <= fallenStar.plusSeconds(60).toEpochMilli() && Long.parseLong(times[10]) < fallenStar.toEpochMilli()) {
                // end < now < end + 1 min
                sendMessage(times, 10, defaultEmbed("Cult Of Fallen Star").setDescription("Cult of fallen start arrives <t:" + fallenStar.getEpochSecond() + ":R>").build());
            } else if (nowEpoch > fallenStar.plusSeconds(60).toEpochMilli() && Long.parseLong(times[10]) < fallenStar.toEpochMilli()) {
                // missed
                sendMessage(times, 10, defaultEmbed("Cult Of Fallen Star").setDescription("Cult of fallen start arrives <t:" + fallenStar.getEpochSecond() + ":R>").build());
            }

            jda.getGuildById("796790757947867156").getTextChannelById("959829695686381658").editMessageById(messageId, String.join("\n", times)).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(String[] times, int index, MessageEmbed embed){
        times[index] = "" + Instant.now().toEpochMilli();
        jda.getGuildById("796790757947867156").getTextChannelById("959887837728223232").sendMessageEmbeds(embed).queue();
    }
}
