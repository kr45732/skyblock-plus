package com.skyblockplus.eventlisteners.skyblockevent;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.eventlisteners.MainListener.guildMap;
import static com.skyblockplus.eventlisteners.skyblockevent.SkyblockEvent.formatter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.skyblockevent.EventMember;
import com.skyblockplus.api.discordserversettings.skyblockevent.RunningEvent;
import com.skyblockplus.api.discordserversettings.skyblockevent.SbEvent;
import com.skyblockplus.eventlisteners.AutomaticGuild;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class SkyblockEventCommand extends Command {

	public SkyblockEventCommand() {
		this.name = "event";
		this.cooldown = globalCooldown + 3;
	}

	public static void endSkyblockEvent(String guildId) {
		JsonElement runningEventSettings = database.getRunningEventSettings(guildId);
		TextChannel announcementChannel = jda.getTextChannelById(higherDepth(runningEventSettings, "announcementId").getAsString());

		List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningEventSettings);

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, null)
			.setColumns(1)
			.setItemsPerPage(25)
			.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Event Leaderboard"))
			.setTimeout(24, TimeUnit.HOURS);

		for (int i = 0; i < guildMemberPlayersList.size(); i++) {
			EventMember eventMember = guildMemberPlayersList.get(i);
			paginateBuilder.addItems(
				"`" +
				(i + 1) +
				")` " +
				fixUsername(eventMember.getUsername()) +
				" | +" +
				formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
			);
		}

		if (paginateBuilder.getItemsSize() > 0) {
			paginateBuilder.build().paginate(announcementChannel, 0);
		} else {
			announcementChannel.sendMessage(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build()).complete();
		}

		try {
			paginateBuilder =
				defaultPaginator(waiter, null)
					.setColumns(1)
					.setItemsPerPage(25)
					.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Prizes"))
					.setTimeout(24, TimeUnit.HOURS);

			ArrayList<String> prizeListKeys = getJsonKeys(higherDepth(runningEventSettings, "prizeMap"));
			for (int i = 0; i < prizeListKeys.size(); i++) {
				try {
					paginateBuilder.addItems(
						"`" +
						(i + 1) +
						")` " +
						higherDepth(runningEventSettings, "prizeMap." + prizeListKeys.get(i)).getAsString() +
						" - " +
						fixUsername(guildMemberPlayersList.get(i).getUsername())
					);
				} catch (Exception ignored) {}
			}

			if (paginateBuilder.getItemsSize() > 0) {
				paginateBuilder.build().paginate(announcementChannel, 0);
				database.setSkyblockEventSettings(guildId, new SbEvent());
				return;
			}
		} catch (Exception ignored) {}
		announcementChannel.sendMessage(defaultEmbed("Prizes").setDescription("None").build()).complete();
		database.setSkyblockEventSettings(guildId, new SbEvent());
	}

	private static List<EventMember> getEventLeaderboardList(JsonElement runningSettings) {
		List<EventMember> guildMemberPlayersList = new ArrayList<>();
		List<CompletableFuture<CompletableFuture<EventMember>>> futuresList = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
			try {
				if (remainingLimit.get() < 5) {
					System.out.println("Sleeping for " + timeTillReset + " seconds");
					TimeUnit.SECONDS.sleep(timeTillReset.get());
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
								.prepareGet("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + guildMemberUuid)
								.execute()
								.toCompletableFuture()
								.thenApply(
									guildMemberOuterProfileJsonResponse -> {
										try {
											try {
												remainingLimit.set(
													Integer.parseInt(guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Remaining"))
												);
												timeTillReset.set(
													Integer.parseInt(guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Reset"))
												);
											} catch (Exception ignored) {}

											JsonElement guildMemberOuterProfileJson = JsonParser.parseString(
												guildMemberOuterProfileJsonResponse.getResponseBody()
											);

											Player guildMemberPlayer = new Player(
												guildMemberUuid,
												guildMemberUsernameResponse,
												higherDepth(guildMember, "profileName").getAsString(),
												guildMemberOuterProfileJson
											);

											if (guildMemberPlayer.isValid()) {
												switch (eventType) {
													case "slayer":
														{
															return new EventMember(
																guildMemberUsernameResponse,
																guildMemberUuid,
																"" +
																(
																	guildMemberPlayer.getTotalSlayer() -
																	higherDepth(guildMember, "startingAmount").getAsDouble()
																),
																higherDepth(guildMember, "profileName").getAsString()
															);
														}
													case "skills":
														{
															int totalSkillsXp = guildMemberPlayer.getTotalSkillsXp();

															if (totalSkillsXp != -1) {
																return new EventMember(
																	guildMemberUsernameResponse,
																	guildMemberUuid,
																	"" +
																	(
																		totalSkillsXp -
																		higherDepth(guildMember, "startingAmount").getAsDouble()
																	),
																	higherDepth(guildMember, "profileName").getAsString()
																);
															}
															break;
														}
													case "catacombs":
														{
															return new EventMember(
																guildMemberUsernameResponse,
																guildMemberUuid,
																"" +
																(
																	guildMemberPlayer.getCatacombsSkill().totalSkillExp -
																	higherDepth(guildMember, "startingAmount").getAsDouble()
																),
																higherDepth(guildMember, "profileName").getAsString()
															);
														}
													case "weight":
														{
															return new EventMember(
																guildMemberUsernameResponse,
																guildMemberUuid,
																"" +
																(
																	guildMemberPlayer.getWeight() -
																	higherDepth(guildMember, "startingAmount").getAsDouble()
																),
																higherDepth(guildMember, "profileName").getAsString()
															);
														}
													default:
														{
															if (eventType.startsWith("collection.")) {
																return new EventMember(
																	guildMemberUsernameResponse,
																	guildMemberUuid,
																	"" +
																	(
																		(
																			higherDepth(
																					guildMemberPlayer.getProfileJson(),
																					eventType.split("-")[0]
																				) !=
																				null
																				? higherDepth(
																					guildMemberPlayer.getProfileJson(),
																					eventType.split("-")[0]
																				)
																					.getAsDouble()
																				: 0
																		) -
																		higherDepth(guildMember, "startingAmount").getAsDouble()
																	),
																	higherDepth(guildMember, "profileName").getAsString()
																);
															}
														}
												}
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

		for (CompletableFuture<CompletableFuture<EventMember>> future : futuresList) {
			try {
				EventMember playerFutureResponse = future.get().get();
				if (playerFutureResponse != null) {
					guildMemberPlayersList.add(playerFutureResponse);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.getStartingAmount())));
		return guildMemberPlayersList;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2 || args.length == 3) {
					switch (args[1]) {
						case "create":
							if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
								ebMessage.delete().complete();
								event
									.getChannel()
									.sendMessage("❌ You must have the Administrator permission in this Guild to use that!")
									.queue();
							} else {
								eb = createSkyblockEvent(event);
								if (eb == null) {
									ebMessage.delete().queue();
								} else {
									ebMessage.editMessage(eb.build()).queue();
								}
							}
							return;
						case "current":
							ebMessage.editMessage(getCurrentSkyblockEvent(event.getGuild().getId()).build()).queue();
							return;
						case "cancel":
							if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
								ebMessage.delete().complete();
								event
									.getChannel()
									.sendMessage("❌ You must have the Administrator permission in this Guild to use that!")
									.queue();
							} else {
								ebMessage.editMessage(cancelSkyblockEvent(event.getGuild().getId()).build()).queue();
							}
							return;
						case "join":
							ebMessage.editMessage(joinSkyblockEvent(event, args).build()).queue();
							return;
						case "leave":
							ebMessage.editMessage(leaveSkyblockEvent(event).build()).queue();
							return;
						case "leaderboard":
						case "lb":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								if (!guildMap.containsKey(event.getGuild().getId())) {
									ebMessage.editMessage(defaultEmbed("No guild found").build()).queue();
									return;
								}

								AutomaticGuild currentGuild = guildMap.get(event.getGuild().getId());

								if (
									(currentGuild.getEventMemberListLastUpdated() != null) &&
									(Duration.between(currentGuild.getEventMemberListLastUpdated(), Instant.now()).toMinutes() < 15)
								) {
									List<EventMember> eventMemberList = currentGuild.getEventMemberList();

									CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor())
										.setColumns(1)
										.setItemsPerPage(25);
									for (int i = 0; i < eventMemberList.size(); i++) {
										EventMember eventMember = eventMemberList.get(i);
										paginateBuilder.addItems(
											"`" +
											(i + 1) +
											")` " +
											fixUsername(eventMember.getUsername()) +
											" | +" +
											formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
										);
									}

									if (paginateBuilder.getItemsSize() > 0) {
										long minutesSinceUpdate = Duration
											.between(currentGuild.getEventMemberListLastUpdated(), Instant.now())
											.toMinutes();

										String minutesSinceUpdateString;
										if (minutesSinceUpdate == 0) {
											minutesSinceUpdateString = " less than a minute ";
										} else if (minutesSinceUpdate == 1) {
											minutesSinceUpdateString = " 1 minute ";
										} else {
											minutesSinceUpdateString = minutesSinceUpdate + " minutes ";
										}

										paginateBuilder.setPaginatorExtras(
											new PaginatorExtras()
												.setEveryPageTitle("Event Leaderboard")
												.setEveryPageText("**Last updated " + minutesSinceUpdateString + " ago**\n")
										);
										paginateBuilder.build().paginate(event.getChannel(), 0);
										ebMessage.delete().queue();
									} else {
										ebMessage
											.editMessage(
												defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build()
											)
											.queue();
									}
									return;
								}
								JsonElement runningSettings = database.getRunningEventSettings(event.getGuild().getId());

								CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor())
									.setColumns(1)
									.setItemsPerPage(25);

								List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningSettings);

								for (int i = 0; i < guildMemberPlayersList.size(); i++) {
									EventMember eventMember = guildMemberPlayersList.get(i);
									paginateBuilder.addItems(
										"`" +
										(i + 1) +
										")` " +
										fixUsername(eventMember.getUsername()) +
										" | +" +
										formatNumber(Double.parseDouble(eventMember.getStartingAmount()))
									);
								}

								paginateBuilder.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Event Leaderboard"));

								if (paginateBuilder.getItemsSize() > 0) {
									paginateBuilder.build().paginate(event.getChannel(), 0);
									ebMessage.delete().queue();
								} else {
									ebMessage
										.editMessage(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
										.queue();
								}

								guildMap.get(event.getGuild().getId()).setEventMemberList(guildMemberPlayersList);
								guildMap.get(event.getGuild().getId()).setEventMemberListLastUpdated(Instant.now());
							} else {
								ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
							}
							return;
						case "end":
							if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
								ebMessage.delete().complete();
								event
									.getChannel()
									.sendMessage("❌ You must have the Administrator permission in this Guild to use that!")
									.queue();
							} else {
								if (database.getSkyblockEventActive(event.getGuild().getId())) {
									endSkyblockEvent(event.getGuild().getId());
									ebMessage.editMessage(defaultEmbed("Success").setDescription("Event Ended").build()).queue();
								} else {
									ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
								}
							}
							return;
					}
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder leaveSkyblockEvent(CommandEvent event) {
		if (database.getSkyblockEventActive(event.getGuild().getId())) {
			JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getAuthor().getId());
			if (linkedAccount != null) {
				String uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();
				database.removeEventMemberFromRunningEvent(event.getGuild().getId(), uuid);

				return defaultEmbed("Success").setDescription("You left the event");
			} else {
				return defaultEmbed("You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link");
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	private EmbedBuilder joinSkyblockEvent(CommandEvent event, String[] args) {
		if (database.getSkyblockEventActive(event.getGuild().getId())) {
			JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getAuthor().getId());
			if (linkedAccount != null) {
				String uuid;
				String username;
				try {
					uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();
					username = higherDepth(linkedAccount, "minecraftUsername").getAsString();
				} catch (Exception e) {
					return defaultEmbed("You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link");
				}

				if (database.eventHasMemberByUuid(event.getGuild().getId(), uuid)) {
					return defaultEmbed("Error")
						.setDescription(
							"You are already in the event! If you want to leave or change profile use `" + BOT_PREFIX + "event leave`"
						);
				}

				JsonElement guildIn = getJson("https://api.hypixel.net/findGuild?key=" + HYPIXEL_API_KEY + "&byUuid=" + uuid);

				try {
					higherDepth(guildIn, "guild").getAsString();
				} catch (Exception e) {
					return defaultEmbed("Error").setDescription("You must be in the guild to join the event");
				}

				if (higherDepth(guildIn, "guild").getAsString().equals(database.getSkyblockEventGuildId(event.getGuild().getId()))) {
					Player player = args.length == 3 ? new Player(username, args[2]) : new Player(username);

					if (player.isValid()) {
						double startingAmount = 0;
						String startingAmountFormatted = "";
						String eventType = higherDepth(database.getRunningEventSettings(event.getGuild().getId()), "eventType")
							.getAsString();
						try {
							switch (eventType) {
								case "slayer":
									{
										startingAmount = player.getTotalSlayer();
										startingAmountFormatted = formatNumber(startingAmount) + " total slayer xp";
										break;
									}
								case "skills":
									{
										startingAmount = player.getTotalSkillsXp();
										startingAmountFormatted = formatNumber(startingAmount) + " total skills xp";
										break;
									}
								case "catacombs":
									{
										startingAmount = player.getCatacombsSkill().totalSkillExp;
										startingAmountFormatted = formatNumber(startingAmount) + " total catacombs xp";
										break;
									}
								case "weight":
									{
										startingAmount = player.getWeight();
										if (player.getTotalSkillsXp() == -1) {
											startingAmount = -1;
										}
										startingAmountFormatted = formatNumber(startingAmount) + " weight";
										break;
									}
								default:
									{
										if (eventType.startsWith("collection.")) {
											startingAmount =
												higherDepth(player.getProfileJson(), eventType.split("-")[0]) != null
													? higherDepth(player.getProfileJson(), eventType.split("-")[0]).getAsDouble()
													: 0;
											startingAmountFormatted =
												formatNumber(startingAmount) + " " + eventType.split("-")[1] + " collection";
											break;
										}
									}
							}

							if (startingAmount != -1) {
								int code = database.addEventMemberToRunningEvent(
									event.getGuild().getId(),
									new EventMember(username, uuid, "" + startingAmount, player.getProfileName())
								);

								if (code == 200) {
									return defaultEmbed("Success")
										.setDescription(
											"**Username:** " +
											username +
											"\n**Profile:** " +
											player.getProfileName() +
											"\n**Starting amount:** " +
											startingAmountFormatted
										);
								} else {
									return defaultEmbed("Error").setDescription("API returned code " + code);
								}
							} else {
								return defaultEmbed("Error").setDescription("Please enable your skills API and retry");
							}
						} catch (Exception ignored) {}
						return defaultEmbed("Error").setDescription("Unable to fetch player data");
					} else {
						return defaultEmbed("Error").setDescription("Invalid player");
					}
				} else {
					return defaultEmbed("Error").setDescription("You must be in the guild to join the event");
				}
			} else {
				return defaultEmbed("You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link");
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	private EmbedBuilder getCurrentSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement currentSettings = database.getRunningEventSettings(guildId);
			EmbedBuilder eb = defaultEmbed("Current Event");

			JsonElement guildJson = getJson(
				"https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + higherDepth(currentSettings, "eventGuildId").getAsString()
			);

			eb.addField("Guild", higherDepth(guildJson, "guild.name").getAsString(), false);
			String eventType = higherDepth(currentSettings, "eventType").getAsString();
			eb.addField(
				"Event Type",
				capitalizeString(eventType.startsWith("collection.") ? eventType.split("-")[1] + " collection" : eventType),
				false
			);

			Instant eventInstantEnding = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());

			Duration duration = Duration.between(Instant.now(), eventInstantEnding);

			eb.addField("End Date", "Ends in " + instantToDHM(duration) + " (" + formatter.format(eventInstantEnding) + " UTC)", false);

			ArrayList<String> prizesKeys = getJsonKeys(higherDepth(currentSettings, "prizeMap"));
			StringBuilder ebString = new StringBuilder();
			for (String prizePlace : prizesKeys) {
				ebString
					.append("• ")
					.append(prizePlace)
					.append(") - ")
					.append(higherDepth(currentSettings, "prizeMap." + prizePlace).getAsString())
					.append("\n");
			}

			if (ebString.length() == 0) {
				ebString = new StringBuilder("None");
			}

			eb.addField("Prizes", ebString.toString(), false);
			eb.addField("Members joined", "" + higherDepth(currentSettings, "membersList").getAsJsonArray().size(), false);

			return eb;
		} else {
			return defaultEmbed("No event running");
		}
	}

	private EmbedBuilder cancelSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			int code = database.setSkyblockEventSettings(guildId, new SbEvent(new RunningEvent(), "false"));

			if (code == 200) {
				return defaultEmbed("Event canceled");
			} else {
				return defaultEmbed("API returned code " + code);
			}
		} else {
			return defaultEmbed("No event running");
		}
	}

	private EmbedBuilder createSkyblockEvent(CommandEvent event) {
		boolean sbEventActive = database.getSkyblockEventActive(event.getGuild().getId());
		if (guildMap.containsKey(event.getGuild().getId()) && !sbEventActive) {
			guildMap.get(event.getGuild().getId()).createSkyblockEvent(event);
			return null;
		} else if (sbEventActive) {
			return defaultEmbed("Error").setDescription("Event already running");
		}

		return defaultEmbed("Error").setDescription("Cannot find server");
	}
}
