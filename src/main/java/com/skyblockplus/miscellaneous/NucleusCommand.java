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

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class NucleusCommand extends Command {

	public NucleusCommand() {
		this.name = "nucleus";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getNuc(String username) {
		Player player = new Player(username);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();
			int achievementCount = higherDepth(player.getHypixelPlayerJson(), "achievements.skyblock_crystal_nucleus", -1);
			Map<String, Integer> sbCounts = new HashMap<>();
			for (JsonElement profile : player.getProfileArray()) {
				if (higherDepth(profile, "members." + player.getUuid() + ".mining_core.crystals") != null) {
					sbCounts.put(
						higherDepth(profile, "cute_name", "null"),
						higherDepth(profile, "members." + player.getUuid() + ".mining_core.crystals")
							.getAsJsonObject()
							.entrySet()
							.stream()
							.map(m -> higherDepth(m.getValue(), "total_placed", -1))
							.filter(m -> m != -1)
							.min(Comparator.naturalOrder())
							.orElse(0)
					);
				}
			}
			int sbCount = sbCounts.values().stream().mapToInt(Integer::intValue).sum();

			eb.addField("Difference", formatNumber(Math.max(achievementCount, 0) - sbCount), false);
			eb.addField(
				"Achievement Nucleus Count",
				achievementCount != -1 ? formatNumber(achievementCount) : "Achievement not found in API",
				false
			);
			eb.addField(
				"Skyblock Nucleus Count",
				sbCount +
				"\n\n**Profiles:**\n" +
				sbCounts.entrySet().stream().map(e -> e.getKey() + " - " + e.getValue()).collect(Collectors.joining("\n• ", "• ", "")),
				false
			);
			return eb;
		}
		return invalidEmbed(player.getFailCause());
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getNuc(player));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
