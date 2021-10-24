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

package com.skyblockplus.features.apply;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class ApplyGuild {

	public final List<ApplyUser> applyUserList;
	public final Message reactMessage;
	public final JsonElement currentSettings;
	public final boolean enable = true;
	public TextChannel waitInviteChannel = null;

	public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
		this.reactMessage = reactMessage;
		this.currentSettings = currentSettings;
		this.applyUserList = getApplyGuildUsersCache(reactMessage.getGuild().getId(), higherDepth(currentSettings, "name").getAsString());
		try {
			this.waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
		} catch (Exception ignored) {}
	}

	public ApplyGuild(Message reactMessage, JsonElement currentSettings, List<ApplyUser> prevApplyUsers) {
		this(reactMessage, currentSettings);
		applyUserList.addAll(prevApplyUsers);
	}

	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!enable) {
			return;
		}

		onMessageReactionAdd_ExistingApplyUser(event);
	}

	public void onMessageReactionAdd_ExistingApplyUser(MessageReactionAddEvent event) {
		ApplyUser findApplyUser = applyUserList
			.stream()
			.filter(applyUser -> applyUser.reactMessageId.equals(event.getMessageId()))
			.findFirst()
			.orElse(null);

		if (findApplyUser != null) {
			if (findApplyUser.onMessageReactionAdd(event)) {
				applyUserList.remove(findApplyUser);
			}
		}
	}

	public void onTextChannelDelete(TextChannelDeleteEvent event) {
		applyUserList.removeIf(applyUser ->
			(applyUser.applicationChannelId != null && applyUser.applicationChannelId.equals(event.getChannel().getId())) ||
			(applyUser.staffChannelId != null && applyUser.staffChannelId.equals(event.getChannel().getId()))
		);
	}

	public String onButtonClick_NewApplyUser(ButtonClickEvent event) {
		if (event.getMessageIdLong() != reactMessage.getIdLong()) {
			return null;
		}

		if (!event.getButton().getId().equals("create_application_button_" + higherDepth(currentSettings, "name").getAsString())) {
			return null;
		}

		ApplyUser runningApplication = applyUserList
			.stream()
			.filter(o1 -> o1.applyingUserId.equals(event.getUser().getId()))
			.findFirst()
			.orElse(null);

		if (runningApplication != null) {
			return "❌ There is already an application open in <#" + runningApplication.applicationChannelId + ">";
		}

		JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getUser().getId());
		if (linkedAccount.isJsonNull() || !higherDepth(linkedAccount, "discordId").getAsString().equals((event.getUser().getId()))) {
			if (linkedAccount.isJsonNull()) {
				return "❌ You are not linked to the bot. Please run `+link [IGN]` and try again.";
			} else {
				return (
					"❌ Account " +
					higherDepth(linkedAccount, "minecraftUsername").getAsString() +
					" is linked with the Discord tag " +
					jda.retrieveUserById(higherDepth(linkedAccount, "discordId").getAsString()).complete().getAsTag() +
					"\nYour current Discord tag is " +
					event.getUser().getAsTag() +
					".\nPlease relink and try again"
				);
			}
		}
		JsonElement blacklisted = streamJsonArray(database.getApplyBlacklist(event.getGuild().getId()))
			.filter(blacklist ->
				higherDepth(blacklist, "uuid").getAsString().equals(higherDepth(linkedAccount, "minecraftUuid").getAsString()) ||
				higherDepth(blacklist, "username").getAsString().equals(higherDepth(linkedAccount, "minecraftUsername").getAsString())
			)
			.findFirst()
			.orElse(null);
		if (blacklisted != null) {
			return "❌ You have been blacklisted with reason `" + higherDepth(blacklisted, "reason").getAsString() + "`";
		}

		Player player = new Player(higherDepth(linkedAccount, "minecraftUsername").getAsString());
		if (!player.isValid()) {
			return "❌ Unable to fetch player data. Failed cause: `" + player.getFailCause() + "`";
		} else {
			if (higherDepth(currentSettings, "ironmanOnly", false) && player.getAllProfileNames(true).length == 0) {
				return "❌ You have no ironman profiles created";
			}
		}

		ApplyUser toAdd = new ApplyUser(
			event,
			currentSettings,
			higherDepth(linkedAccount, "minecraftUsername").getAsString(),
			higherDepth(linkedAccount, "minecraftUuid").getAsString()
		);
		if (toAdd.failCause != null) {
			return "❌ " + toAdd.failCause;
		}

		applyUserList.add(toAdd);

		return "✅ A new application was created in " + event.getGuild().getTextChannelById(toAdd.applicationChannelId).getAsMention();
	}

	public String onButtonClick(ButtonClickEvent event) {
		String waitingForInvite = onButtonClick_WaitingForInviteApplyUser(event);
		if (waitingForInvite != null) {
			return waitingForInvite;
		}

		boolean currentApplyUser = onButtonClick_CurrentApplyUser(event);
		if (currentApplyUser) {
			return "IGNORE_INTERNAL";
		}

		return onButtonClick_NewApplyUser(event);
	}

	public boolean onButtonClick_CurrentApplyUser(ButtonClickEvent event) {
		ApplyUser findApplyUser = applyUserList
			.stream()
			.filter(applyUser -> applyUser.reactMessageId.equals(event.getMessageId()))
			.findFirst()
			.orElse(null);

		return findApplyUser != null && findApplyUser.onButtonClick(event, this);
	}

	public String onButtonClick_WaitingForInviteApplyUser(ButtonClickEvent event) {
		if (!event.getChannel().equals(waitInviteChannel)) {
			return null;
		}

		if (!event.getComponentId().startsWith("apply_user_wait_" + higherDepth(currentSettings, "name").getAsString())) {
			return null;
		}

		if (
			!(
				(
					!higherDepth(currentSettings, "staffPingRoleId").getAsString().equals("none") &&
					event
						.getMember()
						.getRoles()
						.contains(event.getGuild().getRoleById(higherDepth(currentSettings, "staffPingRoleId").getAsString()))
				) ||
				event.getMember().hasPermission(Permission.ADMINISTRATOR)
			)
		) {
			return "❌ You are missing the required permissions in this Guild to use that!";
		}

		try {
			TextChannel toCloseChannel = event
				.getGuild()
				.getTextChannelById(
					event.getComponentId().split("apply_user_wait_" + higherDepth(currentSettings, "name").getAsString() + "_")[1]
				);
			toCloseChannel.sendMessageEmbeds(defaultEmbed("Closing channel").build()).queue();
			toCloseChannel.delete().reason("Application closed").queueAfter(10, TimeUnit.SECONDS);
		} catch (Exception ignored) {}

		event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
		return "✅ Player was invited";
	}
}
