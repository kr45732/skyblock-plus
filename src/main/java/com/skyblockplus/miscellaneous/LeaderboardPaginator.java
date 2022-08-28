/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.data.DataObject;

public class LeaderboardPaginator {

	private final Map<Integer, DataObject> leaderboardCache = new TreeMap<>();
	private final Message message;
	private final String lbType;
	private final Player.Gamemode gamemode;
	private final PaginatorEvent event;
	private String player;
	private int pageFirstRank = 1;
	private int playerRank = -1;
	private String playerAmount = "Not on leaderboard";
	private boolean isPlayer = false;

	public LeaderboardPaginator(
		String lbType,
		Player.Gamemode gamemode,
		Player player,
		int page,
		int rank,
		double amount,
		PaginatorEvent event
	) {
		this.lbType = lbType;
		this.gamemode = gamemode;
		this.player = player != null ? player.getUsername() : null;
		this.event = event;
		this.message = event.getLoadingMessage();

		if (rank != -1) {
			rank = Math.max(1, rank);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, rank - 200, rank + 200));
		} else if (amount != -1) {
			amount = Math.max(0, amount);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, amount));
		} else if (page != -1) {
			page = Math.max(1, page);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, page * 20 - 200, page * 20 + 200));
		} else if (player != null) {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, player.getUuid()));
			isPlayer = true;
		} else {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, 0, 201));
		}

		double closestAmt = -1;
		int idx = 1;
		for (Map.Entry<Integer, DataObject> entry : leaderboardCache.entrySet()) {
			int curRank = entry.getKey();
			double curAmount = entry.getValue().getDouble(lbType, 0.0);

			if (player != null && entry.getValue().getString("username", "").equals(player.getUsername())) {
				playerRank = curRank;
				playerAmount = roundAndFormat(lbType.equals("networth") ? (long) curAmount : curAmount);
			}

			if (amount != -1 && (closestAmt == -1 || Math.abs(curAmount - amount) < closestAmt)) {
				closestAmt = Math.abs(curAmount - amount);
				idx = curRank;
			}
		}

		if (rank != -1) {
			pageFirstRank = ((rank - 1) / 20) * 20 + 1;
		} else if (amount != -1) {
			pageFirstRank = ((idx - 1) / 20) * 20 + 1;
		} else if (page != -1) {
			pageFirstRank = (page - 1) * 20 + 1;
		} else if (player != null) {
			pageFirstRank = ((playerRank - 1) / 20) * 20 + 1;
		}

		event
			.getAction()
			.editMessageEmbeds(getRender().build())
			.setComponents(getActionRow())
			.get()
			.queue(ignored -> waitForEvent(), ignore);
	}

	private EmbedBuilder getRender() {
		StringBuilder columnOne = new StringBuilder();
		StringBuilder columnTwo = new StringBuilder();
		for (int i = pageFirstRank; i < pageFirstRank + 20; i++) {
			DataObject curPlayer = leaderboardCache.getOrDefault(i, null);
			if (curPlayer != null) {
				double curAmount = curPlayer.getDouble(lbType, 0.0);
				String out =
					"`" +
					i +
					")` " +
					fixUsername(curPlayer.getString("username", "?")) +
					": " +
					(roundAndFormat(lbType.equals("networth") ? (long) curAmount : curAmount));

				if (i < pageFirstRank + 10) {
					columnOne.append(out).append("\n");
				} else {
					columnTwo.append(out).append("\n");
				}
			}
		}

		if (columnOne.isEmpty() && columnTwo.isEmpty()) {
			columnOne.append("This page is empty");
		}

		return defaultEmbed("Global Leaderboard | " + capitalizeString(gamemode.toString()))
			.setDescription(
				isPlayer
					? "**Player:** " +
					fixUsername(player) +
					"\n**Rank:** " +
					(playerRank == -1 ? "Not on leaderboard (are APIs enabled?)" : "#" + formatNumber(playerRank)) +
					"\n**" +
					capitalizeString(lbType.replace("_", " ")) +
					":** " +
					playerAmount
					: ("**Type:** " + capitalizeString(lbType.replace("_", " ")))
			)
			.addField("", columnOne.toString(), true)
			.addField("", columnTwo.toString(), true)
			.setFooter("By CrypticPlasma • Page " + ((pageFirstRank - 1) / 20 + 1) + " • dsc.gg/sb+", null);
	}

	private boolean condition(ButtonInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getUser().getId().equals(this.event.getUser().getId()) &&
			event.getMessageId().equals(message.getId())
		);
	}

	public void action(ButtonInteractionEvent event) {
		if (event.getComponentId().equals("leaderboard_paginator_left_button")) {
			if (pageFirstRank != -1) {
				pageFirstRank -= 20;

				if (!leaderboardCache.containsKey(pageFirstRank)) {
					leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, pageFirstRank - 200, pageFirstRank + 20));
				}
			}
		} else if (event.getComponentId().equals("leaderboard_paginator_right_button")) {
			pageFirstRank += 20; // TODO: check if anymore pages on right?

			if (!leaderboardCache.containsKey(pageFirstRank)) {
				leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, pageFirstRank - 19, pageFirstRank + 199));
			}
		} else if (event.getComponentId().equals("leaderboard_paginator_search_button")) {
			event
				.replyModal(
					Modal
						.create("leaderboard_paginator_search_modal_" + event.getMessageId(), "Search")
						.addActionRows(
							ActionRow.of(TextInput.create("rank", "Rank", TextInputStyle.SHORT).setRequired(false).build()),
							ActionRow.of(TextInput.create("player", "Player", TextInputStyle.SHORT).setRequired(false).build()),
							ActionRow.of(TextInput.create("page", "Page", TextInputStyle.SHORT).setRequired(false).build()),
							ActionRow.of(TextInput.create("amount", "Amount", TextInputStyle.SHORT).setRequired(false).build())
						)
						.build()
				)
				.queue(ignored -> waitForEventModal(), ignored -> waitForEventModal());
			return;
		}

		event
			.editMessageEmbeds(getRender().build())
			.setComponents(getActionRow())
			.queue(ignored -> waitForEvent(), ignored -> waitForEvent());
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			ButtonInteractionEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> message.editMessageComponents().queue(ignore, ignore)
		);
	}

	private boolean conditionModal(ModalInteractionEvent event) {
		return (
			event.isFromGuild() &&
			event.getUser().getId().equals(this.event.getUser().getId()) &&
			event.getModalId().equals("leaderboard_paginator_search_modal_" + message.getId())
		);
	}

	public void actionModal(ModalInteractionEvent event) {
		event.deferEdit().queue();

		int rank = -1;
		double amount = -1;
		int page = -1;
		UsernameUuidStruct player = null;

		try {
			rank = Integer.parseInt(event.getValue("rank").getAsString());
		} catch (Exception ignored) {}
		try {
			amount = Double.parseDouble(event.getValue("amount").getAsString());
		} catch (Exception ignored) {}
		try {
			page = Integer.parseInt(event.getValue("page").getAsString());
		} catch (Exception ignored) {}
		try {
			player = usernameToUuid(event.getValue("player").getAsString());
			player = !player.isValid() ? null : player;
		} catch (Exception ignored) {}

		if (rank != -1) {
			rank = Math.max(1, rank);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, rank - 200, rank + 200));
			isPlayer = false;
		} else if (amount != -1) {
			amount = Math.max(0, amount);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, amount));
			isPlayer = false;
		} else if (page != -1) {
			page = Math.max(1, page);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, page * 20 - 200, page * 20 + 200));
			isPlayer = false;
		} else if (player != null) {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, player.uuid()));
			this.player = player.username();
			isPlayer = true;
		}

		double closestAmt = -1;
		int idx = 1;
		for (Map.Entry<Integer, DataObject> entry : leaderboardCache.entrySet()) {
			int curRank = entry.getKey();
			double curAmount = entry.getValue().getDouble(lbType, 0.0);

			if (player != null && entry.getValue().getString("username", "").equals(player.username())) {
				playerRank = curRank;
				playerAmount = roundAndFormat(lbType.equals("networth") ? (long) curAmount : curAmount);
			}

			if (amount != -1 && (closestAmt == -1 || Math.abs(curAmount - amount) < closestAmt)) {
				closestAmt = Math.abs(curAmount - amount);
				idx = curRank;
			}
		}

		if (rank != -1) {
			pageFirstRank = ((rank - 1) / 20) * 20 + 1;
		} else if (amount != -1) {
			pageFirstRank = ((idx - 1) / 20) * 20 + 1;
		} else if (page != -1) {
			pageFirstRank = (page - 1) * 20 + 1;
		} else if (player != null) {
			pageFirstRank = ((playerRank - 1) / 20) * 20 + 1;
		}

		event.getHook().editOriginalEmbeds(getRender().build()).setComponents(getActionRow()).queue(ignored -> waitForEvent(), ignore);
	}

	private ActionRow getActionRow() {
		return ActionRow.of(
			Button
				.primary("leaderboard_paginator_left_button", Emoji.fromFormatted("<:left_button_arrow:885628386435821578>"))
				.withDisabled(pageFirstRank == 1),
			Button.primary("leaderboard_paginator_search_button", "Search").withEmoji(Emoji.fromFormatted("\uD83D\uDD0E")),
			Button.primary("leaderboard_paginator_right_button", Emoji.fromFormatted("<:right_button_arrow:885628386578423908>"))
		);
	}

	private void waitForEventModal() {
		waiter.waitForEvent(
			ModalInteractionEvent.class,
			this::conditionModal,
			this::actionModal,
			1,
			TimeUnit.MINUTES,
			() -> message.editMessageComponents().queue(ignore, ignore)
		);
	}
}
