package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.enchantNames;
import static com.skyblockplus.utils.Constants.petNames;
import static com.skyblockplus.utils.Hypixel.uuidToUsername;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.getGenericInventoryMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

public class QueryAuctionCommand extends Command {

	public QueryAuctionCommand() {
		this.name = "query";
		this.cooldown = globalCooldown;
	}

	private static JsonArray queryAhApi(String query) {
		try {
			HttpGet httpget = new HttpGet("https://api.eastarcti.ca/auctions/");
			httpget.addHeader("content-type", "application/json; charset=UTF-8");

			query = query.replace("[", "\\\\[");
			URI uri = new URIBuilder(httpget.getURI())
				// .addParameter("query", "{\"item_name\":{\"$regex\":\"" + query
				// +
				// "\",\"$options\":\"i\"},\"$or\":[{\"bin\":true},{\"bids\":{\"$lt\":{\"$size\":0}}}]}")
				.addParameter("query", "{\"item_name\":{\"$regex\":\"" + query + "\",\"$options\":\"i\"},\"bin\":true}")
				.addParameter("sort", "{\"starting_bid\":1}")
				.build();
			httpget.setURI(uri);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpget)) {
				return JsonParser.parseReader(new InputStreamReader(httpResponse.getEntity().getContent())).getAsJsonArray();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static EmbedBuilder queryAuctions(String query) {
		for (String enchant : enchantNames) {
			if (query.replace(" ", "_").toLowerCase().contains(enchant)) {
				String enchantName = enchant + ";";
				try {
					enchantName += Integer.parseInt(query.replaceAll("\\D+", ""));
				} catch (Exception e) {
					enchantName += "5";
				}

				JsonArray ahQueryArr = queryAhApi("enchanted book");

				if (ahQueryArr == null) {
					return invalidEmbed("Error fetching auctions data");
				}

				EmbedBuilder eb = defaultEmbed("Query Auctions");

				for (JsonElement lowestBinAh : ahQueryArr) {
					Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
					Duration duration = Duration.between(Instant.now(), endingAt);
					if (duration.getSeconds() <= 0) {
						continue;
					}

					try {
						NBTCompound nbtData = NBTReader.readBase64(higherDepth(lowestBinAh, "item_bytes").getAsString());
						InvItem itemStruct = getGenericInventoryMap(nbtData).get(0);
						if (itemStruct.getId().equals("ENCHANTED_BOOK")) {
							if (itemStruct.getEnchantsFormatted().contains(enchantName)) {
								String lowestBinStr = "";
								lowestBinStr += "**Name:** " + enchantName.replaceAll("[_;]", " ");
								lowestBinStr += "\n**Price:** " + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble());
								lowestBinStr +=
									"\n**Seller:** " + uuidToUsername(higherDepth(lowestBinAh, "auctioneer").getAsString()).playerUsername;
								lowestBinStr += "\n**Auction:** `/ah " + higherDepth(lowestBinAh, "uuid").getAsString() + "`";

								lowestBinStr += "\n**Ends in:** " + instantToDHM(duration);

								eb.addField("Lowest Bin", lowestBinStr, false);
								eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
								return eb;
							}
						}
					} catch (Exception ignored) {}
				}
				return eb.setDescription("No lowest bin found for `" + query + "` enchant");
			}
		}

		for (String pet : petNames) {
			if (query.replace(" ", "_").toUpperCase().contains(pet)) {
				query = query.toLowerCase();
				String rarity = "ANY";
				if (query.contains("common")) {
					rarity = "COMMON";
					query = query.replace("common", "").trim();
				} else if (query.contains("uncommon")) {
					rarity = "UNCOMMON";
					query = query.replace("uncommon", "").trim();
				} else if (query.contains("rare")) {
					rarity = "RARE";
					query = query.replace("rare", "").trim();
				} else if (query.contains("epic")) {
					rarity = "EPIC";
					query = query.replace("epic", "").trim();
				} else if (query.contains("legendary")) {
					rarity = "LEGENDARY";
					query = query.replace("legendary", "").trim();
				} else if (query.contains("mythic")) {
					rarity = "MYTHIC";
					query = query.replace("mythic", "").trim();
				}

				JsonArray ahQueryArr = queryAhApi(query);

				if (ahQueryArr == null) {
					return invalidEmbed("Error fetching auctions data");
				}

				EmbedBuilder eb = defaultEmbed("Query Auctions");

				for (JsonElement lowestBinAh : ahQueryArr) {
					Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
					Duration duration = Duration.between(Instant.now(), endingAt);
					if (duration.getSeconds() <= 0) {
						continue;
					}

					if (!rarity.equals("ANY") && !higherDepth(lowestBinAh, "tier").getAsString().equals(rarity)) {
						continue;
					}

					try {
						NBTCompound nbtData = NBTReader.readBase64(higherDepth(lowestBinAh, "item_bytes").getAsString());
						InvItem itemStruct = getGenericInventoryMap(nbtData).get(0);
						if (itemStruct.getId().equals("PET")) {
							String lowestBinStr = "";
							lowestBinStr +=
								"**Name:** " +
								capitalizeString(higherDepth(lowestBinAh, "tier").getAsString()) +
								" " +
								higherDepth(lowestBinAh, "item_name").getAsString();
							lowestBinStr += "\n**Price:** " + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble());
							lowestBinStr +=
								"\n**Seller:** " + uuidToUsername(higherDepth(lowestBinAh, "auctioneer").getAsString()).playerUsername;
							lowestBinStr += "\n**Auction:** `/ah " + higherDepth(lowestBinAh, "uuid").getAsString() + "`";

							lowestBinStr += "\n**Ends in:** " + instantToDHM(duration);

							eb.setThumbnail(getPetUrl(pet));
							eb.addField("Lowest Bin", lowestBinStr, false);
							return eb;
						}
					} catch (Exception ignored) {}
				}
				return eb.setDescription("No lowest bin found for `" + query + "` pet");
			}
		}

