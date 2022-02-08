/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.features.mayor;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.miscellaneous.TimeCommand.YEAR_0;
import static com.skyblockplus.miscellaneous.TimeCommand.getSkyblockYear;
import static com.skyblockplus.utils.Constants.MAYOR_NAME_TO_SKIN;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.groovy.util.Maps;

public class MayorHandler {

	public static Map<String, String> mayorNameToEmoji = Maps.of(
		"DERPY",
		"<:derpy:940083649129349150>",
		"FOXY",
		"<:foxy:940083649301315614>",
		"DANTE",
		"<:dante:940083649188081715>",
		"PAUL",
		"<:paul:940083649607508009>",
		"AATROX",
		"<:aatrox:940083649041293312>",
		"DIAZ",
		"<:diaz:940083649322303489>",
		"DIANA",
		"<:diana:940083649590739004>",
		"COLE",
		"<:cole:940083649565581362>",
		"BARRY",
		"<:barry:940083649200652338>",
		"JERRY",
		"<:jerry:940083649318125578>",
		"SCORPIUS",
		"<:scorpius:940083649687203951>",
		"MARINA",
		"<:marina:940083649783664660>"
	);

	public static void initialize() {
		long newYearStartEpoch = YEAR_0 + 446400000L * (getSkyblockYear() - 1);
		long newYearToElectionOpen = 217200000;
		long newYearToElectionClose = 105600000;
		long epochMilliNow = Instant.now().toEpochMilli();

		if ((newYearStartEpoch + newYearToElectionOpen >= epochMilliNow) || (newYearStartEpoch + newYearToElectionClose < epochMilliNow)) { // Election booth is open
			updateCurrentElection();
		} else if (newYearStartEpoch + newYearToElectionClose <= (epochMilliNow + 420000)) { // Ended at most 7 min ago
			mayorElected();
			scheduler.schedule(MayorHandler::initialize, 10, TimeUnit.MINUTES);
		} else { // Wait for next open
			scheduler.schedule(
				MayorHandler::initialize,
				newYearStartEpoch + newYearToElectionClose - epochMilliNow + 1000,
				TimeUnit.MILLISECONDS
			);
		}
	}

	public static void mayorElected() {
		JsonElement cur = higherDepth(getJson("https://api.hypixel.net/resources/skyblock/election"), "mayor");
		JsonArray mayors = collectJsonArray(
			streamJsonArray(higherDepth(cur, "election.candidates").getAsJsonArray())
				.sorted(Comparator.comparingInt(m -> -higherDepth(m, "votes").getAsInt()))
		);

		String winner = higherDepth(cur, "name").getAsString();
		int year = higherDepth(cur, "election.year").getAsInt();
		double totalVotes = streamJsonArray(mayors).mapToInt(m -> higherDepth(m, "votes").getAsInt()).sum();

		EmbedBuilder eb = defaultEmbed("Mayor Elected | Year " + year);
		eb.setDescription("**Year:** " + year + "\n**Total Votes:** " + formatNumber(totalVotes));
		eb.setThumbnail("https://mc-heads.net/body/" + MAYOR_NAME_TO_SKIN.get(winner.toUpperCase()) + "/left");
		StringBuilder ebStr = new StringBuilder();
		for (JsonElement curMayor : mayors) {
			String name = higherDepth(curMayor, "name").getAsString();
			int votes = higherDepth(curMayor, "votes").getAsInt();

			if (higherDepth(curMayor, "name").getAsString().equals(winner)) {
				StringBuilder perksStr = new StringBuilder();
				for (JsonElement perk : higherDepth(curMayor, "perks").getAsJsonArray()) {
					perksStr
						.append("\n➜ ")
						.append(higherDepth(perk, "name").getAsString())
						.append(": ")
						.append(parseMcCodes(higherDepth(perk, "description").getAsString()));
				}

				eb.addField(
					mayorNameToEmoji.get(name.toUpperCase()) + " Mayor " + name,
					"\n**Votes:** " + roundProgress(votes / totalVotes) + " (" + formatNumber(votes) + ")\n**Perks:**" + perksStr,
					false
				);
			} else {
				ebStr
					.append("\n")
					.append(mayorNameToEmoji.get(name.toUpperCase()))
					.append(" **")
					.append(name)
					.append(":** ")
					.append(roundProgress(votes / totalVotes))
					.append(" (")
					.append(formatNumber(votes))
					.append(")");
			}
		}
		eb.addField("Loosing Mayors", ebStr.toString(), false);
		eb.addField(
			"Next Election",
			"Opens <t:" + Instant.ofEpochMilli(YEAR_0 + 446400000L * (getSkyblockYear() - 1) + 217200000).getEpochSecond() + ":R>",
			false
		);

		MessageEmbed embed = eb.build();
		for (AutomaticGuild guild : guildMap.values()) {
			guild.onMayorElected(embed); // Send and ping
		}
	}

	public static void updateCurrentElection() {
		try {
			JsonElement cur = higherDepth(getJson("https://api.hypixel.net/resources/skyblock/election"), "current");
			if (higherDepth(cur, "candidates") == null) {
				return;
			}

			JsonArray curMayors = collectJsonArray(
				streamJsonArray(higherDepth(cur, "candidates").getAsJsonArray())
					.sorted(Comparator.comparingInt(m -> -higherDepth(m, "votes").getAsInt()))
			);
			double totalVotes = streamJsonArray(curMayors).mapToInt(m -> higherDepth(m, "votes").getAsInt()).sum();
			int year = higherDepth(cur, "year").getAsInt();
			EmbedBuilder eb = defaultEmbed("Mayor Election Open | Year " + year);
			eb.setDescription(
				"**Year:** " +
				year +
				"\n**Total Votes:** " +
				formatNumber(totalVotes) +
				"\n**Closes:** <t:" +
				Instant.ofEpochMilli(YEAR_0 + 446400000L * (getSkyblockYear() - 1) + 105600000).getEpochSecond() +
				":R>"
			);
			for (JsonElement curMayor : curMayors) {
				StringBuilder perksStr = new StringBuilder();
				for (JsonElement perk : higherDepth(curMayor, "perks").getAsJsonArray()) {
					perksStr
						.append("\n➜ ")
						.append(higherDepth(perk, "name").getAsString())
						.append(": ")
						.append(parseMcCodes(higherDepth(perk, "description").getAsString()));
				}

				int votes = higherDepth(curMayor, "votes").getAsInt();
				String name = higherDepth(curMayor, "name").getAsString();
				eb.addField(
					mayorNameToEmoji.get(name.toUpperCase()) + " " + name,
					"**Votes:** " + roundProgress(votes / totalVotes) + " (" + formatNumber(votes) + ")\n**Perks:**" + perksStr,
					false
				);
			}
			MessageEmbed embed = eb.build();
			for (AutomaticGuild guild : guildMap.values()) {
				guild.onMayorElection(embed, year); // Send or update message
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		scheduler.schedule(MayorHandler::initialize, 5, TimeUnit.MINUTES);
	}
}
