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
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelGuildCache;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildKickerCommand extends Command {

	public GuildKickerCommand() {
		this.name = "guild-kicker";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-kicker" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				String content = event.getMessage().getContentRaw();
				boolean useKey = false;
				if (content.contains("--usekey")) {
					useKey = true;
					content = content.replace("--usekey", "").trim();
				}
				args = content.split(" ", 3);

				if (args.length == 3 && args[1].toLowerCase().startsWith("u:")) {
					paginate(getGuildKicker(args[1].split(":")[1], args[2], useKey, event));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getGuildKicker(String username, String reqs, boolean useKey, CommandEvent event) {
		String[] reqsArr = reqs.split("] \\[");
		if (reqsArr.length > 3) {
			return invalidEmbed("You can only enter a maximum of 3 sets of requirements");
		}
		for (int i = 0; i < reqsArr.length; i++) {
			String[] indvReqs = reqsArr[i].replace("[", "").replace("]", "").split(" ");
			for (String indvReq : indvReqs) {
				String[] reqDashSplit = indvReq.split(":");
				if (reqDashSplit.length != 2) {
					return invalidEmbed(indvReq + " is an invalid requirement format");
				}

				if (
					!reqDashSplit[0].equals("slayer") &&
					!reqDashSplit[0].equals("skills") &&
					!reqDashSplit[0].equals("catacombs") &&
					!reqDashSplit[0].equals("weight")
				) {
					return invalidEmbed(indvReq + " is an invalid requirement type");
				}

				try {
					Double.parseDouble(reqDashSplit[1]);
				} catch (Exception e) {
					return invalidEmbed(indvReq + " is an invalid requirement value");
				}
			}

			reqsArr[i] = reqsArr[i].replace("[", "").replace("]", "");
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

		String guildId = higherDepth(guildJson, "_id").getAsString();
		JsonElement guildLbJson = getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId);

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(20);
		if (higherDepth(guildLbJson, "data") != null && !(useKey)) {
			JsonArray guildMembers = higherDepth(guildLbJson, "data").getAsJsonArray();
			Instant lastUpdated = Instant.now();

			int missingReqsCount = 0;
			for (JsonElement guildMember : guildMembers) {
				Instant curLastUpdated = Instant.parse(higherDepth(guildMember, "last_updated_at").getAsString());
				if (curLastUpdated.isBefore(lastUpdated)) {
					lastUpdated = curLastUpdated;
				}

				double slayer = higherDepth(guildMember, "total_slayer").getAsDouble();
				double skills = higherDepth(guildMember, "average_skill_progress").getAsDouble();
				double catacombs = higherDepth(guildMember, "catacomb").getAsDouble();
				double weight = higherDepth(guildMember, "raw_weight.total").getAsDouble();

				boolean meetsReqs = false;

				for (String req : reqsArr) {
					String[] reqSplit = req.split(" ");
					double slayerReq = 0;
					double skillsReq = 0;
					double catacombsReq = 0;
					double weightReq = 0;
					for (String reqIndividual : reqSplit) {
						switch (reqIndividual.split(":")[0]) {
							case "slayer":
								slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
							case "skills":
								skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
							case "catacombs":
								catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
							case "weight":
								weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
						}
					}

					if (slayer >= slayerReq && skills >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
						meetsReqs = true;
						break;
					}
				}

				if (!meetsReqs) {
					paginateBuilder.addItems(
						"• **" +
						higherDepth(guildMember, "username").getAsString() +
						"** | Slayer: " +
						formatNumber(slayer) +
						" | Skills: " +
						roundAndFormat(skills) +
						" | Cata: " +
						roundAndFormat(catacombs) +
						" | Weight: " +
						roundAndFormat(weight)
					);
					missingReqsCount++;
				}
			}

			Duration duration = Duration.between(lastUpdated, Instant.now());

			paginateBuilder
				.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle("Guild Kick Helper")
						.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
						.setEveryPageText(
							"**Total missing requirements:** " + missingReqsCount + "\n**Updated:** " + instantToDHM(duration) + " ago\n"
						)
				)
				.ifItemsEmpty("Everyone meets the requirements")
				.build()
				.paginate(event.getChannel(), 0);
		} else {
			String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}

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
						guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
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

							return guildMemberProfileJson.thenApply(guildMemberProfileJsonResponse -> {
								Player guildMemberPlayer = new Player(
									guildMemberUuid,
									guildMemberUsernameResponse,
									guildMemberProfileJsonResponse
								);

								if (guildMemberPlayer.isValid()) {
									return memberCacheFromPlayer(guildMemberPlayer);
								}

								return null;
							});
						})
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

			for (String guildMember : guildMemberPlayersList) {
				String[] guildMemberSplit = guildMember.split("=:=");
				double slayer = Double.parseDouble(guildMemberSplit[1]);
				double skills = Double.parseDouble(guildMemberSplit[2]);
				double catacombs = Double.parseDouble(guildMemberSplit[3]);
				double weight = Double.parseDouble(guildMemberSplit[4]);

				boolean meetsReqs = false;

				for (String req : reqsArr) {
					String[] reqSplit = req.split(" ");
					double slayerReq = 0;
					double skillsReq = 0;
					double catacombsReq = 0;
					double weightReq = 0;
					for (String reqIndividual : reqSplit) {
						switch (reqIndividual.split(":")[0]) {
							case "slayer":
								slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
							case "skills":
								skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
							case "catacombs":
								catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
							case "weight":
								weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
								break;
						}
					}

					if (slayer >= slayerReq && skills >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
						meetsReqs = true;
						break;
					}
				}

				if (!meetsReqs) {
					paginateBuilder.addItems(
						"• **" +
						guildMemberSplit[0] +
						"** | Slayer: " +
						formatNumber(slayer) +
						" | Skills: " +
						roundAndFormat(skills) +
						" | Cata: " +
						roundAndFormat(catacombs) +
						" | Weight: " +
						roundAndFormat(weight)
					);
				}
			}

			paginateBuilder
				.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle("Guild Kick Helper")
						.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
						.setEveryPageText(
							"**Total missing requirements:** " +
							paginateBuilder.getItemsSize() +
							(
								lastUpdated != null
									? "\n**Last updated:** " + instantToDHM(Duration.between(lastUpdated, Instant.now())) + " ago"
									: ""
							) +
							"\n"
						)
				)
				.ifItemsEmpty("Everyone meets the requirements")
				.build()
				.paginate(event.getChannel(), 0);
		}
		return null;
	}
}
