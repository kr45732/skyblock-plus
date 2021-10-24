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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.networth.NetworthExecute;
import com.skyblockplus.utils.structs.InvItem;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import me.nullicorn.nedit.NBTReader;

public class AuctionFlipper {

	private static final NetworthExecute calculator = new NetworthExecute().setVerbose(true).setFlipper(true);
	private static final JDAWebhookClient flipperWebhook = new WebhookClientBuilder(
		"https://discord.com/api/webhooks/901508783862325268/SKvSwKeCAfz71rWUrbQi5Runb6Jbebo6gLwaq0cjU1rCIPJ22VtmA-85zIlujHEAqfw1"
	)
		.setExecutorService(scheduler)
		.setHttpClient(okHttpClient)
		.buildJDA();
	public static boolean enable = false;
	private static Instant lastUpdated;

	public static void scheduleFlipper() {
		scheduler.scheduleAtFixedRate(AuctionFlipper::flip, 5, 60, TimeUnit.SECONDS);
	}

	public static void flip() {
		try {
			if (!enable) {
				return;
			}

			calculator.initPrices();
			System.out.println("Flipping...");

			JsonElement auctionJson = getJson("https://moulberry.codes/auction.json");
			Instant thisLastUpdated = Instant.ofEpochSecond(higherDepth(auctionJson, "time").getAsLong());
			if (lastUpdated == null || thisLastUpdated.isAfter(lastUpdated)) {
				lastUpdated = thisLastUpdated;
				JsonArray newAuctions = higherDepth(auctionJson, "new_auctions").getAsJsonArray();

				for (JsonElement auction : newAuctions) {
					if (higherDepth(auction, "bin", false)) {
						InvItem item = null;
						try {
							item = getGenericInventoryMap(NBTReader.readBase64(higherDepth(auction, "item_bytes").getAsString())).get(0);
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (item == null) {
							continue;
						}

						double itemPrice = calculator.calculateItemPrice(item);
						long startingBid = higherDepth(auction, "starting_bid").getAsLong();
						double profit = itemPrice - startingBid;
						if (profit > 1000000) {
							flipperWebhook.send(
								defaultEmbed(item.getName())
									.setDescription(
										"**Current price:** " +
										formatNumber(startingBid) +
										"\n**Calculated value:** " +
										formatNumber(itemPrice) +
										"\n**Estimated profit:** " +
										formatNumber(profit) +
										"\n**Command:** `/viewauction " +
										higherDepth(auction, "uuid").getAsString() +
										"`" +
										"\n**Ending** <t:" +
										Instant.ofEpochMilli(higherDepth(auction, "end").getAsLong()).getEpochSecond() +
										":R>"
									)
									.addField("Price breakdown", calculator.getItemInfo(), false)
									.setThumbnail("https://sky.shiiyu.moe/item.gif/" + item.getId())
									.build()
							);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
