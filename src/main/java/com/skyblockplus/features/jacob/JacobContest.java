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

import static com.skyblockplus.utils.Constants.cropNameToEmoji;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class JacobContest {

	private long time;
	private List<String> crops;

	public boolean reminderHasPassed() {
		return Instant.now().isAfter(getTimeInstant().minusSeconds(301));
	}

	public Instant getTimeInstant() {
		return Instant.ofEpochMilli(time);
	}

	public Duration getDurationUntil() {
		return Duration.between(Instant.now(), getTimeInstant());
	}

	public String getCropsFormatted() {
		return getCropsFormatted(true);
	}

	public String getCropsFormatted(boolean arrows) {
		StringBuilder cropsFormatted = new StringBuilder();
		for (String crop : crops) {
			cropsFormatted.append(arrows ? "➜ " : "").append(cropNameToEmoji.get(crop)).append(" ").append(crop).append("\n");
		}
		return cropsFormatted.toString();
	}
}
