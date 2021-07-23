package com.skyblockplus.guilds;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Constants.guildExpToLevel;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class GuildCommand extends Command {

	public GuildCommand() {
		this.name = "guild";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g" };
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
							try {
								JsonElement expHistory = higherDepth(membersArr.get(finalI), "expHistory");
								List<String> keys = getJsonKeys(expHistory);
								int totalPlayerExp = 0;

								for (int j = 0; j < days; j++) {
									String value = keys.get(j);
									totalPlayerExp += higherDepth(expHistory, value).getAsInt();
								}
								return currentUsername + "=:=" + totalPlayerExp;
							} catch (Exception ignored) {}
							return null;
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

		int guildExp = higherDepth(guildJson, "exp").getAsInt();

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

		return getGuildMembers(guildJson, user, channel, null);
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
			int expNeeded = i >= guildExpToLevel.size() ? guildExpToLevel.get(guildExpToLevel.size() - 1) : guildExpToLevel.get(i);
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
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessageEmbeds(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if ((args.length == 3 || args.length == 4) && ("experience".equals(args[1]) || "exp".equals(args[1]))) {
					int days = 7;
					if (args.length == 4 && args[3].startsWith("days:")) {
						try {
							days = Integer.parseInt(args[3].split("days:")[1]);
						} catch (Exception e) {
							ebMessage.editMessageEmbeds(invalidEmbed("Invalid days amount").build()).queue();
							return;
						}
					}

					if (args[2].startsWith("u:")) {
						String username = args[2].split("u:")[1];
						eb = getGuildExpFromPlayer(username, days, event.getAuthor(), event.getChannel(), null);
						if (eb == null) {
							ebMessage.delete().queue();
						} else {
							ebMessage.editMessageEmbeds(eb.build()).queue();
						}
						return;
					} else if (args[2].startsWith("g:")) {
						String guildName = args[2].split("g:")[1];
						eb = getGuildExpFromName(guildName, days, event);
						if (eb == null) {
							ebMessage.delete().queue();
						} else {
							ebMessage.editMessageEmbeds(eb.build()).queue();
						}
						return;
					}
				} else if (args.length >= 3 && (args[1].equals("information") || args[1].equals("info"))) {
					if (args[2].toLowerCase().startsWith("u:")) {
						String usernameInfo = args[2].split(":")[1];
						ebMessage.editMessageEmbeds(getGuildInfo(usernameInfo).build()).queue();
						return;
					} else if (args[2].startsWith("g:")) {
						String guildName = content.split(":")[1];
						ebMessage.editMessageEmbeds(guildInfoFromGuildName(guildName).build()).queue();
						return;
					}
				} else if (args.length == 3 && "members".equals(args[1])) {
					if (args[2].startsWith("u:")) {
						String playerName = args[2].split("u:")[1];
						eb = getGuildMembersFromPlayer(playerName, event.getAuthor(), event.getChannel(), null);
						if (eb == null) {
							ebMessage.delete().queue();
						} else {
							ebMessage.editMessageEmbeds(eb.build()).queue();
						}
						return;
					} else if (args[2].startsWith("g:")) {
						String guildName = args[2].split("g:")[1];
						eb = getGuildMembersFromName(guildName, event);
						if (eb == null) {
							ebMessage.delete().queue();
						} else {
							ebMessage.editMessageEmbeds(eb.build()).queue();
						}
						return;
					}
				} else if (args.length == 2) {
					ebMessage.editMessageEmbeds(getGuildPlayer(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
