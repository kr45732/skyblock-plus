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

import static com.skyblockplus.utils.utils.JsonUtils.getCopperJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
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
public class CopperSlashCommand extends SlashCommand {

	public CopperSlashCommand() {
		this.name = "copper";
	}

	public static EmbedBuilder getCoinsPerBit() {
		Map<String, Double> values = new HashMap<>();
		NetworthExecute calc = new NetworthExecute().initPrices();
		for (Map.Entry<String, JsonElement> entry : getCopperJson().entrySet()) {
			double cpc = calc.getLowestPrice(entry.getKey()) / entry.getValue().getAsLong();
			if (cpc > 0) {
				values.put(entry.getKey(), cpc);
			}
		}

		EmbedBuilder eb = defaultEmbed("Coins Per Copper");
		for (Map.Entry<String, Double> entry : values
			.entrySet()
			.stream()
			.sorted(Comparator.comparingDouble(v -> -v.getValue()))
			.collect(Collectors.toCollection(ArrayList::new))) {
			eb.appendDescription(
				getEmoji(entry.getKey()) +
				" " +
				idToName(entry.getKey()) +
				" ➜ " +
				roundAndFormat(entry.getValue()) +
				" (" +
				formatNumber(higherDepth(getCopperJson(), entry.getKey(), 0)) +
				" copper)\n"
			);
		}

		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getCoinsPerBit());
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get the coins to copper ratio for items in the SkyMart shop");
	}
}
