package com.skyblockplus.price;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Hypixel.getAuctionsByQuery;
import static com.skyblockplus.utils.Hypixel.uuidToUsername;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.InvItem;
import java.time.Duration;
import java.time.Instant;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class QueryAuctionCommand extends Command {

	public QueryAuctionCommand() {
		this.name = "query";
		this.cooldown = globalCooldown;
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

				JsonArray ahQueryArr = getAuctionsByQuery("enchanted book");
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
			}
		}

		for (String pet : petNames) {
			if (query.replace(" ", "_").toUpperCase().contains(pet)) {
				query = query.toLowerCase();

				String rarity = "ANY";
				for (String rarityName : rarityToNumberMap.keySet()) {
					if (query.contains(rarity.toLowerCase())) {
						rarity = rarityName;
						query = query.replace(rarityName.toLowerCase(), "").trim();
					}
				}

				JsonArray ahQueryArr = getAuctionsByQuery(query);
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
			}
		}

		JsonArray ahQueryArr = getAuctionsByQuery(query);
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
