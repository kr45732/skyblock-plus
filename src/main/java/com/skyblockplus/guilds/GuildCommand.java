package com.skyblockplus.guilds;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
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

	public static EmbedBuilder getGuildExp(String username, User user, MessageChannel channel, InteractionHook hook) {
		UsernameUuidStruct uuidUsername = usernameToUuid(username);
		if (uuidUsername == null) {
			return defaultEmbed("Error fetching player data");
		}

		JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
		if (guildJson == null) {
			return defaultEmbed("Error fetching guild data");
		}

		JsonElement members = higherDepth(guildJson, "guild.members");
		JsonArray membersArr = members.getAsJsonArray();
		List<String> guildExpList = new ArrayList<>();
		List<CompletableFuture<String>> futures = new ArrayList<>();
		for (int i = 0; i < membersArr.size(); i++) {
			int finalI = i;
			futures.add(
				asyncHttpClient
					.prepareGet("https://api.ashcon.app/mojang/v2/user/" + higherDepth(membersArr.get(i), "uuid").getAsString())
					.execute()
					.toCompletableFuture()
					.thenApply(
						uuidToUsernameResponse -> {
							try {
								String currentUsername = higherDepth(
									JsonParser.parseString(uuidToUsernameResponse.getResponseBody()),
									"username"
								)
									.getAsString();
								JsonElement expHistory = higherDepth(membersArr.get(finalI), "expHistory");
								List<String> keys = getJsonKeys(expHistory);
								int totalPlayerExp = 0;

								for (String value : keys) {
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
		int guildRank = -1;
		int guildExp = -1;
		for (int i = 0; i < guildExpList.size(); i++) {
			String[] curGuildRank = guildExpList.get(i).split("=:=");
			if (curGuildRank[0].equals(uuidUsername.playerUsername)) {
				guildRank = i;
				guildExp = Integer.parseInt(curGuildRank[1]);
				break;
			}
		}
		String rankStr =
			"**Player:** " + uuidUsername.playerUsername + "\n**Guild Rank:** #" + (guildRank + 1) + "\n**Exp:** " + formatNumber(guildExp);
		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageTitle(higherDepth(guildJson, "guild.name").getAsString())
				.setEveryPageTitleUrl(
					"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "guild._id").getAsString()
				)
				.setEveryPageText(rankStr)
		);

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

	public static EmbedBuilder getGuildPlayer(String username) {
		UsernameUuidStruct uuidUsername = usernameToUuid(username);
		if (uuidUsername == null) {
			return defaultEmbed("Error fetching player data");
		}

		JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
		if (guildJson == null) {
			return defaultEmbed("Error fetching guild data");
		}

		try {
			String guildName = higherDepth(guildJson, "guild.name").getAsString();
			EmbedBuilder eb = defaultEmbed(
				uuidUsername.playerUsername + " is in " + guildName,
				"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "guild._id").getAsString()
			);
			eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
			eb.setThumbnail("https://cravatar.eu/helmavatar/" + uuidUsername.playerUuid + "/64.png");
			return eb;
		} catch (Exception e) {
			return defaultEmbed(uuidUsername.playerUsername + " is not in a guild");
		}
	}

	public static EmbedBuilder getGuildInfo(String username) {
		UsernameUuidStruct uuidUsername = usernameToUuid(username);
		if (uuidUsername == null) {
			return defaultEmbed("Error fetching player data");
		}

		JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
		if (guildJson == null) {
			return defaultEmbed("Error fetching guild data");
		}

		String guildName = higherDepth(guildJson, "guild.name").getAsString();

		EmbedBuilder eb = defaultEmbed(
			guildName,
			"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "guild._id").getAsString()
		);
		eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

		return eb;
	}

	public static EmbedBuilder guildInfoFromGuildName(String guildName) {
		try {
			String guildId = higherDepth(
				getJson("https://api.hypixel.net/findGuild?key=" + HYPIXEL_API_KEY + "&byName=" + guildName.replace(" ", "%20")),
				"guild"
			)
				.getAsString();
			JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);
			if (guildJson == null) {
				return defaultEmbed("Error fetching guild data");
			}
			guildName = higherDepth(guildJson, "guild.name").getAsString();

			EmbedBuilder eb = defaultEmbed(
				guildName,
				"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "guild._id").getAsString()
			);
			eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
			return eb;
		} catch (Exception e) {
			return defaultEmbed("Error fetching guild data");
		}
	}

	private static String getGuildInfo(JsonElement guildJson) {
		String guildInfo = "";
		String guildName = higherDepth(guildJson, "guild.name").getAsString();

		JsonElement created = higherDepth(guildJson, "guild.created");
		String[] date = Date.from(Instant.ofEpochMilli(created.getAsLong())).toString().split(" ");
		guildInfo += ("• " + guildName + " was created on " + date[1] + " " + date[2] + ", " + date[5]) + "\n";

		JsonArray guildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray();
		for (int i = 0; i < guildMembers.size(); i++) {
			JsonElement currentMember = guildMembers.get(i).getAsJsonObject();
			if (higherDepth(currentMember, "rank").getAsString().equals("Guild Master")) {
				guildInfo +=
					("• " + guildName + "'s guild master is " + uuidToUsername(higherDepth(currentMember, "uuid").getAsString())) + "\n";
				break;
			}
		}

		int numGuildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray().size();
		guildInfo += ("• " + guildName + " has " + numGuildMembers + " members") + "\n";
		JsonArray preferredGames;
		try {
			preferredGames = higherDepth(guildJson, "guild.preferredGames").getAsJsonArray();
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

		int guildExp = higherDepth(guildJson, "guild.exp").getAsInt();

		guildInfo += ("• " + guildName + " is guild level " + guildExpToLevel(guildExp)) + "\n";

		return guildInfo;
	}

	public static EmbedBuilder getGuildMembers(String username, User user, MessageChannel channel, InteractionHook hook) {
		UsernameUuidStruct uuidUsername = usernameToUuid(username);
		if (uuidUsername == null) {
			return defaultEmbed("Error fetching player data");
		}

		JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
		if (guildJson == null) {
			return defaultEmbed("Error fetching guild data");
		}

		JsonArray membersArr = higherDepth(guildJson, "guild.members").getAsJsonArray();
		List<CompletableFuture<String>> futures = new ArrayList<>();
		List<String> guildMembers = new ArrayList<>();
		for (int i = 0; i < membersArr.size(); i++) {
			futures.add(
				asyncHttpClient
					.prepareGet("https://api.ashcon.app/mojang/v2/user/" + higherDepth(membersArr.get(i), "uuid").getAsString())
					.execute()
					.toCompletableFuture()
					.thenApply(
						uuidToUsernameResponse -> {
							try {
								return higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()), "username")
									.getAsString();
							} catch (Exception ignored) {}
							return null;
						}
					)
			);
		}

		for (CompletableFuture<String> future : futures) {
			try {
				guildMembers.add(future.get());
			} catch (Exception ignored) {}
		}

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(3).setItemsPerPage(33);

		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageTitle(higherDepth(guildJson, "guild.name").getAsString())
				.setEveryPageTitleUrl(
					"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "guild._id").getAsString()
				)
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

	private static int guildExpToLevel(int guildExp) {
		int[] guildExpTable = new int[] {
			100000,
			150000,
			250000,
			500000,
			750000,
			1000000,
			1250000,
			1500000,
			2000000,
			2500000,
			2500000,
			2500000,
			2500000,
			2500000,
			3000000,
		};
		int guildLevel = 0;

		for (int i = 0;; i++) {
			int expNeeded = i >= guildExpTable.length ? guildExpTable[guildExpTable.length - 1] : guildExpTable[i];
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
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 3 && ("experience".equals(args[1]) || "exp".equals(args[1]))) {
					if (args[2].toLowerCase().startsWith("u:")) {
						String username = args[2].split(":")[1];
						eb = getGuildExp(username, event.getAuthor(), event.getChannel(), null);
						if (eb == null) {
							ebMessage.delete().queue();
						} else {
							ebMessage.editMessage(eb.build()).queue();
						}
						return;
					}
				} else if (args.length >= 3 && (args[1].equals("information") || args[1].equals("info"))) {
					if (args[2].toLowerCase().startsWith("u:")) {
						String usernameInfo = args[2].split(":")[1];
						ebMessage.editMessage(getGuildInfo(usernameInfo).build()).queue();
						return;
					} else if (args[2].toLowerCase().startsWith("g:")) {
						String guildName = content.split(":")[1];
						ebMessage.editMessage(guildInfoFromGuildName(guildName).build()).queue();
						return;
					}
				} else if (args.length == 3 && "members".equals(args[1])) {
					if (args[2].toLowerCase().startsWith("u:")) {
						String usernameMembers = args[2].split(":")[1];
						eb = getGuildMembers(usernameMembers, event.getAuthor(), event.getChannel(), null);
						if (eb == null) {
							ebMessage.delete().queue();
						} else {
							ebMessage.editMessage(eb.build()).queue();
						}
						return;
					}
				} else if (args.length == 2) {
					ebMessage.editMessage(getGuildPlayer(args[1]).build()).queue();
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
