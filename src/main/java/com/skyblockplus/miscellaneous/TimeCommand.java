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

package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;

import static com.google.common.base.Strings.padStart;
import static com.skyblockplus.utils.Utils.*;

public class TimeCommand extends Command {

    private static final String[] SEASONS = new String[]{
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
    private static final int HOUR_MS = 50000;
    private static final int DAY_MS = 24 * HOUR_MS;
    private static final int MONTH_MS = 31 * DAY_MS;
    private static final int YEAR_MS = SEASONS.length * MONTH_MS;
    private static final long YEAR_0 = 1560275700000L;

    public TimeCommand() {
        this.name = "time";
        this.cooldown = globalCooldown;
        this.botPermissions = defaultPerms();
    }

    public static EmbedBuilder getSkyblockTime() {
        long now = Instant.now().toEpochMilli();

        long currentOffset = (now - YEAR_0) % YEAR_MS;
        int currentMonth = (int) Math.floorDiv(currentOffset, MONTH_MS);
        long currentMonthOffset = (currentOffset - (long) currentMonth * MONTH_MS) % MONTH_MS;
        int currentDay = (int) Math.floorDiv(currentMonthOffset, DAY_MS);
        long currentDayOffset = (currentMonthOffset - (long) currentDay * DAY_MS) % DAY_MS;
        long currentHour = Math.floorDiv(currentDayOffset, HOUR_MS);
        long currentMinute = (long) Math.floor(((double) currentDayOffset - currentHour * HOUR_MS) / HOUR_MS * 60);

        String suffix = "am";
        if (currentHour >= 12) {
            suffix = "pm";
            currentHour -= 12;
        }
        if (currentHour == 0) {
            currentHour = 12;
        }

        return defaultEmbed("Skyblock Time")
                .addField("Year", "" + getSkyblockYear(), false)
                .addField("Date", SEASONS[currentMonth] + " **" + nth(currentDay + 1) + "**", false)
                .addField("Time", currentHour + ":" + padStart("" + ((int) Math.floorDiv(currentMinute, 10) * 10), 2, '0') + suffix, false);
    }

    public static int getSkyblockYear() {
        long now = Instant.now().toEpochMilli();
        long currentYear = Math.floorDiv((now - YEAR_0), YEAR_MS);
        return (int) (currentYear + 1);
    }

    public static String nth(int n) {
        try {
            return n + new String[]{"st", "nd", "rd"}[((n + 90) % 100 - 10) % 10 - 1];
        } catch (Exception e) {
            return n + "th";
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        new CommandExecute(this, event) {
            @Override
            protected void execute() {
                logCommand();

                embed(getSkyblockTime());
            }
        }
                .queue();
    }
}
