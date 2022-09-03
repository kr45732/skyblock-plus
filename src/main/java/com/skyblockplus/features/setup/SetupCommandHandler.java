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

package com.skyblockplus.features.setup;

import static com.skyblockplus.features.jacob.JacobContest.CROP_NAME_TO_EMOJI;
import static com.skyblockplus.features.listeners.MainListener.onApplyReload;
import static com.skyblockplus.features.listeners.MainListener.onVerifyReload;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skyblockplus.settings.SettingsExecute;
import com.skyblockplus.utils.AbstractEventListener;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

public class SetupCommandHandler extends AbstractEventListener {

	private final ButtonInteractionEvent buttonEvent;
	private SettingsExecute settings;
	private FeatureType featureType;
	private Instant lastAction = Instant.now();
	private boolean fetchurOrMayorChannelSet = false;

	public SetupCommandHandler(ButtonInteractionEvent buttonEvent, String feature) {
		this.buttonEvent = buttonEvent;
		this.featureType = FeatureType.valueOf(feature.toUpperCase());

		// TODO: add buttons to go back
		switch (this.featureType) {
			case VERIFY -> buttonEvent
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(
							defaultEmbed("Setup").setDescription("Use the menu below to configure the verification settings").build()
						)
						.setActionRow(
							SelectMenu
								.create("setup_command_" + featureType)
								.addOption("Enable", "enable")
								.addOption("Verification Message", "message")
								.addOption("Verified Roles", "roles")
								.addOption("Verification Channel", "channel")
								.addOption("Nickname Template", "nickname")
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
				.queue();
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
				completed();
			}
			case FETCHUR -> buttonEvent
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(defaultEmbed("Setup").setDescription("Use the menu below to configure the fetchur settings").build())
						.setActionRow(
							SelectMenu
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
							SelectMenu
								.create("setup_command_" + featureType)
								.addOption("Mayor Channel", "channel")
								.addOption("Mayor Role", "role")
								.build()
						)
						.build()
				)
				.queue();
			case JACOB -> // TODO: select menu with each crop and then press on crop to set role
			buttonEvent
				.editMessage(
					new MessageEditBuilder()
						.setEmbeds(defaultEmbed("Setup").setDescription("Use the menu below to configure the jacob settings").build())
						.setActionRow(
							SelectMenu
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
	}

	@Override
	public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
		if (
			event.isFromGuild() &&
			event.getMessageId().equals(buttonEvent.getMessageId()) &&
			event.getUser().getId().equals(buttonEvent.getUser().getId())
		) {
			lastAction = Instant.now();
			switch (featureType) {
				case VERIFY -> {
					String selectedOption = event.getSelectedOptions().get(0).getValue();
					Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
					switch (selectedOption) {
						case "enable" -> {
							event.deferReply(true).queue();
							EmbedBuilder eb = getSettings().setVerifyEnable(true);
							if (!eb.build().getTitle().equals("Settings")) {
								event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
							} else {
								String msg = onVerifyReload(event.getGuild().getId());
								if (msg.equals("Reloaded")) {
									event
										.getHook()
										.editOriginalEmbeds(
											defaultEmbed("Success").setDescription("Enabled automatic verification").build()
										)
										.queue();
								} else {
									event.getHook().editOriginalEmbeds(defaultEmbed("Error").setDescription(msg).build()).queue();
								}
								event.getMessage().editMessageComponents().queue();
								completed();
							}
						}
						case "message" -> event
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "Verification Message", TextInputStyle.PARAGRAPH).build())
									.build()
							)
							.queue();
						case "roles" -> event
							.replyModal(
								modalBuilder
									.addActionRow(
										TextInput
											.create("value", "Verified Roles", TextInputStyle.SHORT)
											.setPlaceholder("Roles given when verifying seperated by commas")
											.build()
									)
									.build()
							)
							.queue();
						case "channel" -> event
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "Verification Channel", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
						case "nickname" -> event
							.replyModal(
								modalBuilder
									.addActionRow(
										TextInput
											.create("value", "Verified Nickname", TextInputStyle.SHORT)
											.setPlaceholder("Template to nick when verified ([PREFIX] [IGN] [POSTFIX])")
											.build()
									)
									.build()
							)
							.queue();
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
										SelectMenu
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
											.build()
									)
									.build()
							)
							.queue();
						case GUILD_COUNTER -> {
							event.deferReply(true).queue();
							EmbedBuilder eb = getSettings().setGuildCounterEnable(getGuildSettings(), true);
							if (eb.build().getTitle().equals("Settings")) {
								event
									.getHook()
									.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild member counter").build())
									.queue();
								completed();
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
							.replyModal(
								Modal
									.create("setup_command_" + featureType, "Setup")
									.addActionRow(TextInput.create("value", "Guild Member Role", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
					}
				}
				case GUILD_APPLY -> {
					String selectedOption = event.getSelectedOptions().get(0).getValue();
					Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
					switch (selectedOption) {
						case "enable" -> {
							event.deferReply(true).queue();
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
								event.getMessage().editMessageComponents().queue();
								completed();
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
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "Application Channel", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
						case "category" -> event
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "New Application Category", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
						case "staff_channel" -> event
							.replyModal(
								modalBuilder.addActionRow(TextInput.create("value", "Staff Channel", TextInputStyle.SHORT).build()).build()
							)
							.queue();
						case "staff_role" -> event
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "Staff Ping Role", TextInputStyle.SHORT).build())
									.build()
							)
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
								modalBuilder
									.addActionRow(TextInput.create("value", "Denied Message", TextInputStyle.PARAGRAPH).build())
									.build()
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
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "Waiting Channel", TextInputStyle.PARAGRAPH).build())
									.build()
							)
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
					}
				}
				case FETCHUR -> {
					String selectedOption = event.getSelectedOptions().get(0).getValue();
					Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
					switch (selectedOption) {
						case "role" -> event
							.replyModal(
								modalBuilder.addActionRow(TextInput.create("value", "Fetchur Role", TextInputStyle.SHORT).build()).build()
							)
							.queue();
						case "channel" -> event
							.replyModal(
								modalBuilder
									.addActionRow(TextInput.create("value", "Fetchur Channel", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
					}
				}
				case MAYOR -> {
					String selectedOption = event.getSelectedOptions().get(0).getValue();
					Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
					switch (selectedOption) {
						case "role" -> event
							.replyModal(
								modalBuilder.addActionRow(TextInput.create("value", "Mayor Role", TextInputStyle.SHORT).build()).build()
							)
							.queue();
						case "channel" -> event
							.replyModal(
								modalBuilder.addActionRow(TextInput.create("value", "Mayor Channel", TextInputStyle.SHORT).build()).build()
							)
							.queue();
					}
				}
				case JACOB -> {
					String selectedOption = event.getSelectedOptions().get(0).getValue();
					Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
					switch (selectedOption) {
						case "enable" -> {
							event.deferReply(true).queue();
							EmbedBuilder eb = getSettings().setJacobEnable(true);
							if (eb.build().getTitle().equals("Settings")) {
								event
									.getHook()
									.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled jacob notifications.").build())
									.queue();
								event.getMessage().editMessageComponents().queue();
								completed();
								return;
							}
							event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
						}
						case "crops" -> event
							.replyModal(modalBuilder.addActionRow(TextInput.create("value", "Crops", TextInputStyle.SHORT).build()).build())
							.queue();
						case "channel" -> event
							.replyModal(
								modalBuilder.addActionRow(TextInput.create("value", "Mayor Channel", TextInputStyle.SHORT).build()).build()
							)
							.queue();
					}
				}
			}
		}
	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if (
			event.isFromGuild() &&
			event.getMessage() != null &&
			event.getMessage().getId().equals(buttonEvent.getMessageId()) &&
			event.getUser().getId().equals(buttonEvent.getUser().getId())
		) {
			(featureType == FeatureType.GUILD ? event.deferEdit() : event.deferReply(true)).queue();

			lastAction = Instant.now();
			switch (featureType) {
				case VERIFY -> {
					EmbedBuilder eb = null;
					switch (event.getModalId().split("setup_command_")[1]) {
						case "message" -> eb = getSettings().setVerifyMessageText(event.getValues().get(0).getAsString());
						case "roles" -> {
							String[] verifyRoles = event.getValues().get(0).getAsString().split(",");
							if (verifyRoles.length == 0 || verifyRoles.length > 3) {
								eb =
									invalidEmbed(
										"You must add at least one verification role and at most three verification roles. (Example: `@role1, @role2`)"
									);
							} else {
								database.setVerifyRolesSettings(event.getGuild().getId(), new JsonArray());
								for (String verifyRole : verifyRoles) {
									eb = getSettings().addVerifyRole(verifyRole.trim());
									if (!eb.build().getTitle().equals("Settings")) {
										break;
									}
								}
							}
						}
						case "channel" -> eb = getSettings().setVerifyMessageTextChannelId(event.getValues().get(0).getAsString());
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
								SelectMenu
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
						case "channel" -> eb = getSettings().setApplyChannel(getGuildSettings(), event.getValues().get(0).getAsString());
						case "category" -> eb = getSettings().setApplyCategory(getGuildSettings(), event.getValues().get(0).getAsString());
						case "staff_channel" -> eb =
							getSettings().setApplyStaffChannel(getGuildSettings(), event.getValues().get(0).getAsString());
						case "accept_message" -> eb =
							getSettings().setApplyAcceptMessage(getGuildSettings(), event.getValues().get(0).getAsString());
						case "deny_message" -> eb =
							getSettings().setApplyDenyMessage(getGuildSettings(), event.getValues().get(0).getAsString());
						case "staff_role" -> eb =
							getSettings().addApplyStaffRole(getGuildSettings(), event.getValues().get(0).getAsString());
						case "waiting_channel" -> eb =
							getSettings().setApplyWaitingChannel(getGuildSettings(), event.getValues().get(0).getAsString());
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
					}
					if (!eb.build().getTitle().equals("Settings")) {
						eb.appendDescription("\n\nPlease try again.");
					}
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
				case GUILD_RANKS -> {
					String[] guildRanks = event.getMessage().getContentRaw().split(",");
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
							event.getMessage().editMessageComponents().queue();
							completed();
							return;
						}
					}
					event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
				}
				case GUILD_ROLE -> {
					EmbedBuilder eb = getSettings().setGuildMemberRole(getGuildSettings(), event.getValues().get(0).getAsString());
					if (eb.build().getTitle().equals("Settings")) {
						eb = getSettings().setGuildMemberRoleEnable(getGuildSettings(), true);
						if (eb.build().getTitle().equals("Settings")) {
							event
								.getHook()
								.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild member role sync.").build())
								.queue();
							event.getMessage().editMessageComponents().queue();
							completed();
							return;
						}
					}
					event.getHook().editOriginalEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
				}
				case MAYOR -> {
					EmbedBuilder eb = null;
					switch (event.getModalId().split("setup_command_")[1]) {
						case "channel" -> {
							eb = getSettings().setMayorChannel(event.getValues().get(0).getAsString());
							if (eb.build().getTitle().equals("Settings")) {
								fetchurOrMayorChannelSet = true;
							}
						}
						case "role" -> eb = getSettings().setMayorPing(event.getValues().get(0).getAsString());
					}
					if (!eb.build().getTitle().equals("Settings")) {
						eb.appendDescription("\n\nPlease try again.");
					}
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
				case FETCHUR -> {
					EmbedBuilder eb = null;
					switch (event.getModalId().split("setup_command_")[1]) {
						case "channel" -> {
							eb = getSettings().setFetchurChannel(event.getValues().get(0).getAsString());
							if (eb.build().getTitle().equals("Settings")) {
								fetchurOrMayorChannelSet = true;
							}
						}
						case "role" -> eb = getSettings().setFetchurPing(event.getValues().get(0).getAsString());
					}
					if (!eb.build().getTitle().equals("Settings")) {
						eb.appendDescription("\n\nPlease try again.");
					}
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
				case JACOB -> {
					EmbedBuilder eb = null;
					switch (event.getModalId().split("setup_command_")[1]) {
						case "channel" -> eb = getSettings().setJacobChannel(event.getValues().get(0).getAsString());
						case "crops" -> {
							List<String> crops = new ArrayList<>();

							if (event.getMessage().getContentRaw().equalsIgnoreCase("all")) {
								crops.addAll(CROP_NAME_TO_EMOJI.keySet());
							} else {
								for (String crop : event.getMessage().getContentRaw().split(",")) {
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
		}
	}

	private SettingsExecute getSettings() {
		return settings != null
			? settings
			: (this.settings = new SettingsExecute(buttonEvent.getGuild(), buttonEvent.getChannel(), buttonEvent.getUser()));
	}

	private JsonObject getGuildSettings() {
		return database.getGuildSettings(buttonEvent.getGuild().getId(), featureType.guildName).getAsJsonObject();
	}

	@Override
	public boolean hasTimedOut() {
		if (Duration.between(lastAction, Instant.now()).abs().toMinutes() > 2) {
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
			return true;
		}
		return false;
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
