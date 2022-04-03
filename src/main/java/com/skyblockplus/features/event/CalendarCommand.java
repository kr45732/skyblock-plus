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
import static com.skyblockplus.miscellaneous.TimeCommand.MONTH_MS;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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
		long nowEpoch = Instant.now().toEpochMilli();
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
		int curYear = getSkyblockYear();
		String[] pets = new String[]{"LION;4", "MONKEY;4", "ELEPHANT;4", "GIRAFFE;4", "BLUE_WHALE;4", "TIGER;4"};
		int index = 0;
		if ((curYear - 1) % 3 == 0) {
			index = 2;
		} else if ((curYear - 2) % 3 == 0) {
			index = 4;
		}

		eb.setDescription(
				"\uD83C\uDFB2 **Bingo Start:** <t:" + now.withDayOfMonth(1).atStartOfDay(z).toInstant().getEpochSecond() + ":R>" +
						"\n\uD83C\uDFB2 **Bingo End:** <t:" + now.withDayOfMonth(1).atStartOfDay(z).toInstant().plus(7, ChronoUnit.DAYS).getEpochSecond() + ":R>" +
						"\n" + getEmoji(pets[index]) + " **Traveling Zoo (Summer):** <t:" + Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 3 * MONTH_MS).getEpochSecond() + ":R>"
						+ "\n" + getEmoji(pets[index + 1]) + " **Traveling Zoo (Winter):** <t:" + Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 9 * MONTH_MS).getEpochSecond() + ":R>"
						+ "\n❄️ **Winter Island:** <t:" + Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS).getEpochSecond() + ":R>"
						+ "\n\uD83D\uDD75️ **Dark Auction:** <t:" + nowDateTime.withMinute(55).toInstant().getEpochSecond() + ":R>"
						+ "\n\uD83C\uDF70 **New Year Celebration:** <t:" + Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 11 * MONTH_MS + 28 * DAY_MS).getEpochSecond() + ":R>"
						+ "\n\uD83D\uDC20 **Spooky Fishing:** <t:" + Instant
						.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS)
						.minus(1, ChronoUnit.HOURS).getEpochSecond() + ":R>"
						+ "\n\uD83C\uDF83 **Spooky Event:** <t:" + Instant.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + 7 * MONTH_MS + 28 * DAY_MS).getEpochSecond() + ":R>"
						+ (currentMayor.equalsIgnoreCase("marina") ?
						("\n\uD83C\uDFA3 **Fishing Festival:** <t:" + Instant.ofEpochMilli(
								YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + Math.floorDiv((nowEpoch - YEAR_0) % YEAR_MS, MONTH_MS) * MONTH_MS
						).getEpochSecond() + ":R>") : "")
						+ "\n⭐ **Cult Of Fallen Star:** <t:" + Instant
						.ofEpochMilli(YEAR_0 + (getSkyblockYear() - 1) * YEAR_MS + currentMonth * MONTH_MS + out * DAY_MS)
						.minus(5, ChronoUnit.MINUTES).getEpochSecond() + ":R>"
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
