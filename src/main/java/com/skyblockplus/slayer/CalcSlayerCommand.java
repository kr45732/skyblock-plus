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

package com.skyblockplus.slayer;

import static com.skyblockplus.utils.Constants.SLAYER_EMOJI_MAP;
import static com.skyblockplus.utils.Constants.SLAYER_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

public class CalcSlayerCommand extends Command {

	public CalcSlayerCommand() {
		this.name = "calcslayer";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getCalcSlayer(String username, String profileName, String slayerType, int targetLevel, long targetXp) {
		slayerType = slayerType.toLowerCase();
		if (!SLAYER_NAMES.contains(slayerType)) {
			return invalidEmbed("Invalid slayer type");
		}

		if (targetXp <= 0 && targetLevel <= 0) {
			return invalidEmbed("Target xp or target level must be provided and at least 1");
		}
		if (targetLevel != -1 && (targetLevel <= 0 || targetLevel > 9)) {
			return invalidEmbed("Target level must be between 1 and 9");
		}

		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			int curXp = player.getSlayer(slayerType);
			targetXp =
				targetLevel != -1
					? higherDepth(
						getLevelingJson(),
						"slayer_xp." +
						switch (slayerType) {
							case "sven" -> "wolf";
							case "rev" -> "zombie";
							case "tara" -> "spider";
							default -> slayerType;
						} +
						".[" +
						(targetLevel - 1) +
						"]"
					)
						.getAsLong()
					: targetXp;

			if (curXp >= targetXp) {
				return invalidEmbed("You already have " + roundAndFormat(targetXp) + " xp");
			}

			long xpNeeded = targetXp - curXp;
			JsonArray bossXpArr = higherDepth(getLevelingJson(), "slayer_boss_xp").getAsJsonArray();
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < (slayerType.equals("rev") ? 5 : 4); i++) {
				double xpPerBoss = bossXpArr.get(i).getAsInt();
				int killsNeeded = (int) Math.ceil(xpNeeded / xpPerBoss);
				long cost =
					killsNeeded *
					switch (i) {
						case 0 -> 2000L;
						case 1 -> 7500L;
						case 2 -> 20000L;
						case 3 -> 50000L;
						default -> 100000L;
					};

				out
					.append("\n")
					.append(SLAYER_EMOJI_MAP.get(slayerType))
					.append(" Tier ")
					.append(toRomanNumerals(i + 1).toUpperCase())
					.append(" ")
					.append(capitalizeString(slayerType))
					.append(": ")
					.append(formatNumber(killsNeeded))
					.append(" ($")
					.append(formatNumber(cost))
					.append(")");
			}

			return player
				.defaultPlayerEmbed()
				.setDescription(
					"**Current XP:** " +
					roundAndFormat(curXp) +
					"\n**Target XP:** " +
					roundAndFormat(targetXp) +
					"\n**XP Needed:** " +
					formatNumber(xpNeeded) +
					"\n**Bosses Needed:**" +
					out
				);
		}

		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				int level = getIntOption("level");
				int xp = getIntOption("xp");
				String type = getStringOption("type", "").toLowerCase();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					embed(getCalcSlayer(player, args.length == 3 ? args[2] : null, type, level, xp));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
