/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022-2023 kr45732
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

package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.utils.StringUtils.getClosestMatch;
import static com.skyblockplus.utils.utils.Utils.ignore;

import java.util.List;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

public class AutoCompleteEvent extends CommandAutoCompleteInteractionEvent {

	public AutoCompleteEvent(CommandAutoCompleteInteractionEvent event) {
		super(event.getJDA(), event.getResponseNumber(), event.getInteraction());
	}

	public void replyClosestMatch(String toMatch, List<String> matchFrom) {
		List<String> matches = getClosestMatch(toMatch, matchFrom, 25);
		matches.removeIf(String::isEmpty);
		if (!matches.isEmpty()) {
			replyChoiceStrings(matches).queue(ignore, ignore);
		}
	}

	public void replyClosestPlayer() {
		replyChoiceStrings(leaderboardDatabase.getClosestPlayers(getOption("player").getAsString())).queue(ignore, ignore);
	}
}
