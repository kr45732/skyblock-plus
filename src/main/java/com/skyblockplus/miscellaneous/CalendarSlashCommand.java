/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.features.mayor.MayorHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class CalendarSlashCommand extends SlashCommand {

	private static final String[] SEASONS = new String[] {
		"Early Spring",
		"Spring",
		"Late Spring",
		"Early Summer",
		"Summer",
		"Late Summer",
		"Early Autumn",
		"Autumn",
		"Late Autumn",
		"Early Winter",
		"Winter",
		"Late Winter",
	};
	private static final long HOUR_MS = 50000;
	public static final long DAY_MS = 24 * HOUR_MS;
	public static final long MONTH_MS = 31 * DAY_MS;
	public static final long YEAR_MS = SEASONS.length * MONTH_MS;
	public static final long YEAR_0 = 1560275700000L;

	public CalendarSlashCommand() {
		this.name = "calendar";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getCalendar());
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get current and upcoming Skyblock events");
	}

	public static EmbedBuilder getCalendar() {
		EmbedBuilder eb = defaultEmbed("Calendar");

		ZoneId z = ZoneId.of("America/New_York");
		LocalDate now = LocalDate.now(z);
		ZonedDateTime nowDateTime = ZonedDateTime.now(z);
		Instant instantNow = Instant.now();
		long nowEpoch = instantNow.toEpochMilli();

		long currentOffset = (nowEpoch - YEAR_0) % YEAR_MS;
		int currentMonthTime = (int) Math.floorDiv(currentOffset, MONTH_MS);
		long currentMonthOffsetTime = (currentOffset - (long) currentMonthTime * MONTH_MS) % MONTH_MS;
		int currentDayTime = (int) Math.floorDiv(currentMonthOffsetTime, DAY_MS);
		long currentDayOffsetTime = (currentMonthOffsetTime - (long) currentDayTime * DAY_MS) % DAY_MS;
		long currentHourTime = Math.floorDiv(currentDayOffsetTime, HOUR_MS);
		long currentMinuteTime = (long) Math.floor(((double) currentDayOffsetTime - currentHourTime * HOUR_MS) / HOUR_MS * 60);

		String suffix = "am";
		if (currentHourTime >= 12) {
			suffix = "pm";
			currentHourTime -= 12;
		}
		if (currentHourTime == 0) {
			currentHourTime = 12;
		}

		eb.setDescription(
			"**Year:** " +
			getSkyblockYear() +
			"\n**Date:** " +
			SEASONS[currentMonthTime] +
			" " +
			nth(currentDayTime + 1) +
			"\n**Time:** " +
			currentHourTime +
			":" +
			padStart("" + ((int) Math.floorDiv(currentMinuteTime, 10) * 10), 2, '0') +
			suffix
		);

		Instant bingoStart = now.withDayOfMonth(1).atStartOfDay(z).toInstant();
		Instant bingoEnd = bingoStart.plus(7, ChronoUnit.DAYS);
		if (bingoEnd.isBefore(instantNow)) {
			bingoStart = now.withMonth(now.getMonth().plus(1).getValue()).withDayOfMonth(1).atStartOfDay(z).toInstant();
			bingoEnd = bingoStart.plus(7, ChronoUnit.DAYS);
		}
		eb.addField(
			"\uD83C\uDFB2 Bingo" + (bingoStart.isBefore(instantNow) ? " (Active)" : ""),
			"**Start:** <t:" + bingoStart.getEpochSecond() + ":R>" + "\n**End:** <t:" + bingoEnd.getEpochSecond() + ":R>",
			false
		);

		int curYearSummer = getSkyblockYear();
		int curYearWinter = getSkyblockYear();
		Instant summerZooStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 3 * MONTH_MS);
		Instant summerZooEnd = summerZooStart.plus(1, ChronoUnit.HOURS);
		if (summerZooEnd.isBefore(instantNow)) {
			summerZooStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 3 * MONTH_MS);
			summerZooEnd = summerZooStart.plus(1, ChronoUnit.HOURS);
			curYearSummer++;
		}
		Instant winterZooStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 9 * MONTH_MS);
		Instant winterZooEnd = winterZooStart.plus(1, ChronoUnit.HOURS);
		if (winterZooEnd.isBefore(instantNow)) {
			winterZooStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 9 * MONTH_MS);
			winterZooEnd = winterZooStart.plus(1, ChronoUnit.HOURS);
			curYearWinter++;
		}
		int curYear = summerZooStart.isBefore(winterZooStart) ? curYearSummer : curYearWinter;
		String[] pets = new String[] { "ELEPHANT;4", "GIRAFFE;4", "BLUE_WHALE;4", "TIGER;4", "LION;4", "MONKEY;4" };
		int index = 0;
		if ((curYear - 1) % 3 == 0) {
			index = 2;
		} else if ((curYear - 2) % 3 == 0) {
			index = 4;
		}

		eb.addField(
			getEmoji(pets[summerZooStart.isBefore(winterZooStart) ? index : (index + 1)]) +
			" Traveling Zoo" +
			((summerZooStart.isBefore(winterZooStart) ? summerZooStart : winterZooStart).isBefore(instantNow) ? " (Active)" : ""),
			"**Open:** <t:" +
			(summerZooStart.isBefore(winterZooStart) ? summerZooStart : winterZooStart).getEpochSecond() +
			":R>\n**Close:** <t:" +
			(summerZooStart.isBefore(winterZooStart) ? summerZooEnd : winterZooEnd).getEpochSecond() +
			":R>",
			false
		);

		Instant winterOpen = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS);
		Instant winterClose = Instant.ofEpochMilli(YEAR_0 + getSkyblockYear() * YEAR_MS);
		eb.addField(
			"❄️ Winter Island" + (winterOpen.isBefore(instantNow) ? " (Active)" : ""),
			"\n**Open:** <t:" + winterOpen.getEpochSecond() + ":R>" + "\n**Close:** <t:" + winterClose.getEpochSecond() + ":R>",
			false
		);

		Instant jerryOpen = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS + 23 * DAY_MS);
		Instant jerryClose = jerryOpen.plus(1, ChronoUnit.HOURS);
		if (bingoEnd.isBefore(instantNow)) {
			jerryOpen = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 11 * MONTH_MS + 23 * DAY_MS);
			jerryClose = jerryOpen.plus(1, ChronoUnit.HOURS);
		}
		eb.addField(
			"<:jerry:940083649318125578> Defend Jerry's Workshop" + (jerryOpen.isBefore(instantNow) ? " (Active)" : ""),
			"\n**Start:** <t:" + jerryOpen.getEpochSecond() + ":R>" + "\n**End:** <t:" + jerryClose.getEpochSecond() + ":R>",
			false
		);

		Instant spookyFishingStart = Instant
			.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS)
			.minus(1, ChronoUnit.HOURS);
		Instant spookyFishingEnd = spookyFishingStart.plus(2, ChronoUnit.HOURS);
		if (spookyFishingEnd.isBefore(instantNow)) {
			spookyFishingStart =
				Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS).minus(1, ChronoUnit.HOURS);
			spookyFishingEnd = spookyFishingEnd.plus(7, ChronoUnit.DAYS);
		}
		eb.addField(
			"\uD83D\uDC20 Spooky Fishing" + (spookyFishingStart.isBefore(instantNow) ? " (Active)" : ""),
			"\n**Start:** <t:" + spookyFishingStart.getEpochSecond() + ":R>" + "\n**End:** <t:" + spookyFishingEnd.getEpochSecond() + ":R>",
			false
		);

		Instant spookyStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS);
		Instant spookyEnd = spookyStart.plus(1, ChronoUnit.HOURS);
		if (spookyEnd.isBefore(instantNow)) {
			spookyStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS);
			spookyEnd = spookyStart.plus(1, ChronoUnit.HOURS);
		}
		eb.addField(
			"\uD83C\uDF83 Spooky Festival" + (spookyStart.isBefore(instantNow) ? " (Active)" : ""),
			"\n**Start:** <t:" + spookyStart.getEpochSecond() + ":R>" + "\n**End:** <t:" + spookyEnd.getEpochSecond() + ":R>",
			false
		);

		Instant newYearStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS + 28 * DAY_MS);
		Instant newYearEnd = newYearStart.plus(1, ChronoUnit.HOURS);
		if (newYearEnd.isBefore(instantNow)) {
			newYearStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 11 * MONTH_MS + 28 * DAY_MS);
			newYearEnd = newYearStart.plus(1, ChronoUnit.HOURS);
		}
		eb.addField(
			"\uD83C\uDF70 New Year Celebration" + (newYearStart.isBefore(instantNow) ? " (Active)" : ""),
			"**Start:** <t:" + newYearStart.getEpochSecond() + ":R>" + "" + "\n**End:** <t:" + newYearEnd.getEpochSecond() + ":R>",
			false
		);

		if (
			currentMayor.equalsIgnoreCase("marina") ||
			(currentMayor.equalsIgnoreCase("Jerry") && currentJerryMayor.equalsIgnoreCase("marina"))
		) {
			Instant fishingStart = Instant.ofEpochMilli(
				YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + Math.floorDiv((nowEpoch - YEAR_0) % YEAR_MS, MONTH_MS) * MONTH_MS
			);
			Instant fishingEnd = fishingStart.plus(1, ChronoUnit.HOURS);
			if (fishingEnd.isBefore(instantNow)) {
				int curMonth = (int) Math.floorDiv((nowEpoch - YEAR_0) % YEAR_MS, MONTH_MS);
				int curYearFish = getSkyblockYear() - 1;
				if (curMonth == 12) {
					curMonth = 1;
					curYearFish++;
				} else {
					curMonth++;
				}
				fishingStart = Instant.ofEpochMilli(YEAR_0 + (curYearFish) * YEAR_MS + curMonth * MONTH_MS);
				fishingEnd = fishingStart.plus(1, ChronoUnit.HOURS);
			}

			eb.addField(
				"\uD83C\uDFA3 Fishing Festival" + (fishingStart.isBefore(instantNow) ? " (Active)" : ""),
				"**Start:** <t:" + fishingStart.getEpochSecond() + ":R>" + "\n**End:** <t:" + fishingEnd.getEpochSecond() + ":R>",
				false
			);
		}

		if (currentMayor.equalsIgnoreCase("cole")) {
			int curYearMayor = currentMayorYear;
			// TODO: mining fiesta https://wiki.hypixel.net/Mining_Fiesta
		}

		int currentMonth = (int) Math.floorDiv(currentOffset, MONTH_MS);
		int currentDay = (int) Math.floorDiv((currentOffset - (long) currentMonth * MONTH_MS) % MONTH_MS, DAY_MS);
		int out = 7;
		if (currentDay > 21) {
			out = 28;
		} else if (currentDay > 14) {
			out = 21;
		} else if (currentDay > 7) {
			out = 14;
		}
		out--;
		Instant cultStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + currentMonth * MONTH_MS + out * DAY_MS);
		if (cultStart.isBefore(instantNow)) {
			int curYearCult = getSkyblockYear() - 1;
			if (out == 28) {
				out = 7;
				if (currentMonth == 12) {
					currentMonth = 1;
					curYearCult++;
				} else {
					currentMonth++;
				}
			} else {
				out += 7;
			}
			cultStart = Instant.ofEpochMilli(YEAR_0 + (curYearCult) * YEAR_MS + currentMonth * MONTH_MS + out * DAY_MS);
		}

		int currentMonthBank = (int) Math.floorDiv(currentOffset, MONTH_MS);
		if (currentMonthBank <= 2) {
			currentMonthBank = 2;
		} else if (currentMonthBank <= 5) {
			currentMonthBank = 5;
		} else if (currentMonthBank <= 8) {
			currentMonthBank = 8;
		} else {
			currentMonthBank = 11;
		}
		Instant bankStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + currentMonthBank * MONTH_MS + 31 * DAY_MS);
		if (bankStart.isBefore(instantNow)) {
			bankStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 2 * MONTH_MS + 31 * DAY_MS);
		}

		eb.addField(
			"Miscellaneous",
			"\uD83D\uDD75️ **Dark Auction:** <t:" +
			(
				nowDateTime.withMinute(55).toInstant().isBefore(instantNow)
					? nowDateTime.withMinute(55).plusHours(1).toInstant().getEpochSecond()
					: nowDateTime.withMinute(55).toInstant().getEpochSecond()
			) +
			":R>" +
			"\n⭐ **Cult Of Fallen Star:** <t:" +
			cultStart.getEpochSecond() +
			":R>" +
			"\n\uD83E\uDE99 **Bank Interest:** <t:" +
			bankStart.getEpochSecond() +
			":R>",
			false
		);

		return eb;
	}

	public static int getSkyblockYear() {
		long now = Instant.now().toEpochMilli();
		long currentYear = Math.floorDiv((now - YEAR_0), YEAR_MS);
		return (int) (currentYear + 1);
	}

	private static String nth(int n) {
		try {
			return n + new String[] { "st", "nd", "rd" }[((n + 90) % 100 - 10) % 10 - 1];
		} catch (Exception e) {
			return n + "th";
		}
	}
}
