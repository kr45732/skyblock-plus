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

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class NucleusCommand extends Command {

	public NucleusCommand() {
		this.name = "nucleus";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getNuc(String username) {
		Player.Profile player = Player.create(username);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		int achievementCount = higherDepth(player.getHypixelPlayerJson(), "achievements.skyblock_crystal_nucleus", -1);
		Map<String, Integer> sbCounts = new HashMap<>();
		for (Player.Profile profile : player.getProfiles().values()) {
			if (higherDepth(profile.getOuterProfileJson(), "members." + player.getUuid() + ".mining_core.crystals") != null) {
				sbCounts.put(
					profile.getProfileName(),
					higherDepth(profile.getOuterProfileJson(), "members." + player.getUuid() + ".mining_core.crystals")
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

		return player
			.defaultPlayerEmbed()
			.addField("Difference", formatNumber(Math.max(achievementCount, 0) - sbCount), false)
			.addField(
				"Achievement Nucleus Count",
				achievementCount != -1 ? formatNumber(achievementCount) : "Achievement not found in API",
				false
			)
			.addField(
				"Skyblock Nucleus Count",
				sbCount +
				"\n\n**Profiles:**\n" +
				sbCounts.entrySet().stream().map(e -> e.getKey() + " - " + e.getValue()).collect(Collectors.joining("\n• ", "• ", "")),
				false
			);
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
