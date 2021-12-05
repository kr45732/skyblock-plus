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

import static com.skyblockplus.utils.Utils.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

public class AuctionFlipper {

	private static final JDAWebhookClient flipperWebhook = new WebhookClientBuilder(
		"https://discord.com/api/webhooks/917160844247334933/WKeMowhugO5-xbLlD8TakRfCskt7D5Sm7giMY8LfN2MzKjxsDUm9Y2yPw61_yzQTgcII"
	)
		.setExecutorService(scheduler)
		.setHttpClient(okHttpClient)
		.buildJDA();
	private static boolean enable = false;
	private static Instant lastUpdated;
	private static final Cache<String, Long> auctionUuidToMessage = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

	public static void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		try {
			if (event.getChannel().getId().equals("912156704383336458")) {
				if (event.getMessage().getEmbeds().get(0).getDescription().startsWith("Insert time: ")) {
					flip();
				}
			}
		} catch (Exception ignored) {}
	}

	public static void flip() {
		if (!enable) {
			return;
		}

		JsonArray underBinJson = getUnderBinJson().getAsJsonArray();
		if (underBinJson != null) {
			for (JsonElement auction : underBinJson) {
				String itemId = higherDepth(auction, "id").getAsString();
				if (isVanillaItem(itemId)) {
					continue;
				}
				String itemName = higherDepth(auction, "name").getAsString();
				long startingBid = higherDepth(auction, "starting_bid").getAsLong();
				long profit = higherDepth(auction, "profit").getAsLong();
				String auctionUuid = higherDepth(auction, "uuid").getAsString();
				flipperWebhook
					.send(
						defaultEmbed(itemName)
							.setDescription(
								"**Current price:** " +
								formatNumber(startingBid) +
								"\n**Previous bin price:** " +
								formatNumber(higherDepth(auction, "past_bin_price").getAsLong()) +
								"\n**Estimated profit:** " +
								formatNumber(profit) +
								"\n**Command:** `/viewauction " +
								auctionUuid +
								"`" +
								"\n**Ending** <t:" +
								Instant.ofEpochMilli(higherDepth(auction, "end").getAsLong()).getEpochSecond() +
								":R>"
							)
							.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId)
							.build()
					)
					.whenComplete((m, e) -> {
						if (m != null) {
							auctionUuidToMessage.put(auctionUuid, m.getId());
						}
					});
			}
		}

		JsonElement endedAuctionsJson = getJson("https://api.hypixel.net/skyblock/auctions_ended");
		Instant jsonLastUpdated = Instant.ofEpochMilli(higherDepth(endedAuctionsJson, "lastUpdated").getAsLong());
		if (lastUpdated == null || lastUpdated.isBefore(jsonLastUpdated)) {
			lastUpdated = jsonLastUpdated;
			Map<String, Long> toEdit = auctionUuidToMessage.getAllPresent(
				streamJsonArray(higherDepth(endedAuctionsJson, "auctions").getAsJsonArray())
					.map(a -> higherDepth(a, "auction_id").getAsString())
					.collect(Collectors.toList())
			);
			for (Map.Entry<String, Long> entry : toEdit.entrySet()) {
				flipperWebhook.edit(entry.getValue(), defaultEmbed("Auction Sold").build());
			}
			auctionUuidToMessage.invalidate(toEdit);
		}
	}

	public static JsonElement getUnderBinJson() {
		try {
			HttpGet httpget = new HttpGet("http://venus.arcator.co.uk:1194/underbin");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI()).addParameter("key", AUCTION_API_KEY).build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = Utils.httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static void setEnable(boolean enable) {
		AuctionFlipper.enable = enable;
	}
}
