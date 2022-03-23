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

package com.skyblockplus.features.jacob;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.apache.groovy.util.Maps;

@Data
public class JacobContest {

	public static final Map<String, String> CROP_NAME_TO_EMOJI = Maps.of(
		"Wheat",
		"<:wheat:939024495581224981>",
		"Carrot",
		"<:carrot:939020943332868167>",
		"Potato",
		"<:potato:939021823780216833>",
		"Pumpkin",
		"<:pumpkin:939021844202266696>",
		"Melon",
		"<:melon:939021703785381888>",
		"Mushroom",
		"<:mushroom:939020905747734558>",
		"Cactus",
		"<:cactus:939020908046209044>",
		"Sugar Cane",
		"<:sugar_canes:939024466007162920>",
		"Nether Wart",
		"<:nether_wart:939021735594950678>",
		"Cocoa Beans",
		"<:cocoa_beans:939021533379170336>"
	);

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
		StringBuilder cropsFormatted = new StringBuilder();
		for (String crop : crops) {
			cropsFormatted.append("➜ ").append(CROP_NAME_TO_EMOJI.get(crop)).append(" ").append(crop).append("\n");
		}
		return cropsFormatted.toString();
	}
}
