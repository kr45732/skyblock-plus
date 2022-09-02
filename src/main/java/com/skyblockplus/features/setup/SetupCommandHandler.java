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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class SetupCommandHandler {

	private final ButtonInteractionEvent buttonEvent;
	private final SettingsExecute settings;
	private String featureType = null;
	private String name;
	private MessageEditData originalMb;
	private Instant lastAction = Instant.now();

	public SetupCommandHandler(ButtonInteractionEvent buttonEvent, String featureType) {
		this.buttonEvent = buttonEvent;
		this.buttonEvent.deferEdit().queue();
		this.settings = new SettingsExecute(buttonEvent.getGuild(), buttonEvent.getChannel(), buttonEvent.getUser());

		switch (featureType) {
			case "verify":
				buttonEvent
					.getHook()
					.editOriginal(
						originalMb =
							new MessageEditBuilder()
								.setEmbeds(
									defaultEmbed("Setup")
										.setDescription("Use the menu below to configure the verification settings")
										.build()
								)
								.setActionRow(
									SelectMenu
										.create("setup_command_" + featureType)
										.addOption("Enable", "enable")
										.addOption("Verification Message", "message")
										.addOption("Verified Roles", "roles")
										.addOption("Verification Channel", "roles")
										.addOption("Nickname Template", "nickname")
										.build()
								)
								.build()
					)
					.queue();
				break;
			case "guild":
				buttonEvent
					.replyEmbeds(
						defaultEmbed("Setup")
							.setDescription("Reply with the name of the Hypixel guild you are setting this up for.")
							.setFooter("Reply with 'cancel' to stop the process • dsc.gg/sb+")
							.build()
					)
					.queue();
				break;
			case "roles":
				buttonEvent
					.getHook()
					.editOriginalEmbeds(
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
					.queue();
				this.featureType = featureType;
				return;
			case "fetchur":
				buttonEvent
					.getHook()
					.editOriginal(
						originalMb =
							new MessageEditBuilder()
								.setEmbeds(
									defaultEmbed("Setup").setDescription("Use the menu below to configure the fetchur settings").build()
								)
								.setActionRow(
									SelectMenu
										.create("setup_command_" + featureType)
										.addOption("Enable", "enable")
										.addOption("Ping Role", "roles")
										.addOption("Fetchur Channel", "roles")
										.build()
								)
								.build()
					)
					.queue();
				break;
			case "mayor":
				buttonEvent
					.getHook()
					.editOriginal(
						originalMb =
							new MessageEditBuilder()
								.setEmbeds(
									defaultEmbed("Setup").setDescription("Use the menu below to configure the mayor settings").build()
								)
								.setActionRow(
									SelectMenu
										.create("setup_command_" + featureType)
										.addOption("Enable", "enable")
										.addOption("Ping Role", "roles")
										.addOption("Mayor Channel", "roles")
										.build()
								)
								.build()
					)
					.queue();
				break;
			case "jacob":
				buttonEvent
					.getHook()
					.editOriginal(
						originalMb =
							new MessageEditBuilder()
								.setEmbeds(
									defaultEmbed("Setup").setDescription("Use the menu below to configure the jacob settings").build()
								)
								.setActionRow(
									SelectMenu
										.create("setup_command_" + featureType)
										.addOption("Enable", "enable")
										.addOption("Crops", "crops")
										.addOption("Mayor Channel", "roles")
										.build()
								)
								.build()
					)
					.queue();
				break;
			default:
				if (featureType.startsWith("guild_apply")) {
					this.name = featureType.split("guild_apply_")[1];
					this.featureType = "guild_apply";
					buttonEvent
						.getHook()
						.editOriginalEmbeds(
							defaultEmbed("Setup")
								.setDescription("Reply with the message that users will see and click to in order to apply.")
								.setFooter("Reply with 'cancel' to stop the process • dsc.gg/sb+")
								.build()
						)
						.queue();
				} else if (featureType.startsWith("guild_role_")) {
					this.name = featureType.split("guild_role_")[1];
					this.featureType = "guild_role";
					buttonEvent
						.getHook()
						.editOriginalEmbeds(
							defaultEmbed("Setup")
								.setDescription("Reply with the guild member role.")
								.setFooter("Reply with 'cancel' to stop the process • dsc.gg/sb+")
								.build()
						)
						.queue();
				} else if (featureType.startsWith("guild_ranks_")) {
					this.name = featureType.split("guild_ranks_")[1];
					this.featureType = "guild_ranks";
					buttonEvent
						.getHook()
						.editOriginalEmbeds(
							defaultEmbed("Setup")
								.setDescription(
									"Reply with the guild rank(s) and the role(s). (Example: `god @role1, moderator @role2, @officer @role3`)."
								)
								.setFooter("Reply with 'cancel' to stop the process • dsc.gg/sb+")
								.build()
						)
						.queue();
				} else if (featureType.startsWith("guild_counter_")) {
					this.name = featureType.split("guild_counter_")[1];
					this.featureType = "guild_counter";
					EmbedBuilder eb = settings.setGuildCounterEnable(getSettings(), true);
					if (eb.build().getTitle().equals("Settings")) {
						buttonEvent
							.getHook()
							.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild member counter").build())
							.queue();
					} else {
						buttonEvent.getHook().editOriginalEmbeds(eb.build()).queue();
					}
					return;
				} else {
					return;
				}
				break;
		}

		this.featureType = this.featureType != null ? this.featureType : featureType;
	}

	public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
		if (
			event.isFromGuild() &&
			event.getMessageId().equals(buttonEvent.getMessageId()) &&
			event.getUser().getId().equals(buttonEvent.getUser().getId())
		) {
			switch (featureType) {
				case "verify" -> {
					String selectedOption = event.getSelectedOptions().get(0).getValue();
					Modal.Builder modalBuilder = Modal.create("setup_command_" + selectedOption, "Setup");
					switch (selectedOption) {
						case "enable" -> {
							EmbedBuilder eb = settings.setVerifyEnable(true);
							if (!eb.build().getTitle().equals("Settings")) {
								event.editMessageEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
							} else {
								String msg = onVerifyReload(event.getGuild().getId());
								if (msg.equals("Reloaded")) {
									event
										.editMessageEmbeds(
											defaultEmbed("Success").setDescription("Successfully enabled the verification feature").build()
										)
										.setComponents()
										.queue();
								} else {
									event.editMessageEmbeds(defaultEmbed("Error").setDescription(msg).build()).setComponents().queue();
								}
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
											.setPlaceholder("Template to nick after verifying ([PREFIX] [IGN] [POSTFIX])")
											.build()
									)
									.build()
							)
							.queue();
					}
				}
			}
		}
	}

	public void onModalInteraction(ModalInteractionEvent event) {
		if (
			event.isFromGuild() &&
			event.getMessage() != null &&
			event.getMessage().getId().equals(buttonEvent.getMessageId()) &&
			event.getUser().getId().equals(buttonEvent.getUser().getId())
		) {
			switch (featureType) {
				case "verify" -> {
					switch (event.getModalId().split("setup_command_")[1]) {
						case "message" -> {
							EmbedBuilder eb = settings.setVerifyMessageText(event.getValues().get(0).getAsString());
							if (!eb.build().getTitle().equals("Settings")) {
								event.editMessageEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
							} else {
								event.editMessage(originalMb).queue();
							}
						}
						case "roles" -> {
							EmbedBuilder eb = null;
							String[] verifyRoles = event.getValues().get(0).getAsString().split(",");
							if (verifyRoles.length == 0 || verifyRoles.length > 3) {
								eb =
									defaultEmbed(
										"You must add at least one verification role and at most three verification roles. (Example: `@role1, @role2`)"
									);
							} else {
								database.setVerifyRolesSettings(event.getGuild().getId(), new JsonArray());
								for (String verifyRole : verifyRoles) {
									eb = settings.addVerifyRole(verifyRole.trim());
									if (!eb.build().getTitle().equals("Settings")) {
										break;
									}
								}
							}
							if (!eb.build().getTitle().equals("Settings")) {
								event.editMessageEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
							} else {
								event.editMessage(originalMb).queue();
							}
						}
						case "channel" -> {
							EmbedBuilder eb = settings.setVerifyMessageTextChannelId(event.getValues().get(0).getAsString());
							if (!eb.build().getTitle().equals("Settings")) {
								event.editMessageEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
							} else {
								event.editMessage(originalMb).queue();
							}
						}
						case "nickname" -> {
							EmbedBuilder eb = settings.setVerifyNickname(event.getValues().get(0).getAsString());
							if (!eb.build().getTitle().equals("Settings")) {
								event.editMessageEmbeds(eb.appendDescription("\n\nPlease try again.").build()).queue();
							} else {
								event.editMessage(originalMb).queue();
							}
						}
					}
				}
			}
		}
	}

	private void handleReply(MessageReceivedEvent event) {
		EmbedBuilder eb = null;
		EmbedBuilder eb2 = defaultEmbed("Setup").setFooter("Reply with 'cancel' to stop the process • dsc.gg/sb+");
		int state = 0;
		switch (featureType) {
			case "guild":
				if (state == 0) {
					eb = settings.createNewGuild(event.getMessage().getContentRaw());
					if (eb.build().getTitle().equals("Settings")) {
						this.name = eb.build().getDescription().split("`")[1].split("`")[0].toLowerCase().replace(" ", "_");
						eb =
							defaultEmbed("Setup")
								.setDescription("Choose one of the buttons below to setup the corresponding automatic guild feature");
						buttonEvent
							.getChannel()
							.sendMessageEmbeds(eb.build())
							.setActionRow(
								Button.primary("setup_command_guild_apply_" + name, "Automated Apply"),
								Button.primary("setup_command_guild_role_" + name, "Guild Member Role"),
								Button.primary("setup_command_guild_ranks_" + name, "Guild Ranks"),
								Button.primary("setup_command_guild_counter_" + name, "Guild Member Counter")
							)
							.queue();
						return;
					}
				}
				break;
			case "guild_apply":
				switch (state) {
					case 0 -> {
						eb = settings.setApplyMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the channel where the message will be sent.");
					}
					case 1 -> {
						eb = settings.setApplyChannel(getSettings(), event.getMessage().getContentRaw());
						StringBuilder categoriesStr = new StringBuilder();
						for (Category category : event.getGuild().getCategories()) {
							categoriesStr.append("\n").append(category.getName()).append(" - ").append(category.getId());
						}
						eb2.setDescription(
							"Reply with the category where new applications should be made.\n\nList of all categories:" + categoriesStr
						);
					}
					case 2 -> {
						eb = settings.setApplyCategory(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the staff channel where applications will be sent.");
					}
					case 3 -> {
						eb = settings.setApplyStaffChannel(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with a staff role that should be pinged when an application is received. Reply with 'none' for no ping."
						);
					}
					case 4 -> {
						eb = settings.addApplyStaffRole(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the message that should be sent if an application is accepted.");
					}
					case 5 -> {
						eb = settings.setApplyAcceptMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the message that should be sent if an application is denied.");
					}
					case 6 -> {
						eb = settings.setApplyDenyMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with the message that should be sent if an application is waitlisted. Reply with 'none' if you do not want this."
						);
					}
					case 7 -> {
						eb = settings.setApplyWaitlistMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with the channel where the players who were accepted or waitlisted will be sent. Reply with 'none' if you do not want this."
						);
					}
					case 8 -> {
						eb = settings.setApplyWaitingChannel(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with the requirements that an applicant must meet. Separate multiple requirements with a comma and a space. (Example: `weight:4000 skills:40, slayer:1500000 catacombs:30, weight:5000`). Reply with 'none' if you do not want this."
						);
					}
					case 9 -> {
						if (!event.getMessage().getContentRaw().equalsIgnoreCase("none")) {
							String[] reqs = event.getMessage().getContentRaw().split(", ");
							if (reqs.length == 0 || reqs.length > 3) {
								eb = defaultEmbed("You must add at least one requirement and at most three requirements");
							} else {
								database.setApplyReqs(event.getGuild().getId(), name, new JsonArray());
								for (String req : reqs) {
									eb = settings.addApplyRequirement(getSettings(), req);
									if (!eb.build().getTitle().equals("Settings")) {
										break;
									}
								}
							}
						} else {
							eb = defaultEmbed("Settings");
						}
						eb2.setDescription("Reply with 'enable' to enable this automatic application system.");
					}
					case 10 -> {
						if (event.getMessage().getContentRaw().equalsIgnoreCase("enable")) {
							eb = settings.setApplyEnable(getSettings(), true);
							if (eb.build().getTitle().equals("Settings")) {
								String msg = onApplyReload(event.getGuild().getId());
								if (msg.contains("• Reloaded `" + name + "`")) {
									sendEmbed(defaultEmbed("Success").setDescription("Enabled this automatic application systen."));
								} else {
									if (!msg.contains("• `" + name + "` is disabled")) {
										msg = "`" + name + "` is disabled";
									} else {
										msg =
											"Error Reloading for `" + name + msg.split("• Error Reloading for `" + name)[1].split("\n")[0];
									}
									sendEmbed(defaultEmbed("Error").setDescription(msg));
								}
							} else {
								sendEmbed(eb);
							}
						} else {
							sendEmbed(defaultEmbed("Canceled the process"));
						}
						return;
					}
				}
				break;
			case "guild_role":
				eb = settings.setGuildMemberRole(getSettings(), event.getMessage().getContentRaw());
				if (eb.build().getTitle().equals("Settings")) {
					eb = settings.setGuildMemberRoleEnable(getSettings(), true);
					if (eb.build().getTitle().equals("Settings")) {
						sendEmbed(defaultEmbed("Success").setDescription("Enabled guild member role sync."));
					} else {
						sendEmbed(eb);
					}
					return;
				}
				break;
			case "guild_ranks":
				String[] guildRanks = event.getMessage().getContentRaw().split(", ");
				if (guildRanks.length == 0) {
					eb = defaultEmbed("You must specify at least one rank");
				} else {
					JsonObject obj = database.getGuildSettings(event.getGuild().getId(), name).getAsJsonObject();
					obj.add("guildRanks", new JsonArray());
					database.setGuildSettings(event.getGuild().getId(), obj);

					for (String guildRank : guildRanks) {
						String[] guildRanksSplit = guildRank.split("\\s+");
						eb =
							settings.addGuildRank(
								getSettings(),
								guildRanksSplit.length >= 1 ? guildRanksSplit[0] : null,
								guildRanksSplit.length >= 2 ? guildRanksSplit[1] : ""
							);
						if (!eb.build().getTitle().equals("Settings")) {
							break;
						}
					}
				}

				if (eb.build().getTitle().equals("Settings")) {
					eb = settings.setGuildRanksEnable(getSettings(), true);
					if (eb.build().getTitle().equals("Settings")) {
						sendEmbed(defaultEmbed("Success").setDescription("Enabled guild ranks sync."));
					} else {
						sendEmbed(eb);
					}
					return;
				}
				break;
			case "fetchur":
				switch (state) {
					case 0 -> {
						eb = settings.setFetchurChannel(event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the role that should be pinged when the notifications are sent or 'none'.");
					}
					case 1 -> {
						eb = settings.setFetchurPing(event.getMessage().getContentRaw());
						if (eb.build().getTitle().equals("Settings")) {
							sendEmbed(eb);
							return;
						}
					}
				}
				break;
			case "mayor":
				switch (state) {
					case 0 -> {
						eb = settings.setMayorChannel(event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the role that should be pinged when the notifications are sent or 'none'.");
					}
					case 1 -> {
						eb = settings.setMayorPing(event.getMessage().getContentRaw());
						if (eb.build().getTitle().equals("Settings")) {
							sendEmbed(eb);
							return;
						}
					}
				}
				break;
			case "jacob":
				switch (state) {
					case 0 -> {
						eb = settings.setJacobChannel(event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the crops (separated by a comma) that should be pinged or 'all' to add all crops");
					}
					case 1 -> {
						List<String> crops = new ArrayList<>();

						if (event.getMessage().getContentRaw().equalsIgnoreCase("all")) {
							crops.addAll(CROP_NAME_TO_EMOJI.keySet());
						} else {
							for (String crop : event.getMessage().getContentRaw().split(",")) {
								crops.add(capitalizeString(crop.trim()));
							}
						}

						for (String crop : crops) {
							eb = settings.addJacobCrop(crop, null);
							if (!eb.build().getTitle().equals("Settings")) {
								break;
							}
						}
						eb2.setDescription("Reply with 'enable' to enable farming event notifications or anything else to cancel.");
					}
					case 2 -> {
						if (event.getMessage().getContentRaw().equalsIgnoreCase("enable")) {
							sendEmbed(settings.setJacobEnable(true));
						} else {
							sendEmbed(defaultEmbed("Canceled the process"));
						}
						return;
					}
				}
				break;
		}
	}

	public boolean isValid() {
		return featureType != null;
	}

	private void sendEmbed(EmbedBuilder eb) {
		buttonEvent.getChannel().sendMessageEmbeds(eb.build()).queue();
	}

	private JsonObject getSettings() {
		return database.getGuildSettings(buttonEvent.getGuild().getId(), name).getAsJsonObject();
	}

	public boolean hasTimedOut() {
		if (Duration.between(lastAction, Instant.now()).abs().toMinutes() > 2) {
			try {
				buttonEvent
					.getMessage()
					.editMessageEmbeds(defaultEmbed("Setup").setDescription("Timeout").build())
					.setComponents()
					.queue(ignore, ignore);
			} catch (Exception ignored) {}
			return true;
		}
		return false;
	}
}
