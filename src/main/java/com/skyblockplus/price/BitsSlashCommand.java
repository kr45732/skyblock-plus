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

import static com.skyblockplus.utils.utils.JsonUtils.getBitsJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.defaultPaginator;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class BitsSlashCommand extends SlashCommand {

	public BitsSlashCommand() {
		this.name = "bits";
	}

	public static EmbedBuilder getCoinsPerBit(SlashCommandEvent event) {
		Map<String, Double> values = new HashMap<>();
		NetworthExecute calc = new NetworthExecute().initPrices();
		for (Map.Entry<String, JsonElement> entry : getBitsJson().entrySet()) {
			double cpb = calc.getLowestPrice(entry.getKey()) / entry.getValue().getAsLong();
			if (cpb > 0) {
				values.put(entry.getKey(), cpb);
			}
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser())
			.setItemsPerPage(20)
			.updateExtras(e -> e.setEveryPageTitle("Coins Per Bit"));
		for (Map.Entry<String, Double> entry : values
			.entrySet()
			.stream()
			.sorted(Comparator.comparingDouble(v -> -v.getValue()))
			.collect(Collectors.toCollection(ArrayList::new))) {
			paginateBuilder.addItems(
				getEmoji(entry.getKey()) +
				" " +
				idToName(entry.getKey()) +
				" ➜ " +
				roundAndFormat(entry.getValue()) +
				" (" +
				formatNumber(higherDepth(getBitsJson(), entry.getKey(), 0)) +
				" bits)"
			);
		}

		event.paginate(paginateBuilder);
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(getCoinsPerBit(event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get the coins to bits ratio for items in the bits shop");
	}
}
