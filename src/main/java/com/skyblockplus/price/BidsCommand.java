package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.capitalizeString;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.parseMcCodes;
import static com.skyblockplus.utils.Utils.simplifyNumber;
import static com.skyblockplus.utils.Utils.usernameToUuid;
import static com.skyblockplus.utils.Utils.uuidToUsername;

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
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class BidsCommand extends Command {

	public BidsCommand() {
		this.name = "bids";
		this.cooldown = globalCooldown;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
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

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
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
			long daysUntil = duration.toMinutes() / 1400;
			long hoursUntil = duration.toMinutes() / 60 % 24;
			long minutesUntil = duration.toMinutes() % 60;
			String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
			timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
			timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";

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
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			URI uri = new URIBuilder(httpget.getURI()).addParameter("query", "{\"bids.bidder\":\"" + uuid + "\"}").build();
			httpget.setURI(uri);

			HttpResponse httpresponse = httpclient.execute(httpget);
			return JsonParser.parseReader(new InputStreamReader(httpresponse.getEntity().getContent())).getAsJsonArray();
		} catch (Exception ignored) {} finally {
			try {
				httpclient.close();
			} catch (Exception e) {
				System.out.println("== Stack Trace (Auction Query Close Http Client) ==");
				e.printStackTrace();
			}
		}
		return null;
	}
}
