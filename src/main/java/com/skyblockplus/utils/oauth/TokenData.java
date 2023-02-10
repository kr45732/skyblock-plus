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
	private Instant expiresAt;
	private Instant lastMetadataUpdate;
	private JsonObject metaData;

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

	public Instant expiresAt() {
		return expiresAt;
	}

	public void refreshData(JsonElement json) {
		this.accessToken = higherDepth(json, "access_token").getAsString();
		this.refreshToken = higherDepth(json, "refresh_token").getAsString();
		this.tokenType = higherDepth(json, "token_type").getAsString();
		this.expiresAt = Instant.now().plusSeconds(higherDepth(json, "expires_in").getAsLong());
	}

	public boolean isExpired() {
		return expiresAt().isBefore(Instant.now());
	}

	public boolean canUpdateMetadata() {
		return lastMetadataUpdate == null || Duration.between(lastMetadataUpdate, Instant.now()).toMinutes() >= 5;
	}

	public boolean updateMetaData(JsonObject body) {
		lastMetadataUpdate = Instant.now();
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
		Player player,
		boolean runCheck
	) {
		try {
			TokenData tokenData = oAuthClient.getToken(discord);
			if (tokenData == null) {
				return CompletableFuture.completedFuture(false);
			}

			if (runCheck && !tokenData.canUpdateMetadata()) {
				return CompletableFuture.completedFuture(false);
			}

			JsonObject body = new JsonObject();
			JsonObject metadata = new JsonObject();
			if (linkedAccount != null) {
				String platformUsername = linkedAccount.username();

				metadata.addProperty("verified", 1);
				if (player != null) {
					platformUsername += " | " + player.getProfileName();
					metadata.addProperty("level", (long) Math.max(0, player.getLevel()));
					metadata.addProperty("networth", (long) Math.max(0, player.getNetworth()));
					metadata.addProperty("weight", (long) Math.max(0, player.getWeight()));
					metadata.addProperty("lily_weight", (long) Math.max(0, player.getLilyWeight()));

					tokenData.setMetaData(metadata);
				} else if (tokenData.metaData != null) {
					metadata.add("level", tokenData.metaData.get("level"));
					metadata.add("networth", tokenData.metaData.get("networth"));
					metadata.add("weight", tokenData.metaData.get("weight"));
					metadata.add("lily_weight", tokenData.metaData.get("lily_weight"));
				} else {
					metadata.addProperty("level", 1);
					metadata.addProperty("networth", 1);
					metadata.addProperty("weight", 1);
					metadata.addProperty("lily_weight", 1);
				}

				body.addProperty("platform_name", "Skyblock Plus");
				body.addProperty("platform_username", platformUsername);
			} else {
				metadata.addProperty("verified", 0);
			}
			body.add("metadata", metadata);

			return CompletableFuture.supplyAsync(() -> tokenData.updateMetaData(body), executor);
		} catch (Exception e) {
			e.printStackTrace();
			return CompletableFuture.completedFuture(false);
		}
	}

	public void setMetaData(JsonObject metaData) {
		this.metaData = metaData;
	}
}
