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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.apache.groovy.util.Maps;
import org.springframework.stereotype.Component;

@Component
public class ReforgeSlashCommand extends SlashCommand {

	public static final Map<String, String> statToEmoji = Maps.of(
		"HEALTH",
		"❤️",
		"DEFENSE",
		getEmoji("IRON_CHESTPLATE"),
		"STRENGTH",
		getEmoji("BLAZE_POWDER"),
		"SPEED",
		getEmoji("SUGAR"),
		"CRIT_CHANCE",
		"☣️",
		"CRIT_DAMAGE",
		"☠️",
		"INTELLIGENCE",
		getEmoji("ENCHANTED_BOOK"),
		"MINING_SPEED",
		"⛏️",
		"BONUS_ATTACK_SPEED",
		"⚔️",
		"SEA_CREATURE_CHANCE",
		"\uD83D\uDC20",
		"MAGIC_FIND",
		"\uD83C\uDF1F",
		"PET_LUCK",
		"♣️",
		"TRUE_DEFENSE",
		getEmoji("DIAMOND_CHESTPLATE"),
		"FEROCITY",
		"\uD83D\uDDE1️",
		"MINING_FORTUNE",
		"☘️",
		"DAMAGE",
		getEmoji("IRON_SWORD")
	);

	public ReforgeSlashCommand() {
		this.name = "reforge";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getReforgeStone(event.getOptionStr("item")));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get the reforge stone stats for each rarity")
			.addOption(OptionType.STRING, "item", "Reforge stone name", true, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("item")) {
			event.replyClosestMatch(
				event.getFocusedOption().getValue(),
				getReforgeStonesJson().keySet().stream().map(Utils::idToName).collect(Collectors.toList())
			);
		}
	}

	public static EmbedBuilder getReforgeStone(String reforgeStone) {
		JsonObject reforgeStonesJson = getReforgeStonesJson();
		String closestMatch = getClosestMatchFromIds(nameToId(reforgeStone), reforgeStonesJson.keySet());
		JsonElement reforgeStoneJson = higherDepth(reforgeStonesJson, closestMatch);

		EmbedBuilder eb = defaultEmbed(idToName(closestMatch));
		eb.setDescription(
			"**Reforge:** " +
			higherDepth(reforgeStoneJson, "reforgeName").getAsString() +
			"\n**Item Types:** " +
			Arrays
				.stream(higherDepth(reforgeStoneJson, "itemTypes").getAsString().split("\n"))
				.map(Utils::capitalizeString)
				.collect(Collectors.joining(", "))
		);
		if (higherDepth(reforgeStoneJson, "reforgeAbility", null) != null) {
			eb.appendDescription("\n**Ability:** " + parseMcCodes(higherDepth(reforgeStoneJson, "reforgeAbility").getAsString()));
		}
		JsonElement reforgeStats = higherDepth(reforgeStoneJson, "reforgeStats");
		if (reforgeStats == null) {
			reforgeStats = JsonParser.parseString("{\"COMMON\":{},\"UNCOMMON\":{},\"RARE\":{},\"EPIC\":{},\"LEGENDARY\":{},\"MYTHIC\":{}}");
		}
		for (Map.Entry<String, JsonElement> stat : reforgeStats.getAsJsonObject().entrySet()) {
			eb.addField(
				capitalizeString(stat.getKey()),
				"Cost: " +
				formatNumber(higherDepth(reforgeStoneJson, "reforgeCosts." + stat.getKey()).getAsLong()) +
				"\n" +
				(
					higherDepth(reforgeStoneJson, "reforgeAbility." + stat.getKey(), null) != null
						? (
							"➜ Ability: " +
							parseMcCodes(higherDepth(reforgeStoneJson, "reforgeAbility." + stat.getKey(), null)).replace("\n", " ") +
							"\n"
						)
						: ""
				) +
				stat
					.getValue()
					.getAsJsonObject()
					.entrySet()
					.stream()
					.map(e ->
						statToEmoji.get(e.getKey().toUpperCase()) +
						" " +
						capitalizeString(e.getKey().replace("_", " ")) +
						" ➜ " +
						e.getValue().getAsInt()
					)
					.collect(Collectors.joining("\n")),
				false
			);
		}
		return eb.setThumbnail(getItemThumbnail(closestMatch));
	}
}
