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
import static com.skyblockplus.utils.Utils.httpClient;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class OAuthClient {

	private final String clientId;
	private final String clientSecret;
	private final Map<String, String> states = new HashMap<>();
	private final Map<String, TokenData> discordToToken = new HashMap<>();

	public OAuthClient(String clientId, String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public String generateState(String redirectUri) {
		String state = UUID.randomUUID().toString();
		states.put(state, redirectUri);
		return state;
	}

	public String consumeState(String state) {
		return states.remove(state);
	}

	public TokenData postToken(String code, String redirectUri) throws IOException {
		HttpPost httpPost = new HttpPost("https://discord.com/api/oauth2/token");

		List<NameValuePair> nameValuePairs = new ArrayList<>();
		nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
		nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
		nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
		nameValuePairs.add(new BasicNameValuePair("code", code));
		nameValuePairs.add(new BasicNameValuePair("redirect_uri", redirectUri));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);
		httpPost.setEntity(entity);
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

		try (
			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
			InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
		) {
			return TokenData.from(JsonParser.parseReader(in));
		}
	}

	public TokenData getToken(String discord) throws IOException {
		TokenData tokenData = discordToToken.get(discord);
		if (tokenData != null && tokenData.isExpired()) {
			HttpPost httpPost = new HttpPost("https://discord.com/api/oauth2/token");

			List<NameValuePair> nameValuePairs = new ArrayList<>();
			nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
			nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
			nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
			nameValuePairs.add(new BasicNameValuePair("refresh_token", tokenData.refreshToken()));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);
			httpPost.setEntity(entity);
			httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				tokenData = TokenData.from(JsonParser.parseReader(in));
				discordToToken.put(discord, tokenData);
			}
		}
		return tokenData;
	}

	public String getDiscord(TokenData tokenData) throws IOException {
		HttpGet httpGet = new HttpGet("https://discord.com/api/users/@me");
		httpGet.setHeader("Authorization", tokenData.tokenType() + " " + tokenData.accessToken());

		try (
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
		) {
			String discord = "" + higherDepth(JsonParser.parseReader(in), "id").getAsLong();
			discordToToken.put(discord, tokenData);
			return discord;
		}
	}
}
