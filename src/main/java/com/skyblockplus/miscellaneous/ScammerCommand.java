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

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class ScammerCommand extends Command {

	public ScammerCommand() {
		this.name = "scammer";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getScammer(String username) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
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

		eb.setDescription(client.getError() + " This account **IS** marked as a scammer");
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getScammer(player));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
