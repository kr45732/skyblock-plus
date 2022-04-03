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

package com.skyblockplus.features.event;

import static com.skyblockplus.features.mayor.MayorHandler.currentMayor;
import static com.skyblockplus.miscellaneous.TimeCommand.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class CalendarCommand extends Command {

	public CalendarCommand() {
		this.name = "calendar";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "cal" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCalendar() {
		EmbedBuilder eb = defaultEmbed("Calendar");

		ZoneId z = ZoneId.of("America/New_York");
		LocalDate now = LocalDate.now(z);
		ZonedDateTime nowDateTime = ZonedDateTime.now(z);
		Instant instantNow = Instant.now();
		long nowEpoch = instantNow.toEpochMilli();

		Instant bingoStart = now.withDayOfMonth(1).atStartOfDay(z).toInstant();
		Instant bingoEnd = bingoStart.plus(7, ChronoUnit.DAYS);
		if (bingoEnd.isBefore(instantNow)) {
			bingoStart = now.withMonth(now.getMonth().plus(1).getValue()).withDayOfMonth(1).atStartOfDay(z).toInstant();
			bingoEnd = bingoStart.plus(7, ChronoUnit.DAYS);
		}
		eb.addField(
			"Bingo",
			"\uD83C\uDFB2 **Start:** <t:" +
			bingoStart.getEpochSecond() +
			":R>" +
			"\n\uD83C\uDFB2 **End:** <t:" +
			bingoEnd.getEpochSecond() +
			":R>",
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
		String[] pets = new String[] { "LION;4", "MONKEY;4", "ELEPHANT;4", "GIRAFFE;4", "BLUE_WHALE;4", "TIGER;4" };
		int index = 0;
		if ((curYear - 1) % 3 == 0) {
			index = 2;
		} else if ((curYear - 2) % 3 == 0) {
			index = 4;
		}

		eb
			.addField(
				"Traveling Zoo",
				getEmoji(pets[summerZooStart.isBefore(winterZooStart) ? index : (index + 1)]) +
				" **Open:** <t:" +
				(summerZooStart.isBefore(winterZooStart) ? summerZooStart : winterZooStart).getEpochSecond() +
				":R>\n" +
				getEmoji(pets[summerZooStart.isBefore(winterZooStart) ? index : (index + 1)]) +
				" **Close:** <t:" +
				(summerZooStart.isBefore(winterZooStart) ? summerZooEnd : winterZooEnd).getEpochSecond() +
				":R>",
				false
			)
			.addField(
				"Winter Island",
				"\n❄️ **Open:** <t:" +
				Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS).getEpochSecond() +
				":R>" +
				"\n❄️ **Close:** <t:" +
				Instant.ofEpochMilli(YEAR_0 + getSkyblockYear() * YEAR_MS).getEpochSecond() +
				":R>",
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
			"Spooky Fishing",
			"\n\uD83D\uDC20 **Start:** <t:" +
			spookyFishingStart.getEpochSecond() +
			":R>" +
			"\n\uD83D\uDC20 **End:** <t:" +
			spookyFishingEnd.getEpochSecond() +
			":R>",
			false
		);

		Instant spookyStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS);
		Instant spookyEnd = spookyStart.plus(1, ChronoUnit.HOURS);
		if (spookyEnd.isBefore(instantNow)) {
			spookyStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS);
			spookyEnd = spookyStart.plus(1, ChronoUnit.HOURS);
		}
		eb.addField(
			"Spooky Festival",
			"\n\uD83C\uDF83 **Start:** <t:" +
			spookyStart.getEpochSecond() +
			":R>" +
			"\n\uD83C\uDF83 **End:** <t:" +
			spookyEnd.getEpochSecond() +
			":R>",
			false
		);

		Instant newYearStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS + 28 * DAY_MS);
		Instant newYearEnd = newYearStart.plus(1, ChronoUnit.HOURS);
		if (newYearEnd.isBefore(instantNow)) {
			newYearStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear()) * YEAR_MS + 11 * MONTH_MS + 28 * DAY_MS);
			newYearEnd = newYearStart.plus(1, ChronoUnit.HOURS);
		}
		eb.addField(
			"New Year Celebration",
			"\uD83C\uDF70 **Start:** <t:" +
			newYearStart.getEpochSecond() +
			":R>" +
			"\n\uD83C\uDF70 **End:** <t:" +
			newYearEnd.getEpochSecond() +
			":R>",
			false
		);

		if (currentMayor.equalsIgnoreCase("marina")) {
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
				"Fishing Festival",
				"\uD83C\uDFA3 **Start:** <t:" +
				fishingStart.getEpochSecond() +
				":R>" +
				"\n\uD83C\uDFA3 **End:** <t:" +
				fishingEnd.getEpochSecond() +
				":R>",
				false
			);
		}

		long currentOffset = (nowEpoch - YEAR_0) % YEAR_MS;
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
		Instant cultStart = Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + currentMonth * MONTH_MS + out * DAY_MS);
		if (cultStart.plus(5, ChronoUnit.MINUTES).isBefore(instantNow)) {
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
			":R>",
			false
		);

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(getCalendar());
			}
		}
			.queue();
	}
}
