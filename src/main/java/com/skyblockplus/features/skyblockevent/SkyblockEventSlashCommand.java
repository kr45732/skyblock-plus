/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.JsonUtils.getJsonKeys;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.command.Subcommand;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SkyblockEventSlashCommand extends SlashCommand {

	private static final Logger log = LoggerFactory.getLogger(SkyblockEventSlashCommand.class);

	public SkyblockEventSlashCommand() {
		this.name = "event";
	}

	public static Future<List<EventMember>> fetchEventMembers(JsonElement runningSettings, String guildId) {
		guildMap.get(guildId).setEventCurrentlyUpdating(true);
		return executor.submit(() -> {
			List<EventMember> eventMembers = new ArrayList<>();
			List<CompletableFuture<EventMember>> futuresList = new ArrayList<>();
			List<Player.Profile> players = new ArrayList<>();
			String eventType = higherDepth(runningSettings, "eventType").getAsString();

			for (JsonElement eventMember : higherDepth(runningSettings, "membersList").getAsJsonArray()) {
				futuresList.add(
					CompletableFuture.supplyAsync(
						() -> {
							String failCause = null;
							String username = higherDepth(eventMember, "username").getAsString();
							double curChange = -1;

							String uuid = higherDepth(eventMember, "uuid").getAsString();
							String profileName = higherDepth(eventMember, "profileName").getAsString();

							Player.Profile player = new Player(uuid, profileName, false).getSelectedProfile();
							if (player.isValid()) {
								players.add(player);

								username = player.getUsername();
								double startingAmount = higherDepth(eventMember, "startingAmount").getAsDouble();
								curChange =
									switch (eventType) {
										case "catacombs" -> player.getCatacombsXp() - startingAmount;
										default -> {
											if (eventType.startsWith("collection.")) {
												if (player.isCollectionsApiEnabled()) {
													yield higherDepth(player.profileJson(), eventType.split("-")[0], 0.0) - startingAmount;
												} else {
													failCause = "Collections API disabled";
												}
											} else if (eventType.startsWith("slayer.")) {
												double slayerXp = 0;
												String[] slayerTypes = eventType.split("slayer.")[1].split("-");
												for (String slayerType : slayerTypes) {
													slayerXp += player.getSlayerXp(slayerType);
												}
												yield slayerXp - startingAmount;
											} else if (eventType.startsWith("skills.")) {
												if (player.isSkillsApiEnabled()) {
													double skillsXp = 0;
													String[] skillTypes = eventType.split("skills.")[1].split("-");
													for (String skillType : skillTypes) {
														skillsXp += Math.max(player.getSkillXp(skillType), 0);
													}
													yield skillsXp - startingAmount;
												} else {
													failCause = "Skills API disabled";
												}
											} else if (eventType.startsWith("weight.")) {
												if (player.isSkillsApiEnabled()) {
													String[] weightTypes = eventType.split("weight.")[1].split("-");
													yield player.getWeight(weightTypes) - startingAmount;
												} else {
													failCause = "Skills API disabled";
												}
											} else {
												throw new IllegalStateException("Unexpected value: " + eventType);
											}

											yield -1;
										}
									};
							} else {
								failCause = player.getFailCause();
							}

							return new EventMember(failCause, username, uuid, "" + curChange, profileName);
						},
						playerRequestExecutor
					)
				);
			}

			for (CompletableFuture<EventMember> future : futuresList) {
				try {
					eventMembers.add(future.get());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			leaderboardDatabase.insertIntoLeaderboard(players);

			eventMembers.sort(Comparator.comparingDouble(e -> -e.parseStartingAmount()));
			guildMap.get(guildId).setEventMembers(eventMembers);
			guildMap.get(guildId).setEventLastUpdated(Instant.now());
			guildMap.get(guildId).setEventCurrentlyUpdating(false);

			return eventMembers;
		});
	}

	public static String getEventTypeFormatted(String eventType) {
		if (eventType.startsWith("collection.")) {
			return eventType.split("-")[1] + " collection";
		} else if (eventType.startsWith("skills.")) {
			String[] types = eventType.split("skills.")[1].split("-");
			return (types.length == 9 ? "skills" : String.join(", ", types) + " skill" + (types.length > 1 ? "s" : "")) + " xp";
		} else if (eventType.startsWith("slayer.")) {
			String[] types = eventType.split("slayer.")[1].split("-");
			return (types.length == 6 ? "slayer" : String.join(", ", types) + " slayer" + (types.length > 1 ? "s" : "")) + " xp";
		} else if (eventType.startsWith("weight.")) {
			String[] types = eventType.split("weight.")[1].split("-");
			return types.length == 20 ? "weight" : String.join(", ", types) + " weight" + (types.length > 1 ? "s" : "");
		}

		return eventType;
	}

	public static void getLeaderboardFormatted(List<EventMember> eventMembers, CustomPaginator.Builder paginateBuilder) {
		for (int i = 0; i < eventMembers.size(); i++) {
			EventMember eventMember = eventMembers.get(i);

			String ebStr = "`" + (i + 1) + ")` " + escapeText(eventMember.getUsername()) + " | ";
			if (eventMember.getFailCause() == null) {
				ebStr += "+" + formatNumber(eventMember.parseStartingAmount());
			} else {
				ebStr += eventMember.getFailCause();
			}
			paginateBuilder.addStrings(ebStr);
		}
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Main event command");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static class CreateSubcommand extends Subcommand {

		public CreateSubcommand() {
			this.name = "create";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		public static EmbedBuilder createSkyblockEvent(SlashCommandEvent event) {
			if (!higherDepth(database.getSkyblockEventSettings(event.getGuild().getId()), "eventType", "").isEmpty()) {
				return errorEmbed("Event already running");
			} else if (guildMap.containsKey(event.getGuild().getId())) {
				AutomaticGuild automaticGuild = guildMap.get(event.getGuild().getId());
				if (automaticGuild.skyblockEventHandler == null) {
					automaticGuild.setSkyblockEventHandler(new SkyblockEventHandler(event));
					return null;
				} else {
					return errorEmbed("Someone is already creating an event in this server");
				}
			} else {
				return errorEmbed("Cannot find server");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(createSkyblockEvent(event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Interactive message to create a Skyblock event");
		}
	}

	public static class CurrentSubcommand extends Subcommand {

		public CurrentSubcommand() {
			this.name = "current";
		}

		public static EmbedBuilder getCurrentSkyblockEvent(String guildId) {
			JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(currentSettings, "eventType", "").isEmpty()) {
				EmbedBuilder eb = defaultEmbed("Current Event");

				if (!higherDepth(currentSettings, "eventGuildId", "").isEmpty()) {
					HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
					if (!guildJson.isValid()) {
						return guildJson.getErrorEmbed();
					}
					eb.addField("Guild", guildJson.get("name").getAsString(), false);
				}

				eb.addField(
					"Event Type",
					capitalizeString(getEventTypeFormatted(higherDepth(currentSettings, "eventType").getAsString())),
					false
				);

				eb.addField("End Date", "Ends <t:" + higherDepth(currentSettings, "timeEndingSeconds").getAsLong() + ":R>", false);

				StringBuilder ebString = new StringBuilder();
				for (Map.Entry<String, JsonElement> prize : higherDepth(currentSettings, "prizeMap").getAsJsonObject().entrySet()) {
					ebString.append("`").append(prize.getKey()).append(")` ").append(prize.getValue().getAsString()).append("\n");
				}

				if (ebString.length() == 0) {
					ebString = new StringBuilder("None");
				}

				eb.addField("Prizes", ebString.toString(), false);
				eb.addField("Members Joined", "" + higherDepth(currentSettings, "membersList").getAsJsonArray().size(), false);

				return eb;
			} else {
				return defaultEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(getCurrentSkyblockEvent(event.getGuild().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get information about the current event");
		}
	}

	public static class JoinSubcommand extends Subcommand {

		public JoinSubcommand() {
			this.name = "join";
		}

		public static EmbedBuilder joinSkyblockEvent(String username, String profile, Member member, String guildId) {
			JsonElement eventSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(eventSettings, "eventType", "").isEmpty()) {
				String uuid;
				if (member != null) {
					LinkedAccount linkedAccount = database.getByDiscord(member.getId());
					if (linkedAccount == null) {
						return errorEmbed("You must be linked to run this command. Use `/link <player>` to link");
					}

					uuid = linkedAccount.uuid();
					username = linkedAccount.username();
				} else {
					UsernameUuidStruct uuidStruct = usernameToUuid(username);
					if (!uuidStruct.isValid()) {
						return errorEmbed(uuidStruct.failCause());
					}

					uuid = usernameToUuid(username).uuid();
					username = uuidStruct.username();
				}

				if (database.eventHasMemberByUuid(guildId, uuid)) {
					return errorEmbed(
						member != null
							? "You are already in the event! If you want to leave or change profile use `/event leave`"
							: "Player is already in the event"
					);
				}

				if (member != null) {
					if (!higherDepth(eventSettings, "eventGuildId", "").isEmpty()) {
						HypixelResponse guildJson = getGuildFromPlayer(uuid);
						if (!guildJson.isValid()) {
							return guildJson.getErrorEmbed();
						}

						if (!guildJson.get("_id").getAsString().equals(higherDepth(eventSettings, "eventGuildId").getAsString())) {
							return errorEmbed("You must be in the guild to join the event");
						}
					}

					String requiredRole = higherDepth(eventSettings, "whitelistRole", "");
					if (!requiredRole.isEmpty() && member.getRoles().stream().noneMatch(r -> r.getId().equals(requiredRole))) {
						return errorEmbed("You must have the <@&" + requiredRole + "> role to join this event");
					}
				}

				Player.Profile player = Player.create(username, profile);
				if (player.isValid()) {
					try {
						double startingAmount = 0;
						String startingAmountFormatted = "";

						String eventType = higherDepth(eventSettings, "eventType").getAsString();

						if ((eventType.startsWith("skills") || eventType.startsWith("weight")) && !player.isSkillsApiEnabled()) {
							return errorEmbed(
								member != null ? "Please enable your skills API before joining" : "Player's skills API is disabled"
							);
						}
						if (eventType.startsWith("collection") && !player.isCollectionsApiEnabled()) {
							return errorEmbed(
								member != null
									? "Please enable your collections API before joining"
									: "Player's collections API is disabled"
							);
						}

						if (eventType.equals("catacombs")) {
							startingAmount = player.getCatacombsXp();
							startingAmountFormatted = formatNumber(startingAmount) + " total catacombs xp";
						} else if (eventType.startsWith("collection.")) {
							startingAmount =
								higherDepth(player.profileJson(), eventType.split("-")[0]) != null
									? higherDepth(player.profileJson(), eventType.split("-")[0]).getAsDouble()
									: 0;
							startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
						} else if (eventType.startsWith("slayer.")) {
							String[] slayerTypes = eventType.split("slayer.")[1].split("-");
							for (String slayerType : slayerTypes) {
								startingAmount += player.getSlayerXp(slayerType);
							}
							startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
						} else if (eventType.startsWith("skills.")) {
							String[] skillTypes = eventType.split("skills.")[1].split("-");
							for (String skillType : skillTypes) {
								startingAmount += Math.max(player.getSkillXp(skillType), 0);
							}
							startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
						} else if (eventType.startsWith("weight.")) {
							String weightTypes = eventType.split("weight.")[1];
							startingAmount = player.getWeight(weightTypes.split("-"));
							startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
						}

						try {
							int minAmt = Integer.parseInt(higherDepth(eventSettings, "minAmount").getAsString());
							if (minAmt != -1 && startingAmount < minAmt) {
								return errorEmbed(
									(member != null ? "You" : "Player") +
									" must have at least " +
									formatNumber(minAmt) +
									" " +
									getEventTypeFormatted(eventType) +
									" to join"
								);
							}
						} catch (Exception ignored) {}

						try {
							int maxAmt = Integer.parseInt(higherDepth(eventSettings, "maxAmount").getAsString());
							if (maxAmt != -1 && startingAmount > maxAmt) {
								return errorEmbed(
									(member != null ? "You" : "Player") +
									" must have no more than " +
									formatNumber(maxAmt) +
									" " +
									getEventTypeFormatted(eventType) +
									" to join"
								);
							}
						} catch (Exception ignored) {}

						int code = database.addMemberToSkyblockEvent(
							guildId,
							new EventMember(player.getUsername(), player.getUuid(), "" + startingAmount, player.getProfileName())
						);

						if (code == 200) {
							return defaultEmbed(member != null ? "Joined event" : "Added player to event")
								.setDescription(
									"**Username:** " +
									player.getEscapedUsername() +
									"\n**Profile:** " +
									player.getProfileName() +
									"\n**Starting amount:** " +
									startingAmountFormatted
								);
						} else {
							return errorEmbed("API returned code " + code);
						}
					} catch (Exception ignored) {}
				}

				return player.getErrorEmbed();
			} else {
				return errorEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(joinSkyblockEvent(null, event.getOptionStr("profile"), event.getMember(), event.getGuild().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Join the current event").addOptions(profilesCommandOption);
		}
	}

	public static class AddSubcommand extends Subcommand {

		public AddSubcommand() {
			this.name = "add";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(
				JoinSubcommand.joinSkyblockEvent(
					event.getOptionStr("player"),
					event.getOptionStr("profile"),
					null,
					event.getGuild().getId()
				)
			);
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Force add a player to the current event")
				.addOption(OptionType.STRING, "player", "Player username or mention", true, true)
				.addOptions(profilesCommandOption);
		}
	}

	public static class LeaveSubcommand extends Subcommand {

		public LeaveSubcommand() {
			this.name = "leave";
		}

		public static EmbedBuilder leaveSkyblockEvent(String guildId, String userId) {
			if (!higherDepth(database.getSkyblockEventSettings(guildId), "eventType", "").isEmpty()) {
				LinkedAccount linkedAccount = database.getByDiscord(userId);
				if (linkedAccount != null) {
					int code = database.removeMemberFromSkyblockEvent(guildId, linkedAccount.uuid());

					if (code == 200) {
						return defaultEmbed("Success").setDescription("You left the event");
					} else {
						return errorEmbed("An error occurred when leaving the event");
					}
				} else {
					return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
				}
			} else {
				return defaultEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(leaveSkyblockEvent(event.getGuild().getId(), event.getUser().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Leave the current event");
		}
	}

	public static class RemoveSubcommand extends Subcommand {

		public RemoveSubcommand() {
			this.name = "remove";
		}

		public static EmbedBuilder removeFromSkyblockEvent(String guildId, String player) {
			if (!higherDepth(database.getSkyblockEventSettings(guildId), "eventType", "").isEmpty()) {
				UsernameUuidStruct usernameUuidStruct = usernameToUuid(player);
				if (!usernameUuidStruct.isValid()) {
					return errorEmbed(usernameUuidStruct.failCause());
				}

				int code = database.removeMemberFromSkyblockEvent(guildId, usernameUuidStruct.uuid());
				if (code == 200) {
					return defaultEmbed("Success").setDescription("Removed " + usernameUuidStruct.username() + " from the event");
				} else {
					return errorEmbed("An error occurred when leaving the event");
				}
			} else {
				return defaultEmbed("No event running");
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(removeFromSkyblockEvent(event.getGuild().getId(), event.getOptionStr("player")));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Force remove a player from the current event")
				.addOption(OptionType.STRING, "player", "Player username or mention", true, true);
		}
	}

	public static class LeaderboardSubcommand extends Subcommand {

		public LeaderboardSubcommand() {
			this.name = "leaderboard";
		}

		public static EmbedBuilder getEventLeaderboard(
			Guild guild,
			User user,
			SlashCommandEvent slashCommandEvent,
			ButtonInteractionEvent buttonEvent
		) {
			String guildId = guild.getId();

			JsonElement runningSettings = database.getSkyblockEventSettings(guildId);
			if (higherDepth(runningSettings, "eventType", "").isEmpty()) {
				return errorEmbed("No event running");
			}

			if (higherDepth(runningSettings, "membersList").getAsJsonArray().isEmpty()) {
				return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
			}

			AutomaticGuild currentGuild = guildMap.get(guildId);

			if (currentGuild.eventLastUpdated == null || Duration.between(currentGuild.eventLastUpdated, Instant.now()).toMinutes() >= 15) {
				fetchEventMembers(runningSettings, guildId);
			}

			if (currentGuild.eventLastUpdated == null) {
				return defaultEmbed("Event Leaderboard").setDescription("Event data not loaded, please try again in a few seconds");
			}

			CustomPaginator.Builder paginateBuilder = defaultPaginator(user).setItemsPerPage(25);
			paginateBuilder
				.getExtras()
				.setEveryPageText(
					"**Last Updated:** " +
					getRelativeTimestamp(currentGuild.eventLastUpdated) +
					(currentGuild.eventCurrentlyUpdating ? " (currently updating)" : "") +
					"\n"
				)
				.setEveryPageTitle("Event Leaderboard");
			getLeaderboardFormatted(currentGuild.eventMembers, paginateBuilder);

			if (slashCommandEvent != null) {
				slashCommandEvent.paginate(paginateBuilder);
			} else {
				paginateBuilder.build().paginate(buttonEvent.getHook(), 1);
			}
			return null;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(getEventLeaderboard(event.getGuild(), event.getUser(), event, null));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Get the leaderboard for current event");
		}
	}

	public static class EndSubcommand extends Subcommand {

		public EndSubcommand() {
			this.name = "end";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		public static EmbedBuilder endSkyblockEvent(Guild guild, boolean silent) {
			String guildId = guild.getId();

			JsonElement eventSettings = database.getSkyblockEventSettings(guildId);
			if (higherDepth(eventSettings, "eventType", "").isEmpty()) {
				return errorEmbed("No event running");
			}

			if (silent) {
				try {
					guild
						.getTextChannelById(higherDepth(eventSettings, "announcementId").getAsString())
						.editMessageEmbedsById(
							higherDepth(eventSettings, "announcementMessageId").getAsString(),
							defaultEmbed("Skyblock Event").setDescription("Event canceled").build()
						)
						.setComponents()
						.queue();
				} catch (Exception ignored) {}

				guildMap.get(guildId).cancelEvent();
				int code = database.setSkyblockEventSettings(guildId, new EventSettings());
				if (code == 200) {
					return defaultEmbed("Event canceled");
				} else {
					return errorEmbed("API returned code " + code);
				}
			}

			List<EventMember> eventLeaderboardList;
			try {
				eventLeaderboardList = fetchEventMembers(eventSettings, guildId).get();
			} catch (Exception e) {
				return errorEmbed("Error fetching event leaderboard, please try again in a few seconds");
			}

			TextChannel announcementChannel = jda.getTextChannelById(higherDepth(eventSettings, "announcementId").getAsString());
			try {
				announcementChannel
					.editMessageEmbedsById(
						higherDepth(eventSettings, "announcementMessageId").getAsString(),
						defaultEmbed("Skyblock Event").setDescription("Event has ended").build()
					)
					.setComponents()
					.queue();
			} catch (Exception ignored) {}

			CustomPaginator.Builder paginateBuilder = defaultPaginator()
				.setItemsPerPage(25)
				.updateExtras(extra -> extra.setEveryPageTitle("Event Leaderboard"))
				.setTimeout(24, TimeUnit.HOURS);
			getLeaderboardFormatted(eventLeaderboardList, paginateBuilder);

			try {
				if (paginateBuilder.size() > 0) {
					paginateBuilder.build().paginate(announcementChannel, 0);
				} else {
					announcementChannel
						.sendMessageEmbeds(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
						.queue();
				}
			} catch (Exception ignored) {}

			try {
				paginateBuilder =
					defaultPaginator()
						.setItemsPerPage(25)
						.updateExtras(extra -> extra.setEveryPageTitle("Prizes"))
						.setTimeout(24, TimeUnit.HOURS);

				List<String> prizeListKeys = getJsonKeys(higherDepth(eventSettings, "prizeMap"));
				for (int i = 0; i < prizeListKeys.size(); i++) {
					try {
						paginateBuilder.addStrings(
							"`" +
							(i + 1) +
							")` " +
							higherDepth(eventSettings, "prizeMap." + prizeListKeys.get(i)).getAsString() +
							" - " +
							(i < eventLeaderboardList.size() ? escapeText(eventLeaderboardList.get(i).getUsername()) : " None")
						);
					} catch (Exception ignored) {}
				}

				if (paginateBuilder.size() > 0) {
					paginateBuilder.build().paginate(announcementChannel, 0);
				}
			} catch (Exception ignored) {}

			log.info("Skyblock event ended: " + guild.getId() + " | " + eventSettings);

			guildMap.get(guildId).cancelEvent();
			int code = database.setSkyblockEventSettings(guildId, new EventSettings());
			if (code == 200) {
				return defaultEmbed("Ended Skyblock event");
			} else {
				return defaultEmbed("API returned code " + code);
			}
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(endSkyblockEvent(event.getGuild(), event.getOptionBoolean("silent", false)));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData(name, "Force end the event")
				.addOption(OptionType.BOOLEAN, "silent", "If the event should silently be canceled");
		}
	}
}
