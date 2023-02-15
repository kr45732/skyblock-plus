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

import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.apache.http.message.BasicHeader;

public final class TokenData {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresAt;
	private long lastMetadataUpdate = -1;
	private JsonObject body;

	public TokenData(JsonElement json) {
		refreshData(json);
	}

	public String accessToken() {
		return accessToken;
	}

	public String refreshToken() {
		return refreshToken;
	}

	public String tokenType() {
		return tokenType;
	}

	public void refreshData(JsonElement json) {
		this.accessToken = higherDepth(json, "access_token").getAsString();
		this.refreshToken = higherDepth(json, "refresh_token").getAsString();
		this.tokenType = higherDepth(json, "token_type").getAsString();
		this.expiresAt = Instant.now().plusSeconds(higherDepth(json, "expires_in").getAsLong()).toEpochMilli();
	}

	public boolean isExpired() {
		return Instant.ofEpochMilli(expiresAt).isBefore(Instant.now());
	}

	public boolean shouldUpdateMetadata() {
		return lastMetadataUpdate == -1 || Duration.between(Instant.ofEpochMilli(lastMetadataUpdate), Instant.now()).toMinutes() >= 5;
	}

	public long getMetadata(String key, long defaultValue) {
		return higherDepth(body, "metadata." + key, defaultValue);
	}

	public boolean updateMetadata(JsonObject body) {
		this.lastMetadataUpdate = Instant.now().toEpochMilli();
		this.body = body;
		JsonElement data = putJson(
			"https://discord.com/api/v10/users/@me/applications/" + selfUserId + "/role-connection",
			body,
			new BasicHeader("Authorization", "Bearer " + accessToken())
		);
		return higherDepth(data, "metadata") != null;
	}

	public static CompletableFuture<Boolean> updateLinkedRolesMetadata(
		String discord,
		LinkedAccount linkedAccount,
		Player.Profile player,
		boolean runCheck
	) {
		try {
			TokenData tokenData = oAuthClient.getToken(discord);
			if (tokenData == null) {
				return CompletableFuture.completedFuture(false);
			}

			if (runCheck && !tokenData.shouldUpdateMetadata()) {
				return CompletableFuture.completedFuture(false);
			}

			JsonObject body = new JsonObject();
			JsonObject metadata = new JsonObject();
			if (linkedAccount != null) {
				String platformUsername = linkedAccount.username();

				if (player != null && player.isValid()) {
					platformUsername += " | " + player.getProfileName();
					metadata.addProperty("level", (long) Math.max(1, player.getLevel()));
					metadata.addProperty("networth", (long) Math.max(1, player.getNetworth()));
					metadata.addProperty("weight", (long) Math.max(1, player.getWeight()));
					metadata.addProperty("lily_weight", (long) Math.max(1, player.getLilyWeight()));
				} else {
					metadata.addProperty("level", tokenData.getMetadata("level", 1));
					metadata.addProperty("networth", tokenData.getMetadata("networth", 1));
					metadata.addProperty("weight", tokenData.getMetadata("weight", 1));
					metadata.addProperty("lily_weight", tokenData.getMetadata("lily_weight", 1));
				}

				metadata.addProperty("verified", 1);
				body.addProperty("platform_name", "Skyblock Plus");
				body.addProperty("platform_username", platformUsername);
			} else {
				metadata.addProperty("verified", 0);
			}
			body.add("metadata", metadata);

			// Don't update if same as the old metadata
			if (tokenData.body != null && tokenData.body.equals(body)) {
				return CompletableFuture.completedFuture(true);
			}

			return CompletableFuture.supplyAsync(() -> tokenData.updateMetadata(body), executor);
		} catch (Exception e) {
			e.printStackTrace();
			return CompletableFuture.completedFuture(false);
		}
	}
}
