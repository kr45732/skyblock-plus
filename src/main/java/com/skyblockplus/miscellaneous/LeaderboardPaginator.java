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
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorEvent;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bson.Document;

public class LeaderboardPaginator {

	private final Map<Integer, Document> leaderboardCache = new TreeMap<>();
	private final Message message;
	private final String lbType;
	private final Player.Gamemode gamemode;
	private final Player player;
	private final PaginatorEvent event;
	private int pageFirstRank;
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
		this.player = player;
		this.event = event;
		this.message = event.getLoadingMessage();

		if (rank != -1) {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, rank - 200, rank + 200));
		} else if (amount != -1) {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, amount));
		} else if (page != -1) {
			page = Math.max(1, page);
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, page * 20 - 200, page * 20 + 200));
		} else {
			leaderboardCache.putAll(leaderboardDatabase.getLeaderboard(lbType, gamemode, player.getUuid()));
			isPlayer = true;
		}

		double closestAmt = -1;
		int idx = 1;
		for (Map.Entry<Integer, Document> entry : leaderboardCache.entrySet()) {
			int curRank = entry.getValue().getInteger("rank");
			double curAmount = entry.getValue().get(lbType, 0.0);

			if (entry.getValue().get("username", "").equals(player.getUsername())) {
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
		} else {
			pageFirstRank = ((playerRank - 1) / 20) * 20 + 1;
		}

		event
			.getAction()
			.editMessageEmbeds(getRender().build())
			.setActionRow(
				Button
					.primary("leaderboard_paginator_left_button", Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>"))
					.withDisabled(pageFirstRank == 1),
				Button.primary("leaderboard_paginator_right_button", Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>"))
			)
			.get()
			.queue(ignored -> waitForEvent());
	}

	private EmbedBuilder getRender() {
		StringBuilder columnOne = new StringBuilder();
		StringBuilder columnTwo = new StringBuilder();
		for (int i = pageFirstRank; i < pageFirstRank + 20; i++) {
			Document curPlayer = leaderboardCache.getOrDefault(i, null);
			if (curPlayer != null) {
				double curAmount = curPlayer.get(lbType, 0.0);
				String out =
					"`" +
					i +
					")` " +
					fixUsername(curPlayer.get("username", "?")) +
					": " +
					(roundAndFormat(lbType.equals("networth") ? (long) curAmount : curAmount));

				if (i < pageFirstRank + 10) {
					columnOne.append(out).append("\n");
				} else {
					columnTwo.append(out).append("\n");
				}
			}
		}

		return defaultEmbed(
			"Global Leaderboard | " + capitalizeString(gamemode.toString()),
			"https://hypixel-leaderboard.senither.com/players"
		)
			.setDescription(
				isPlayer
					? "**Player:** " +
					player.getUsernameFixed() +
					"\n**Rank:** " +
					(playerRank == -1 ? "Not on leaderboard" : "#" + (playerRank)) +
					"\n**" +
					capitalizeString(lbType.replace("_", " ")) +
					":** " +
					playerAmount
					: ""
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
		}

		event
			.editMessageEmbeds(getRender().build())
			.setActionRow(
				Button
					.primary("leaderboard_paginator_left_button", Emoji.fromMarkdown("<:left_button_arrow:885628386435821578>"))
					.withDisabled(pageFirstRank == 1),
				Button.primary("leaderboard_paginator_right_button", Emoji.fromMarkdown("<:right_button_arrow:885628386578423908>"))
			)
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
}
