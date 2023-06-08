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

package com.skyblockplus.price;

import static com.skyblockplus.utils.AuctionFlipper.*;
import static com.skyblockplus.utils.utils.HypixelUtils.calculateWithTaxes;
import static com.skyblockplus.utils.utils.HypixelUtils.isVanillaItem;
import static com.skyblockplus.utils.utils.JsonUtils.getAveragePriceJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.formatNumber;
import static com.skyblockplus.utils.utils.StringUtils.roundAndFormat;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class FlipsSlashCommand extends SlashCommand {

	public FlipsSlashCommand() {
		this.name = "flips";
	}

	public static EmbedBuilder getFlips() {
		if (underBinJson == null || underBinJson.getAsJsonObject().isEmpty()) {
			String ebStr = "No auction flips found at the moment";
			if (underBinJsonLastUpdated != null) {
				ebStr = "**Next Update:** <t:" + underBinJsonLastUpdated.plusSeconds(60).getEpochSecond() + ":R>\n\n" + ebStr;
			}
			return errorEmbed(ebStr);
		}

		EmbedBuilder eb = defaultEmbed("Flips")
			.setDescription(
				"**Next Update:** <t:" +
				underBinJsonLastUpdated.plusSeconds(60).getEpochSecond() +
				":R>\n**Live Updates:** [**Join**](https://discord.gg/GZWZbcxpv4)"
			);
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

			eb.addField(
				getEmoji(itemId) + " " + itemName,
				"**Price:** " +
				roundAndFormat(buyPrice) +
				"\n**Estimated Profit:** " +
				roundAndFormat((long) profit) +
				"\n**Resell Price:** " +
				roundAndFormat((long) resellPrice) +
				"\n**Sales Per Hour:** " +
				formatNumber(sales) +
				"\n**Command:** `/viewauction " +
				auctionUuid +
				"`",
				false
			);
		}

		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getFlips());
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get current auction flips");
	}
}
