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
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.groovy.util.Maps;

public class ReforgeStoneCommand extends Command {

	public static final Map<String, String> statToEmoji = Maps.of(
		"HEALTH",
		"❤️",
		"DEFENSE",
		"<:iron_chestplate:939021594775408691>",
		"STRENGTH",
		"<:blaze_powder:939020829486874635>",
		"SPEED",
		"<:sugar:939024464564342805>",
		"CRIT_CHANCE",
		"☣️",
		"CRIT_DAMAGE",
		"☠️",
		"INTELLIGENCE",
		"<:common:939021148484698132>",
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
		"<:diamond_chestplate:939021063428395098>",
		"FEROCITY",
		"\uD83D\uDDE1️",
		"MINING_FORTUNE",
		"☘️"
	);

	public ReforgeStoneCommand() {
		this.name = "reforgestone";
		this.aliases = new String[] { "reforge" };
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				setArgs(2);
				embed(getReforgeStone(args[1]));
			}
		}
			.queue();
	}
}
