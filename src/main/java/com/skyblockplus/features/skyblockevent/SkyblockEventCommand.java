package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.RunningEvent;
import com.skyblockplus.api.serversettings.skyblockevent.SbEvent;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelResponse;
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
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyblockEventCommand extends Command {

	private static final Logger log = LoggerFactory.getLogger(SkyblockEventCommand.class);

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
			announcementChannel
				.sendMessageEmbeds(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
				.complete();
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
		announcementChannel.sendMessageEmbeds(defaultEmbed("Prizes").setDescription("None").build()).complete();
		database.setSkyblockEventSettings(guildId, new SbEvent());
	}

	private static List<EventMember> getEventLeaderboardList(JsonElement runningSettings) {
		List<EventMember> guildMemberPlayersList = new ArrayList<>();
		List<CompletableFuture<CompletableFuture<EventMember>>> futuresList = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

			CompletableFuture<String> guildMemberUsername = asyncUuidToUsername(guildMemberUuid);
			futuresList.add(
				guildMemberUsername.thenApply(guildMemberUsernameResponse -> {
					try {
						if (remainingLimit.get() < 5) {
							log.info("Sleeping for " + timeTillReset + " seconds");
							TimeUnit.SECONDS.sleep(timeTillReset.get());
						}
					} catch (Exception ignored) {}

					CompletableFuture<JsonElement> guildMemberProfileJson = asyncSkyblockProfilesFromUuid(guildMemberUuid, HYPIXEL_API_KEY);

					return guildMemberProfileJson.thenApply(guildMemberProfileJsonResponse -> {
						Player guildMemberPlayer = new Player(guildMemberUuid, guildMemberUsernameResponse, guildMemberProfileJsonResponse);

						if (guildMemberPlayer.isValid()) {
							switch (eventType) {
								case "slayer":
									{
										return new EventMember(
											guildMemberUsernameResponse,
											guildMemberUuid,
											"" +
											(guildMemberPlayer.getTotalSlayer() - higherDepth(guildMember, "startingAmount").getAsDouble()),
											higherDepth(guildMember, "profileName").getAsString()
										);
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
											"" + (guildMemberPlayer.getWeight() - higherDepth(guildMember, "startingAmount").getAsDouble()),
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
														higherDepth(guildMemberPlayer.profileJson(), eventType.split("-")[0]) != null
															? higherDepth(guildMemberPlayer.profileJson(), eventType.split("-")[0])
																.getAsDouble()
															: 0
													) -
													higherDepth(guildMember, "startingAmount").getAsDouble()
												),
												higherDepth(guildMember, "profileName").getAsString()
											);
										} else if (eventType.startsWith("skills.")) {
											String skillType = eventType.split("skills.")[1];
											double skillXp = skillType.equals("all")
												? guildMemberPlayer.getTotalSkillsXp()
												: guildMemberPlayer.getSkillXp(skillType);

											if (skillXp != -1) {
												return new EventMember(
													guildMemberUsernameResponse,
													guildMemberUuid,
													"" + (skillXp - higherDepth(guildMember, "startingAmount").getAsDouble()),
													higherDepth(guildMember, "profileName").getAsString()
												);
											}
										}
									}
							}
						}

						return null;
					});
				})
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
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 || args.length == 3) {
					if (
						(args[1].equals("create") || args[1].equals("cancel") || args[1].equals("end")) &&
						!event.getMember().hasPermission(Permission.ADMINISTRATOR)
					) {
						ebMessage.delete().complete();
						event.getChannel().sendMessage("❌ You must have the Administrator permission in this Guild to use that!").queue();
						return;
					}

					switch (args[1]) {
						case "create":
							paginate(createSkyblockEvent(event));
							return;
						case "current":
							embed(getCurrentSkyblockEvent(event.getGuild().getId()));
							return;
						case "cancel":
							embed(cancelSkyblockEvent(event.getGuild().getId()));
							return;
						case "join":
							embed(joinSkyblockEvent(event, args));
							return;
						case "leave":
							embed(leaveSkyblockEvent(event));
							return;
						case "leaderboard":
						case "lb":
							paginate(getEventLeaderboard(event));
							return;
						case "end":
							if (database.getSkyblockEventActive(event.getGuild().getId())) {
								endSkyblockEvent(event.getGuild().getId());
								embed(defaultEmbed("Success").setDescription("Event Ended"));
							} else {
								embed(defaultEmbed("No event running"));
							}
							return;
					}
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getEventLeaderboard(CommandEvent event) {
		if (!database.getSkyblockEventActive(event.getGuild().getId())) {
			return defaultEmbed("No event running");
		}

		if (!guildMap.containsKey(event.getGuild().getId())) {
			return defaultEmbed("No guild found");
		}

		AutomaticGuild currentGuild = guildMap.get(event.getGuild().getId());

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(25);

		if (
			(currentGuild.eventMemberList != null) &&
			(currentGuild.eventMemberListLastUpdated != null) &&
			(Duration.between(currentGuild.eventMemberListLastUpdated, Instant.now()).toMinutes() < 15)
		) {
			List<EventMember> eventMemberList = currentGuild.eventMemberList;
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
				long minutesSinceUpdate = Duration.between(currentGuild.eventMemberListLastUpdated, Instant.now()).toMinutes();

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
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}

		JsonElement runningSettings = database.getRunningEventSettings(event.getGuild().getId());
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

		guildMap.get(event.getGuild().getId()).setEventMemberList(guildMemberPlayersList);
		guildMap.get(event.getGuild().getId()).setEventMemberListLastUpdated(Instant.now());

		if (paginateBuilder.getItemsSize() > 0) {
			paginateBuilder.build().paginate(event.getChannel(), 0);
			return null;
		}

		return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
	}

	private EmbedBuilder leaveSkyblockEvent(CommandEvent event) {
		if (database.getSkyblockEventActive(event.getGuild().getId())) {
			JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getAuthor().getId());
			if (linkedAccount != null) {
				String uuid = higherDepth(linkedAccount, "minecraftUuid").getAsString();
				database.removeEventMemberFromRunningEvent(event.getGuild().getId(), uuid);

				return defaultEmbed("Success").setDescription("You left the event");
			} else {
				return defaultEmbed(
					"You must be linked to run this command. Use `" + getGuildPrefix(event.getGuild().getId()) + "link [IGN]` to link"
				);
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
					return defaultEmbed(
						"You must be linked to run this command. Use `" + getGuildPrefix(event.getGuild().getId()) + "link [IGN]` to link"
					);
				}

				if (database.eventHasMemberByUuid(event.getGuild().getId(), uuid)) {
					return invalidEmbed(
						"You are already in the event! If you want to leave or change profile use `" +
						getGuildPrefix(event.getGuild().getId()) +
						"event leave`"
					);
				}

				HypixelResponse guildJson = getGuildFromPlayer(uuid);

				if (guildJson.isNotValid()) {
					return invalidEmbed(guildJson.failCause);
				}

				if (!guildJson.get("_id").getAsString().equals(database.getSkyblockEventGuildId(event.getGuild().getId()))) {
					return invalidEmbed("You must be in the guild to join the event");
				}
				Player player = args.length == 3 ? new Player(username, args[2]) : new Player(username);

				if (player.isValid()) {
					try {
						double startingAmount = 0;
						String startingAmountFormatted = "";
						String eventType = higherDepth(database.getRunningEventSettings(event.getGuild().getId()), "eventType")
							.getAsString();

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
											higherDepth(player.profileJson(), eventType.split("-")[0]) != null
												? higherDepth(player.profileJson(), eventType.split("-")[0]).getAsDouble()
												: 0;
										startingAmountFormatted =
											formatNumber(startingAmount) + " " + eventType.split("-")[1] + " collection";
									} else if (eventType.startsWith("skills.")) {
										String skillType = eventType.split("skills.")[1];
										startingAmount = skillType.equals("all") ? player.getTotalSkillsXp() : player.getSkillXp(skillType);
										startingAmountFormatted =
											formatNumber(startingAmount) +
											" " +
											(skillType.equals("all") ? "total skills" : skillType) +
											"  xp";
										break;
									}
									break;
								}
						}

						if (startingAmount == -1) {
							return invalidEmbed("Please enable your skills API and try again");
						}

						int code = database.addEventMemberToRunningEvent(
							event.getGuild().getId(),
							new EventMember(username, uuid, "" + startingAmount, player.getProfileName())
						);

						if (code == 200) {
							return defaultEmbed("Joined event")
								.setDescription(
									"**Username:** " +
									username +
									"\n**Profile:** " +
									player.getProfileName() +
									"\n**Starting amount:** " +
									startingAmountFormatted
								);
						} else {
							return invalidEmbed("API returned code " + code);
						}
					} catch (Exception ignored) {}
				}

				return invalidEmbed(player.getFailCause());
			} else {
				return invalidEmbed(
					"You must be linked to run this command. Use `" + getGuildPrefix(event.getGuild().getId()) + "link [IGN]` to link"
				);
			}
		} else {
			return invalidEmbed("No event running");
		}
	}

	private EmbedBuilder getCurrentSkyblockEvent(String guildId) {
		if (database.getSkyblockEventActive(guildId)) {
			JsonElement currentSettings = database.getRunningEventSettings(guildId);
			EmbedBuilder eb = defaultEmbed("Current Event");

			HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
			if (guildJson.isNotValid()) {
				return invalidEmbed(guildJson.failCause);
			}
			eb.addField("Guild", guildJson.get("name").getAsString(), false);

			String eventType = higherDepth(currentSettings, "eventType").getAsString();
			String eventTypeFormatted = eventType;
			if (eventType.startsWith("collection.")) {
				eventTypeFormatted = eventType.split("-")[1] + " collection";
			} else if (eventType.startsWith("skills.")) {
				eventTypeFormatted = eventType.split("skills.")[1].equals("all") ? "skills" : eventType.split("skills.")[1];
			}

			eb.addField("Event Type", capitalizeString(eventTypeFormatted), false);

			Instant eventInstantEnding = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());

			eb.addField("End Date", "Ends in <t:" + eventInstantEnding.getEpochSecond() + ":R>", false);

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
			return invalidEmbed("Event already running");
		}

		return invalidEmbed("Cannot find server");
	}
}
