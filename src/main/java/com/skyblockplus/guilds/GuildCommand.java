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

package com.skyblockplus.guilds;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Constants.GUILD_EXP_TO_LEVEL;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class GuildCommand extends Command {

	public GuildCommand() {
		this.name = "guild";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getGuildExp(
		JsonElement guildJson,
		long days,
		String playerUsername,
		User user,
		MessageChannel channel,
		InteractionHook hook
	) {
		JsonElement members = higherDepth(guildJson, "members");
		JsonArray membersArr = members.getAsJsonArray();
		List<String> guildExpList = new ArrayList<>();
		List<CompletableFuture<String>> futures = new ArrayList<>();
		for (int i = 0; i < membersArr.size(); i++) {
			int finalI = i;
			futures.add(
				asyncUuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString())
					.thenApply(
						currentUsername -> {
							JsonElement expHistory = higherDepth(membersArr.get(finalI), "expHistory");
							List<String> keys = getJsonKeys(expHistory);
							int totalPlayerExp = 0;

							for (int j = 0; j < days; j++) {
								String value = keys.get(j);
								totalPlayerExp += higherDepth(expHistory, value, 0);
							}
							return currentUsername + "=:=" + totalPlayerExp;
						}
					)
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

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(2).setItemsPerPage(20);
		PaginatorExtras extras = new PaginatorExtras()
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
		paginateBuilder.setPaginatorExtras(extras);

		for (int i = 0; i < guildExpList.size(); i++) {
			String[] curG = guildExpList.get(i).split("=:=");
			paginateBuilder.addItems(
				"`" + (i + 1) + ")` " + fixUsername(curG[0]) + ": " + formatNumber(Integer.parseInt(curG[1])) + " EXP  "
			);
		}

		if (channel != null) {
			paginateBuilder.build().paginate(channel, 0);
		} else {
			paginateBuilder.build().paginate(hook, 0);
		}

		return null;
	}

	public static EmbedBuilder getGuildExpFromPlayer(String username, long days, User user, MessageChannel channel, InteractionHook hook) {
		if (days < 1 || days > 7) {
			return invalidEmbed("Days must be between 1 to 7");
		}

		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.playerUuid);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		JsonElement guildJson = hypixelResponse.response;

		return getGuildExp(guildJson, days, usernameUuid.playerUsername, user, channel, hook);
	}

	public static EmbedBuilder getGuildExpFromName(String guildName, long days, CommandEvent event) {
		if (days < 1 || days > 7) {
			return invalidEmbed("Days must be between 1 to 7");
		}

		HypixelResponse hypixelResponse = getGuildFromName(guildName);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		JsonElement guildJson = hypixelResponse.response;

		return getGuildExp(guildJson, days, null, event.getAuthor(), event.getChannel(), null);
	}

	public static EmbedBuilder getGuildPlayer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.playerUuid);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		JsonElement guildJson = hypixelResponse.response;

		try {
			String guildName = higherDepth(guildJson, "name").getAsString();
			EmbedBuilder eb = defaultEmbed(
				usernameUuid.playerUsername + " is in " + guildName,
				"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString()
			);
			eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
			eb.setThumbnail("https://cravatar.eu/helmavatar/" + usernameUuid.playerUuid + "/64.png");
			return eb;
		} catch (Exception e) {
			return defaultEmbed(usernameUuid.playerUsername + " is not in a guild");
		}
	}

	public static EmbedBuilder getGuildInfo(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.playerUuid);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		JsonElement guildJson = hypixelResponse.response;

		String guildName = higherDepth(guildJson, "name").getAsString();

		EmbedBuilder eb = defaultEmbed(
			guildName,
			"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString()
		);
		eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

		return eb;
	}

	public static EmbedBuilder guildInfoFromGuildName(String guildName) {
		try {
			HypixelResponse guildResponse = getGuildFromName(guildName);
			if (guildResponse.isNotValid()) {
				return invalidEmbed(guildResponse.failCause);
			}
			JsonElement guildJson = guildResponse.response;
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
		String[] date = Date.from(Instant.ofEpochMilli(created.getAsLong())).toString().split(" ");
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
						uuidToUsername(higherDepth(currentMember, "uuid").getAsString()).playerUsername
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

	public static EmbedBuilder getGuildMembers(JsonElement guildJson, User user, MessageChannel channel, InteractionHook hook) {
		JsonArray membersArr = higherDepth(guildJson, "members").getAsJsonArray();
		List<CompletableFuture<String>> futures = new ArrayList<>();
		List<String> guildMembers = new ArrayList<>();
		for (int i = 0; i < membersArr.size(); i++) {
			futures.add(asyncUuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString()));
		}

		for (CompletableFuture<String> future : futures) {
			try {
				guildMembers.add(future.get());
			} catch (Exception ignored) {}
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(3).setItemsPerPage(33);

		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageTitle(higherDepth(guildJson, "name").getAsString())
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "_id").getAsString())
		);

		for (String member : guildMembers) {
			if (member != null) {
				paginateBuilder.addItems("• " + fixUsername(member) + "  ");
			}
		}

		if (channel != null) {
			paginateBuilder.build().paginate(channel, 0);
		} else {
			paginateBuilder.build().paginate(hook, 0);
		}
		return null;
	}

	public static EmbedBuilder getGuildMembersFromPlayer(String username, User user, MessageChannel channel, InteractionHook hook) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.failCause);
		}

		HypixelResponse hypixelResponse = getGuildFromPlayer(usernameUuid.playerUuid);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		JsonElement guildJson = hypixelResponse.response;

		return getGuildMembers(guildJson, user, channel, hook);
	}

	public static EmbedBuilder getGuildMembersFromName(String guildName, CommandEvent event) {
		HypixelResponse hypixelResponse = getGuildFromName(guildName);
		if (hypixelResponse.isNotValid()) {
			return invalidEmbed(hypixelResponse.failCause);
		}
		JsonElement guildJson = hypixelResponse.response;

		return getGuildMembers(guildJson, event.getAuthor(), event.getChannel(), null);
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

				if ((args.length == 3 || args.length == 4) && ("experience".equals(args[1]) || "exp".equals(args[1]))) {
					int days = 7;
					if (args.length == 4 && args[3].startsWith("days:")) {
						try {
							days = Integer.parseInt(args[3].split("days:")[1]);
						} catch (Exception e) {
							embed(invalidEmbed("Invalid days amount"));
							return;
						}
					}

					if (args[2].startsWith("u:")) {
						paginate(getGuildExpFromPlayer(args[2].split("u:")[1], days, event.getAuthor(), event.getChannel(), null));
						return;
					} else if (args[2].startsWith("g:")) {
						paginate(getGuildExpFromName(args[2].split("g:")[1], days, event));
						return;
					}
				} else if (args.length >= 3 && (args[1].equals("information") || args[1].equals("info"))) {
					if (args[2].toLowerCase().startsWith("u:")) {
						embed(getGuildInfo(args[2].split(":")[1]));
						return;
					} else if (args[2].startsWith("g:")) {
						embed(guildInfoFromGuildName(args[2].split(":")[1]));
						return;
					}
				} else if (args.length == 3 && "members".equals(args[1])) {
					if (args[2].startsWith("u:")) {
						paginate(getGuildMembersFromPlayer(args[2].split("u:")[1], event.getAuthor(), event.getChannel(), null));
						return;
					} else if (args[2].startsWith("g:")) {
						paginate(getGuildMembersFromName(args[2].split("g:")[1], event));
						return;
					}
				} else if (args.length == 2) {
					embed(getGuildPlayer(args[1]));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
