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

package com.skyblockplus.features.skyblockevent;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
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
import java.util.concurrent.CompletableFuture;
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
import org.springframework.stereotype.Component;

@Component
public class SkyblockEventSlashCommand extends SlashCommand {

	public SkyblockEventSlashCommand() {
		this.name = "event";
	}

	public static class CreateSubcommand extends Subcommand {

		public CreateSubcommand() {
			this.name = "create";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(createSkyblockEvent(event));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("create", "Interactive message to create a Skyblock event");
		}

		public static EmbedBuilder createSkyblockEvent(SlashCommandEvent event) {
			if (!higherDepth(database.getSkyblockEventSettings(event.getGuild().getId()), "eventType", "").isEmpty()) {
				return invalidEmbed("Event already running");
			} else if (guildMap.containsKey(event.getGuild().getId())) {
				AutomaticGuild automaticGuild = guildMap.get(event.getGuild().getId());
				if (automaticGuild.skyblockEventHandler == null) {
					automaticGuild.setSkyblockEventHandler(new SkyblockEventHandler(event));
					return null;
				} else {
					return invalidEmbed("Someone is already creating an event in this server");
				}
			} else {
				return invalidEmbed("Cannot find server");
			}
		}
	}

	public static class CurrentSubcommand extends Subcommand {

