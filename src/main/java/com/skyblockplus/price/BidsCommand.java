package com.skyblockplus.price;

import static com.skyblockplus.Main.executor;
import static com.skyblockplus.Main.httpClient;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class BidsCommand extends Command {

	public BidsCommand() {
		this.name = "bids";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getPlayerBids(String username) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct == null) {
			return defaultEmbed("Error fetching player data");
		}

		JsonArray bids = queryAhApi(usernameUuidStruct.playerUuid);

		if (bids == null || bids.size() == 0) {
			return defaultEmbed("No bids found for " + usernameUuidStruct.playerUsername);
		}

		EmbedBuilder eb = defaultEmbed(
			usernameUuidStruct.playerUsername,
			"https://auctions.craftlink.xyz/players/" + usernameUuidStruct.playerUuid
		);
		for (JsonElement bid : bids) {
			String auctionDesc;
			String itemName;
			boolean isPet = higherDepth(bid, "item_lore").getAsString().toLowerCase().contains("pet");

			Instant endingAt = Instant.ofEpochMilli(higherDepth(bid, "end").getAsLong());
			Duration duration = Duration.between(Instant.now(), endingAt);
			String timeUntil = instantToDHM(duration);

			if (higherDepth(bid, "item_name").getAsString().equals("Enchanted Book")) {
				itemName = parseMcCodes(higherDepth(bid, "item_lore").getAsString().split("\n")[0]);
			} else {
				itemName =
					(isPet ? capitalizeString(higherDepth(bid, "tier").getAsString().toLowerCase()) + " " : "") +
					higherDepth(bid, "item_name").getAsString();
			}

			long highestBid = higherDepth(bid, "highest_bid_amount").getAsInt();
			JsonArray bidsArr = higherDepth(bid, "bids").getAsJsonArray();
			if (timeUntil.length() > 0) {
				auctionDesc = "Current bid: " + simplifyNumber(highestBid);
				auctionDesc += " | Ending in " + timeUntil;
				auctionDesc += "\nHighest bidder: " + uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString());
				for (int i = bidsArr.size() - 1; i >= 0; i--) {
					JsonElement curBid = bidsArr.get(i);
					if (higherDepth(curBid, "bidder").getAsString().equals(usernameUuidStruct.playerUuid)) {
						auctionDesc += "\nYour highest bid: " + simplifyNumber(higherDepth(curBid, "amount").getAsDouble());
						break;
					}
				}
			} else {
				auctionDesc = "Auction sold for " + simplifyNumber(highestBid) + " coins";
				auctionDesc +=
					"\n " + uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()) + " won the auction";
			}

			eb.setThumbnail("https://cravatar.eu/helmavatar/" + usernameUuidStruct.playerUuid + "/64.png");
			eb.addField(itemName, auctionDesc, false);
		}

		return eb;
	}

	private static JsonArray queryAhApi(String uuid) {
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI()).addParameter("query", "{\"bids.bidder\":\"" + uuid + "\"}").build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					ebMessage.editMessage(getPlayerBids(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
