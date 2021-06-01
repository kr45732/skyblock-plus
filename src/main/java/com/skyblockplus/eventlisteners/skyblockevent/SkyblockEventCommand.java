package com.skyblockplus.eventlisteners.skyblockevent;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.eventlisteners.MainListener.getGuildMap;
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
import com.skyblockplus.utils.Player;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class SkyblockEventCommand extends Command {

	final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.of("UTC"));

	public SkyblockEventCommand() {
		this.name = "event";
		this.cooldown = 10;
	}

	public static void endSkyblockEvent(String guildId) {
		JsonElement runningEventSettings = database.getRunningEventSettings(guildId);
		JsonArray membersArr = higherDepth(runningEventSettings, "membersList").getAsJsonArray();
		TextChannel announcementChannel = jda.getTextChannelById(higherDepth(runningEventSettings, "announcementId").getAsString());

		CountDownLatch httpGetsFinishedLatch = new CountDownLatch(1);
		List<Player> guildMemberPlayersList = new ArrayList<>();

		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

			try {
				if (remainingLimit < 5) {
					TimeUnit.SECONDS.sleep(timeTillReset);
					System.out.println("Sleeping for " + timeTillReset + " seconds");
				}
			} catch (Exception ignored) {}

			asyncHttpClient
				.prepareGet("https://api.ashcon.app/mojang/v2/user/" + guildMemberUuid)
				.execute()
				.toCompletableFuture()
				.thenApply(
					uuidToUsernameResponse -> {
						try {
							return higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()), "username").getAsString();
						} catch (Exception ignored) {}
						return null;
					}
				)
				.thenApply(
					guildMemberUsernameResponse -> {
						asyncHttpClient
							.prepareGet("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + guildMemberUuid)
							.execute()
							.toCompletableFuture()
							.thenApply(
								guildMemberOuterProfileJsonResponse -> {
									try {
										try {
											remainingLimit =
												Integer.parseInt(guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Remaining"));
											timeTillReset =
												Integer.parseInt(guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Reset"));
										} catch (Exception ignored) {}

										JsonElement guildMemberOuterProfileJson = JsonParser.parseString(
											guildMemberOuterProfileJsonResponse.getResponseBody()
										);
										Player guildMemberPlayer = new Player(
											guildMemberUuid,
											guildMemberUsernameResponse,
											higherDepth(guildMember, "profileName").getAsString(),
											guildMemberOuterProfileJson,
											higherDepth(guildMember, "startingAmount").getAsLong()
										);

										if (guildMemberPlayer.isValid()) {
											guildMemberPlayersList.add(guildMemberPlayer);
											return true;
										}
									} catch (Exception ignored) {}
									guildMemberPlayersList.add(null);
									return false;
								}
							)
							.whenComplete(
								(o, throwable) -> {
									if (guildMemberPlayersList.size() == membersArr.size()) {
										httpGetsFinishedLatch.countDown();
									}
								}
							);
						return null;
					}
				);
		}

		try {
			if (!httpGetsFinishedLatch.await(20, TimeUnit.SECONDS)) {
				try {
					announcementChannel.sendMessage(defaultEmbed("Event Over").setDescription("Error fetching data").build()).queue();
				} catch (Exception e) {
					System.out.println("Error ending event");
					e.printStackTrace();
				}
				database.updateSkyblockEventSettings(guildId, new SbEvent());
				return;
			}
		} catch (Exception e) {
			System.out.println("== Stack Trace (Event End Latch) ==");
			e.printStackTrace();
		}

		List<EventMember> eventMemberList = new ArrayList<>();

		String eventType = higherDepth(runningEventSettings, "eventType").getAsString();
		for (Player guildMember : guildMemberPlayersList) {
			try {
				switch (eventType) {
					case "slayer":
						{
							eventMemberList.add(
								new EventMember(
									guildMember.getUsername(),
									guildMember.getUuid(),
									"" + (guildMember.getSlayer() - guildMember.getStartingAmount()),
									guildMember.getProfileName()
								)
							);
							break;
						}
					case "skills":
						{
							int totalSkillsXp = guildMember.getTotalSkillsXp();

							if (totalSkillsXp == -1) {
								continue;
							}

							eventMemberList.add(
								new EventMember(
									guildMember.getUsername(),
									guildMember.getUuid(),
									"" + (guildMember.getTotalSkillsXp() - guildMember.getStartingAmount()),
									guildMember.getProfileName()
								)
							);
							break;
						}
					case "catacombs":
						{
							eventMemberList.add(
								new EventMember(
									guildMember.getUsername(),
									guildMember.getUuid(),
									"" + (guildMember.getCatacombsSkill().totalSkillExp - guildMember.getStartingAmount()),
									guildMember.getProfileName()
								)
							);
							break;
						}
				}
			} catch (Exception ignored) {}
		}

		eventMemberList.sort(Comparator.comparingInt(o1 -> -Integer.parseInt(o1.getStartingAmount())));

		StringBuilder ebString = new StringBuilder();
		EmbedBuilder eb = defaultEmbed("Event Leaderboard");

		for (int i = 0; i < eventMemberList.size(); i++) {
			EventMember eventMember = eventMemberList.get(i);
			ebString
				.append("`")
				.append(i + 1)
				.append(")` ")
				.append(eventMember.getUsername())
				.append(" | +")
				.append(formatNumber(Long.parseLong(eventMember.getStartingAmount())))
				.append("\n");
		}

		eb.setDescription(ebString.toString());
		announcementChannel.sendMessage(eb.build()).complete();

		try {
			StringBuilder ebStringPrize = new StringBuilder();
			ArrayList<String> prizeListKeys = getJsonKeys(higherDepth(runningEventSettings, "prizeMap"));
			for (int i = 0; i < prizeListKeys.size(); i++) {
				ebStringPrize
					.append(i + 1)
					.append(") ")
					.append(higherDepth(runningEventSettings, "prizeMap." + prizeListKeys.get(i)).getAsString())
					.append(" - ")
					.append(eventMemberList.get(i).getUsername())
					.append(" (")
					.append(eventMemberList.get(i).getUuid())
					.append(")")
					.append("\n");
			}

			if (ebStringPrize.length() > 0) {
				announcementChannel.sendMessage(defaultEmbed("Prizes").setDescription(ebStringPrize.toString()).build()).complete();
				database.updateSkyblockEventSettings(guildId, new SbEvent());
				return;
			}
		} catch (Exception ignored) {}
		announcementChannel.sendMessage(defaultEmbed("Prizes").setDescription("None").build()).complete();
		database.updateSkyblockEventSettings(guildId, new SbEvent());
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
								return;
							}

							Map<String, AutomaticGuild> guildMap = getGuildMap();
							if (
								guildMap.containsKey(event.getGuild().getId()) && !database.getSkyblockEventActive(event.getGuild().getId())
							) {
								ebMessage
									.editMessage(
										defaultEmbed("Create a Skyblock competition")
											.setDescription("Please enter the name of the guild you want to track\nTo cancel type `exit`")
											.build()
									)
									.queue();

								guildMap.get(event.getGuild().getId()).createSkyblockEvent(event);
							} else if (database.getSkyblockEventActive(event.getGuild().getId())) {
								ebMessage.editMessage(defaultEmbed("Error").setDescription("Event already running").build()).queue();
							} else {
								ebMessage.editMessage(defaultEmbed("Error").setDescription("Cannot find server").build()).queue();
							}
							return;
						case "current":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								JsonElement currentSettings = database.getRunningEventSettings(event.getGuild().getId());
								eb = defaultEmbed("Current Event");

								JsonElement guildJson = getJson(
									"https://api.hypixel.net/guild?key=" +
									HYPIXEL_API_KEY +
									"&id=" +
									higherDepth(currentSettings, "eventGuildId").getAsString()
								);

								eb.addField("Guild", higherDepth(guildJson, "guild.name").getAsString(), false);
								eb.addField("Event Type", capitalizeString(higherDepth(currentSettings, "eventType").getAsString()), false);

								Instant eventInstantEnding = Instant.ofEpochSecond(
									higherDepth(currentSettings, "timeEndingSeconds").getAsLong()
								);

								eb.addField("End Date", formatter.format(eventInstantEnding) + " UTC", false);

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
								eb.addField(
									"Members joined",
									"" + higherDepth(currentSettings, "membersList").getAsJsonArray().size(),
									false
								);

								ebMessage.editMessage(eb.build()).queue();
							} else {
								ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
							}
							return;
						case "cancel":
							if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
								ebMessage.delete().complete();
								event
									.getChannel()
									.sendMessage("❌ You must have the Administrator permission in this Guild to use that!")
									.queue();
								return;
							}

							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								int code = database.updateSkyblockEventSettings(
									event.getGuild().getId(),
									new SbEvent(new RunningEvent(), "false")
								);

								if (code == 200) {
									ebMessage.editMessage(defaultEmbed("Event canceled").build()).queue();
								} else {
									ebMessage.editMessage(defaultEmbed("API returned code " + code).build()).queue();
								}
							} else {
								ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
							}
							return;
						case "join":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getAuthor().getId());
								if (linkedAccount != null) {
									String uuid;
									String username;
									try {
										uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();
										username = higherDepth(linkedAccount, "minecraftUsername").getAsString();
									} catch (Exception e) {
										ebMessage
											.editMessage(
												defaultEmbed(
													"You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link"
												)
													.build()
											)
											.queue();
										return;
									}

									if (database.eventHasMemberByUuid(event.getGuild().getId(), uuid)) {
										ebMessage
											.editMessage(
												defaultEmbed("Error")
													.setDescription(
														"You are already in the event! If you want to leave or change profile run `" +
														BOT_PREFIX +
														"event leave`"
													)
													.build()
											)
											.queue();
										return;
									}

									JsonElement guildIn = getJson(
										"https://api.hypixel.net/findGuild?key=" + HYPIXEL_API_KEY + "&byUuid=" + uuid
									);

									try {
										higherDepth(guildIn, "guild").getAsString();
									} catch (Exception e) {
										ebMessage
											.editMessage(
												defaultEmbed("Error").setDescription("You must be in the guild to join the event").build()
											)
											.queue();
										return;
									}

									if (
										higherDepth(guildIn, "guild")
											.getAsString()
											.equals(database.getSkyblockEventGuildId(event.getGuild().getId()))
									) {
										Player player = args.length == 3 ? new Player(username, args[2]) : new Player(username);

										if (player.isValid()) {
											long startingAmount = 0;
											String startingAmountFormatted = "";
											String eventType = higherDepth(
												database.getRunningEventSettings(event.getGuild().getId()),
												"eventType"
											)
												.getAsString();
											try {
												switch (eventType) {
													case "slayer":
														{
															startingAmount = player.getSlayer();
															startingAmountFormatted = startingAmount + " total slayer xp";
															break;
														}
													case "skills":
														{
															startingAmount = player.getTotalSkillsXp();
															startingAmountFormatted = startingAmount + " total skills xp";
															break;
														}
													case "catacombs":
														{
															startingAmount = player.getCatacombsSkill().totalSkillExp;
															startingAmountFormatted = startingAmount + " total catacombs xp";
															break;
														}
												}

												if (startingAmount != -1) {
													int code = database.addEventMemberToRunningEvent(
														event.getGuild().getId(),
														new EventMember(username, uuid, "" + startingAmount, player.getProfileName())
													);

													if (code == 200) {
														ebMessage
															.editMessage(
																defaultEmbed("Success")
																	.setDescription(
																		"You have joined the " +
																		eventType +
																		" Skyblock event as " +
																		username +
																		" on profile " +
																		player.getProfileName() +
																		" with a starting amount of " +
																		startingAmountFormatted
																	)
																	.build()
															)
															.queue();
													} else {
														ebMessage
															.editMessage(
																defaultEmbed("Error").setDescription("API returned code " + code).build()
															)
															.queue();
													}
													return;
												}
											} catch (Exception ignored) {}
											ebMessage
												.editMessage(defaultEmbed("Error").setDescription("Unable to fetch player data").build())
												.queue();
										} else {
											ebMessage.editMessage(defaultEmbed("Error").setDescription("Invalid player").build()).queue();
										}
									} else {
										ebMessage
											.editMessage(
												defaultEmbed("Error").setDescription("You must be in the guild to join the event").build()
											)
											.queue();
									}
								} else {
									ebMessage
										.editMessage(
											defaultEmbed(
												"You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link"
											)
												.build()
										)
										.queue();
								}
							} else {
								ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
							}
							return;
						case "leave":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getAuthor().getId());
								if (linkedAccount != null) {
									String uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();

									ebMessage.editMessage(defaultEmbed("Success").setDescription("You left the event").build()).queue();

									database.removeEventMemberToRunningEvent(event.getGuild().getId(), uuid);
								} else {
									ebMessage
										.editMessage(
											defaultEmbed(
												"You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link"
											)
												.build()
										)
										.queue();
								}
							} else {
								ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
							}
							return;
						case "leaderboard":
						case "lb":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								if (!getGuildMap().containsKey(event.getGuild().getId())) {
									ebMessage.editMessage(defaultEmbed("No guild found").build()).queue();
									return;
								}

								AutomaticGuild currentGuild = getGuildMap().get(event.getGuild().getId());

								if (
									(currentGuild.getEventMemberListLastUpdated() != null) &&
									(Duration.between(currentGuild.getEventMemberListLastUpdated(), Instant.now()).toMinutes() < 15)
								) {
									List<EventMember> eventMemberList = currentGuild.getEventMemberList();
									StringBuilder ebString = new StringBuilder();
									for (int i = 0; i < eventMemberList.size(); i++) {
										EventMember eventMember = eventMemberList.get(i);
										ebString
											.append("`")
											.append(i + 1)
											.append(")` ")
											.append(eventMember.getUsername())
											.append(" | +")
											.append(formatNumber(Long.parseLong(eventMember.getStartingAmount())))
											.append("\n");
									}
									eb = defaultEmbed("Event Leaderboard");
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

									eb.setDescription("**Last updated " + minutesSinceUpdateString + " ago**\n\n" + ebString);
									ebMessage.editMessage(eb.build()).queue();
									return;
								}

								JsonArray membersArr = higherDepth(
									database.getRunningEventSettings(event.getGuild().getId()),
									"membersList"
								)
									.getAsJsonArray();

								CountDownLatch httpGetsFinishedLatch = new CountDownLatch(1);
								List<Player> guildMemberPlayersList = new ArrayList<>();

								for (JsonElement guildMember : membersArr) {
									String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

									try {
										if (remainingLimit < 5) {
											TimeUnit.SECONDS.sleep(timeTillReset);
											System.out.println("Sleeping for " + timeTillReset + " seconds");
										}
									} catch (Exception ignored) {}

									asyncHttpClient
										.prepareGet("https://api.ashcon.app/mojang/v2/user/" + guildMemberUuid)
										.execute()
										.toCompletableFuture()
										.thenApply(
											uuidToUsernameResponse -> {
												try {
													return higherDepth(
														JsonParser.parseString(uuidToUsernameResponse.getResponseBody()),
														"username"
													)
														.getAsString();
												} catch (Exception ignored) {}
												return null;
											}
										)
										.thenApply(
											guildMemberUsernameResponse -> {
												asyncHttpClient
													.prepareGet(
														"https://api.hypixel.net/skyblock/profiles?key=" +
														HYPIXEL_API_KEY +
														"&uuid=" +
														guildMemberUuid
													)
													.execute()
													.toCompletableFuture()
													.thenApply(
														guildMemberOuterProfileJsonResponse -> {
															try {
																try {
																	remainingLimit =
																		Integer.parseInt(
																			guildMemberOuterProfileJsonResponse.getHeader(
																				"RateLimit-Remaining"
																			)
																		);
																	timeTillReset =
																		Integer.parseInt(
																			guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Reset")
																		);
																} catch (Exception ignored) {}

																JsonElement guildMemberOuterProfileJson = JsonParser.parseString(
																	guildMemberOuterProfileJsonResponse.getResponseBody()
																);
																Player guildMemberPlayer = new Player(
																	guildMemberUuid,
																	guildMemberUsernameResponse,
																	higherDepth(guildMember, "profileName").getAsString(),
																	guildMemberOuterProfileJson,
																	higherDepth(guildMember, "startingAmount").getAsLong()
																);

																if (guildMemberPlayer.isValid()) {
																	guildMemberPlayersList.add(guildMemberPlayer);
																	return true;
																}
															} catch (Exception ignored) {}
															guildMemberPlayersList.add(null);
															return false;
														}
													)
													.whenComplete(
														(o, throwable) -> {
															if (guildMemberPlayersList.size() == membersArr.size()) {
																httpGetsFinishedLatch.countDown();
															}
														}
													);
												return null;
											}
										);
								}

								try {
									if (!httpGetsFinishedLatch.await(20, TimeUnit.SECONDS)) {
										ebMessage.editMessage(defaultEmbed("Error fetching data").build()).queue();
										return;
									}
								} catch (Exception e) {
									System.out.println("== Stack Trace (Event Leaderboard Latch) ==");
									e.printStackTrace();
								}

								List<EventMember> eventMemberList = new ArrayList<>();

								String eventType = higherDepth(database.getRunningEventSettings(event.getGuild().getId()), "eventType")
									.getAsString();
								for (Player guildMember : guildMemberPlayersList) {
									try {
										switch (eventType) {
											case "slayer":
												{
													eventMemberList.add(
														new EventMember(
															guildMember.getUsername(),
															guildMember.getUuid(),
															"" + (guildMember.getSlayer() - guildMember.getStartingAmount()),
															guildMember.getProfileName()
														)
													);
													break;
												}
											case "skills":
												{
													int totalSkillsXp = guildMember.getTotalSkillsXp();

													if (totalSkillsXp == -1) {
														continue;
													}

													eventMemberList.add(
														new EventMember(
															guildMember.getUsername(),
															guildMember.getUuid(),
															"" + (guildMember.getTotalSkillsXp() - guildMember.getStartingAmount()),
															guildMember.getProfileName()
														)
													);
													break;
												}
											case "catacombs":
												{
													eventMemberList.add(
														new EventMember(
															guildMember.getUsername(),
															guildMember.getUuid(),
															"" +
															(
																guildMember.getCatacombsSkill().totalSkillExp -
																guildMember.getStartingAmount()
															),
															guildMember.getProfileName()
														)
													);
													break;
												}
										}
									} catch (Exception ignored) {}
								}

								eventMemberList.sort(Comparator.comparingInt(o1 -> -Integer.parseInt(o1.getStartingAmount())));

								StringBuilder ebString = new StringBuilder();
								for (int i = 0; i < eventMemberList.size(); i++) {
									EventMember eventMember = eventMemberList.get(i);
									ebString
										.append("`")
										.append(i + 1)
										.append(")` ")
										.append(eventMember.getUsername())
										.append(" | +")
										.append(formatNumber(Long.parseLong(eventMember.getStartingAmount())))
										.append("\n");
								}

								eb = defaultEmbed("Event Leaderboard");
								eb.setDescription(ebString.toString());
								ebMessage.editMessage(eb.build()).queue();

								getGuildMap().get(event.getGuild().getId()).setEventMemberList(eventMemberList);
								getGuildMap().get(event.getGuild().getId()).setEventMemberListLastUpdated(Instant.now());
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
								return;
							}

							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								endSkyblockEvent(event.getGuild().getId());
								ebMessage
									.editMessage(defaultEmbed("Success").setDescription("Event Ended").build())
									.queueAfter(3, TimeUnit.SECONDS);
							} else {
								ebMessage.editMessage(defaultEmbed("No event running").build()).queue();
							}
							return;
					}
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}
}
