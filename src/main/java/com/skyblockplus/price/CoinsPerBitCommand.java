/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
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

import static com.skyblockplus.utils.Constants.BITS_ITEM_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class CoinsPerBitCommand extends Command {

	public CoinsPerBitCommand() {
		this.name = "coinsperbit";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "cpb" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCoinsPerBit() {
		Map<String, Double> values = new HashMap<>();
		NetworthExecute calc = new NetworthExecute().initPrices();
		for (Map.Entry<String, JsonElement> entry : getBitsJson().entrySet()) {
			values.put(entry.getKey(), calc.getLowestPrice(entry.getKey()) / entry.getValue().getAsLong());
		}
		EmbedBuilder eb = defaultEmbed("Coins Per Bit");
		for (Map.Entry<String, Double> entry : values
			.entrySet()
			.stream()
			.sorted(Comparator.comparingDouble(v -> -v.getValue()))
			.collect(Collectors.toList())) {
			eb.appendDescription(getEmoji(entry.getKey()) + " " + idToName(entry.getKey()) + " ➜ " + formatNumber(entry.getValue()) + "\n");
		}
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				embed(getCoinsPerBit());
			}
		}
			.queue();
	}
}
