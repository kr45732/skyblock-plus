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

package com.skyblockplus.utils;

import static com.skyblockplus.utils.ApiHandler.getQueryApiUrl;
import static com.skyblockplus.utils.Utils.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

public class AuctionFlipper {

	private static final JDAWebhookClient flipperWebhook = new WebhookClientBuilder(
		isMainBot()
			? "https://discord.com/api/webhooks/917160844247334933/WKeMowhugO5-xbLlD8TakRfCskt7D5Sm7giMY8LfN2MzKjxsDUm9Y2yPw61_yzQTgcII"
			: "https://discord.com/api/webhooks/917959010622255144/ljWuFDr73A_PfyBBUUQWUE17nlFPFhbe3TUP-MxaIzlp_o-jYojrWRAF-hQGYxaxcZfM"
	)
		.setExecutorService(scheduler)
		.setHttpClient(okHttpClient)
		.buildJDA();
	private static final Cache<String, String> auctionUuidToMessage = Caffeine.newBuilder().expireAfterWrite(45, TimeUnit.MINUTES).build();
	private static boolean enable = false;
	private static Instant lastUpdated = Instant.now();
	public static JsonElement underBinJson;

	public static boolean onGuildMessageReceived(MessageReceivedEvent event) {
		try {
			if (event.getChannel().getId().equals("958771784004567063") && event.isWebhookMessage()) {
				lastUpdated = Instant.now();
				String desc = event.getMessage().getEmbeds().get(0).getDescription();
				if (desc.contains(" query auctions into database in ")) {
					queryItems = null;
				}
				if (enable && isMainBot() && desc.contains("Successfully updated under bins file in ")) {
					flip();
				}
				return true;
			}
		} catch (Exception ignored) {}
		return false;
	}

	public static void initialize(boolean enable) {
		AuctionFlipper.enable = enable;
	}

	public static void flip() {
		underBinJson = getUnderBinJson();
		if (underBinJson != null) {
			JsonElement avgAuctionJson = getAverageAuctionJson();

			for (JsonElement auction : underBinJson
				.getAsJsonObject()
				.entrySet()
				.stream()
				.map(Map.Entry::getValue)
				.sorted(Comparator.comparingLong(c -> -higherDepth(c, "profit", 0L)))
				.limit(15)
				.collect(Collectors.toCollection(ArrayList::new))) {
				String itemId = higherDepth(auction, "id").getAsString();
				if (isVanillaItem(itemId) || itemId.equals("BEDROCK")) {
					continue;
				}

				int sales = higherDepth(avgAuctionJson, itemId + ".sales", 0);
				if (sales < 5) {
					continue;
				}

				double pastBinPrice = Math.min(
					higherDepth(auction, "past_bin_price").getAsLong(),
					calculateWithTaxes(higherDepth(avgAuctionJson, itemId + ".price").getAsDouble())
				);
				long startingBid = higherDepth(auction, "starting_bid").getAsLong();
				double profit = pastBinPrice - startingBid;

				if (profit < 1000000) {
					continue;
				}

				String itemName = higherDepth(auction, "name").getAsString();
				String auctionUuid = higherDepth(auction, "uuid").getAsString();

				flipperWebhook
					.send(
						defaultEmbed(itemName)
							.addField("Price", roundAndFormat(startingBid), true)
							.addField("Previous Lowest Bin", roundAndFormat(pastBinPrice), true)
							.addField("Estimated Profit", roundAndFormat(profit), true)
							.addField("Sales Per Hour", formatNumber(sales), true)
							.addField("Command", "`/viewauction " + auctionUuid + "`", true)
							.setThumbnail(getItemThumbnail(itemId))
							.build()
					)
					.whenComplete((m, e) -> {
						if (m != null) {
							auctionUuidToMessage.put(auctionUuid, "" + m.getId());
						}
					});
			}
		}

		JsonElement endedAuctionsJson = getJson("https://api.hypixel.net/skyblock/auctions_ended");
		if (higherDepth(endedAuctionsJson, "auctions") == null) {
			return;
		}

		Instant jsonLastUpdated = Instant.ofEpochMilli(higherDepth(endedAuctionsJson, "lastUpdated").getAsLong());
		if (lastUpdated == null || lastUpdated.isBefore(jsonLastUpdated)) {
			lastUpdated = jsonLastUpdated;
			Map<String, String> toEdit = auctionUuidToMessage.getAllPresent(
				streamJsonArray(higherDepth(endedAuctionsJson, "auctions"))
					.map(a -> higherDepth(a, "auction_id").getAsString())
					.collect(Collectors.toSet())
			);
			for (String messageId : toEdit.values()) {
				flipperWebhook.edit(messageId, defaultEmbed("Auction Sold").build());
			}
			auctionUuidToMessage.invalidateAll(toEdit.keySet());
		}
	}

	public static JsonElement getUnderBinJson() {
		try {
			HttpGet httpGet = new HttpGet(getQueryApiUrl("underbin"));
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpGet.getURI()).addParameter("key", AUCTION_API_KEY).build();
			httpGet.setURI(uri);

			try (
				CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
				InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())
			) {
				return JsonParser.parseReader(in);
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static double calculateWithTaxes(double price) {
		return price * (price >= 1000000 ? 0.98 : 0.99);
	}
}
