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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Constants.ALL_SKILL_NAMES;
import static com.skyblockplus.utils.Constants.collectionNameToId;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.data.DataObject;

public class LeaderboardPaginator {

	private final Map<Integer, DataObject> leaderboardCache = new TreeMap<>();
	private final String lbType;
	private final Player.Gamemode gamemode;
	private final SlashCommandEvent slashCommandEvent;
	private Message message;
	private String player;
	private int pageFirstRank = 1;
	private int playerRank = -1;
	private String playerAmount = "None";

	public LeaderboardPaginator(
		String lbType,
		Player.Gamemode gamemode,
		Player.Profile player,
		int page,
		int rank,
		double amount,
		SlashCommandEvent slashCommandEvent
	) {
		this.lbType = lbType;
		this.gamemode = gamemode;
		this.slashCommandEvent = slashCommandEvent;

		if (rank != -1 || page != -1) {
			int clampedRank = (rank != -1 ? (rank - 1) / 20 : page - 1) * 20;
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, clampedRank - 200, clampedRank + 200));
		} else if (amount != -1) {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, amount));
		} else if (player != null) {
			this.player = player.getUsername();
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, player.getUuid()));
		} else {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, 0, 200));
		}

		double closestAmt = -1;
		int idx = 1;
		for (Map.Entry<Integer, DataObject> entry : leaderboardCache.entrySet()) {
			int curRank = entry.getKey();
			double curAmount = entry.getValue().getDouble(lbType);

			if (this.player != null && entry.getValue().getString("username", "").equals(this.player)) {
				playerRank = curRank;
				playerAmount = formatOrSimplify(curAmount);
			}

			if (amount != -1 && (closestAmt == -1 || Math.abs(curAmount - amount) < closestAmt)) {
				closestAmt = Math.abs(curAmount - amount);
				idx = curRank;
			}
		}

		if (this.player != null && player != null && playerAmount.equals("None")) {
			if (lbType.equals("networth")) {
				if (!player.isInventoryApiEnabled()) {
					playerAmount = "Inventory API is disabled";
				}
			} else if (ALL_SKILL_NAMES.contains(lbType)) {
				if (!player.isSkillsApiEnabled()) {
					playerAmount = "Skills API is Disabled";
				}
			} else if (collectionNameToId.containsKey(lbType)) {
				if (!player.isCollectionsApiEnabled()) {
					playerAmount = "Collections API is disabled";
				}
			}
		}

		if (rank != -1) {
			pageFirstRank = ((rank - 1) / 20) * 20 + 1;
		} else if (amount != -1) {
			pageFirstRank = ((idx - 1) / 20) * 20 + 1;
		} else if (page != -1) {
			pageFirstRank = (page - 1) * 20 + 1;
		} else if (this.player != null) {
			pageFirstRank = ((playerRank - 1) / 20) * 20 + 1;
		}

		slashCommandEvent
			.getHook()
			.editOriginalEmbeds(getRender().build())
			.setComponents(getActionRow())
			.queue(
				m -> {
					this.message = m;
					waitForEvent();
				},
				ignore
			);
	}

	private EmbedBuilder getRender() {
		StringBuilder columnOne = new StringBuilder();
		StringBuilder columnTwo = new StringBuilder();
		for (int i = pageFirstRank; i < pageFirstRank + 20; i++) {
			DataObject curPlayer = leaderboardCache.getOrDefault(i, null);
			if (curPlayer != null) {
				double curAmount = curPlayer.getDouble(lbType, 0.0);
				String out = "`" + i + ")` " + escapeText(curPlayer.getString("username", "?")) + ": " + formatOrSimplify(curAmount);

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

		return defaultEmbed(
			"Global" + (gamemode == Player.Gamemode.ALL ? "" : " " + capitalizeString(gamemode.toString())) + " Player Leaderboard"
		)
			.setDescription(
				player != null
					? "**Player:** " +
					escapeText(player) +
					"\n**Rank:** " +
					(playerRank == -1 ? "Not on leaderboard" : "#" + formatNumber(playerRank)) +
					"\n**" +
					capitalizeString(lbType.replace("_", " ")) +
					":** " +
					playerAmount
					: ("**Type:** " + capitalizeString(lbType.replace("_", " ")))
			)
			.addField("", columnOne.toString(), true)
			.addField("", columnTwo.toString(), true)
			.setFooter("SB+ is open source • sbplus.codes/gh • Page " + ((pageFirstRank - 1) / 20 + 1));
	}

	private boolean condition(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof ButtonInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getUser().getId().equals(slashCommandEvent.getUser().getId()) &&
				event.getMessageId().equals(message.getId())
			);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			return (
				event.isFromGuild() &&
				event.getUser().getId().equals(slashCommandEvent.getUser().getId()) &&
				event.getModalId().equals("leaderboard_paginator_search_modal_" + message.getId())
			);
		}
		return false;
	}

	private void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof ButtonInteractionEvent event) {
			onButtonInteraction(event);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			onModalInteraction(event);
		}
	}

	public void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getComponentId().equals("leaderboard_paginator_left_button")) {
			pageFirstRank -= 20;

			if (!leaderboardCache.containsKey(pageFirstRank) || !leaderboardCache.containsKey(pageFirstRank + 19)) {
				leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, pageFirstRank - 181, pageFirstRank + 19));
			}
		} else if (event.getComponentId().equals("leaderboard_paginator_right_button")) {
			pageFirstRank += 20; // TODO: check if anymore pages on right?

			if (!leaderboardCache.containsKey(pageFirstRank) || !leaderboardCache.containsKey(pageFirstRank + 19)) {
				leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, pageFirstRank - 1, pageFirstRank + 199));
			}
		} else if (event.getComponentId().equals("leaderboard_paginator_search_button")) {
			event
				.replyModal(
					Modal
						.create("leaderboard_paginator_search_modal_" + event.getMessageId(), "Search")
						.addComponents(
							ActionRow.of(TextInput.create("rank", "Rank", TextInputStyle.SHORT).setRequired(false).build()),
							ActionRow.of(TextInput.create("player", "Player", TextInputStyle.SHORT).setRequired(false).build()),
							ActionRow.of(TextInput.create("page", "Page", TextInputStyle.SHORT).setRequired(false).build()),
							ActionRow.of(TextInput.create("amount", "Amount", TextInputStyle.SHORT).setRequired(false).build())
						)
						.build()
				)
				.queue(ignored -> waitForEvent(), ignore);
			return;
		}

		event.editMessageEmbeds(getRender().build()).setComponents(getActionRow()).queue(ignored -> waitForEvent(), ignore);
	}

	public void onModalInteraction(ModalInteractionEvent event) {
		event
			.deferEdit()
			.queue(hook -> {
				int rank = -1;
				double amount = -1;
				int page = -1;
				UsernameUuidStruct player = null;

				try {
					rank = Math.max(1, Integer.parseInt(event.getValue("rank").getAsString()));
				} catch (Exception ignored) {}
				try {
					amount = Math.max(0, Double.parseDouble(event.getValue("amount").getAsString()));
				} catch (Exception ignored) {}
				try {
					page = Math.max(1, Integer.parseInt(event.getValue("page").getAsString()));
				} catch (Exception ignored) {}
				try {
					player = usernameToUuid(event.getValue("player").getAsString());
					player = player.isValid() ? player : null;
				} catch (Exception ignored) {}

				if (rank != -1 || page != -1) {
					int clampedRank = (rank != -1 ? (rank - 1) / 20 : page - 1) * 20;
					leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, clampedRank - 200, clampedRank + 200));
					this.player = null;
				} else if (amount != -1) {
					leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, amount));
					this.player = null;
				} else if (player != null) {
					leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, player.uuid()));
					this.player = player.username();
					playerRank = -1;
					playerAmount = "None";
				}

				double closestAmt = -1;
				int idx = 1;
				for (Map.Entry<Integer, DataObject> entry : leaderboardCache.entrySet()) {
					int curRank = entry.getKey();
					double curAmount = entry.getValue().getDouble(lbType, 0.0);

					if (player != null && entry.getValue().getString("username", "").equals(player.username())) {
						playerRank = curRank;
						playerAmount = formatOrSimplify(curAmount);
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

				hook.editOriginalEmbeds(getRender().build()).setComponents(getActionRow()).queue(ignored -> waitForEvent(), ignore);
			});
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

	public void waitForEvent() {
		waiter.waitForEvent(
			GenericInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> {
				try {
					message.editMessageComponents().queue(ignore, ignore);
				} catch (Exception ignored) {}
			}
		);
	}
}
