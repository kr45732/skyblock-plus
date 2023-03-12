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

package com.skyblockplus.utils.utils;

import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.skyblockplus.utils.SkyblockProfilesParser;
import com.skyblockplus.utils.exceptionhandler.ExceptionExecutor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import org.apache.http.Header;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

	public static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	public static final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
	private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
	private static final HttpClient asyncHttpClient = HttpClient
		.newBuilder()
		.executor(new ExceptionExecutor(20, 20, 45L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()).setAllowCoreThreadTimeOut(true))
		.build();

	public static String getUrl(String url) {
		try {
			HttpGet httpGet = new HttpGet(url);
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent());
				BufferedReader buff = new BufferedReader(in)
			) {
				return buff.lines().parallel().collect(Collectors.joining("\n"));
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement getJson(String jsonUrl) {
		return getJson(jsonUrl, HYPIXEL_API_KEY);
	}

	public static JsonElement getJson(String jsonUrl, String hypixelApiKey) {
		return getJson(jsonUrl, hypixelApiKey, false);
	}

	public static JsonElement getJson(String jsonUrl, String hypixelApiKey, boolean isSkyblockProfiles) {
		boolean isMain = hypixelApiKey.equals(HYPIXEL_API_KEY);
		try {
			if (
				jsonUrl.contains(hypixelApiKey) && (isMain ? remainingLimit.get() < 5 : keyCooldownMap.get(hypixelApiKey).isRateLimited())
			) {
				long timeTillResetInt = isMain ? timeTillReset.get() : keyCooldownMap.get(hypixelApiKey).getTimeTillReset();
				log.info("Sleeping for " + timeTillResetInt + " seconds (" + isMain + ")");
				TimeUnit.SECONDS.sleep(timeTillResetInt);
			}
		} catch (Exception ignored) {}

		try {
			HttpGet httpGet = new HttpGet(jsonUrl);
			if (jsonUrl.contains("raw.githubusercontent.com")) {
				httpGet.setHeader("Authorization", "token " + GITHUB_TOKEN);
			}
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				if (jsonUrl.contains("api.hypixel.net")) {
					if (jsonUrl.contains(hypixelApiKey)) {
						try {
							(isMain ? remainingLimit : keyCooldownMap.get(hypixelApiKey).remainingLimit()).set(
									Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Remaining").getValue())
								);
							(isMain ? timeTillReset : keyCooldownMap.get(hypixelApiKey).timeTillReset()).set(
									Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Reset").getValue())
								);
						} catch (Exception ignored) {}
					}

					if (httpResponse.getStatusLine().getStatusCode() == 502) {
						JsonObject obj = new JsonObject();
						obj.addProperty("cause", "Hypixel API returned 502 bad gateway. The API may be down.");
						return obj;
					} else if (httpResponse.getStatusLine().getStatusCode() == 522) {
						JsonObject obj = new JsonObject();
						obj.addProperty("cause", "Hypixel API returned 522 connection timed out. The API may be down.");
						return obj;
					}
				}

				try (
					InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent());
					JsonReader jsonIn = new JsonReader(in)
				) {
					return isSkyblockProfiles ? SkyblockProfilesParser.parse(jsonIn) : JsonParser.parseReader(jsonIn);
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonObject getJsonObject(String url) {
		try {
			return getJson(url).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}

	public static CompletableFuture<HttpResponse<InputStream>> asyncGet(String url) {
		return asyncHttpClient.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofInputStream());
	}

	public static CompletableFuture<JsonElement> asyncGetJson(String url) {
		return asyncGet(url)
			.thenApplyAsync(
				r -> {
					try (InputStreamReader in = new InputStreamReader(r.body())) {
						return JsonParser.parseReader(in);
					} catch (Exception e) {
						return null;
					}
				},
				executor
			);
	}

	public static JsonElement postUrl(String url, Object body) {
		try {
			HttpPost httpPost = new HttpPost(url);

			StringEntity entity = new StringEntity(body.toString(), "UTF-8");
			httpPost.setEntity(entity);

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in);
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement postJson(String url, JsonElement body, Header... headers) {
		try {
			HttpPost httpPost = new HttpPost(url);

			StringEntity entity = new StringEntity(body.toString(), "UTF-8");
			httpPost.setEntity(entity);
			httpPost.setHeaders(headers);
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setHeader("Accept", "application/json");

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in);
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement putJson(String url, JsonElement body, Header... headers) {
		try {
			HttpPut httpPut = new HttpPut(url);

			StringEntity entity = new StringEntity(body.toString(), "UTF-8");
			httpPut.setEntity(entity);
			httpPut.setHeaders(headers);
			httpPut.setHeader("Content-Type", "application/json");
			httpPut.setHeader("Accept", "application/json");

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpPut);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in);
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static JsonElement deleteUrl(String url, Header... headers) {
		try {
			HttpDelete httpDelete = new HttpDelete(url);

			httpDelete.setHeader("Content-Type", "application/json");
			httpDelete.setHeader("Accept", "application/json");
			httpDelete.setHeaders(headers);

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpDelete);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in);
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static void closeHttpClient() {
		try {
			log.info("Closing Http Client");
			httpClient.close();
			log.info("Successfully Closed Http Client");
		} catch (Exception e) {
			log.error("", e);
		}
	}
}
