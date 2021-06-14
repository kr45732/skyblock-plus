package com.skyblockplus.guilds;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.Main.asyncHttpClient;
import static com.skyblockplus.guilds.GuildLeaderboardsCommand.keyCooldownMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class GuildKickerCommand extends Command {

	public GuildKickerCommand() {
		this.name = "guild-kicker";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "g-kicker" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

				String content = event.getMessage().getContentRaw();

				String[] args = content.split(" ", 3);

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 3 && args[1].toLowerCase().startsWith("u:")) {
					eb = getGuildKicker(args[1].split(":")[1], args[2], event);

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder getGuildKicker(String username, String reqs, CommandEvent event) {
		String[] reqsArr = reqs.split("] \\[");
		if (reqsArr.length > 3) {
			return defaultEmbed("Error").setDescription("You can only enter a maximum of 3 sets of requirements");
		}
		for (int i = 0; i < reqsArr.length; i++) {
			String[] indvReqs = reqsArr[i].replace("[", "").replace("]", "").split(" ");
			for (String indvReq : indvReqs) {
				String[] reqDashSplit = indvReq.split(":");
				if (reqDashSplit.length != 2) {
					return defaultEmbed("Error").setDescription(indvReq + " is an invalid requirement format");
				}

				if (
					!reqDashSplit[0].equals("slayer") &&
					!reqDashSplit[0].equals("skills") &&
					!reqDashSplit[0].equals("catacombs") &&
					!reqDashSplit[0].equals("weight")
				) {
					return defaultEmbed("Error").setDescription(indvReq + " is an invalid requirement type");
				}

				try {
					Double.parseDouble(reqDashSplit[1]);
				} catch (Exception e) {
					return defaultEmbed("Error").setDescription(indvReq + " is an invalid requirement value");
				}
			}

			reqsArr[i] = reqsArr[i].replace("[", "").replace("]", "");
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		JsonElement guildJson = getJson(
			"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + usernameUuidStruct.playerUuid
		);

		if (guildJson != null && !higherDepth(guildJson, "guild").isJsonNull()) {
			String guildId = higherDepth(guildJson, "guild._id").getAsString();
			JsonElement guildLbJson = getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId);

			CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(20);
			if (higherDepth(guildLbJson, "data") != null) {
				JsonArray guildMembers = higherDepth(guildLbJson, "data").getAsJsonArray();

				int missingReqsCount = 0;
				for (JsonElement guildMember : guildMembers) {
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

				paginateBuilder
					.setPaginatorExtras(
						new PaginatorExtras()
							.setEveryPageTitle("Guild Kick Helper")
							.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
							.setEveryPageText("**Total missing requirements: " + missingReqsCount + "**\n")
					)
					.ifItemsEmpty("Everyone meets the requirements")
					.build()
					.paginate(event.getChannel(), 0);

				return null;
			} else {
				String HYPIXEL_KEY = database.getServerHypixelApiKey(event.getGuild().getId());

				EmbedBuilder eb = checkHypixelKey(HYPIXEL_KEY);
				if (eb != null) {
					return eb;
				}

				JsonArray guildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray();
				List<CompletableFuture<CompletableFuture<String>>> futuresList = new ArrayList<>();

				for (JsonElement guildMember : guildMembers) {
					String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
					try {
						if (keyCooldownMap.get(HYPIXEL_KEY).remainingLimit.get() < 5) {
							System.out.println("Sleeping for " + keyCooldownMap.get(HYPIXEL_KEY).timeTillReset + " seconds");
							TimeUnit.SECONDS.sleep(keyCooldownMap.get(HYPIXEL_KEY).timeTillReset.get());
						}
					} catch (Exception ignored) {}

					futuresList.add(
						asyncHttpClient
							.prepareGet("https://api.ashcon.app/mojang/v2/user/" + guildMemberUuid)
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
							.thenApply(
								guildMemberUsernameResponse ->
									asyncHttpClient
										.prepareGet(
											"https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_KEY + "&uuid=" + guildMemberUuid
										)
										.execute()
										.toCompletableFuture()
										.thenApply(
											guildMemberOuterProfileJsonResponse -> {
												try {
													try {
														keyCooldownMap
															.get(HYPIXEL_KEY)
															.remainingLimit.set(
																Integer.parseInt(
																	guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Remaining")
																)
															);
														keyCooldownMap
															.get(HYPIXEL_KEY)
															.timeTillReset.set(
																Integer.parseInt(
																	guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Reset")
																)
															);
													} catch (Exception ignored) {}

													JsonElement guildMemberOuterProfileJson = JsonParser.parseString(
														guildMemberOuterProfileJsonResponse.getResponseBody()
													);

													Player guildMemberPlayer = new Player(
														guildMemberUuid,
														guildMemberUsernameResponse,
														guildMemberOuterProfileJson
													);

													double slayer = guildMemberPlayer.getTotalSlayer();
													double skills = guildMemberPlayer.getSkillAverage();
													double catacombs = guildMemberPlayer.getCatacombsLevel();
													double weight = guildMemberPlayer.getWeight();

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

														if (
															slayer >= slayerReq &&
															skills >= skillsReq &&
															catacombs >= catacombsReq &&
															weight >= weightReq
														) {
															meetsReqs = true;
															break;
														}
													}

													if (!meetsReqs) {
														return (
															"• **" +
															guildMemberUsernameResponse +
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
												} catch (Exception e) {
													e.printStackTrace();
												}
												return null;
											}
										)
							)
					);
				}

				for (CompletableFuture<CompletableFuture<String>> future : futuresList) {
					try {
						String playerFutureResponse = future.get().get();
						if (playerFutureResponse != null) {
							paginateBuilder.addItems(playerFutureResponse);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				paginateBuilder
					.setPaginatorExtras(
						new PaginatorExtras()
							.setEveryPageTitle("Guild Kick Helper")
							.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
							.setEveryPageText("**Total missing requirements: " + paginateBuilder.getItemsSize() + "**\n")
					)
					.ifItemsEmpty("Everyone meets the requirements")
					.build()
					.paginate(event.getChannel(), 0);

				return null;
			}
		}

		return defaultEmbed("Unable to fetch guild data");
	}
}
