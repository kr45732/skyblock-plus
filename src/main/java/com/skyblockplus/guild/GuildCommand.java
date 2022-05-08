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

package com.skyblockplus.guild;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.GUILD_EXP_TO_LEVEL;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class GuildCommand extends Command {

	public GuildCommand() {
		this.name = "guild";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getGuildExp(JsonElement guildJson, long days, String playerUsername, PaginatorEvent event) {
		JsonElement members = higherDepth(guildJson, "members");
		JsonArray membersArr = members.getAsJsonArray();
		List<String> guildExpList = new ArrayList<>();
		List<CompletableFuture<String>> futures = new ArrayList<>();
		for (int i = 0; i < membersArr.size(); i++) {
			int finalI = i;
			futures.add(
				asyncUuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString())
					.thenApply(currentUsername -> {
						JsonElement expHistory = higherDepth(membersArr.get(finalI), "expHistory");
						List<String> keys = getJsonKeys(expHistory);
						int totalPlayerExp = 0;

						for (int j = 0; j < days; j++) {
							String value = keys.get(j);
							totalPlayerExp += higherDepth(expHistory, value, 0);
						}
						return currentUsername + "=:=" + totalPlayerExp;
					})
			);
		}

		for (CompletableFuture<String> future : futures) {
			try {
				String futureResponse = future.get();
				if (futureResponse != null) {
					guildExpList.add(futureResponse);
				}
			} catch (Exception ignored) {}
		}

		guildExpList.sort(Comparator.comparingInt(o1 -> -Integer.parseInt(o1.split("=:=")[1])));

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);
		PaginatorExtras extras = paginateBuilder
			.getExtras()
			.setEveryPageTitle(higherDepth(guildJson, "name").getAsString())
			.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString());

		if (playerUsername != null) {
			int guildRank = -2;
			int guildExp = -1;
			for (int i = 0; i < guildExpList.size(); i++) {
				String[] curGuildRank = guildExpList.get(i).split("=:=");
				if (curGuildRank[0].equals(playerUsername)) {
					guildRank = i;
					guildExp = Integer.parseInt(curGuildRank[1]);
					break;
				}
			}
			extras.setEveryPageText(
				"**Player:** " + playerUsername + "\n**Guild Rank:** #" + (guildRank + 1) + "\n**Exp:** " + formatNumber(guildExp)
			);
		}

		for (int i = 0; i < guildExpList.size(); i++) {
			String[] curG = guildExpList.get(i).split("=:=");
			paginateBuilder.addItems(
				"`" + (i + 1) + ")` " + fixUsername(curG[0]) + ": " + formatNumber(Integer.parseInt(curG[1])) + " EXP  "
			);
		}

		event.paginate(paginateBuilder);

		return null;
	}

	public static EmbedBuilder getGuildExpFromPlayer(String username, long days, PaginatorEvent event) {
		if (days < 1 || days > 7) {
			return invalidEmbed("Days must be between 1 to 7");
		}

		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause());
		}
		JsonElement guildJson = hypixelResponse.response();

		return getGuildExp(guildJson, days, usernameUuid.username(), event);
	}

	public static EmbedBuilder getGuildExpFromName(String guildName, long days, PaginatorEvent event) {
		if (days < 1 || days > 7) {
			return invalidEmbed("Days must be between 1 to 7");
		}

		HypixelResponse hypixelResponse = getGuildFromName(guildName);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause());
		}
		JsonElement guildJson = hypixelResponse.response();

		return getGuildExp(guildJson, days, null, event);
	}

	public static EmbedBuilder getGuildInfoFromPlayer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause());
		}
		JsonElement guildJson = hypixelResponse.response();

		String guildName = higherDepth(guildJson, "name").getAsString();

		EmbedBuilder eb = defaultEmbed(
			guildName,
			"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString()
		);
		eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

		return eb;
	}

	public static EmbedBuilder getGuildInfoFromName(String guildName) {
		try {
			HypixelResponse guildResponse = getGuildFromName(guildName);
			if (guildResponse.isNotValid()) {
				return invalidEmbed(guildResponse.failCause());
			}
			JsonElement guildJson = guildResponse.response();
			guildName = higherDepth(guildJson, "name").getAsString();

			EmbedBuilder eb = defaultEmbed(
				guildName,
				"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString()
			);
			eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
			return eb;
		} catch (Exception e) {
			return defaultEmbed("Error fetching guild data");
		}
	}

	private static String getGuildInfo(JsonElement guildJson) {
		String guildInfo = "";
		String guildName = higherDepth(guildJson, "name").getAsString();

		JsonElement created = higherDepth(guildJson, "created");
		String[] date = Date.from(Instant.ofEpochMilli(created.getAsLong())).toString().split("\\s+");
		guildInfo += ("• " + guildName + " was created on " + date[1] + " " + date[2] + ", " + date[5]) + "\n";

		JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
		for (int i = 0; i < guildMembers.size(); i++) {
			JsonElement currentMember = guildMembers.get(i).getAsJsonObject();
			if (higherDepth(currentMember, "rank").getAsString().equals("Guild Master")) {
				guildInfo +=
					(
						"• " +
						guildName +
						"'s guild master is " +
						uuidToUsername(higherDepth(currentMember, "uuid").getAsString()).username()
					) +
					"\n";
				break;
			}
		}

		int numGuildMembers = higherDepth(guildJson, "members").getAsJsonArray().size();
		guildInfo += ("• " + guildName + " has " + numGuildMembers + " members") + "\n";
		JsonArray preferredGames;
		try {
			preferredGames = higherDepth(guildJson, "preferredGames").getAsJsonArray();
		} catch (Exception e) {
			preferredGames = new JsonArray();
		}
		if (preferredGames.size() > 1) {
			String prefString = preferredGames.toString();
			prefString = prefString.substring(1, prefString.length() - 1).toLowerCase().replace("\"", "").replace(",", ", ");
			String firstHalf = prefString.substring(0, prefString.lastIndexOf(","));
			String lastHalf = prefString.substring(prefString.lastIndexOf(",") + 1);
			if (preferredGames.size() > 2) {
				guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + ", and" + lastHalf) + "\n";
			} else {
				guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + " and" + lastHalf) + "\n";
			}
		} else if (preferredGames.size() == 1) {
			guildInfo += ("• " + guildName + "'s preferred game is " + preferredGames.get(0).getAsString().toLowerCase()) + "\n";
		}

		int guildExp = higherDepth(guildJson, "exp", 0);

		guildInfo += ("• " + guildName + " is guild level " + guildExpToLevel(guildExp)) + "\n";

		return guildInfo;
	}

	public static EmbedBuilder getGuildMembers(JsonElement guildJson, PaginatorEvent event) {
		JsonArray membersArr = higherDepth(guildJson, "members").getAsJsonArray();
		Map<CompletableFuture<String>, Integer> futures = new HashMap<>();
		Map<String, Integer> guildMembers = new HashMap<>();
		Map<String, Integer> ranksMap = streamJsonArray(higherDepth(guildJson, "ranks").getAsJsonArray())
			.collect(Collectors.toMap(m -> higherDepth(m, "name").getAsString(), m -> higherDepth(m, "priority", 0)));
		for (JsonElement member : membersArr) {
			String rank = higherDepth(member, "rank").getAsString();
			futures.put(
				asyncUuidToUsername(higherDepth(member, "uuid").getAsString()),
				rank.equals("Guild Master") ? 50 : ranksMap.getOrDefault(higherDepth(member, "rank").getAsString(), 0)
			);
		}

		for (Map.Entry<CompletableFuture<String>, Integer> future : futures.entrySet()) {
			try {
				guildMembers.put(future.getKey().get(), future.getValue());
			} catch (Exception ignored) {}
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(3).setItemsPerPage(33);

		paginateBuilder
			.getPaginatorExtras()
			.setEveryPageText("**Size:** " + membersArr.size())
			.setEveryPageTitle(higherDepth(guildJson, "name").getAsString() + " Members")
			.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString())
			.setEveryPageFirstFieldTitle("Members:");

		for (String member : guildMembers
			.entrySet()
			.stream()
			.sorted(Comparator.comparingInt(m -> -m.getValue()))
			.map(Map.Entry::getKey)
			.collect(Collectors.toList())) {
			if (member != null) {
				paginateBuilder.addItems("• [" + fixUsername(member) + "](" + skyblockStatsLink(member, null) + ")  ");
			}
		}

		event.paginate(paginateBuilder);
		return null;
	}

	public static EmbedBuilder getGuildMembersFromPlayer(String username, PaginatorEvent event) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause());
		}
		JsonElement guildJson = hypixelResponse.response();

		return getGuildMembers(guildJson, event);
	}

	public static EmbedBuilder getGuildMembersFromName(String guildName, PaginatorEvent event) {
		HypixelResponse hypixelResponse = getGuildFromName(guildName);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause());
		}
		JsonElement guildJson = hypixelResponse.response();

		return getGuildMembers(guildJson, event);
	}

	private static int guildExpToLevel(int guildExp) {
		int guildLevel = 0;

		for (int i = 0;; i++) {
			int expNeeded = i >= GUILD_EXP_TO_LEVEL.size()
				? GUILD_EXP_TO_LEVEL.get(GUILD_EXP_TO_LEVEL.size() - 1)
				: GUILD_EXP_TO_LEVEL.get(i);
			guildExp -= expNeeded;
			if (guildExp < 0) {
				return guildLevel;
			} else {
				guildLevel++;
			}
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length >= 2 && (args[1].equals("experience") || args[1].equals("exp"))) {
					int days = getIntOption("days", 7);

					setArgs(3);
					if (args.length >= 3 && args[2].startsWith("g:")) {
						paginate(getGuildExpFromName(args[2].split("g:")[1], days, new PaginatorEvent(event)));
					} else {
						if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
							return;
						}

						paginate(getGuildExpFromPlayer(player, days, new PaginatorEvent(event)));
					}
				} else if (args.length >= 2 && "members".equals(args[1])) {
					setArgs(3);

					if (args.length >= 3 && args[2].startsWith("g:")) {
						paginate(getGuildMembersFromName(args[2].split("g:")[1], new PaginatorEvent(event)));
					} else {
						if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
							return;
						}

						paginate(getGuildMembersFromPlayer(player, new PaginatorEvent(event)));
					}
				} else {
					setArgs(2);

					if (args.length >= 2 && args[1].startsWith("g:")) {
						embed(getGuildInfoFromName(args[1].split("g:")[1]));
					} else {
						if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
							return;
						}

						embed(getGuildInfoFromPlayer(player));
					}
				}
			}
		}
			.queue();
	}
}
