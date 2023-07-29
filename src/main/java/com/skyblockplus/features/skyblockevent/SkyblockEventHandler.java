/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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
import static com.skyblockplus.features.skyblockevent.SkyblockEventSlashCommand.getEventTypeFormatted;
import static com.skyblockplus.utils.ApiHandler.getGuildFromName;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.getCollectionsJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class SkyblockEventHandler {

	private final SlashCommandEvent slashCommandEvent;
	private final EventSettings eventSettings = new EventSettings();
	private final EmbedBuilder eb = defaultEmbed("Skyblock Event").setDescription("Use the menu below to choose the event type");
	private Message message;
	private GuildMessageChannel announcementChannel;
	private String guildName;
	private SelectMenuState selectMenuState = SelectMenuState.TYPE_GENERIC;

	public SkyblockEventHandler(SlashCommandEvent slashCommandEvent) {
		this.slashCommandEvent = slashCommandEvent;
		slashCommandEvent
			.getHook()
			.editOriginalEmbeds(eb.build())
			.setActionRow(
				StringSelectMenu
					.create("skyblock_event_" + selectMenuState)
					.addOption("Slayer", "slayer")
					.addOption("Skills", "skills")
					.addOption("Catacombs", "catacombs")
					.addOption("Weight", "weight")
					.addOption("Collections", "collections")
					.build()
			)
			.queue(
				m -> {
					message = m;
					waitForEvent();
				},
				ignore
			);
	}

	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		switch (selectMenuState) {
			case TYPE_GENERIC -> {
				String selectedType = event.getSelectedOptions().get(0).getValue();
				switch (selectedType) {
					case "catacombs" -> {
						selectMenuState = SelectMenuState.CONFIG_GENERIC;
						eventSettings.setEventType(selectedType);
						eb.addField("Event Type", getEventTypeFormatted(eventSettings.getEventType()), false);
						event.editMessage(getGenericConfigMessage()).queue();
					}
					case "weight" -> {
						selectMenuState = SelectMenuState.TYPE_WEIGHT;
						List<SelectOption> weightOptions = Stream
							.of(SLAYER_NAMES, SKILL_NAMES, DUNGEON_CLASS_NAMES)
							.flatMap(Collection::stream)
							.map(name -> SelectOption.of(capitalizeString(name), name))
							.collect(Collectors.toCollection(ArrayList::new));
						event
							.editMessageEmbeds(
								eb.setDescription("Use the menu below to choose the weight types that should be tracked").build()
							)
							.setActionRow(
								StringSelectMenu
									.create("skyblock_event_" + selectMenuState)
									.addOption("All", "all")
									.addOption("Catacombs", "catacombs")
									.addOptions(weightOptions)
									.setMaxValues(weightOptions.size() + 1)
									.build()
							)
							.queue();
					}
					case "slayer" -> {
						selectMenuState = SelectMenuState.TYPE_SLAYER;
						List<SelectOption> slayerOptions = SLAYER_NAMES
							.stream()
							.map(name -> SelectOption.of(capitalizeString(name), name))
							.collect(Collectors.toCollection(ArrayList::new));
						event
							.editMessageEmbeds(eb.setDescription("Use the menu below to choose the slayers that should be tracked").build())
							.setActionRow(
								StringSelectMenu
									.create("skyblock_event_" + selectMenuState)
									.addOption("All", "all")
									.addOptions(slayerOptions)
									.setMaxValues(slayerOptions.size())
									.build()
							)
							.queue();
					}
					case "skills" -> {
						selectMenuState = SelectMenuState.TYPE_SKILLS;
						List<SelectOption> skillsOptions = SKILL_NAMES
							.stream()
							.map(name -> SelectOption.of(capitalizeString(name), name))
							.collect(Collectors.toCollection(ArrayList::new));
						event
							.editMessageEmbeds(eb.setDescription("Use the menu below to choose the skills that should be tracked").build())
							.setActionRow(
								StringSelectMenu
									.create("skyblock_event_" + selectMenuState)
									.addOption("All", "all")
									.addOptions(skillsOptions)
									.setMaxValues(skillsOptions.size())
									.build()
							)
							.queue();
					}
					case "collections" -> event
						.replyModal(
							Modal
								.create("skyblock_event_type_collections", "Skyblock Event")
								.addActionRow(TextInput.create("value", "Collection Type", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
				}
			}
			case TYPE_WEIGHT -> {
				selectMenuState = SelectMenuState.CONFIG_GENERIC;
				Set<String> weightTypes = event.getSelectedOptions().stream().map(SelectOption::getValue).collect(Collectors.toSet());
				if (weightTypes.remove("all")) { // Returns true if set contains 'all'
					weightTypes.addAll(SLAYER_NAMES);
					weightTypes.addAll(SKILL_NAMES);
					weightTypes.addAll(DUNGEON_CLASS_NAMES);
					weightTypes.add("catacombs");
				}
				eventSettings.setEventType("weight." + String.join("-", weightTypes));
				eb.addField("Event Type", getEventTypeFormatted(eventSettings.getEventType()), false);
				event.editMessage(getGenericConfigMessage()).queue();
			}
			case TYPE_SLAYER -> {
				selectMenuState = SelectMenuState.CONFIG_GENERIC;
				Set<String> slayerTypes = event.getSelectedOptions().stream().map(SelectOption::getValue).collect(Collectors.toSet());
				if (slayerTypes.remove("all")) { // Returns true if set contains 'all'
					slayerTypes.addAll(SLAYER_NAMES);
				}
				eventSettings.setEventType("slayer." + String.join("-", slayerTypes));
				eb.addField("Event Type", getEventTypeFormatted(eventSettings.getEventType()), false);
				event.editMessage(getGenericConfigMessage()).queue();
			}
			case TYPE_SKILLS -> {
				selectMenuState = SelectMenuState.CONFIG_GENERIC;
				Set<String> skillTypes = event.getSelectedOptions().stream().map(SelectOption::getValue).collect(Collectors.toSet());
				if (skillTypes.remove("all")) { // Returns true if set contains 'all'
					skillTypes.addAll(SKILL_NAMES);
				}
				eventSettings.setEventType("skills." + String.join("-", skillTypes));
				eb.addField("Event Type", getEventTypeFormatted(eventSettings.getEventType()), false);
				event.editMessage(getGenericConfigMessage()).queue();
			}
			case CONFIG_GENERIC -> {
				switch (event.getSelectedOptions().get(0).getValue()) {
					case "create_event" -> {
						if (eventSettings.getAnnouncementId() == null || eventSettings.getAnnouncementId().isEmpty()) {
							event
								.editMessageEmbeds(
									eb
										.setDescription("An announcement channel must be set before creating the event. Please try again.")
										.build()
								)
								.queue();
							waitForEvent();
							return;
						}
						if (eventSettings.getTimeEndingSeconds() == null || eventSettings.getTimeEndingSeconds().isEmpty()) {
							event
								.editMessageEmbeds(
									eb.setDescription("The duration must be set before creating the event. Please try again.").build()
								)
								.queue();
							waitForEvent();
							return;
						}
						if (!eventSettings.isMinMaxValid()) {
							event
								.editMessageEmbeds(
									eb.setDescription("The maximum value cannot be less than the minimum value. Please try again.").build()
								)
								.queue();
							waitForEvent();
							return;
						}

						event
							.deferEdit()
							.queue(hook -> {
								EmbedBuilder announcementEb = defaultEmbed("Skyblock Event")
									.setDescription("A new Skyblock event has been created!")
									.addField("Event Type", getEventTypeFormatted(eventSettings.getEventType()), false);

								if (guildName != null) {
									announcementEb.addField("Guild", guildName, false);
								}

								announcementEb.addField("End Date", "Ends <t:" + eventSettings.getTimeEndingSeconds() + ":R>", false);

								StringBuilder ebString = new StringBuilder();
								for (Map.Entry<Integer, String> prize : eventSettings.getPrizeMap().entrySet()) {
									ebString.append("`").append(prize.getKey()).append(")` ").append(prize.getValue()).append("\n");
								}
								announcementEb
									.addField("Prizes", ebString.isEmpty() ? "None" : ebString.toString(), false)
									.addField(
										"Join the event",
										"Click the join button below or run `/event join` to join" +
										(eventSettings.getEventGuildId().isEmpty() ? "." : " and in the guild."),
										false
									)
									.addField(
										"Leaderboard",
										"Click the leaderboard button below or run `/event leaderboard` to view the leaderboard",
										false
									);

								announcementChannel
									.sendMessageEmbeds(announcementEb.build())
									.setActionRow(
										Button.success("event_message_join", "Join Event"),
										Button.primary("event_message_leaderboard", "Event Leaderboard")
									)
									.queue(m -> {
										eventSettings.setAnnouncementMessageId(m.getId());
										if (setSkyblockEventInDatabase()) {
											hook
												.editOriginalEmbeds(
													defaultEmbed("Skyblock Event")
														.setDescription(
															"Event successfully started in " + announcementChannel.getAsMention()
														)
														.build()
												)
												.setComponents()
												.queue();
											guildMap.get(event.getGuild().getId()).scheduleSbEventFuture(gson.toJsonTree(eventSettings));
										} else {
											m.delete().queue(ignore, ignore);
											hook
												.editOriginalEmbeds(
													defaultEmbed("Skyblock Event").setDescription("Error starting event").build()
												)
												.setComponents()
												.queue();
										}
										guildMap.get(event.getGuild().getId()).setSkyblockEventHandler(null);
									});
							});
						return;
					}
					case "guild" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_guild", "Skyblock Event")
								.addActionRow(TextInput.create("value", "Guild Name", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
					case "duration" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_duration", "Skyblock Event")
								.addActionRow(
									TextInput.create("value", "Duration", TextInputStyle.SHORT).setPlaceholder("Duration in hours").build()
								)
								.build()
						)
						.queue();
					case "channel" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_channel", "Skyblock Event")
								.addActionRow(TextInput.create("value", "Channel", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
					case "prizes" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_prizes", "Skyblock Event")
								.addActionRow(
									TextInput
										.create("value", "Prizes", TextInputStyle.PARAGRAPH)
										.setPlaceholder("Use the format position:prize, separating each prize with a new line")
										.build()
								)
								.build()
						)
						.queue();
					case "minimum_amount" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_minimum_amount", "Skyblock Event")
								.addActionRow(TextInput.create("value", "Minimum Amount", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
					case "maximum_amount" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_maximum_amount", "Skyblock Event")
								.addActionRow(TextInput.create("value", "Maximum Amount", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
					case "whitelist_role" -> event
						.replyModal(
							Modal
								.create("skyblock_event_config_whitelist_role", "Skyblock Event")
								.addActionRow(TextInput.create("value", "Whitelist Role", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
				}
			}
		}

		waitForEvent();
	}

	public void onModalInteraction(ModalInteractionEvent event) {
		switch (event.getModalId().split("skyblock_event_")[1]) {
			case "type_collections" -> {
				String selectedName = event.getValues().get(0).getAsString();
				List<String> collections = new ArrayList<>();
				for (Map.Entry<String, JsonElement> collection : getCollectionsJson().entrySet()) {
					String collectionName = higherDepth(collection.getValue(), "name").getAsString();
					if (collectionName.equalsIgnoreCase(selectedName)) {
						eventSettings.setEventType("collection." + collection.getKey() + "-" + collectionName.toLowerCase());
						selectMenuState = SelectMenuState.CONFIG_GENERIC;
						eb.addField("Event Type", getEventTypeFormatted(eventSettings.getEventType()), false);
						event.editMessage(getGenericConfigMessage()).queue();
						waitForEvent();
						return;
					}
					collections.add(collectionName);
				}

				event
					.editMessageEmbeds(
						eb
							.setDescription(
								"`" +
								selectedName +
								"` is invalid. Did you mean `" +
								getClosestMatch(selectedName, collections).toLowerCase() +
								"`? Please try again."
							)
							.build()
					)
					.queue();
			}
			case "config_guild" -> {
				HypixelResponse response = getGuildFromName(event.getValues().get(0).getAsString().trim());
				if (!response.isValid()) {
					event.editMessageEmbeds(eb.setDescription(response.failCause() + ". Please try again.").build()).queue();
				} else {
					guildName = response.get("name").getAsString();
					eventSettings.setEventGuildId(response.get("_id").getAsString());
					eb.addField("Guild", guildName, false);
					event.editMessage(getGenericConfigMessage()).queue();
				}
			}
			case "config_channel" -> {
				String textChannel = event.getValues().get(0).getAsString();
				try {
					announcementChannel = (GuildMessageChannel) event.getGuild().getGuildChannelById(textChannel.replaceAll("[<#>]", ""));
				} catch (Exception ignored) {}
				try {
					announcementChannel = event.getGuild().getTextChannelsByName(textChannel.replaceAll("[<#>]", ""), true).get(0);
				} catch (Exception ignored) {}
				if (announcementChannel == null) {
					event.editMessageEmbeds(eb.setDescription("`" + textChannel + "` is invalid. Please try again.").build()).queue();
				} else {
					eventSettings.setAnnouncementId(announcementChannel.getId());
					eb.addField("Announcement Channel", announcementChannel.getAsMention(), false);
					event.editMessage(getGenericConfigMessage()).queue();
				}
			}
			case "config_duration" -> {
				String textDuration = event.getValues().get(0).getAsString();
				try {
					int eventDuration = Integer.parseInt(textDuration);
					if (eventDuration <= 0 || eventDuration > 672) {
						eb.setDescription("The event must be at least an hour and at most 4 weeks (672 hours). Please try again.");
						event.editMessageEmbeds(eb.build()).queue();
					} else {
						Instant endingTime = Instant.now().plus(eventDuration, ChronoUnit.HOURS);
						eventSettings.setTimeEndingSeconds("" + endingTime.getEpochSecond());
						eb.addField("End Date", "Ends " + getRelativeTimestamp(endingTime), false);
						event.editMessage(getGenericConfigMessage()).queue();
					}
				} catch (Exception e) {
					event.editMessageEmbeds(eb.setDescription("`" + textDuration + "` is invalid. Please try again.").build()).queue();
				}
			}
			case "config_minimum_amount" -> {
				String textMinAmt = event.getValues().get(0).getAsString();
				try {
					int minAmount = Integer.parseInt(textMinAmt);
					if (minAmount < 0) {
						event.editMessageEmbeds(eb.setDescription("Minimum value cannot be negative. Please try again.").build()).queue();
					} else {
						eventSettings.setMinAmount("" + minAmount);
						eb.addField("Minimum Amount", formatNumber(minAmount), false);
						event.editMessage(getGenericConfigMessage()).queue();
					}
				} catch (Exception e) {
					event.editMessageEmbeds(eb.setDescription("`" + textMinAmt + "` is invalid. Please try again.").build()).queue();
				}
			}
			case "config_maxmimum_amount" -> {
				String textMaxAmt = event.getValues().get(0).getAsString();
				try {
					int maxAmount = Integer.parseInt(textMaxAmt);
					if (maxAmount <= 0) {
						event
							.editMessageEmbeds(eb.setDescription("Maximum value must be greater than 0. Please try again.").build())
							.queue();
					} else {
						eventSettings.setMaxAmount("" + maxAmount);
						eb.addField("Maximum Amount", formatNumber(maxAmount), false);
						event.editMessage(getGenericConfigMessage()).queue();
					}
				} catch (Exception e) {
					event.editMessageEmbeds(eb.setDescription("`" + textMaxAmt + "` is invalid. Please try again.").build()).queue();
				}
			}
			case "config_whitelist_role" -> {
				String textRole = event.getValues().get(0).getAsString();
				Role role = null;
				try {
					role = event.getGuild().getRoleById(textRole.replaceAll("[<@&>]", ""));
				} catch (Exception ignored) {}
				try {
					role = event.getGuild().getRolesByName(textRole.replaceAll("[<@&>]", ""), true).get(0);
				} catch (Exception ignored) {}

				if (role == null) {
					eb.setDescription("The provided role does not exist");
				} else if (role.isPublicRole()) {
					eb.setDescription("The role cannot be the everyone role");
					role = null;
				} else if (role.isManaged()) {
					eb.setDescription("The role cannot be a managed role");
					role = null;
				}

				if (role == null) {
					event.editMessageEmbeds(eb.appendDescription(". Please try again.").build()).queue();
				} else {
					eventSettings.setWhitelistRole(role.getId());
					eb.addField("Required role", role.getAsMention(), false);
					event.editMessage(getGenericConfigMessage()).queue();
				}
			}
			case "config_prizes" -> {
				String textPrizes = event.getValues().get(0).getAsString();
				String[] prizeList = textPrizes.split("\n");
				Map<Integer, String> prizeListMap = new TreeMap<>();
				for (String prizeLevel : prizeList) {
					try {
						String[] prizeLevelArr = prizeLevel.split(":");
						prizeListMap.put(Integer.parseInt(prizeLevelArr[0].trim()), prizeLevelArr[1].trim());
					} catch (Exception ignored) {}
				}

				if (prizeListMap.size() == 0) {
					event.editMessageEmbeds(eb.setDescription("`" + textPrizes + "` is invalid. Please try again.").build()).queue();
				} else {
					eb.addField(
						"Prizes",
						prizeListMap
							.entrySet()
							.stream()
							.map(prize -> "`" + prize.getKey() + ")` " + prize.getValue() + "\n")
							.collect(Collectors.joining()),
						false
					);
					eventSettings.setPrizeMap(prizeListMap);
					event.editMessage(getGenericConfigMessage()).queue();
				}
			}
		}

		waitForEvent();
	}

	public boolean condition(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getMessageId().equals(message.getId()) &&
				event.getUser().getId().equals(slashCommandEvent.getUser().getId())
			);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			return ( //					modalState != null &&
				event.isFromGuild() &&
				event.getMessage() != null &&
				event.getMessage().getId().equals(message.getId()) &&
				event.getUser().getId().equals(slashCommandEvent.getUser().getId())
			);
		}
		return false;
	}

	public void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			onStringSelectInteraction(event);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			onModalInteraction(event);
		}
	}

	public void waitForEvent() {
		waiter.waitForEvent(
			GenericInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> {
				guildMap.get(message.getGuild().getId()).setSkyblockEventHandler(null);
				message
					.editMessageEmbeds(defaultEmbed("Skyblock Event").setDescription("Timeout").build())
					.setComponents()
					.queue(ignore, ignore);
			}
		);
	}

	private MessageEditData getGenericConfigMessage() {
		return new MessageEditBuilder()
			.setEmbeds(eb.setDescription("Use the menu below to configure the event settings and start the event").build())
			.setActionRow(
				StringSelectMenu
					.create("skyblock_event_" + selectMenuState)
					.addOption("Create Event", "create_event")
					.addOption("Guild", "guild")
					.addOption("Duration", "duration")
					.addOption("Channel", "channel")
					.addOption("Prizes", "prizes")
					.addOption("Minimum Amount", "minimum_amount")
					.addOption("Maximum Amount", "maximum_amount")
					.addOption("Whitelist Role", "whitelist_role")
					.build()
			)
			.build();
	}

	private boolean setSkyblockEventInDatabase() {
		if (database.getServerSettings(slashCommandEvent.getGuild().getId()) == null) {
			database.newServerSettings(
				slashCommandEvent.getGuild().getId(),
				new ServerSettingsModel(slashCommandEvent.getGuild().getName(), slashCommandEvent.getGuild().getId())
			);
		}

		return database.setSkyblockEventSettings(slashCommandEvent.getGuild().getId(), eventSettings) == 200;
	}

	private enum SelectMenuState {
		TYPE_GENERIC,
		TYPE_WEIGHT,
		TYPE_SLAYER,
		TYPE_SKILLS,
		CONFIG_GENERIC,
	}
}
