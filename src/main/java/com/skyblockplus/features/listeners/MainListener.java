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

package com.skyblockplus.features.listeners;

import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.inventory.InventoryListPaginator;
import com.skyblockplus.utils.AuctionFlipper;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

public class MainListener extends ListenerAdapter {

	public static final Map<String, AutomaticGuild> guildMap = new ConcurrentHashMap<>();
	private static String lastRepoComitSha = null;

	public static String onApplyReload(String guildId) {
		String reloadStatus = "Error reloading";
		if (guildMap.containsKey(guildId)) {
			reloadStatus = guildMap.get(guildId).reloadApplyConstructor(guildId);
		}
		return reloadStatus;
	}

	public static String onVerifyReload(String guildId) {
		String reloadStatus = "Error reloading";
		if (guildMap.containsKey(guildId)) {
			reloadStatus = guildMap.get(guildId).reloadVerifyConstructor(guildId);
		}
		return reloadStatus;
	}

	@Override
	public void onGuildReady(GuildReadyEvent event) {
		if (event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
			return;
		}

		if (isUniqueGuild(event.getGuild().getId())) {
			guildMap.put(event.getGuild().getId(), new AutomaticGuild(event));
		}
	}

	@Override
	public void onGuildLeave(@NotNull GuildLeaveEvent event) {
		guildMap.get(event.getGuild().getId()).onGuildLeave();
		guildMap.remove(event.getGuild().getId());
		database.deleteServerSettings(event.getGuild().getId());

		try {
			logCommand(event.getGuild(), "Left guild | Users: " + event.getGuild().getMemberCount());
		} catch (Exception ignored) {}
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		if (event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
			return;
		}

		if (isUniqueGuild(event.getGuild().getId())) {
			try {
				EmbedBuilder eb = defaultEmbed("Thank you!")
					.setDescription(
						"- Thank you for adding me to " +
						event.getGuild().getName() +
						"`\n- You can view my commands by running `/help`\n- Make sure to check out `/setup` or the forum post [**here**](" +
						FORUM_POST_LINK +
						") on how to setup the customizable features of this bot!"
					);
				TextChannel channel = event
					.getGuild()
					.getTextChannels()
					.stream()
					.filter(textChannel -> textChannel.getName().toLowerCase().contains("general") && textChannel.canTalk())
					.min(Comparator.naturalOrder())
					.orElse(
						event
							.getGuild()
							.getTextChannels()
							.stream()
							.filter(GuildMessageChannel::canTalk)
							.min(Comparator.naturalOrder())
							.orElse(null)
					);
				if (channel != null) {
					channel.sendMessageEmbeds(eb.build()).queue(ignore, ignore);
				}
			} catch (Exception ignored) {}

			logCommand(
				event.getGuild(),
				"Joined guild | #" +
				jda.getGuilds().size() +
				" | Users: " +
				event.getGuild().retrieveMetaData().complete().getApproximateMembers()
			);

			guildMap.put(event.getGuild().getId(), new AutomaticGuild(event));
		}
	}

	private boolean isUniqueGuild(String guildId) {
		return !guildMap.containsKey(guildId);
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.isFromGuild()) {
			return;
		}

		if (event.getUser() != null && event.getUser().isBot()) {
			return;
		}

		if (guildMap.containsKey(event.getGuild().getId())) {
			guildMap.get(event.getGuild().getId()).onMessageReactionAdd(event);
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return;
		}

		if (
			isMainBot() && event.getGuild().getId().equals("796790757947867156") && event.getChannel().getId().equals("869278025018114108")
		) {
			String commitSha = higherDepth(
				getJson("https://api.github.com/repos/NotEnoughUpdates/NotEnoughUpdates-REPO/commits?per_page=1"),
				"[0].sha",
				null
			);
			if (!Objects.equals(commitSha, lastRepoComitSha)) {
				lastRepoComitSha = commitSha;
				updateItemMappings();
			}
			return;
		}

		if (AuctionFlipper.onGuildMessageReceived(event)) {
			return;
		}

		if (guildMap.containsKey(event.getGuild().getId())) {
			guildMap.get(event.getGuild().getId()).onGuildMessageReceived(event);
		}
	}

	@Override
	public void onChannelDelete(ChannelDeleteEvent event) {
		if (!event.isFromType(ChannelType.TEXT)) {
			return;
		}

		if (guildMap.containsKey(event.getGuild().getId())) {
			guildMap.get(event.getGuild().getId()).onTextChannelDelete(event);
		}
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getUser().isBot()) {
			return;
		}

		if (event.getGuild() == null) {
			event.editButton(event.getButton().asDisabled().withLabel("Disabled").withStyle(ButtonStyle.DANGER)).queue();
			event.getHook().editOriginal(client.getError() + " This button has been disabled").queue();
			return;
		}

		if (guildMap.containsKey(event.getGuild().getId())) {
			guildMap.get(event.getGuild().getId()).onButtonClick(event);
		}
	}

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		if (event.getUser().isBot() || event.getGuild() == null) {
			return;
		}

		for (InventoryListPaginator paginator : InventoryListPaginator.paginators) {
			if (paginator.onModalInteraction(event)) {
				return;
			}
		}
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (guildMap.containsKey(event.getGuild().getId())) {
			guildMap.get(event.getGuild().getId()).onGuildMemberJoin(event);
		}
	}
}
