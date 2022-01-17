/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.InvItem;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;

public class TrackAuctionsCommand extends Command {

	private static final Map<String, UsernameUuidStruct> commandAuthorToTrackingUser = new HashMap<>();
	private static Instant lastUpdated = null;

	public TrackAuctionsCommand() {
		this.name = "track";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder trackAuctions(String username, String userId) {
		if (commandAuthorToTrackingUser.containsKey(userId)) {
			UsernameUuidStruct stoppedTracking = commandAuthorToTrackingUser.get(userId);
			return invalidEmbed(
				"You are already tracking [**" + stoppedTracking.username() + "**](" + stoppedTracking.getAuctionUrl() + ")"
			);
		}

		UsernameUuidStruct uuidStruct = usernameToUuid(username);
		if (uuidStruct.isNotValid()) {
			return invalidEmbed(uuidStruct.failCause());
		}

		commandAuthorToTrackingUser.put(userId, uuidStruct);

		return defaultEmbed("Auction tracker")
			.setDescription(
				"Now tracking the auctions of [**" +
				uuidStruct.username() +
				"**](" +
				uuidStruct.getAuctionUrl() +
				"). You will receive a DM whenever one of this player's auctions sells."
			);
	}

	private static void trackAuctionRunnable() {
		if (commandAuthorToTrackingUser.isEmpty()) {
			return;
		}

		JsonElement endedAuctionsJson = getJson("https://api.hypixel.net/skyblock/auctions_ended");
		Instant jsonLastUpdated = Instant.ofEpochMilli(higherDepth(endedAuctionsJson, "lastUpdated").getAsLong());

		if (lastUpdated == null || lastUpdated.isBefore(jsonLastUpdated)) {
			lastUpdated = jsonLastUpdated;

			JsonArray endedAuctionsArray = higherDepth(endedAuctionsJson, "auctions").getAsJsonArray();
			for (JsonElement endedAuction : endedAuctionsArray) {
				String seller = higherDepth(endedAuction, "seller").getAsString();

				String itemName = "???";
				try {
					InvItem item = getGenericInventoryMap(NBTReader.readBase64(higherDepth(endedAuction, "item_bytes").getAsString()))
						.get(0);
					itemName =
						(item.getCount() > 1 ? item.getCount() + "x " : "") +
						(
							item.getId().equals("ENCHANTED_BOOK")
								? parseMcCodes(item.getLore().split("\n")[0])
								: (item.getId().equals("PET") ? capitalizeString(item.getRarity()) + " " : "")
						) +
						item.getName();
				} catch (Exception ignored) {}
				String finalItemName = itemName;
				String soldFor = formatNumber(higherDepth(endedAuction, "price").getAsLong());
				long endedAt = higherDepth(endedAuction, "timestamp").getAsLong() / 1000;

				commandAuthorToTrackingUser
					.entrySet()
					.stream()
					.filter(entry -> entry.getValue().uuid().equals(seller))
					.forEach(entry ->
						jda
							.openPrivateChannelById(entry.getKey())
							.queue(dm ->
								dm
									.sendMessageEmbeds(
										defaultEmbed("Auction tracker")
											.setDescription(
												"**Seller:** " +
												entry.getValue().username() +
												"\n**Item:** " +
												finalItemName +
												"\n**Sold for:** " +
												soldFor +
												"\n**Ended:** <t:" +
												endedAt +
												":R>"
											)
											.build()
									)
									.queue(ignore, ignore)
							)
					);
			}
		}
	}

	public static void initialize() {
		scheduler.scheduleAtFixedRate(TrackAuctionsCommand::trackAuctionRunnable, 0, 30, TimeUnit.SECONDS);
	}

	public static EmbedBuilder stopTrackingAuctions(String userId) {
		if (commandAuthorToTrackingUser.containsKey(userId)) {
			UsernameUuidStruct stoppedTracking = commandAuthorToTrackingUser.get(userId);
			commandAuthorToTrackingUser.remove(userId);
			return defaultEmbed("Auction tracker")
				.setDescription("Stopped tracking [**" + stoppedTracking.username() + "**](" + stoppedTracking.getAuctionUrl() + ")");
		}

		return defaultEmbed("Auction tracker").setDescription("You are not tracking anyone");
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if ((args.length == 3 || args.length == 2) && args[1].equals("auctions")) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					embed(trackAuctions(player, event.getAuthor().getId()));
					return;
				} else if (args.length == 2 && args[1].equals("stop")) {
					embed(stopTrackingAuctions(event.getAuthor().getId()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
