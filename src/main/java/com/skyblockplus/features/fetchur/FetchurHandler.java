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

package com.skyblockplus.features.fetchur;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.scheduler;

import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.miscellaneous.FetchurCommand;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class FetchurHandler {

	public static void initialize() {
		ZoneId z = ZoneId.of("America/New_York");
		ZonedDateTime now = ZonedDateTime.now(z);
		LocalDate tomorrow = now.toLocalDate().plusDays(1);

		scheduler.scheduleAtFixedRate(
			() -> {
				MessageEmbed embed = FetchurCommand.getFetchurItem().build();

				int updateCount = 0;
				for (AutomaticGuild guild : guildMap.values()) {
					if(guild.onFetchur(embed)){
						updateCount ++;
					}

					if(updateCount != 0 && updateCount % 25 == 0){
						try {
							TimeUnit.SECONDS.sleep(1);
						} catch (Exception ignored) {
						}
					}
				}
			},
			Duration.between(now, tomorrow.atStartOfDay(z)).plusMillis(1).toMillis(),
			TimeUnit.DAYS.toMillis(1),
			TimeUnit.MILLISECONDS
		);
	}
}
