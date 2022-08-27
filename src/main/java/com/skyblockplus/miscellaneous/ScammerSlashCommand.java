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

import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class ScammerSlashCommand extends SlashCommand {

	public ScammerSlashCommand() {
		this.name = "scammer";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getScammer(event.player));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Check if a player is marked as a scammer in SBZ's database")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getScammer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (!usernameUuid.isValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		JsonElement scammerJson = getScammerJson(usernameUuid.uuid());
		EmbedBuilder eb = defaultEmbed(usernameUuid.usernameFixed(), "https://mine.ly/" + usernameUuid.uuid())
			.setFooter("Scammer check powered by SkyBlockZ (discord.gg/skyblock)");
		if (scammerJson == null) {
			return eb.setDescription(
				client.getSuccess() +
				" This player is not marked as a scammer, however still exercise caution when trading with any player!"
			);
		}

		eb.setDescription(client.getError() + " This player **IS** marked as a scammer");
		eb.addField("Reason", higherDepth(scammerJson, "result.reason", "No reason provided"), false);
		if (higherDepth(scammerJson, "is_alt", false)) {
			eb.setDescription("This account **IS** marked as an alt");
		}
		if (higherDepth(scammerJson, "result.discord.[0]") != null) {
			eb.addField(
				"Discord Account(s)",
				streamJsonArray(higherDepth(scammerJson, "result.discord").getAsJsonArray())
					.map(e -> "<@" + e.getAsString() + ">")
					.collect(Collectors.joining(" ")),
				false
			);
		}
		if (higherDepth(scammerJson, "result.alts.[0]") != null) {
			eb.addField(
				"Minecraft Alt(s)",
				streamJsonArray(higherDepth(scammerJson, "result.alts").getAsJsonArray())
					.map(JsonElement::getAsString)
					.collect(Collectors.joining(" ")),
				false
			);
		}
		return eb;
	}
}
