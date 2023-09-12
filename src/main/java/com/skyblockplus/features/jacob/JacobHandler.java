/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

package com.skyblockplus.features.jacob;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.miscellaneous.CalendarSlashCommand.getSkyblockYear;
import static com.skyblockplus.utils.utils.HttpUtils.getJsonObject;
import static com.skyblockplus.utils.utils.JsonUtils.collectJsonArray;
import static com.skyblockplus.utils.utils.StringUtils.getRelativeTimestamp;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonObject;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class JacobHandler {

	private static JacobData jacobData = null;

	public static void initialize() {
		if (jacobData != null) {
			JacobContest nextContest = jacobData.getNextContest();
			if (nextContest != null) {
				scheduler.schedule(
					() -> {
						try {
							MessageEmbed embed = defaultEmbed("Jacob's Contest")
								.setDescription(
									"The next farming contest is starting " + getRelativeTimestamp(nextContest.getTimeInstant()) + "\n"
								)
								.addField("Crops", nextContest.getCropsFormatted(), false)
								.build();

							int updateCount = 0;
							for (AutomaticGuild guild : guildMap.values()) {
								if (guild.onFarmingContest(nextContest.getCrops(), embed)) {
									updateCount++;
								}

								if (updateCount != 0 && updateCount % 25 == 0) {
									try {
										TimeUnit.SECONDS.sleep(1);
									} catch (Exception ignored) {}
								}
							}

							initialize();
						} catch (Exception e) {
							e.printStackTrace();
						}
					},
					nextContest.getDurationUntil().minusMinutes(5).toMillis(),
					TimeUnit.MILLISECONDS
				);
				return;
			}
		}

		scheduler.schedule(
			() -> {
				if (jacobData == null || jacobData.getYear() != getSkyblockYear() || !jacobData.isComplete()) {
					setJacobDataFromApi();
				}
				initialize();
			},
			15,
			TimeUnit.MINUTES
		);
	}

	public static JacobData getJacobData() {
		return jacobData;
	}

	public static void setJacobData(JacobData jacobData) {
		JacobHandler.jacobData = jacobData;
	}

	public static void setJacobDataFromApi() {
		JsonObject rawJacobData = getJsonObject("https://api.elitebot.dev/Contests/at/" + getSkyblockYear());
		rawJacobData.add(
			"contests",
			collectJsonArray(
				rawJacobData
					.getAsJsonObject("contests")
					.entrySet()
					.stream()
					.map(e -> {
						JsonObject contest = new JsonObject();
						contest.addProperty("time", Long.parseLong(e.getKey()) * 1000);
						contest.add("crops", e.getValue());
						return contest;
					})
			)
		);

		setJacobData(gson.fromJson(rawJacobData, JacobData.class));
	}
}
