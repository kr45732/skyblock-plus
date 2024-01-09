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

package com.skyblockplus.features.mayor;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.miscellaneous.CalendarSlashCommand.YEAR_0;
import static com.skyblockplus.miscellaneous.CalendarSlashCommand.getSkyblockYear;
import static com.skyblockplus.utils.ApiHandler.getHypixelApiUrl;
import static com.skyblockplus.utils.Constants.MAYOR_NAME_TO_SKIN;
import static com.skyblockplus.utils.Constants.mayorNameToEmoji;
import static com.skyblockplus.utils.utils.HttpUtils.getJson;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.features.listeners.AutomaticGuild;
import java.io.File;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.http.client.utils.URIBuilder;

public class MayorHandler {

	public static String currentMayor = "";
	public static String currentJerryMayor = "";
	public static int currentMayorYear = 0;
	public static ScheduledFuture<?> jerryFuture;
	public static ScheduledFuture<?> mayorElectedFuture;
	public static MessageEmbed jerryEmbed = errorEmbed("Jerry is not currently mayor").build();

	public static void initialize() {
		try {
			if (currentMayor.isEmpty()) {
				JsonElement mayorJson = getJson(getHypixelApiUrl("/resources/skyblock/election", false));
				currentMayor = higherDepth(mayorJson, "mayor.name", "");
				currentMayorYear = higherDepth(mayorJson, "mayor.election.year", 0);
			}

			long msTillElected = YEAR_0 + 446400000L * (getSkyblockYear() - 1) + 105600000 - Instant.now().toEpochMilli() + 300000;
			if (mayorElectedFuture == null && msTillElected > 0) {
				mayorElectedFuture = scheduler.schedule(MayorHandler::mayorElected, msTillElected, TimeUnit.MILLISECONDS);
			}

			updateCurrentElection();

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
			JsonElement cur = higherDepth(getJson(getHypixelApiUrl("/resources/skyblock/election", false)), "mayor");
			JsonArray mayors = collectJsonArray(
				streamJsonArray(higherDepth(cur, "election.candidates"))
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
							.append(cleanMcCodes(higherDepth(perk, "description").getAsString()));
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
			eb.addField("Next Election", "Opens " + getRelativeTimestamp(YEAR_0 + 446400000L * (getSkyblockYear() - 1) + 217200000), false);

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

				if (updateCount != 0 && updateCount % 12 == 0) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (Exception ignored) {}
				}
			}

			scheduler.schedule(() -> mayorElectedFuture = null, 30, TimeUnit.MINUTES);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateMayorJerryRotations() {
		try {
			if (!currentMayor.equals("Jerry")) {
				jerryFuture = null;
				jerryEmbed = errorEmbed("Jerry is not currently mayor").build();
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
			JsonElement cur = higherDepth(getJson(getHypixelApiUrl("/resources/skyblock/election", false)), "current");
			if (higherDepth(cur, "candidates") == null) { // Election not open
				return;
			}

			JsonArray curMayors = collectJsonArray(
				streamJsonArray(higherDepth(cur, "candidates")).sorted(Comparator.comparingInt(m -> -higherDepth(m, "votes").getAsInt()))
			);
			double totalVotes = streamJsonArray(curMayors).mapToInt(m -> higherDepth(m, "votes").getAsInt()).sum();
			int year = higherDepth(cur, "year").getAsInt();
			EmbedBuilder eb = defaultEmbed("Mayor Election Open | Year " + year);
			eb.setDescription(
				"**Year:** " +
				year +
				"\n**Total Votes:** " +
				formatNumber(totalVotes) +
				"\n**Closes:** " +
				getRelativeTimestamp(
					YEAR_0 + 446400000L * (year == getSkyblockYear() ? getSkyblockYear() : getSkyblockYear() - 1) + 105600000
				)
			);
			for (JsonElement curMayor : curMayors) {
				StringBuilder perksStr = new StringBuilder();
				for (JsonElement perk : higherDepth(curMayor, "perks").getAsJsonArray()) {
					perksStr
						.append("\n➜ ")
						.append(higherDepth(perk, "name").getAsString())
						.append(": ")
						.append(cleanMcCodes(higherDepth(perk, "description").getAsString()));
				}

				int votes = higherDepth(curMayor, "votes").getAsInt();
				String name = higherDepth(curMayor, "name").getAsString();
				eb.addField(
					mayorNameToEmoji.get(name.toUpperCase()) +
					" " +
					name +
					" | " +
					formatNumber(votes) +
					" (" +
					roundProgress(votes / totalVotes) +
					")",
					perksStr.toString(),
					false
				);
			}

			File mayorGraphFile = null;
			try {
				mayorGraphFile = new File(rendersDirectory + "/mayor_graph.png");
				ImageIO.write(
					ImageIO.read(
						new URIBuilder("https://quickchart.io/chart")
							.addParameter("bkg", "#2b2d31")
							.addParameter(
								"c",
								"{ type: 'bar', data: { labels: [" +
								streamJsonArray(curMayors)
									.map(m -> "'" + higherDepth(m, "name").getAsString() + "'")
									.collect(Collectors.joining(",")) +
								"], datasets: [{ data: [" +
								streamJsonArray(curMayors)
									.map(m -> higherDepth(m, "votes").getAsString())
									.collect(Collectors.joining(",")) +
								"], backgroundColor: getGradientFillHelper('vertical', [\"#023020\"," +
								" \"#32CD32\"]), }] }, options: { title: { display: true, text:" +
								" 'Mayor Election Graph | Year " +
								year +
								"' }, legend: { display: false, } } }"
							)
							.build()
							.toURL()
					),
					"png",
					mayorGraphFile
				);
			} catch (Exception ignored) {}

			MessageEmbed embed;
			if (mayorGraphFile == null || !mayorGraphFile.exists()) {
				embed = eb.build();
			} else {
				embed = eb.setImage("attachment://mayor_graph.png").build();
			}

			int updateCount = 0;
			for (AutomaticGuild guild : guildMap.values()) {
				if (guild.onMayorElection(embed, mayorGraphFile, year)) { // Send or update message
					updateCount++;
				}

				if (updateCount != 0 && updateCount % 12 == 0) {
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
