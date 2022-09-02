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
import static com.skyblockplus.miscellaneous.CalendarSlashCommand.YEAR_0;
import static com.skyblockplus.miscellaneous.CalendarSlashCommand.getSkyblockYear;
import static com.skyblockplus.utils.Constants.MAYOR_NAME_TO_SKIN;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.groovy.util.Maps;

public class MayorHandler {

	public static String currentMayor = "";
	public static String currentJerryMayor = "";
	public static int currentMayorYear = 0;
	public static ScheduledFuture<?> jerryFuture;
	public static MessageEmbed jerryEmbed = invalidEmbed("Jerry is not the current mayor").build();
	public static MessageEmbed votesEmbed = defaultEmbed("Mayor Election Graph").setDescription("Data not loaded").build();
	public static final Map<String, String> mayorNameToEmoji = Maps.of(
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
		try {
			if (currentMayor.isEmpty()) {
				JsonElement mayorJson = getJson("https://api.hypixel.net/resources/skyblock/election");
				currentMayor = higherDepth(mayorJson, "mayor.name", "");
				currentMayorYear = higherDepth(mayorJson, "mayor.election.year", 0);
			}

			long newYearStartEpoch = YEAR_0 + 446400000L * (getSkyblockYear() - 1);
			long newYearToElectionOpen = 217200000;
			long newYearToElectionClose = 105600000;

			long currentTime = Instant.now().toEpochMilli();
			long closeTime = newYearStartEpoch + newYearToElectionClose;
			long openTime = newYearStartEpoch + newYearToElectionOpen;

			if (closeTime < currentTime && currentTime < closeTime + 420000) { // Ended at most 7 min ago
				scheduler.schedule(MayorHandler::mayorElected, 5, TimeUnit.MINUTES);
				scheduler.schedule(MayorHandler::initialize, 15, TimeUnit.MINUTES);
			} else if (closeTime < currentTime && currentTime < openTime) { // Election booth is closed so wait for next open
				scheduler.schedule(MayorHandler::initialize, 5, TimeUnit.MINUTES);
			} else { // Election is open
				updateCurrentElection();
				scheduler.schedule(MayorHandler::initialize, 5, TimeUnit.MINUTES);
			}

			if (currentMayor.equals("Jerry") && jerryFuture == null) {
				jerryFuture = scheduler.schedule(MayorHandler::updateMayorJerryRotations, 30, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void mayorElected() {
		MessageEmbed embed;
		Button button = null;

		try {
			JsonElement cur = higherDepth(getJson("https://api.hypixel.net/resources/skyblock/election"), "mayor");
			JsonArray mayors = collectJsonArray(
				streamJsonArray(higherDepth(cur, "election.candidates").getAsJsonArray())
					.sorted(Comparator.comparingInt(m -> -higherDepth(m, "votes").getAsInt()))
			);

			currentMayor = higherDepth(cur, "name").getAsString();
			currentMayorYear = higherDepth(cur, "election.year").getAsInt();
			double totalVotes = streamJsonArray(mayors).mapToInt(m -> higherDepth(m, "votes").getAsInt()).sum();

			EmbedBuilder eb = defaultEmbed("Mayor Elected | Year " + currentMayorYear);
			eb.setDescription("**Year:** " + currentMayorYear + "\n**Total Votes:** " + formatNumber(totalVotes));
			eb.setThumbnail("https://mc-heads.net/body/" + MAYOR_NAME_TO_SKIN.get(currentMayor.toUpperCase()) + "/left");
			StringBuilder ebStr = new StringBuilder();
			for (JsonElement curMayor : mayors) {
				String name = higherDepth(curMayor, "name").getAsString();
				int votes = higherDepth(curMayor, "votes").getAsInt();

				if (higherDepth(curMayor, "name").getAsString().equals(currentMayor)) {
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
			eb.addField("Losing Mayors", ebStr.toString(), false);
			eb.addField(
				"Next Election",
				"Opens <t:" + Instant.ofEpochMilli(YEAR_0 + 446400000L * (getSkyblockYear() - 1) + 217200000).getEpochSecond() + ":R>",
				false
			);

			embed = eb.build();
			if (currentMayor.equals("Jerry")) {
				button = Button.primary("mayor_jerry_button", "Current Jerry Mayor");
			}
		} catch (Exception e) {
			e.printStackTrace();
			scheduler.schedule(MayorHandler::mayorElected, 5, TimeUnit.MINUTES);
			return;
		}

		try {
			int updateCount = 0;
			for (AutomaticGuild guild : guildMap.values()) {
				if (guild.onMayorElected(embed, button)) { // Send and ping
					updateCount++;
				}

				if (updateCount != 0 && updateCount % 7 == 0) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (Exception ignored) {}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateMayorJerryRotations() {
		try {
			if (!currentMayor.equals("Jerry")) {
				jerryFuture = null;
				jerryEmbed = invalidEmbed("Jerry is not the current mayor").build();
				return;
			}

			JsonElement jerryJson = higherDepth(getJson("https://api.skytils.gg/api/mayor/jerry"), "mayor");
			currentJerryMayor = higherDepth(jerryJson, "name").getAsString();
			EmbedBuilder eb = defaultEmbed(currentJerryMayor)
				.setThumbnail("https://mc-heads.net/body/" + MAYOR_NAME_TO_SKIN.get(currentJerryMayor.toUpperCase()) + "/left");
			for (JsonElement perk : higherDepth(jerryJson, "perks").getAsJsonArray()) {
				eb.addField(higherDepth(perk, "name").getAsString(), higherDepth(perk, "description").getAsString(), false);
			}

			jerryEmbed = eb.build();
		} catch (Exception e) {
			e.printStackTrace();
		}

		jerryFuture = scheduler.schedule(MayorHandler::updateMayorJerryRotations, 5, TimeUnit.MINUTES);
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
				Instant
					.ofEpochMilli(YEAR_0 + 446400000L * (year == getSkyblockYear() ? getSkyblockYear() : getSkyblockYear() - 1) + 105600000)
					.getEpochSecond() +
				":R>"
			);
			StringBuilder votesStr = new StringBuilder();
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

				int voteGlass = (int) (20.0 * votes / totalVotes);
				votesStr
					.append(mayorNameToEmoji.get(name.toUpperCase()))
					.append(" ")
					.append(getEmoji("STAINED_GLASS_PANE:5", "g").repeat(voteGlass))
					.append(getEmoji("STAINED_GLASS_PANE:8", "l").repeat(20 - voteGlass))
					.append("\n");
			}
			votesEmbed = defaultEmbed("Mayor Election Graph | Year " + year).setDescription(votesStr).build();

			MessageEmbed embed = eb.build();
			Button button = Button.primary("mayor_graph_button", "View Graph");

			int updateCount = 0;
			for (AutomaticGuild guild : guildMap.values()) {
				if (guild.onMayorElection(embed, button, year)) { // Send or update message
					updateCount++;
				}

				if (updateCount != 0 && updateCount % 7 == 0) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (Exception ignored) {}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