		JsonArray ahQueryArr = queryAhApi(query);

		if (ahQueryArr == null) {
			return invalidEmbed("Error fetching auctions data");
		}

		EmbedBuilder eb = defaultEmbed("Query Auctions").setDescription("Found `" + ahQueryArr.size() + "` bins matching `" + query + "`");

		for (JsonElement lowestBinAh : ahQueryArr) {
			Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
			Duration duration = Duration.between(Instant.now(), endingAt);
			if (duration.getSeconds() <= 0) {
				continue;
			}

			String lowestBinStr = "";
			lowestBinStr += "**Name:** " + higherDepth(lowestBinAh, "item_name").getAsString();
			lowestBinStr += "\n**Price:** " + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble());
			lowestBinStr += "\n**Seller:** " + uuidToUsername(higherDepth(lowestBinAh, "auctioneer").getAsString()).playerUsername;
			lowestBinStr += "\n**Auction:** `/ah " + higherDepth(lowestBinAh, "uuid").getAsString() + "`";

			lowestBinStr += "\n**Ends in:** " + instantToDHM(duration);

			try {
				NBTCompound nbtData = NBTReader.readBase64(higherDepth(lowestBinAh, "item_bytes").getAsString());
				InvItem itemStruct = getGenericInventoryMap(nbtData).get(0);
				if (itemStruct.getId().equals("ENCHANTED_BOOK")) {
					eb.setThumbnail("https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK");
				} else if (itemStruct.getId().equals("PET")) {
					eb.setThumbnail(getPetUrl(itemStruct.getName().split("] ")[1].toUpperCase().replace(" ", "_")));
				} else {
					eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemStruct.getId());
				}
			} catch (Exception ignored) {}

			eb.addField("Lowest Bin", lowestBinStr, false);
			break;
		}

		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String[] args = event.getMessage().getContentRaw().split(" ", 2);

				logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

				if (args.length == 2) {
					ebMessage.editMessageEmbeds(queryAuctions(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
