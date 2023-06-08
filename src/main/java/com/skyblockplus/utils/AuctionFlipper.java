/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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
import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.utils.HttpUtils.getJson;
import static com.skyblockplus.utils.utils.HttpUtils.okHttpClient;
import static com.skyblockplus.utils.utils.HypixelUtils.calculateWithTaxes;
import static com.skyblockplus.utils.utils.HypixelUtils.isVanillaItem;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.database.LeaderboardDatabase;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AuctionFlipper {

	private static final JDAWebhookClient flipperWebhook = new WebhookClientBuilder(
		isMainBot()
			? "https://discord.com/api/webhooks/917160844247334933/WKeMowhugO5-xbLlD8TakRfCskt7D5Sm7giMY8LfN2MzKjxsDUm9Y2yPw61_yzQTgcII"
			: "https://discord.com/api/webhooks/1116395938789998652/5kwXE0t5qLSYXh03NSUMnDRO6Gin6eDMCvyAz9WemDfc8Q9xPjYaubigXcHEcbouh8cF"
	)
		.setExecutorService(scheduler)
		.setHttpClient(okHttpClient)
		.buildJDA();
	private static final Cache<String, FlipItem> auctionUuidToMessage = Caffeine
		.newBuilder()
		.expireAfterWrite(45, TimeUnit.MINUTES)
		.build();
	public static JsonElement underBinJson;
	public static Instant underBinJsonLastUpdated = null;
	private static boolean enable = false;

	public static boolean onGuildMessageReceived(MessageReceivedEvent event) {
		try {
			if (event.getChannel().getId().equals("958771784004567063") && event.isWebhookMessage()) {
				String desc = event.getMessage().getEmbeds().get(0).getDescription();
				if (desc.contains(" query auctions into database in ")) {
					resetQueryItems();
				}
				if (enable && desc.contains("Successfully updated under bins file in ")) {
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
		try {
			underBinJson = getJson(getQueryApiUrl("underbin").toString());
		} catch (Exception ignored) {}

		if (underBinJson != null) {
			underBinJsonLastUpdated = Instant.now();
			JsonElement avgAuctionJson = getAveragePriceJson();

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

				double resellPrice = Math.min(
					higherDepth(auction, "past_bin_price").getAsLong(),
					higherDepth(avgAuctionJson, itemId + ".price").getAsDouble()
				);
				long buyPrice = higherDepth(auction, "starting_bid").getAsLong();
				double profit = calculateWithTaxes(resellPrice) - buyPrice;

				if (profit < 1000000) {
					continue;
				}

				String itemName = higherDepth(auction, "name").getAsString();
				String auctionUuid = higherDepth(auction, "uuid").getAsString();

				flipperWebhook
					.send(
						defaultEmbed(itemName)
							.addField("Price", roundAndFormat(buyPrice), true)
							.addField("Resell Price", roundAndFormat((long) resellPrice), true)
							.addField("Estimated Profit", roundAndFormat((long) profit), true)
							.addField("Sales Per Hour", formatNumber(sales), true)
							.addField("Command", "`/viewauction " + auctionUuid + "`", true)
							.setThumbnail(getItemThumbnail(itemId))
							.build()
					)
					.whenComplete((m, e) -> {
						if (m != null) {
							auctionUuidToMessage.put(auctionUuid, new FlipItem(m.getId(), itemName, (long) profit));
						}
					});
			}
		}

		JsonElement endedAuctionsJson = getJson("https://api.hypixel.net/skyblock/auctions_ended");
		if (higherDepth(endedAuctionsJson, "auctions") != null) {
			Map<UUID, LeaderboardDatabase.AuctionAnalyzer> auctionAnalyzers = new HashMap<>();

			for (JsonElement auction : higherDepth(endedAuctionsJson, "auctions").getAsJsonArray()) {
				long price = higherDepth(auction, "price").getAsLong();
				String auctionId = higherDepth(auction, "auction_id").getAsString();
				FlipItem flipItem = auctionUuidToMessage.getIfPresent(auctionId);
				if (flipItem != null) {
					auctionUuidToMessage.invalidate(auctionId);
					flipperWebhook.edit(
						flipItem.messageId(),
						defaultEmbed(flipItem.name())
							.setDescription("Sold for " + formatNumber(price) + "\nEstimated profit: " + roundAndFormat(flipItem.profit()))
							.build()
					);
				}

				try {
					if (!higherDepth(auction, "bin", false)) {
						continue;
					}

					NBTCompound nbt = NBTReader.readBase64(higherDepth(auction, "item_bytes").getAsString()).getList("i").getCompound(0);
					if (nbt.getInt("Count", 1) > 1 || !nbt.containsKey("tag.ExtraAttributes.uuid")) {
						continue;
					}

					JsonObject attributes = gson.toJsonTree(nbt.getCompound("tag.ExtraAttributes")).getAsJsonObject();
					UUID uuid = UUID.fromString(attributes.remove("uuid").getAsString());

					long end = higherDepth(auction, "timestamp").getAsLong();
					if (auctionAnalyzers.containsKey(uuid)) {
						if (auctionAnalyzers.get(uuid).end() <= end) {
							auctionAnalyzers.remove(uuid);
						} else {
							continue;
						}
					}

					attributes.remove("timestamp");
					attributes.remove("originTag");
					if (attributes.has("petInfo")) {
						JsonObject petInfo = JsonParser.parseString(attributes.remove("petInfo").getAsString()).getAsJsonObject();
						petInfo.remove("hideRightClick");
						petInfo.remove("hideInfo");
						petInfo.remove("active");
						petInfo.remove("uuid");
						attributes.add("petInfo", petInfo);
					}

					auctionAnalyzers.put(
						uuid,
						new LeaderboardDatabase.AuctionAnalyzer(uuid, flipItem != null, price, attributes.toString(), end)
					);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (!auctionAnalyzers.isEmpty()) {
				leaderboardDatabase.insertAuctionAnalyzer(auctionAnalyzers.values());
			}
		}
	}

	private record FlipItem(long messageId, String name, long profit) {}
}
