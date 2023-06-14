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

package com.skyblockplus.features.verify;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.general.LinkSlashCommand;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class VerifyGuild {

	public final boolean enable;
	public JsonElement verifySettings;

	public VerifyGuild(JsonElement verifySettings) {
		this.enable = true;
		this.verifySettings = verifySettings;
	}

	public VerifyGuild() {
		this.enable = false;
	}

	public void onButtonClick(ButtonInteractionEvent event) {
		if (!enable) {
			return;
		}

		event
			.replyModal(
				Modal
					.create("verify_modal", "Verification")
					.addActionRow(TextInput.create("value", "Your In-Game Name", TextInputStyle.SHORT).build())
					.build()
			)
			.queue(ignore, ignore);
	}

	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (!higherDepth(verifySettings, "enableAutomaticSync", false)) {
			return;
		}

		LinkedAccount linkedUser = database.getByDiscord(event.getUser().getId());
		if (linkedUser == null) {
			return;
		}

		if (
			streamJsonArray(guildMap.get(event.getGuild().getId()).getBlacklist())
				.anyMatch(blacklist -> higherDepth(blacklist, "uuid").getAsString().equals(linkedUser.uuid()))
		) {
			return;
		}

		String[] result = LinkSlashCommand.updateLinkedUser(verifySettings, linkedUser, event.getMember(), true);
		if (higherDepth(verifySettings, "dmOnSync", false)) {
			event
				.getUser()
				.openPrivateChannel()
				.queue(
					privateChannel ->
						privateChannel
							.sendMessageEmbeds(
								defaultEmbed("Member synced")
									.setDescription(
										"You have automatically been synced in `" +
										event.getGuild().getName() +
										"`" +
										(
											!result[1].equals("false")
												? result[1].equals("true")
													? "\n• Successfully synced your roles"
													: "\n• Error syncing your roles"
												: ""
										) +
										(
											!result[0].equals("false")
												? result[0].equals("true")
													? "\n• Successfully synced your nickname"
													: "\n• Error syncing your nickname"
												: ""
										)
									)
									.build()
							)
							.queue(ignore, ignore),
					ignore
				);
		}
	}

	public void reloadSettingsJson(JsonElement newVerifySettings) {
		this.verifySettings = newVerifySettings.deepCopy();
	}
}
