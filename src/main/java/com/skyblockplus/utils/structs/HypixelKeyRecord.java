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

package com.skyblockplus.utils.structs;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.ToString;

@ToString
public final class HypixelKeyRecord {

	private final AtomicInteger remainingLimit;
	private final AtomicInteger timeTillReset;
	private final Instant time;

	public HypixelKeyRecord(AtomicInteger remainingLimit, AtomicInteger timeTillReset) {
		this.remainingLimit = remainingLimit;
		this.timeTillReset = timeTillReset;
		this.time = Instant.now();
	}

	public boolean isRateLimited() {
		return remainingLimit.get() < 5 && timeTillReset.get() > 0 && time.plusSeconds(timeTillReset.get()).isAfter(Instant.now());
	}

	public AtomicInteger remainingLimit() {
		return remainingLimit;
	}

	public AtomicInteger timeTillReset() {
		return timeTillReset;
	}

	public long getTimeTillReset() {
		return Math.max(0, Duration.between(Instant.now(), time.plusSeconds(timeTillReset.get())).getSeconds());
	}
}
