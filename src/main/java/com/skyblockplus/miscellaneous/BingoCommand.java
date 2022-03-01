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

package com.skyblockplus.miscellaneous;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Constants.FETCHUR_ITEMS;
import static com.skyblockplus.utils.Utils.*;

public class BingoCommand extends Command {

	public static JsonElement bingoInfo;

	public BingoCommand() {
		this.name = "bingo";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();

		bingoInfo = getJson("https://api.hypixel.net/resources/skyblock/bingo");
	}

	public static EmbedBuilder getPlayerBingo(String username) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}

		JsonElement bingoJson = null;
		JsonArray bingoArr = new JsonArray();
		try{
			bingoJson = streamJsonArray(higherDepth( getJson("https://api.hypixel.net/skyblock/bingo?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuidStruct.uuid()), "events").getAsJsonArray()).filter(e -> higherDepth(e, "key", -1) == higherDepth(bingoInfo, "id", -1)).findFirst().orElse(null);
			bingoArr = higherDepth(bingoJson, "completed_goals").getAsJsonArray();
		}catch (Exception ignored){}

		EmbedBuilder eb = defaultEmbed(usernameUuidStruct.username(), skyblockStatsLink(usernameUuidStruct.username(), null));
		StringBuilder regGoals = new StringBuilder();
		StringBuilder communityGoals = new StringBuilder();
		for (JsonElement goal : higherDepth(bingoInfo, "goals").getAsJsonArray()) {
			if(higherDepth(goal, "progress", -1) != -1){
				long progress = higherDepth(goal, "progress").getAsLong();
				long nextTier = higherDepth(goal, "tiers.[0]", 0);
				for (JsonElement tier : higherDepth(goal, "tiers").getAsJsonArray()) {
					if(progress > tier.getAsInt()){
						nextTier = tier.getAsInt();
						break;
					}
				}
				communityGoals.append("\n➜ ").append(higherDepth(goal, "name").getAsString()).append(": ").append(formatNumber(higherDepth(goal, "progress").getAsLong())).append("/").append(formatNumber(nextTier)).append(" (").append(roundProgress((double) progress / nextTier)).append(")");
			}else {
				regGoals.append("\n").append(streamJsonArray(bingoArr).anyMatch(g -> g.getAsString().equals(higherDepth(goal, "id").getAsString())) ? "✅" : "❌").append(" ").append(higherDepth(goal, "name").getAsString()).append(": ").append(parseMcCodes(higherDepth(goal, "lore").getAsString()));

			}
		}
		eb.setDescription((bingoJson == null ? "**Note: no active bingo profile found**\n" : "") + "**Points:** " + higherDepth(bingoJson, "points", 0));
		eb.addField("Self Goals", regGoals.toString(), false);
		eb.addField("Community Goals", communityGoals.toString(), false);
		eb.setThumbnail(usernameUuidStruct.getAvatarlUrl());
		return eb;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
					return;
				}

				embed(getPlayerBingo(player));
			}
		}
			.queue();
	}
}
