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

package com.skyblockplus.features.apply;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.StringUtils.capitalizeString;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.Player;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public class ApplyGuild {

	public final List<ApplyUser> applyUserList = new ArrayList<>();
	public final String reactMessageId;
	public final JsonElement currentSettings;
	public final boolean enable = true;
	public TextChannel waitInviteChannel = null;

	public ApplyGuild(Message reactMessage, JsonElement currentSettings, List<ApplyUser> prevApplyUsers) {
		this.reactMessageId = reactMessage.getId();
		this.currentSettings = currentSettings;

		if (prevApplyUsers != null) { // Triggered by reload command
			applyUserList.addAll(prevApplyUsers);
		} else if (isMainBot()) { // Triggered by initial startup
			String guildId = reactMessage.getGuild().getId();
			String guildName = higherDepth(currentSettings, "guildName", null);

			try {
				JsonArray applyUsersCache;
				try {
					applyUsersCache =
						JsonParser.parseString(higherDepth(currentSettings, "applyUsersCache").getAsString()).getAsJsonArray();
				} catch (Exception e) {
					e.printStackTrace();
					applyUsersCache = new JsonArray();
				}

				for (JsonElement applyUser : applyUsersCache) {
					if (reactMessage.getGuild().getTextChannelById(higherDepth(applyUser, "applicationChannelId", null)) != null) {
						applyUserList.add(gson.fromJson(applyUser, ApplyUser.class));
					}
				}

				if (!applyUserList.isEmpty()) {
					AutomaticGuild
						.getLog()
						.info(
							"Retrieved ApplyUser cache - size={" +
							applyUserList.size() +
							"}, guildId={" +
							guildId +
							"}, name={" +
							guildName +
							"}"
						);
				}
			} catch (Exception e) {
				AutomaticGuild.getLog().error("guildId={" + guildId + "}, name={" + guildName + "}", e);
			}
		}

		try {
			this.waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "applyWaitingChannel").getAsString());
		} catch (Exception ignored) {}
	}

	public boolean onStringSelectInteraction(StringSelectInteractionEvent event) {
		if (!enable) {
			return false;
		}

		Optional<ApplyUser> applyUser = applyUserList.stream().filter(e -> e.reactMessageId.equals(event.getMessageId())).findAny();

		if (applyUser.isPresent()) {
			applyUser.get().onStringSelectInteraction(event, this);
			return true;
		}

		return false;
	}

	public void onTextChannelDelete(ChannelDeleteEvent event) {
		for (Iterator<ApplyUser> iterator = applyUserList.iterator(); iterator.hasNext();) {
			ApplyUser applyUser = iterator.next();
			if ((applyUser.applicationChannelId != null && applyUser.applicationChannelId.equals(event.getChannel().getId()))) {
				if (applyUser.state == 2) {
					event
						.getGuild()
						.getTextChannelById(applyUser.staffChannelId)
						.deleteMessageById(applyUser.reactMessageId)
						.queue(ignore, ignore);
				}
				iterator.remove();
			} else if (applyUser.staffChannelId != null && applyUser.staffChannelId.equals(event.getChannel().getId())) {
				iterator.remove();
			}
		}
	}

	public boolean onButtonClick(ButtonInteractionEvent event) {
		if (onButtonClick_CurrentApplyUser(event)) {
			return true;
		}

		if (onButtonClick_WaitingForInviteApplyUser(event)) {
			return true;
		}

		return onButtonClick_NewApplyUser(event);
	}

	public boolean onButtonClick_CurrentApplyUser(ButtonInteractionEvent event) {
		for (ApplyUser applyUser : applyUserList) {
			if (applyUser.reactMessageId.equals(event.getMessageId())) {
				applyUser.onButtonClick(event, this);
				return true;
			}

			if (
				// reactMessageId is message waiting for staff to accept or deny
				event.getComponentId().equals("apply_user_cancel") &&
				applyUser.applicationChannelId.equals(event.getChannel().getId()) &&
				applyUser.state == 2
			) {
				applyUserList.remove(applyUser);

				// Edit original message
				event.getMessage().editMessageComponents().queue();

				// Edit deferred message
				event
					.getHook()
					.editOriginalEmbeds(defaultEmbed("Canceling Application").build())
					.queue(ignored -> event.getGuildChannel().delete().reason("Application canceled").queueAfter(10, TimeUnit.SECONDS));

				// Staff channel message
				event
					.getGuild()
					.getTextChannelById(applyUser.staffChannelId)
					.editMessageById(
						applyUser.reactMessageId,
						applyUser.playerUsername + " (<@" + applyUser.applyingUserId + ">) canceled their application"
					)
					.setEmbeds()
					.setComponents()
					.queue();
				return true;
			}
		}

		return false;
	}

	public boolean onButtonClick_WaitingForInviteApplyUser(ButtonInteractionEvent event) {
		if (waitInviteChannel == null) {
			return false;
		}

		if (!event.getChannel().getId().equals(waitInviteChannel.getId())) {
			return false;
		}

		if (!event.getComponentId().startsWith("apply_user_wait_" + higherDepth(currentSettings, "guildName").getAsString())) {
			return false;
		}

		if (!isApplyAdmin(event.getMember())) {
			event
				.getHook()
				.editOriginal(client.getError() + " You are missing the required permissions in this guild to use that!")
				.queue();
			return true;
		}

		try {
			// 0 = channel, 1 = user, 2 = role
			String[] channelRoleSplit = event
				.getComponentId()
				.split("apply_user_wait_" + higherDepth(currentSettings, "guildName").getAsString() + "_")[1].split("_");
			try {
				event
					.getGuild()
					.addRoleToMember(UserSnowflake.fromId(channelRoleSplit[1]), event.getGuild().getRoleById(channelRoleSplit[2]))
					.queue(ignore, ignore);
			} catch (Exception ignored) {}
			TextChannel applicationChannel = event.getGuild().getTextChannelById(channelRoleSplit[0]);
			applyUserList.removeIf(applyUser -> applyUser.applicationChannelId.equals(applicationChannel.getId()));
			applicationChannel
				.sendMessageEmbeds(defaultEmbed("Closing Channel").build())
				.queue(ignored -> applicationChannel.delete().reason("Player invited").queueAfter(10, TimeUnit.SECONDS, ignore), ignore);
		} catch (Exception ignored) {}

		event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS, ignore, ignore);
		event.getHook().editOriginal(client.getSuccess() + " Player was invited").queue();
		return true;
	}

	public boolean onButtonClick_NewApplyUser(ButtonInteractionEvent event) {
		if (!event.getMessageId().equals(reactMessageId)) {
			return false;
		}

		if (!event.getComponentId().equals("apply_user_create_" + higherDepth(currentSettings, "guildName").getAsString())) {
			return false;
		}

		ApplyUser runningApplication = applyUserList
			.stream()
			.filter(o1 -> o1.applyingUserId.equals(event.getUser().getId()))
			.findFirst()
			.orElse(null);

		if (runningApplication != null) {
			event
				.getHook()
				.editOriginal(
					client.getError() + " You already have an application open in <#" + runningApplication.applicationChannelId + ">"
				)
				.queue();
			return true;
		}

		LinkedAccount linkedAccount = database.getByDiscord(event.getUser().getId());
		if (linkedAccount == null) {
			event
				.getHook()
				.editOriginal(client.getError() + " You are not linked to the bot. Please run `/link <player>` and try again.")
				.queue();
			return true;
		}

		JsonArray currentBlacklist = guildMap.get(event.getGuild().getId()).getBlacklist();
		JsonElement blacklisted = streamJsonArray(currentBlacklist)
			.filter(blacklist -> higherDepth(blacklist, "uuid").getAsString().equals(linkedAccount.uuid()))
			.findFirst()
			.orElse(null);
		if (blacklisted != null) {
			event
				.getHook()
				.editOriginal(
					client.getError() + " You have been blacklisted with reason `" + higherDepth(blacklisted, "reason").getAsString() + "`"
				)
				.queue();
			return true;
		}

		if (higherDepth(currentSettings, "applyScammerCheck", false)) {
			JsonElement scammerJson = getScammerJson(linkedAccount.uuid());
			String scammerReason = scammerJson != null ? higherDepth(scammerJson, "details.reason", "No reason provided") : null;
			if (scammerReason != null) {
				event
					.getHook()
					.editOriginalEmbeds(
						defaultEmbed("Error")
							.setDescription("You have been marked as a scammer with reason `" + scammerReason + "`")
							.setFooter("Scammer check powered by SkyBlockZ (discord.gg/skyblock)")
							.build()
					)
					.queue();
				return true;
			}
		}

		Player.Profile player = Player.create(linkedAccount.username());
		if (!player.isValid()) {
			event.getHook().editOriginal(client.getError() + " Failed to fetch player data: `" + player.getFailCause() + "`").queue();
			return true;
		} else {
			Player.Gamemode gamemode = Player.Gamemode.of(higherDepth(currentSettings, "applyGamemode", "all"));
			if (player.getMatchingProfileNames(gamemode).isEmpty()) {
				event
					.getHook()
					.editOriginal(client.getError() + " You have no " + gamemode.toString().toLowerCase() + " profiles created")
					.queue();
				return true;
			}
		}

		if (higherDepth(currentSettings, "applyCheckApi", false)) {
			boolean invEnabled = player.isInventoryApiEnabled();
			boolean bankEnabled = player.isBankApiEnabled();
			boolean collectionsEnabled = player.isCollectionsApiEnabled();
			boolean vaultEnabled = player.isVaultApiEnabled();
			boolean skillsEnabled = player.isSkillsApiEnabled();

			if (!invEnabled || !bankEnabled || !collectionsEnabled || !vaultEnabled || !skillsEnabled) {
				String out =
					(invEnabled ? "" : "inventory, ") +
					(bankEnabled ? "" : "bank, ") +
					(collectionsEnabled ? "" : "collections, ") +
					(vaultEnabled ? "" : "vault, ") +
					(skillsEnabled ? "" : "skills, ");
				out = capitalizeString(out.substring(0, out.length() - 2));

				event
					.getHook()
					.editOriginal(client.getError() + " " + out + " API" + (out.contains(",") ? "s" : "") + " not enabled")
					.queue();
				return true;
			}
		}

		new ApplyUser(event, player, this);
		return true;
	}

	public boolean isApplyAdmin(Member member) {
		if (guildMap.get(member.getGuild().getId()).isAdmin(member)) {
			return true;
		}

		JsonArray applyStaffRoles = higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray();
		if (applyStaffRoles.isEmpty()) {
			return false;
		}

		for (JsonElement staffPingRole : applyStaffRoles) {
			if (member.getRoles().stream().anyMatch(e -> e.getId().equals(staffPingRole.getAsString()))) {
				return true;
			}
		}

		return false;
	}
}
