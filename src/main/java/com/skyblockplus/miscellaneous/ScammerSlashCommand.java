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

import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.ApiHandler.uuidToUsername;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.JsonUtils.streamJsonArray;
import static com.skyblockplus.utils.utils.StringUtils.nameMcLink;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class ScammerSlashCommand extends SlashCommand {

	public ScammerSlashCommand() {
		this.name = "scammer";
	}

	public static EmbedBuilder getScammer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (!usernameUuid.isValid()) {
			return errorEmbed(usernameUuid.failCause());
		}

		JsonElement scammerJson = getScammerJson(usernameUuid.uuid());
		EmbedBuilder eb = defaultEmbed(usernameUuid.escapeUsername(), nameMcLink(usernameUuid.uuid()))
			.setFooter("Scammer check powered by SkyBlockZ (discord.gg/skyblock)");
		if (scammerJson == null) {
			return eb.setDescription(
				client.getSuccess() + " This player is not marked as a scammer, however still be cautious when trading" + " with any player"
			);
		}

		eb.setDescription(client.getError() + " This player **IS** marked as a scammer");
		eb.addField("Reason", higherDepth(scammerJson, "details.reason", "No reason provided"), false);
		if (higherDepth(scammerJson, "alt", false)) {
			eb.setDescription("This account **IS** marked as an alt");
		}
		if (!higherDepth(scammerJson, "details.discordIds").getAsJsonArray().isEmpty()) {
			JsonArray discordArr = higherDepth(scammerJson, "details.discordIds").getAsJsonArray();
			eb.addField(
				"Discord Account" + (discordArr.size() > 1 ? "s" : ""),
				streamJsonArray(discordArr).map(e -> "<@" + e.getAsString() + ">").collect(Collectors.joining(" ")),
				false
			);
		}
		if (!higherDepth(scammerJson, "details.alts").getAsJsonArray().isEmpty()) {
			JsonArray altsArr = higherDepth(scammerJson, "details.alts").getAsJsonArray();
			eb.addField(
				"Minecraft Alt" + (altsArr.size() > 1 ? "s" : ""),
				streamJsonArray(altsArr).map(e -> uuidToUsername(e.getAsString()).username()).collect(Collectors.joining(", ")),
				false
			);
		}
		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getScammer(event.player));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Check if a player is marked as a scammer in the SBZ database")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
