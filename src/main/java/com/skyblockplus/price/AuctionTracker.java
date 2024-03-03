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

package com.skyblockplus.price;

import static com.skyblockplus.utils.ApiHandler.getHypixelApiUrl;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.utils.HttpUtils.getJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class AuctionTracker {

	public static final Map<String, UsernameUuidStruct> commandAuthorToTrackingUser = new HashMap<>();
	private static Instant lastUpdated = null;

	public static EmbedBuilder trackAuctions(String username, String userId) {
		UsernameUuidStruct curTrack = commandAuthorToTrackingUser.getOrDefault(userId, null);

		UsernameUuidStruct uuidStruct = usernameToUuid(username);
		if (!uuidStruct.isValid()) {
			return errorEmbed(uuidStruct.failCause());
		}

		if (curTrack != null && curTrack.uuid().equals(uuidStruct.uuid())) {
			return errorEmbed(
				"You are already tracking the auctions of [**" + uuidStruct.username() + "**](" + uuidStruct.getAuctionUrl() + ")"
			);
		}

		commandAuthorToTrackingUser.put(userId, uuidStruct);
		return defaultEmbed("Auction Tracker")
			.setDescription(
				(curTrack != null ? "Stopped tracking [**" + curTrack.username() + "**](" + curTrack.getAuctionUrl() + "). " : "") +
				"Now tracking the auctions of [**" +
				uuidStruct.username() +
				"**](" +
				uuidStruct.getAuctionUrl() +
				"). You will receive a DM whenever any of their auctions sells."
			);
	}

	private static void trackAuctionRunnable() {
		try {
			if (commandAuthorToTrackingUser.isEmpty()) {
				return;
			}

			JsonElement endedAuctionsJson = getJson(getHypixelApiUrl("/skyblock/auctions_ended", false));
			if (higherDepth(endedAuctionsJson, "auctions") == null) {
				return;
			}

			Instant jsonLastUpdated = Instant.ofEpochMilli(higherDepth(endedAuctionsJson, "lastUpdated").getAsLong());
			if (lastUpdated == null || lastUpdated.isBefore(jsonLastUpdated)) {
				lastUpdated = jsonLastUpdated;

				JsonArray endedAuctionsArray = higherDepth(endedAuctionsJson, "auctions").getAsJsonArray();
				for (JsonElement endedAuction : endedAuctionsArray) {
					String seller = higherDepth(endedAuction, "seller").getAsString();

					String itemName = "???";
					try {
						InvItem item = nbtToItem(higherDepth(endedAuction, "item_bytes").getAsString());
						itemName = (item.getCount() > 1 ? item.getCount() + "x " : "");
						if (item.getId().equals("ENCHANTED_BOOK")) {
							itemName += cleanMcCodes(item.getLore().get(0));
						} else {
							itemName += (item.getId().equals("PET") ? capitalizeString(item.getRarity()) + " " : "") + item.getName();
						}
					} catch (Exception ignored) {}
					String finalItemName = itemName;
					String soldFor = formatNumber(higherDepth(endedAuction, "price").getAsLong());
					long endedAt = higherDepth(endedAuction, "timestamp").getAsLong();

					commandAuthorToTrackingUser
						.entrySet()
						.stream()
						.filter(entry -> entry.getValue().uuid().equals(seller))
						.forEach(entry -> {
							try {
								jda
									.retrieveUserById(entry.getKey())
									.queue(
										user ->
											user
												.openPrivateChannel()
												.queue(
													dm ->
														dm
															.sendMessageEmbeds(
																defaultEmbed("Auction Tracker")
																	.setDescription(
																		"**Seller:** " +
																		entry.getValue().username() +
																		"\n**Item:** " +
																		finalItemName +
																		"\n**Sold for:** " +
																		soldFor +
																		"\n**Ended:** " +
																		getRelativeTimestamp(endedAt)
																	)
																	.build()
															)
															.queue(ignore, ignore),
													ignore
												),
										ignore
									);
							} catch (Exception ignored) {}
						});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initialize() {
		scheduler.scheduleAtFixedRate(AuctionTracker::trackAuctionRunnable, 0, 30, TimeUnit.SECONDS);
	}

	public static EmbedBuilder stopTrackingAuctions(String userId) {
		if (commandAuthorToTrackingUser.containsKey(userId)) {
			UsernameUuidStruct stoppedTracking = commandAuthorToTrackingUser.get(userId);
			commandAuthorToTrackingUser.remove(userId);
			return defaultEmbed("Auction Tracker")
				.setDescription(
					"Stopped tracking the auctions of [**" + stoppedTracking.username() + "**](" + stoppedTracking.getAuctionUrl() + ")"
				);
		}

		return errorEmbed("You are not tracking this player");
	}

	public static void insertAhTrack(String key, UsernameUuidStruct value) {
		commandAuthorToTrackingUser.put(key, value);
	}
}
