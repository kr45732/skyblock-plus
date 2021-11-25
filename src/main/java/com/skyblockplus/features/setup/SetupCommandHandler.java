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

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.features.listeners.MainListener.*;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.settings.SettingsExecute;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;

public class SetupCommandHandler {

	private final ButtonClickEvent buttonEvent;
	private final SettingsExecute settings;
	private int state = 0;
	private String featureType = null;
	private int attemptsLeft = 3;
	private String name;

	public SetupCommandHandler(ButtonClickEvent buttonEvent, String featureType) {
		settings = new SettingsExecute(buttonEvent.getGuild(), buttonEvent.getMessage(), buttonEvent.getChannel(), buttonEvent.getUser());
		this.buttonEvent = buttonEvent;

		switch (featureType) {
			case "verify":
				buttonEvent
					.getHook()
					.editOriginalEmbeds(
						defaultEmbed("Setup")
							.setDescription("Reply with the message that users will see when verifying.")
							.setFooter("Reply with 'cancel' to stop the process")
							.build()
					)
					.queue();
				break;
			case "guild":
				buttonEvent
					.getHook()
					.editOriginalEmbeds(
						defaultEmbed("Setup")
							.setDescription("Reply with the name of the Hypixel guild you are setting this up for.")
							.setFooter("Reply with 'cancel' to stop the process")
							.setFooter("Reply with 'cancel' to stop the process")
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
								"**__Overview__**\n" +
								"1) When a user runs `roles claim [ign]` their stats are fetched\n" +
								"2) Depending on the roles setup for this server and the users stats, the corresponding roles will be given\n\n" +
								"**__Setup__**\n" +
								"- In order to enable automatic roles, there must be at least one role setting enabled:\n" +
								"- `settings roles add [roleName] [value] [@role]` - add a level to a role. Maximum for all roles __combined__ is 120.\n" +
								"- `settings roles remove [roleName] [value]` - remove a level from a role.\n" +
								"- `settings roles set [roleName] [@role]` - set a one level role's role\n" +
								"- `settings roles enable [roleName]` - enable a role.\n" +
								"• Tutorial video linked [__here__](https://streamable.com/wninsw)\n\n" +
								"**__Enable__**\n" +
								"- Once all these settings are set run `settings roles enable` to enable roles.\n" +
								"- To view all the roles, their descriptions, and examples, type `settings roles`\n" +
								"- For more help type `help settings roles` or watch the video linked above\n"
							)
							.build()
					)
					.queue();
				this.featureType = featureType;
				return;
			case "prefix":
				buttonEvent
					.getHook()
					.editOriginalEmbeds(
						defaultEmbed("Setup")
							.setDescription(
								"Reply with the prefix you want to set. The prefix must be a least one character and no more than five."
							)
							.setFooter("Reply with 'cancel' to stop the process")
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
								.setFooter("Reply with 'cancel' to stop the process")
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
								.setFooter("Reply with 'cancel' to stop the process")
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
								.setFooter("Reply with 'cancel' to stop the process")
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
							.editOriginalEmbeds(defaultEmbed("Success").setDescription("Enabled guild member counter.").build())
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
		waitForReply();
	}

	private boolean checkReply(GuildMessageReceivedEvent event) {
		return (
			event.getChannel().getId().equals(buttonEvent.getChannel().getId()) &&
			event.getAuthor().getId().equals(buttonEvent.getUser().getId())
		);
	}

	private void cancel() {
		switch (featureType) {
			case "verify":
				database.setVerifySettings(buttonEvent.getGuild().getId(), gson.toJsonTree(new AutomatedVerify()));
				break;
			case "apply":
			case "guild":
				if (name != null) {
					database.removeGuildSettings(buttonEvent.getGuild().getId(), name);
				}
				break;
		}
	}

	private void handleReply(GuildMessageReceivedEvent event) {
		if (event.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
			sendEmbed(defaultEmbed("Canceled the process"));
			cancel();
			return;
		}

		EmbedBuilder eb = null;
		EmbedBuilder eb2 = defaultEmbed("Setup").setFooter("Reply with 'cancel' to stop the process");
		switch (featureType) {
			case "verify":
				switch (state) {
					case 0:
						eb = settings.setVerifyMessageText(event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the channel where the message should be sent.");
						break;
					case 1:
						eb = settings.setVerifyMessageTextChannelId(event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with the role(s) a user should be given once verified. Separate multiple roles with a space."
						);
						break;
					case 2:
						String[] verifyRoles = event.getMessage().getContentRaw().split(" ");
						if (verifyRoles.length == 0 || verifyRoles.length > 3) {
							eb =
								defaultEmbed(
									"You must add at least one verification role and at most three verification roles. (Example: `@role1, @role2`)"
								);
						} else {
							database.setVerifyRolesSettings(event.getGuild().getId(), new JsonArray());
							for (String verifyRole : verifyRoles) {
								eb = settings.addVerifyRole(verifyRole);
								if (!eb.build().getTitle().equals("Settings")) {
									break;
								}
							}
						}

						eb2.setDescription(
							"Reply with the template that will be used to nick a user after verification. This follows the format `<PREFIX> [IGN] <POSTFIX>` where the prefix and postfix can be set. Reply with 'none' if you do not want this."
						);
						break;
					case 3:
						eb = settings.setVerifyNickname(event.getMessage().getContentRaw());
						eb2.setDescription("Reply with 'enable' to enable verification or anything else to cancel.");
						break;
					case 4:
						if (event.getMessage().getContentRaw().equalsIgnoreCase("enable")) {
							eb = settings.setVerifyEnable("true");
							if (eb.build().getTitle().equals("Settings")) {
								String msg = onVerifyReload(event.getGuild().getId());
								if (msg.equals("Reloaded")) {
									sendEmbed(defaultEmbed("Success").setDescription("Enabled automatic verification."));
								} else {
									sendEmbed(defaultEmbed("Error").setDescription(msg));
								}
							} else {
								sendEmbed(eb);
							}
						} else {
							sendEmbed(defaultEmbed("Canceled the process"));
							cancel();
						}
						return;
				}
				break;
			case "guild":
				switch (state) {
					case 0:
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
						break;
				}
				break;
			case "guild_apply":
				switch (state) {
					case 0:
						eb = settings.setApplyMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the channel where the message will be sent.");
						break;
					case 1:
						eb = settings.setApplyChannel(getSettings(), event.getMessage().getContentRaw());
						StringBuilder categoriesStr = new StringBuilder();
						for (Category category : event.getGuild().getCategories()) {
							categoriesStr.append("\n").append(category.getName()).append(" - ").append(category.getId());
						}
						eb2.setDescription(
							"Reply with the category where new applications should be made.\nList of all categories: " + categoriesStr
						);
						break;
					case 2:
						eb = settings.setApplyCategory(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the staff channel where applications should besent.");
						break;
					case 3:
						eb = settings.setApplyStaffChannel(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with a staff role that should be pinged when an application is received. Reply with 'none' for no ping."
						);
						break;
					case 4:
						eb = settings.addApplyStaffRole(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the message that should be sent if an application is accepted.");
						break;
					case 5:
						eb = settings.setApplyAcceptMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with the message that should be sent if an application is denied.");
						break;
					case 6:
						eb = settings.setApplyDenyMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with the message that should be sent if an application is waitlisted. Reply with 'none' if you do not want this."
						);
						break;
					case 7:
						eb = settings.setApplyWaitlistMessage(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription(
							"Reply with the channel where the players who were accepted or waitlisted will be sent. Reply with 'none' if you do not want this."
						);
						break;
					case 8:
						eb = settings.setApplyWaitingChannel(getSettings(), event.getMessage().getContentRaw());
						eb2.setDescription("Reply with 'yes' if the applications should be ironman only.");
						break;
					case 9:
						eb = settings.setApplyIronman(getSettings(), event.getMessage().getContentRaw().equalsIgnoreCase("yes"));
						eb2.setDescription(
							"Reply with the requirements that an applicant must meet. Separate multiple requirements with a comma and a space. (Example: `weight:4000 skills:40, slayer:1500000 catacombs:30, weight:5000`). Reply with 'none' if you do not want this."
						);
						break;
					case 10:
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
						break;
					case 11:
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
							cancel();
						}
						return;
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
						String[] guildRanksSplit = guildRank.split(" ");
						eb =
							settings.addGuildRank(
								getSettings(),
								guildRanksSplit.length >= 1 ? guildRanksSplit[0] : null,
								guildRanksSplit.length >= 2 ? guildRanksSplit[1] : null
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
			case "prefix":
				eb = settings.setPrefix(event.getMessage().getContentRaw());
				if (eb.build().getTitle().equals("Settings")) {
					sendEmbed(eb);
					return;
				}
				break;
		}

		if (!eb.build().getTitle().equals("Settings")) {
			attemptsLeft--;
			if (attemptsLeft == 0) {
				sendEmbed(defaultEmbed("Canceled (3/3 failed attempts)"));
				cancel();
				return;
			} else {
				sendEmbed(eb.appendDescription("\nPlease try again."));
			}
		} else {
			state++;
			sendEmbed(eb2);
		}

		waitForReply();
	}

	public boolean isValid() {
		return featureType != null;
	}

	private void sendEmbed(EmbedBuilder eb) {
		buttonEvent.getChannel().sendMessageEmbeds(eb.build()).queue();
	}

	private void waitForReply() {
		waiter.waitForEvent(
			GuildMessageReceivedEvent.class,
			this::checkReply,
			this::handleReply,
			1,
			TimeUnit.MINUTES,
			() -> {
				cancel();
				buttonEvent.getChannel().sendMessageEmbeds(defaultEmbed("Timeout").build()).queue();
			}
		);
	}

	private JsonObject getSettings() {
		return database.getGuildSettings(buttonEvent.getGuild().getId(), name).getAsJsonObject();
	}
}
