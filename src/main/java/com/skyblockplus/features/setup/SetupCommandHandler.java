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

package com.skyblockplus.features.setup;

import static com.skyblockplus.features.listeners.MainListener.onApplyReload;
import static com.skyblockplus.features.listeners.MainListener.onVerifyReload;
import static com.skyblockplus.utils.Constants.cropNameToEmoji;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skyblockplus.settings.SettingsExecute;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

public class SetupCommandHandler {

	private final ButtonInteractionEvent buttonEvent;
	private SettingsExecute settings;
	private FeatureType featureType;
	private boolean fetchurOrMayorChannelSet = false;

	public SetupCommandHandler(ButtonInteractionEvent buttonEvent, String feature) {
		this.buttonEvent = buttonEvent;
		this.featureType = FeatureType.valueOf(feature.toUpperCase());

		// TODO: add buttons to go back
		// TODO: add sb event notif setup
		switch (this.featureType) {
			case VERIFY -> buttonEvent
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(
							defaultEmbed("Setup").setDescription("Use the menu below to configure the verification settings").build()
						)
						.setActionRow(
							StringSelectMenu
								.create("setup_command_" + featureType)
								.addOption("Enable", "enable")
								.addOption("Verification Message", "message")
								.addOption("Verified Roles", "roles")
								.addOption("Verification Channel", "channel")
								.addOption("Nickname Template", "nickname")
								.addOption("Remove Role", "remove_role")
								.addOption("Toggle Sync Roles & Name", "sync")
								.addOption("Toggle DM On Join Sync", "dm_on_join")
								.addOption("Toggle Automatic Roles Claim", "roles_claim")
								.build()
						)
						.build()
				)
				.queue();
			case GUILD -> buttonEvent
				.replyModal(
					Modal
						.create("setup_command_" + featureType, "Setup")
						.addActionRow(
							TextInput
								.create("value", "Guild Name", TextInputStyle.SHORT)
								.setPlaceholder("Name of your Hypixel guild")
								.build()
						)
						.build()
				)
				.queue(ignored -> buttonEvent.getHook().editOriginalComponents().queue());
			case ROLES -> {
				buttonEvent
					.editMessageEmbeds(
						defaultEmbed("Setup")
							.setDescription(
								"""
														**__Overview__**
														1) When a user runs `roles claim [player]` their stats are fetched
														2) Depending on the roles setup for this server and the users stats, the corresponding roles will be given

														**__Setup__**
														- In order to enable automatic roles, there must be at least one role setting enabled:
														- `settings roles add [role_name] [value] [@role]` - add a level to a role
														- `settings roles remove [role_name] [value]` - remove a level from a role
														- `settings roles set [role_name] [@role]` - set a one level role's role
														- `settings roles enable [role_name]` - enable a role.
														• Tutorial video linked [__here__](https://streamable.com/wninsw)

														**__Enable__**
														- Once all these settings are set run `settings roles enable` to enable roles
														- To view all the roles, their descriptions, and examples, type `settings roles`
														- For more help type `help settings roles` or watch the video linked above
														"""
							)
							.build()
					)
					.setComponents()
					.queue();
				return;
			}
			case FETCHUR -> buttonEvent
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(defaultEmbed("Setup").setDescription("Use the menu below to configure the fetchur settings").build())
						.setActionRow(
							StringSelectMenu
								.create("setup_command_" + featureType)
								.addOption("Fetchur Channel", "channel")
								.addOption("Fetchur Role", "role")
								.build()
						)
						.build()
				)
				.queue();
			case MAYOR -> buttonEvent
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(defaultEmbed("Setup").setDescription("Use the menu below to configure the mayor settings").build())
						.setActionRow(
							StringSelectMenu
								.create("setup_command_" + featureType)
								.addOption("Mayor Channel", "channel")
								.addOption("Mayor Role", "role")
								.build()
						)
						.build()
				)
				.queue();
			case JACOB -> buttonEvent // TODO: select menu with each crop and then press on crop to set role
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(defaultEmbed("Setup").setDescription("Use the menu below to configure the jacob settings").build())
						.setActionRow(
							StringSelectMenu
								.create("setup_command_" + featureType)
								.addOption("Enable", "enable")
								.addOption("Jacob Channel", "channel")
								.addOption("Crops", "crops")
								.build()
						)
						.build()
				)
				.queue();
		}
		waitForEvent();
	}

	private boolean condition(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof ModalInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getMessage() != null &&
				event.getMessage().getId().equals(buttonEvent.getMessageId()) &&
				event.getUser().getId().equals(buttonEvent.getUser().getId())
			);
		} else if (genericEvent instanceof StringSelectInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getMessageId().equals(buttonEvent.getMessageId()) &&
				event.getUser().getId().equals(buttonEvent.getUser().getId())
			);
		} else if (genericEvent instanceof EntitySelectInteractionEvent event) {
			if (
				event.isFromGuild() &&
				event.getUser().getId().equals(buttonEvent.getUser().getId()) &&
				event.getComponentId().startsWith("setup_command_")
			) {
				String[] split = event.getComponentId().split("setup_command_")[1].split("_", 2);
				return split.length >= 1 && split[0].equals(buttonEvent.getMessageId());
			}
		}
		return false;
	}

	private void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof ModalInteractionEvent event) {
			if (onModalInteraction(event)) {
				return;
			}
		} else if (genericEvent instanceof StringSelectInteractionEvent event) {
			if (onStringSelectInteraction(event)) {
				return;
			}
		} else if (genericEvent instanceof EntitySelectInteractionEvent event) {
			if (onEntitySelectInteraction(event)) {
				return;
			}
		}
		waitForEvent();
	}

	public boolean onStringSelectInteraction(StringSelectInteractionEvent event) {
		switch (featureType) {
			case VERIFY -> {
				String selectedOption = event.getSelectedOptions().get(0).getValue();
				switch (selectedOption) {
					case "enable" -> {
						event.deferReply(true).complete();
						EmbedBuilder eb = getSettings().setVerifyEnable(true);
						if (!eb.build().getTitle().equals("Settings")) {
							event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
						} else {
							String msg = onVerifyReload(event.getGuild().getId());
							if (msg.equals("Reloaded")) {
								event
									.getHook()
									.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled automatic verification").build())
									.queue();
							} else {
								event.getHook().editOriginalEmbeds(defaultEmbed("Error").setDescription(msg).build()).queue();
							}
							event.getMessage().editMessageComponents().queue();
							return true;
						}
					}
					case "message" -> event
						.replyModal(
							Modal
								.create("setup_command_" + selectedOption, "Setup")
								.addActionRow(TextInput.create("value", "Verification Message", TextInputStyle.PARAGRAPH).build())
								.build()
						)
						.queue();
					case "roles" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Roles given when verifying").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.ROLE
								)
								.setMaxValues(3)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Verification Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "nickname" -> event
						.replyModal(
							Modal
								.create("setup_command_" + selectedOption, "Setup")
								.addActionRow(
									TextInput
										.create("value", "Verified Nickname", TextInputStyle.SHORT)
										.setPlaceholder("Template to nick when verified ([PREFIX] [IGN] [POSTFIX])")
										.build()
								)
								.build()
						)
						.queue();
					case "remove_role" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Role removed when verified").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.ROLE
								)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "sync" -> {
						event.deferReply(true).complete();
						event
							.getHook()
							.editOriginalEmbeds(
								getSettings()
									.setVerifySyncEnable(
										!higherDepth(
											database.getVerifySettings(event.getGuild().getId()).getAsJsonObject(),
											"enableAutomaticSync",
											false
										)
									)
									.build()
							)
							.queue();
					}
					case "dm_on_join" -> {
						event.deferReply(true).complete();
						event
							.getHook()
							.editOriginalEmbeds(
								getSettings()
									.setVerifyDmOnSync(
										!higherDepth(
											database.getVerifySettings(event.getGuild().getId()).getAsJsonObject(),
											"dmOnSync",
											true
										)
									)
									.build()
							)
							.queue();
					}
					case "roles_claim" -> {
						event.deferReply(true).complete();
						event
							.getHook()
							.editOriginalEmbeds(
								getSettings()
									.setVerifyRolesClaimEnable(
										!higherDepth(
											database.getVerifySettings(event.getGuild().getId()).getAsJsonObject(),
											"enableRolesClaim",
											false
										)
									)
									.build()
							)
							.queue();
					}
				}
			}
			case GUILD -> {
				featureType =
					FeatureType.valueOf(event.getSelectedOptions().get(0).getValue().toUpperCase()).setGuildName(featureType.guildName);
				switch (featureType) {
					case GUILD_APPLY -> event
						.editMessage(
							new MessageEditBuilder()
								.setEmbeds(
									defaultEmbed("Setup")
										.setDescription("Use the menu below to configure the guild application settings")
										.build()
								)
								.setActionRow(
									StringSelectMenu
										.create("setup_command_" + featureType)
										.addOption("Enable", "enable")
										.addOption("Application Message", "message")
										.addOption("Application Channel", "channel")
										.addOption("New Application Category", "category")
										.addOption("Staff Channel", "staff_channel")
										.addOption("Staff Role", "staff_role")
										.addOption("Accepted Message", "accept_message")
										.addOption("Denied Message", "deny_message")
										.addOption("Waitlisted Message", "waitlist_message")
										.addOption("Waiting Channel", "waiting_channel")
										.addOption("Requirements", "requirements")
										.addOption("Toggle Scammer Check", "scammer_check")
										.addOption("Required Gamemode", "gamemode")
										.addOption("Toggle APIs Enabled Check", "check_api")
										.build()
								)
								.build()
						)
						.queue();
					case GUILD_COUNTER -> {
						event.deferReply(true).complete();
						EmbedBuilder eb = getSettings().setGuildCounterEnable(getGuildSettings(), true);
						if (eb.build().getTitle().equals("Settings")) {
							event
								.getHook()
								.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild member counter").build())
								.queue(ignore, ignore);
							return true;
						} else {
							event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again").build()).queue();
						}
					}
					case GUILD_RANKS -> event
						.replyModal(
							Modal
								.create("setup_command_" + featureType, "Setup")
								.addActionRow(
									TextInput
										.create("value", "Guild Ranks", TextInputStyle.SHORT)
										.setPlaceholder("Use format: rank - @role, rank - @role, ...")
										.build()
								)
								.build()
						)
						.queue();
					case GUILD_ROLE -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Guild Member Role").build())
						.setActionRow(
							EntitySelectMenu
								.create("setup_command_" + buttonEvent.getMessageId(), EntitySelectMenu.SelectTarget.ROLE)
								.build()
						)
						.setEphemeral(true)
						.queue();
				}
			}
			case GUILD_APPLY -> {
				String selectedOption = event.getSelectedOptions().get(0).getValue();
				Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
				switch (selectedOption) {
					case "enable" -> {
						event.deferReply(true).complete();
						EmbedBuilder eb = getSettings().setApplyEnable(getGuildSettings(), true);
						if (eb.build().getTitle().equals("Settings")) {
							String msg = onApplyReload(event.getGuild().getId());
							if (msg.contains("• Reloaded `" + featureType.guildName + "`")) {
								event
									.getHook()
									.editOriginalEmbeds(
										defaultEmbed("Success").setDescription("Enabled this automatic application system.").build()
									)
									.queue();
							} else {
								if (!msg.contains("• `" + featureType.guildName + "` is disabled")) {
									msg = "`" + featureType.guildName + "` is disabled";
								} else {
									msg =
										"Error Reloading for `" +
										featureType.guildName +
										msg.split("• Error Reloading for `" + featureType.guildName)[1].split("\n")[0];
								}
								event.getHook().editOriginalEmbeds(defaultEmbed("Error").setDescription(msg).build()).queue();
							}
							event.getMessage().editMessageComponents().queue(ignore, ignore);
							return true;
						} else {
							event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
						}
					}
					case "message" -> event
						.replyModal(
							modalBuilder
								.addActionRow(TextInput.create("value", "Application Message", TextInputStyle.PARAGRAPH).build())
								.build()
						)
						.queue();
					case "channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Application Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "category" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("New Application Category").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.CATEGORY)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "staff_channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Staff Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "staff_role" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Staff Ping Role").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.ROLE
								)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "accept_message" -> event
						.replyModal(
							modalBuilder
								.addActionRow(TextInput.create("value", "Accepted Message", TextInputStyle.PARAGRAPH).build())
								.build()
						)
						.queue();
					case "deny_message" -> event
						.replyModal(
							modalBuilder.addActionRow(TextInput.create("value", "Denied Message", TextInputStyle.PARAGRAPH).build()).build()
						)
						.queue();
					case "waitlist_message" -> event
						.replyModal(
							modalBuilder
								.addActionRow(TextInput.create("value", "Waitlisted Message", TextInputStyle.PARAGRAPH).build())
								.build()
						)
						.queue();
					case "waiting_channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Waiting Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "requirements" -> event
						.replyModal(
							modalBuilder
								.addActionRow(
									TextInput
										.create("value", "Requirements", TextInputStyle.PARAGRAPH)
										.setPlaceholder("type:amount type:amount, type:amount, ...")
										.build()
								)
								.build()
						)
						.queue();
					case "gamemode" -> event
						.replyModal(
							modalBuilder
								.addActionRow(
									TextInput
										.create("value", "Required Gamemode", TextInputStyle.SHORT)
										.setPlaceholder("Options: all, regular, ironman, stranded, ironman_stranded")
										.build()
								)
								.build()
						)
						.queue();
					case "scammer_check" -> {
						event.deferReply(true).complete();
						JsonObject guildSettings = getGuildSettings();
						event
							.getHook()
							.editOriginalEmbeds(
								getSettings()
									.setApplyScammerCheck(guildSettings, !higherDepth(guildSettings, "applyScammerCheck", false))
									.build()
							)
							.queue();
					}
					case "check_api" -> {
						event.deferReply(true).complete();
						JsonObject guildSettings = getGuildSettings();
						event
							.getHook()
							.editOriginalEmbeds(
								getSettings()
									.setApplyCheckApiEnable(guildSettings, !higherDepth(guildSettings, "applyCheckApi", false))
									.build()
							)
							.queue();
					}
				}
			}
			case FETCHUR -> {
				String selectedOption = event.getSelectedOptions().get(0).getValue();
				switch (selectedOption) {
					case "role" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Fetchur Role").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.ROLE
								)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Fetchur Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
				}
			}
			case MAYOR -> {
				String selectedOption = event.getSelectedOptions().get(0).getValue();
				switch (selectedOption) {
					case "role" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Mayor Role").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.ROLE
								)
								.build()
						)
						.setEphemeral(true)
						.queue();
					case "channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Mayor Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
				}
			}
			case JACOB -> {
				String selectedOption = event.getSelectedOptions().get(0).getValue();
				switch (selectedOption) {
					case "enable" -> {
						event.deferReply(true).complete();
						EmbedBuilder eb = getSettings().setJacobEnable(true);
						if (eb.build().getTitle().equals("Settings")) {
							event
								.getHook()
								.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled jacob notifications.").build())
								.queue();
							event.getMessage().editMessageComponents().queue(ignore, ignore);
							return true;
						}
						event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
					}
					case "crops" -> event
						.replyModal(
							Modal
								.create("setup_command_" + selectedOption, "Setup")
								.addActionRow(TextInput.create("value", "Crops", TextInputStyle.SHORT).build())
								.build()
						)
						.queue();
					case "channel" -> event
						.replyEmbeds(defaultEmbed("Setup").setDescription("Jacob Channel").build())
						.setActionRow(
							EntitySelectMenu
								.create(
									"setup_command_" + buttonEvent.getMessageId() + "_" + selectedOption,
									EntitySelectMenu.SelectTarget.CHANNEL
								)
								.setChannelTypes(ChannelType.TEXT)
								.build()
						)
						.setEphemeral(true)
						.queue();
				}
			}
		}
		return false;
	}

	public boolean onEntitySelectInteraction(EntitySelectInteractionEvent event) {
		event.deferReply(true).complete();

		String[] split = event.getComponentId().split("setup_command_")[1].split("_", 2);
		switch (featureType) {
			case VERIFY -> {
				EmbedBuilder eb = null;
				switch (split[1]) {
					case "roles" -> {
						database.setVerifyRolesSettings(event.getGuild().getId(), new JsonArray());
						for (Role verifyRole : event.getMentions().getRoles()) {
							eb = getSettings().addVerifyRole(verifyRole.getId());
							if (!eb.build().getTitle().equals("Settings")) {
								break;
							}
						}
						if (eb.build().getTitle().equals("Settings")) {
							eb.setDescription(
								"Set verified roles: " +
								event.getMentions().getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(" "))
							);
						}
					}
					case "channel" -> eb = getSettings().setVerifyMessageTextChannelId(event.getMentions().getChannels().get(0).getId());
					case "remove_role" -> eb = getSettings().setVerifyRemoveRole(event.getMentions().getRoles().get(0).getId());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
			case GUILD_APPLY -> {
				EmbedBuilder eb = null;
				switch (split[1]) {
					case "channel" -> eb =
						getSettings().setApplyChannel(getGuildSettings(), event.getMentions().getChannels().get(0).getId());
					case "staff_channel" -> eb =
						getSettings().setApplyStaffChannel(getGuildSettings(), event.getMentions().getChannels().get(0).getId());
					case "staff_role" -> eb =
						getSettings().addApplyStaffRole(getGuildSettings(), event.getMentions().getRoles().get(0).getId());
					case "waiting_channel" -> eb =
						getSettings().setApplyWaitingChannel(getGuildSettings(), event.getMentions().getChannels().get(0).getId());
					case "category" -> eb =
						getSettings().setApplyCategory(getGuildSettings(), event.getMentions().getChannels().get(0).getId());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
			case GUILD_ROLE -> {
				EmbedBuilder eb = getSettings().setGuildMemberRole(getGuildSettings(), event.getMentions().getRoles().get(0).getId());
				if (eb.build().getTitle().equals("Settings")) {
					eb = getSettings().setGuildMemberRoleEnable(getGuildSettings(), true);
					if (eb.build().getTitle().equals("Settings")) {
						event
							.getHook()
							.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild member role sync.").build())
							.queue();
						event.getMessage().editMessageComponents().queue(ignore, ignore);
						return true;
					}
				}
				event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
			}
			case MAYOR -> {
				EmbedBuilder eb = null;
				switch (split[1]) {
					case "channel" -> {
						eb = getSettings().setMayorChannel(event.getMentions().getChannels().get(0).getId());
						if (eb.build().getTitle().equals("Settings")) {
							fetchurOrMayorChannelSet = true;
						}
					}
					case "role" -> eb = getSettings().setMayorPing(event.getMentions().getRoles().get(0).getId());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
			case FETCHUR -> {
				EmbedBuilder eb = null;
				switch (split[1]) {
					case "channel" -> {
						eb = getSettings().setFetchurChannel(event.getMentions().getChannels().get(0).getId());
						if (eb.build().getTitle().equals("Settings")) {
							fetchurOrMayorChannelSet = true;
						}
					}
					case "role" -> eb = getSettings().setFetchurPing(event.getMentions().getRoles().get(0).getId());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
			case JACOB -> {
				EmbedBuilder eb = null;
				switch (split[1]) {
					case "channel" -> eb = getSettings().setJacobChannel(event.getMentions().getChannels().get(0).getId());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		}
		return false;
	}

	public boolean onModalInteraction(ModalInteractionEvent event) {
		(featureType == FeatureType.GUILD ? event.deferEdit() : event.deferReply(true)).complete();
		switch (featureType) {
			case VERIFY -> {
				EmbedBuilder eb = null;
				switch (event.getModalId().split("setup_command_")[1]) {
					case "message" -> eb = getSettings().setVerifyMessageText(event.getValues().get(0).getAsString());
					case "nickname" -> eb = getSettings().setVerifyNickname(event.getValues().get(0).getAsString());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
			case GUILD -> {
				String textValue = event.getValues().get(0).getAsString();
				EmbedBuilder eb = getSettings().createNewGuild(textValue);
				if (
					eb.build().getTitle().equals("Settings") ||
					eb.getDescriptionBuilder().toString().equals("An automated guild already exists for this guild")
				) {
					featureType.setGuildName(textValue.toLowerCase().replace(" ", "_"));
					event
						.getHook()
						.editOriginalEmbeds(
							defaultEmbed("Setup").setDescription("Use the buttons below to setup the corresponding features").build()
						)
						.setActionRow(
							StringSelectMenu
								.create("setup_command_" + featureType)
								.addOption("Automated Apply", "guild_apply")
								.addOption("Guild Member Role", "guild_role")
								.addOption("Guild Ranks", "guild_ranks")
								.addOption("Guild Member Counter", "guild_counter")
								.build()
						)
						.queue();
				} else {
					event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again").build()).queue();
				}
			}
			case GUILD_APPLY -> {
				EmbedBuilder eb = null;
				switch (event.getModalId().split("setup_command_")[1]) {
					case "message" -> eb = getSettings().setApplyMessage(getGuildSettings(), event.getValues().get(0).getAsString());
					case "accept_message" -> eb =
						getSettings().setApplyAcceptMessage(getGuildSettings(), event.getValues().get(0).getAsString());
					case "deny_message" -> eb =
						getSettings().setApplyDenyMessage(getGuildSettings(), event.getValues().get(0).getAsString());
					case "waitlist_message" -> eb =
						getSettings().setApplyWaitlistMessage(getGuildSettings(), event.getValues().get(0).getAsString());
					case "requirements" -> {
						String[] reqs = event.getValues().get(0).getAsString().split(",");
						if (reqs.length == 0 || reqs.length > 3) {
							eb = defaultEmbed("You must add at least one requirement and at most three requirements.");
						} else {
							database.setApplyReqs(event.getGuild().getId(), featureType.guildName, new JsonArray());
							for (String req : reqs) {
								eb = getSettings().addApplyRequirement(getGuildSettings(), req);
								if (!eb.build().getTitle().equals("Settings")) {
									break;
								}
							}
						}
					}
					case "gamemode" -> eb = getSettings().setApplyGamemode(getGuildSettings(), event.getValues().get(0).getAsString());
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
			case GUILD_RANKS -> {
				String[] guildRanks = event.getValues().get(0).getAsString().split(",");
				EmbedBuilder eb = null;
				if (guildRanks.length == 0) {
					eb = invalidEmbed("You must specify at least one rank");
				} else {
					JsonObject obj = database.getGuildSettings(event.getGuild().getId(), featureType.guildName).getAsJsonObject();
					obj.add("guildRanks", new JsonArray());
					database.setGuildSettings(event.getGuild().getId(), obj);

					for (String guildRank : guildRanks) {
						String[] guildRanksSplit = guildRank.split("-");
						eb =
							getSettings()
								.addGuildRank(
									getGuildSettings(),
									guildRanksSplit.length >= 1 ? guildRanksSplit[0].trim() : null,
									guildRanksSplit.length >= 2 ? guildRanksSplit[1].trim() : ""
								);
						if (!eb.build().getTitle().equals("Settings")) {
							break;
						}
					}
				}

				if (eb.build().getTitle().equals("Settings")) {
					eb = getSettings().setGuildRanksEnable(getGuildSettings(), true);
					if (eb.build().getTitle().equals("Settings")) {
						event
							.getHook()
							.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild ranks sync.").build())
							.queue();
						event.getMessage().editMessageComponents().queue(ignore, ignore);
						return true;
					}
				}
				event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
			}
			case JACOB -> {
				EmbedBuilder eb = null;
				switch (event.getModalId().split("setup_command_")[1]) {
					case "crops" -> {
						String in = event.getValues().get(0).getAsString();
						List<String> crops = new ArrayList<>();

						if (in.equalsIgnoreCase("all")) {
							crops.addAll(cropNameToEmoji.keySet());
						} else {
							for (String crop : in.split(",")) {
								crops.add(capitalizeString(crop.trim()));
							}
						}

						for (String crop : crops) {
							eb = getSettings().addJacobCrop(crop, null);
							if (!eb.build().getTitle().equals("Settings")) {
								break;
							}
						}
					}
				}
				if (!eb.build().getTitle().equals("Settings")) {
					eb.appendDescription("\n\nPlease try again.");
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		}
		return false;
	}

	private SettingsExecute getSettings() {
		return settings != null
			? settings
			: (this.settings = new SettingsExecute(buttonEvent.getGuild(), buttonEvent.getChannel(), buttonEvent.getUser()));
	}

	private JsonObject getGuildSettings() {
		return database.getGuildSettings(buttonEvent.getGuild().getId(), featureType.guildName).getAsJsonObject();
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			GenericInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> {
				try {
					if ((featureType == FeatureType.FETCHUR || featureType == FeatureType.MAYOR) && fetchurOrMayorChannelSet) {
						buttonEvent.getMessage().editMessageComponents().queue(ignore, ignore);
					} else {
						buttonEvent
							.getMessage()
							.editMessageEmbeds(defaultEmbed("Setup").setDescription("Timeout").build())
							.setComponents()
							.queue(ignore, ignore);
					}
				} catch (Exception ignored) {}
			}
		);
	}

	private enum FeatureType {
		VERIFY,
		GUILD,
		ROLES,
		FETCHUR,
		MAYOR,
		JACOB,
		GUILD_ROLE,
		GUILD_RANKS,
		GUILD_COUNTER,
		GUILD_APPLY;

		String guildName;

		FeatureType() {
			guildName = null;
		}

		FeatureType setGuildName(String guildName) {
			this.guildName = guildName;
			return this;
		}
	}
}
