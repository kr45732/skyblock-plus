/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.utils.HttpUtils.asyncPutJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;

public final class TokenData {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresAt;
	private long lastMetadataUpdate = -1;

	@Getter
	private JsonObject body;

	public TokenData(JsonElement json) {
		refreshData(json);
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

			// Deep copy otherwise it will modify the cached body too
			JsonObject body = tokenData.getBody() != null ? tokenData.getBody().deepCopy() : new JsonObject();
			JsonObject metadata = body.has("metadata") ? body.getAsJsonObject("metadata") : new JsonObject();
			if (linkedAccount != null) {
				String platformUsername = linkedAccount.username();

				if (player != null && player.isValid()) {
					platformUsername += " | " + player.getProfileName();

					long level = (long) player.getLevel();
					if (level > 0) {
						metadata.addProperty("level", level);
					}

					long networth = (long) player.getNetworth();
					if (networth > 0) {
						metadata.addProperty("networth", networth);
					}

					if (player.isSkillsApiEnabled()) {
						long weight = (long) player.getWeight();
						if (weight > 0) {
							metadata.addProperty("weight", weight);
						}

						long lilyWeight = (long) player.getLilyWeight();
						if (lilyWeight > 0) {
							metadata.addProperty("lily_weight", lilyWeight);
						}
					}
				} else {
					// Append the profile name if it exists
					String platformUsernameCached = higherDepth(body, "platform_username", null);
					if (platformUsernameCached != null && platformUsernameCached.contains(" | ")) {
						platformUsername += " | " + platformUsernameCached.split(" \\| ")[1];
					}
				}

				metadata.addProperty("verified", 1);
				body.addProperty("platform_name", "Skyblock Plus");
				body.addProperty("platform_username", platformUsername);
			} else {
				metadata.addProperty("verified", 0);
			}
			body.add("metadata", metadata);

			tokenData.refreshLastMetadataUpdate();

			// Don't update if same as the old body
			if (tokenData.getBody() != null && tokenData.getBody().equals(body)) {
				return CompletableFuture.completedFuture(true);
			}

			return tokenData.updateMetadata(body);
		} catch (Exception e) {
			e.printStackTrace();
			return CompletableFuture.completedFuture(false);
		}
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

	public boolean refreshData(JsonElement json) {
		try {
			this.accessToken = higherDepth(json, "access_token").getAsString();
			this.refreshToken = higherDepth(json, "refresh_token").getAsString();
			this.tokenType = higherDepth(json, "token_type").getAsString();
			this.expiresAt = Instant.now().plusSeconds(higherDepth(json, "expires_in").getAsLong()).toEpochMilli();
			return true;
		} catch (Exception e) {
			return false; // Authorization revoked
		}
	}

	public boolean isExpired() {
		return Instant.ofEpochMilli(expiresAt).isBefore(Instant.now());
	}

	public boolean shouldUpdateMetadata() {
		return lastMetadataUpdate == -1 || Duration.between(Instant.ofEpochMilli(lastMetadataUpdate), Instant.now()).toMinutes() >= 5;
	}

	public void refreshLastMetadataUpdate() {
		this.lastMetadataUpdate = Instant.now().toEpochMilli();
	}

	public CompletableFuture<Boolean> updateMetadata(JsonObject body) {
		this.body = body;
		return asyncPutJson(
			"https://discord.com/api/v10/users/@me/applications/" + BOT_ID + "/role-connection",
			body,
			"Content-Type",
			"application/json",
			"Accept",
			"application/json",
			"Authorization",
			"Bearer " + accessToken()
		)
			.thenApplyAsync(
				r -> {
					try (InputStreamReader in = new InputStreamReader(r.body())) {
						return higherDepth(JsonParser.parseReader(in), "metadata") != null;
					} catch (Exception e) {
						return false;
					}
				},
				executor
			);
	}
}
