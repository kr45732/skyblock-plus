package com.skyblockplus.guilds;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.structs.HypixelGuildCache.memberCacheFromPlayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class GuildLeaderboardsCommand extends Command {

	public GuildLeaderboardsCommand() {
		this.name = "guild-leaderboard";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-lb" };
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

				if (args.length != 3) {
					eb = errorEmbed(this.name);
					ebMessage.editMessageEmbeds(eb.build()).queue();
					return;
				}

				if (args[2].toLowerCase().startsWith("u:")) {
					eb = getLeaderboard(args[1], args[2].split(":")[1], event);

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessageEmbeds(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}

	private EmbedBuilder getLeaderboard(String lbType, String username, CommandEvent event) {
		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		int lbTypeNum;
		switch (lbType) {
			case "slayer":
				lbTypeNum = 1;
				break;
			case "skills":
				lbTypeNum = 2;
				break;
			case "catacombs":
				lbTypeNum = 3;
				break;
			case "weight":
				lbTypeNum = 4;
				break;
			case "sven_xp":
				lbTypeNum = 5;
				break;
			case "rev_xp":
				lbTypeNum = 6;
				break;
			case "tara_xp":
				lbTypeNum = 7;
				break;
			case "enderman_xp":
				lbTypeNum = 8;
				break;
			default:
				return defaultEmbed(
					lbType +
					" is an invalid leaderboard type. Valid types are: `slayer`, `skills`, `catacombs`, `weight`, `sven_xp`, `rev_xp`, `tara_xp`, and `enderman_xp`"
				);
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.failCause);
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.playerUuid);
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.failCause);
		}

		JsonElement guildJson = guildResponse.response;
		String guildName = higherDepth(guildJson, "name").getAsString();
		String guildId = higherDepth(guildJson, "_id").getAsString();

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(2).setItemsPerPage(20);
		HypixelGuildCache guildCache = hypixelGuildsCacheMap.getOrDefault(guildId, null);
		List<String> guildMemberPlayersList = new ArrayList<>();
		Instant lastUpdated = null;

		if (guildCache != null && Duration.between(guildCache.lastUpdated, Instant.now()).toMinutes() < 15) {
			guildMemberPlayersList = guildCache.membersCache;
			lastUpdated = guildCache.lastUpdated;
		} else {
			JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
			List<CompletableFuture<CompletableFuture<String>>> futuresList = new ArrayList<>();

			for (JsonElement guildMember : guildMembers) {
				String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

				CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);
				futuresList.add(
					guildMemberUsername.thenApply(
						guildMemberUsernameResponse -> {
							try {
								if (keyCooldownMap.get(hypixelKey).remainingLimit.get() < 5) {
									System.out.println("Sleeping for " + keyCooldownMap.get(hypixelKey).timeTillReset + " seconds");
									TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).timeTillReset.get());
								}
							} catch (Exception ignored) {}

							CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(
								guildMemberUuid,
								hypixelKey
							);

							return guildMemberProfileJson.thenApply(
								guildMemberProfileJsonResponse -> {
									Player guildMemberPlayer = new Player(
										guildMemberUuid,
										guildMemberUsernameResponse,
										guildMemberProfileJsonResponse
									);

									if (guildMemberPlayer.isValid()) {
										return memberCacheFromPlayer(guildMemberPlayer);
									}

									return null;
								}
							);
						}
					)
				);
			}

			for (CompletableFuture<CompletableFuture<String>> future : futuresList) {
				try {
					String playerFutureResponse = future.get().get();
					if (playerFutureResponse != null) {
						guildMemberPlayersList.add(playerFutureResponse);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			hypixelGuildsCacheMap.put(guildId, new HypixelGuildCache(Instant.now(), guildMemberPlayersList));
		}

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.split("=:=")[lbTypeNum])));

		int guildRank = -1;
		String amt = "-1";
		for (int i = 0, guildMemberPlayersListSize = guildMemberPlayersList.size(); i < guildMemberPlayersListSize; i++) {
			String[] guildPlayer = guildMemberPlayersList.get(i).split("=:=");
			String formattedAmt = roundAndFormat(Double.parseDouble(guildPlayer[lbTypeNum]));
			paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(guildPlayer[0]) + ": " + formattedAmt);

			if (guildPlayer[0].equals(usernameUuidStruct.playerUsername)) {
				guildRank = i;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Player:** " +
			usernameUuidStruct.playerUsername +
			"\n**Guild Rank:** #" +
			(guildRank + 1) +
			"\n**" +
			capitalizeString(lbType.replace("_", " ")) +
			":** " +
			amt +
			(lastUpdated != null ? "\n**Last updated:** " + instantToDHM(Duration.between(lastUpdated, Instant.now())) + " ago" : "");

		paginateBuilder
			.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(guildName)
					.setEveryPageText(ebStr)
					.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
			)
			.build()
			.paginate(event.getChannel(), 0);

		return null;
	}
}