		public CurrentSubcommand() {
			this.name = "current";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(getCurrentSkyblockEvent(event.getGuild().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("current", "Get information about the current event");
		}

		public static EmbedBuilder getCurrentSkyblockEvent(String guildId) {
			JsonElement currentSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(currentSettings, "eventType", "").isEmpty()) {
				EmbedBuilder eb = defaultEmbed("Current Event");

				if (!higherDepth(currentSettings, "eventGuildId", "").isEmpty()) {
					HypixelResponse guildJson = getGuildFromId(higherDepth(currentSettings, "eventGuildId").getAsString());
					if (!guildJson.isValid()) {
						return invalidEmbed(guildJson.failCause());
					}
					eb.addField("Guild", guildJson.get("name").getAsString(), false);
				}

				eb.addField(
					"Event Type",
					capitalizeString(getEventTypeFormatted(higherDepth(currentSettings, "eventType").getAsString())),
					false
				);

				Instant eventInstantEnding = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());

				eb.addField("End Date", "Ends <t:" + eventInstantEnding.getEpochSecond() + ":R>", false);

				ArrayList<String> prizesKeys = getJsonKeys(higherDepth(currentSettings, "prizeMap"));
				StringBuilder ebString = new StringBuilder();
				for (String prizePlace : prizesKeys) {
					ebString
						.append("`")
						.append(prizePlace)
						.append(")` ")
						.append(higherDepth(currentSettings, "prizeMap." + prizePlace).getAsString())
						.append("\n");
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
	}

	public static class JoinSubcommand extends Subcommand {

		public JoinSubcommand() {
			this.name = "join";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(joinSkyblockEvent(null, event.getOptionStr("profile"), event.getMember(), event.getGuild().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("join", "Join the current event").addOptions(profilesCommandOption);
		}

		public static EmbedBuilder joinSkyblockEvent(String username, String profile, Member member, String guildId) {
			JsonElement eventSettings = database.getSkyblockEventSettings(guildId);
			if (!higherDepth(eventSettings, "eventType", "").isEmpty()) {
				String uuid;
				if (member != null) {
					LinkedAccount linkedAccount = database.getByDiscord(member.getId());
					if (linkedAccount == null) {
						return invalidEmbed("You must be linked to run this command. Use `/link <player>` to link");
					}

					uuid = linkedAccount.uuid();
					username = linkedAccount.username();
				} else {
					UsernameUuidStruct uuidStruct = usernameToUuid(username);
					if (!uuidStruct.isValid()) {
						return invalidEmbed(uuidStruct.failCause());
					}

					uuid = usernameToUuid(username).uuid();
					username = uuidStruct.username();
				}

				if (database.eventHasMemberByUuid(guildId, uuid)) {
					return invalidEmbed(
						member != null
							? "You are already in the event! If you want to leave or change profile use `/event leave`"
							: "Player is already in the event"
					);
				}

				if (member != null) {
					if (!higherDepth(eventSettings, "eventGuildId", "").isEmpty()) {
						HypixelResponse guildJson = getGuildFromPlayer(uuid);
						if (!guildJson.isValid()) {
							return invalidEmbed(guildJson.failCause());
						}

						if (!guildJson.get("_id").getAsString().equals(higherDepth(eventSettings, "eventGuildId").getAsString())) {
							return invalidEmbed("You must be in the guild to join the event");
						}
					}

					String requiredRole = higherDepth(eventSettings, "whitelistRole", "");
					if (!requiredRole.isEmpty() && member.getRoles().stream().noneMatch(r -> r.getId().equals(requiredRole))) {
						return invalidEmbed("You must have the <@&" + requiredRole + "> role to join this event");
					}
				}

				Player.Profile player = Player.create(username, profile);
				if (player.isValid()) {
					try {
						double startingAmount = 0;
						String startingAmountFormatted = "";

						String eventType = higherDepth(eventSettings, "eventType").getAsString();

						if ((eventType.startsWith("skills") || eventType.startsWith("weight")) && !player.isSkillsApiEnabled()) {
							return invalidEmbed(
								member != null ? "Please enable your skills API before joining" : "Player's skills API is disabled"
							);
						}

						switch (eventType) {
							case "slayer" -> {
								startingAmount = player.getTotalSlayer();
								startingAmountFormatted = formatNumber(startingAmount) + " total slayer xp";
							}
							case "skills" -> {
								startingAmount = player.getTotalSkillsXp();
								startingAmountFormatted = formatNumber(startingAmount) + " total skills xp";
							}
							case "catacombs" -> {
								startingAmount = player.getCatacombs().totalExp();
								startingAmountFormatted = formatNumber(startingAmount) + " total catacombs xp";
							}
							case "weight" -> {
								startingAmount = player.getWeight();
								startingAmountFormatted = formatNumber(startingAmount) + " weight";
							}
							default -> {
								if (eventType.startsWith("collection.")) {
									startingAmount =
										higherDepth(player.profileJson(), eventType.split("-")[0]) != null
											? higherDepth(player.profileJson(), eventType.split("-")[0]).getAsDouble()
											: 0;
									startingAmountFormatted = formatNumber(startingAmount) + " " + eventType.split("-")[1] + " collection";
								} else if (eventType.startsWith("skills.")) {
									String skillType = eventType.split("skills.")[1];
									startingAmount = skillType.equals("all") ? player.getTotalSkillsXp() : player.getSkillXp(skillType);
									startingAmountFormatted =
										formatNumber(startingAmount) +
										" " +
										(skillType.equals("all") ? "total skills" : skillType) +
										"  xp";
								} else if (eventType.startsWith("weight.")) {
									String weightTypes = eventType.split("weight.")[1];
									startingAmount = player.getWeight(weightTypes.split("-"));
									startingAmountFormatted = formatNumber(startingAmount) + " " + getEventTypeFormatted(eventType);
								}
							}
						}

						try {
							int minAmt = Integer.parseInt(higherDepth(eventSettings, "minAmount").getAsString());
							if (minAmt != -1 && startingAmount < minAmt) {
								return invalidEmbed(
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
								return invalidEmbed(
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
									player.getUsername() +
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

				return player.getFailEmbed();
			} else {
				return invalidEmbed("No event running");
			}
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
			return new SubcommandData("add", "Force add a player to the current event")
				.addOption(OptionType.STRING, "player", "Player username or mention", true, true)
				.addOptions(profilesCommandOption);
		}
	}

	public static class LeaveSubcommand extends Subcommand {

		public LeaveSubcommand() {
			this.name = "leave";
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(leaveSkyblockEvent(event.getGuild().getId(), event.getUser().getId()));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("leave", "Leave the current event");
		}

		public static EmbedBuilder leaveSkyblockEvent(String guildId, String userId) {
			if (!higherDepth(database.getSkyblockEventSettings(guildId), "eventType", "").isEmpty()) {
				LinkedAccount linkedAccount = database.getByDiscord(userId);
				if (linkedAccount != null) {
					int code = database.removeMemberFromSkyblockEvent(guildId, linkedAccount.uuid());

					if (code == 200) {
						return defaultEmbed("Success").setDescription("You left the event");
					} else {
						return invalidEmbed("An error occurred when leaving the event");
					}
				} else {
					return defaultEmbed("You must be linked to run this command. Use `/link <player>` to link");
				}
			} else {
				return defaultEmbed("No event running");
			}
		}
	}

	public static class LeaderboardSubcommand extends Subcommand {

		public LeaderboardSubcommand() {
			this.name = "leaderboard";
			this.cooldown = globalCooldown + 2;
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.paginate(getEventLeaderboard(event.getGuild(), event.getUser(), event, null));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("leaderboard", "Get the leaderboard for current event");
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
				return defaultEmbed("No event running");
			}

			AutomaticGuild currentGuild = guildMap.get(guildId);

			CustomPaginator.Builder paginateBuilder = defaultPaginator(user).setColumns(1).setItemsPerPage(25);

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

				if (paginateBuilder.size() > 0) {
					if (slashCommandEvent != null) {
						slashCommandEvent.paginate(
							paginateBuilder.updateExtras(extra ->
								extra
									.setEveryPageTitle("Event Leaderboard")
									.setEveryPageText(
										"**Last Updated <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>**\n"
									)
							)
						);
					} else {
						paginateBuilder
							.updateExtras(extra ->
								extra
									.setEveryPageTitle("Event Leaderboard")
									.setEveryPageText(
										"**Last Updated:** <t:" + currentGuild.eventMemberListLastUpdated.getEpochSecond() + ":R>\n"
									)
							)
							.build()
							.paginate(buttonEvent.getHook(), 0);
					}
					return null;
				}

				return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
			}

			if (currentGuild.eventCurrentlyUpdating) {
				return invalidEmbed("The leaderboard is currently updating, please try again in a few seconds");
			}

			List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningSettings, guildId);
			if (guildMemberPlayersList == null) {
				return invalidEmbed("A Hypixel API key must be set for events with over 45 members");
			}

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

			paginateBuilder.getExtras().setEveryPageTitle("Event Leaderboard");

			guildMap.get(guildId).setEventMemberList(guildMemberPlayersList);
			guildMap.get(guildId).setEventMemberListLastUpdated(Instant.now());

			if (paginateBuilder.size() > 0) {
				if (slashCommandEvent != null) {
					slashCommandEvent.paginate(paginateBuilder);
				} else {
					paginateBuilder.build().paginate(buttonEvent.getHook(), 0);
				}
				return null;
			}

			return defaultEmbed("Event Leaderboard").setDescription("No one joined the event");
		}
	}

	public static class EndSubcommand extends Subcommand {

		public EndSubcommand() {
			this.name = "end";
			this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
		}

		@Override
		protected void execute(SlashCommandEvent event) {
			event.embed(endSkyblockEvent(event.getGuild(), event.getOptionBoolean("silent", false)));
		}

		@Override
		protected SubcommandData getCommandData() {
			return new SubcommandData("end", "Force end the event")
				.addOption(OptionType.BOOLEAN, "silent", "If the event should silently be canceled");
		}

		public static EmbedBuilder endSkyblockEvent(Guild guild, boolean silent) {
			String guildId = guild.getId();
			JsonElement runningEventSettings = database.getSkyblockEventSettings(guildId);
			if (higherDepth(runningEventSettings, "eventType", "").isEmpty()) {
				return defaultEmbed("No event running");
			}

			if (silent) {
				try {
					guild
						.getTextChannelById(higherDepth(runningEventSettings, "announcementId").getAsString())
						.retrieveMessageById(higherDepth(runningEventSettings, "announcementMessageId").getAsString())
						.queue(m ->
							m
								.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event canceled").build())
								.setComponents()
								.queue()
						);
				} catch (Exception ignored) {}

				guildMap.get(guild.getId()).setEventMemberListLastUpdated(null);
				int code = database.setSkyblockEventSettings(guildId, new EventSettings());

				if (code == 200) {
					return defaultEmbed("Event canceled");
				} else {
					return defaultEmbed("API returned code " + code);
				}
			}

			TextChannel announcementChannel = jda.getTextChannelById(higherDepth(runningEventSettings, "announcementId").getAsString());
			guildMap.get(guildId).setEventMemberListLastUpdated(null);
			List<EventMember> guildMemberPlayersList = getEventLeaderboardList(runningEventSettings, guildId);
			if (guildMemberPlayersList == null) {
				return invalidEmbed("A Hypixel API key must be set for events over 45 members so the leaderboard can be calculated");
			}
			guildMap.get(guildId).setEventMemberListLastUpdated(null);

			try {
				announcementChannel
					.retrieveMessageById(higherDepth(runningEventSettings, "announcementMessageId").getAsString())
					.queue(m ->
						m
							.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Event has ended").build())
							.setComponents()
							.queue()
					);
			} catch (Exception ignored) {}

			CustomPaginator.Builder paginateBuilder = defaultPaginator()
				.setColumns(1)
				.setItemsPerPage(25)
				.updateExtras(extra -> extra.setEveryPageTitle("Event Leaderboard"))
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

			try {
				if (paginateBuilder.size() > 0) {
					paginateBuilder.build().paginate(announcementChannel, 0);
				} else {
					announcementChannel
						.sendMessageEmbeds(defaultEmbed("Event Leaderboard").setDescription("No one joined the event").build())
						.complete();
				}
			} catch (Exception ignored) {}

			try {
				paginateBuilder =
					defaultPaginator()
						.setColumns(1)
						.setItemsPerPage(25)
						.updateExtras(extra -> extra.setEveryPageTitle("Prizes"))
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
							(i < guildMemberPlayersList.size() ? fixUsername(guildMemberPlayersList.get(i).getUsername()) : " None")
						);
					} catch (Exception ignored) {}
				}

				if (paginateBuilder.size() > 0) {
					paginateBuilder.build().paginate(announcementChannel, 0);
				} else {
					announcementChannel.sendMessageEmbeds(defaultEmbed("Prizes").setDescription("None").build()).complete();
				}
			} catch (Exception ignored) {}

			database.setSkyblockEventSettings(guildId, new EventSettings());
			guildMap.get(guildId).cancelSbEventFuture();
			return defaultEmbed("Success").setDescription("Ended Skyblock event");
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

	public static List<EventMember> getEventLeaderboardList(JsonElement runningSettings, String guildId) {
		List<EventMember> guildMemberPlayersList = new ArrayList<>();
		List<CompletableFuture<EventMember>> futuresList = new ArrayList<>();
		List<Player.Profile> players = new ArrayList<>();
		JsonArray membersArr = higherDepth(runningSettings, "membersList").getAsJsonArray();
		String eventType = higherDepth(runningSettings, "eventType").getAsString();

		String key = database.getServerHypixelApiKey(guildId);
		key = checkHypixelKey(key) == null ? key : null; // Set key to null if invalid
		if (membersArr.size() > 40 && key == null) {
			return null;
		}
		String hypixelKey = key;

		guildMap.get(guildId).setEventCurrentlyUpdating(true);
		for (JsonElement guildMember : membersArr) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
			String guildMemberProfile = higherDepth(guildMember, "profileName").getAsString();

			try {
				if (hypixelKey != null ? keyCooldownMap.get(hypixelKey).isRateLimited() : remainingLimit.get() < 5) {
					System.out.println(
						"Sleeping for " +
						(hypixelKey != null ? keyCooldownMap.get(hypixelKey).getTimeTillReset() : timeTillReset) +
						" seconds"
					);
					TimeUnit.SECONDS.sleep(hypixelKey != null ? keyCooldownMap.get(hypixelKey).getTimeTillReset() : timeTillReset.get());
				}
			} catch (Exception ignored) {}

			futuresList.add(
				asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey != null ? hypixelKey : HYPIXEL_API_KEY)
					.thenApplyAsync(
						guildMemberProfileJsonResponse -> {
							Player.Profile guildMemberPlayer = new Player(
								guildMemberUuid,
								uuidToUsername(guildMemberUuid).username(),
								guildMemberProfile,
								guildMemberProfileJsonResponse,
								false
							)
								.getSelectedProfile();

							if (guildMemberPlayer.isValid()) {
								players.add(guildMemberPlayer);

								Double curChange =
									switch (eventType) {
										case "slayer" -> guildMemberPlayer.getTotalSlayer() -
										higherDepth(guildMember, "startingAmount").getAsDouble();
										case "catacombs" -> guildMemberPlayer.getCatacombs().totalExp() -
										higherDepth(guildMember, "startingAmount").getAsDouble();
										case "weight" -> guildMemberPlayer.getWeight() -
										higherDepth(guildMember, "startingAmount").getAsDouble();
										default -> {
											if (eventType.startsWith("collection.")) {
												yield higherDepth(guildMemberPlayer.profileJson(), eventType.split("-")[0], 0.0) -
												higherDepth(guildMember, "startingAmount").getAsDouble();
											} else if (eventType.startsWith("skills.")) {
												String skillType = eventType.split("skills.")[1];
												double skillXp = skillType.equals("all")
													? guildMemberPlayer.getTotalSkillsXp()
													: guildMemberPlayer.getSkillXp(skillType);

												if (skillXp != -1) {
													yield skillXp - higherDepth(guildMember, "startingAmount").getAsDouble();
												}
											} else if (eventType.startsWith("weight.")) {
												String weightTypes = eventType.split("weight.")[1];
												double weightAmt = guildMemberPlayer.getWeight(weightTypes.split("-"));

												if (weightAmt != -1) {
													yield weightAmt - higherDepth(guildMember, "startingAmount").getAsDouble();
												}
											}

											yield null;
										}
									};

								if (curChange != null) {
									return new EventMember(
										guildMemberPlayer.getUsername(),
										guildMemberUuid,
										"" + curChange,
										higherDepth(guildMember, "profileName").getAsString()
									);
								}
							}
							return null;
						},
						executor
					)
			);
		}

		for (CompletableFuture<EventMember> future : futuresList) {
			try {
				EventMember playerFutureResponse = future.get();
				if (playerFutureResponse != null) {
					guildMemberPlayersList.add(playerFutureResponse);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		leaderboardDatabase.insertIntoLeaderboard(players);

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.getStartingAmount())));

		guildMap.get(guildId).setEventCurrentlyUpdating(false);
		return guildMemberPlayersList;
	}

	public static String getEventTypeFormatted(String eventType) {
		if (eventType.startsWith("collection.")) {
			return eventType.split("-")[1] + " collection";
		} else if (eventType.startsWith("skills.")) {
			return eventType.split("skills.")[1].equals("all") ? "skills" : eventType.split("skills.")[1];
		} else if (eventType.startsWith("weight.")) {
			String[] types = eventType.split("weight.")[1].split("-");
			return types.length == 18 ? "weight" : String.join(", ", types) + " weight" + (types.length > 1 ? "s" : "");
		}

		return eventType;
	}
}
