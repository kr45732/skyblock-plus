/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
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

package com.skyblockplus.utils.oauth;

import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import java.time.Instant;

public record TokenData(String accessToken, String refreshToken, String tokenType, Instant expiresAt) {
	public static TokenData from(JsonElement json) {
		String accessToken = higherDepth(json, "access_token").getAsString();
		String refreshToken = higherDepth(json, "refresh_token").getAsString();
		String tokenType = higherDepth(json, "token_type").getAsString();
		Instant expiresAt = Instant.now().plusSeconds(higherDepth(json, "expires_in").getAsLong());
		return new TokenData(accessToken, refreshToken, tokenType, expiresAt);
	}
	public boolean isExpired() {
		return expiresAt().isBefore(Instant.now());
	}
}
