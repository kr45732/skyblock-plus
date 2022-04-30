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
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class SlayerCommand extends Command {

	public SlayerCommand() {
		this.name = "slayer";
		this.aliases = new String[] { "slayers" };
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerSlayer(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			int svenOneKills = player.getSlayerBossKills("wolf", 0);
			int svenTwoKills = player.getSlayerBossKills("wolf", 1);
			int svenThreeKills = player.getSlayerBossKills("wolf", 2);
			int svenFourKills = player.getSlayerBossKills("wolf", 3);

			int revOneKills = player.getSlayerBossKills("zombie", 0);
			int revTwoKills = player.getSlayerBossKills("zombie", 1);
			int revThreeKills = player.getSlayerBossKills("zombie", 2);
			int revFourKills = player.getSlayerBossKills("zombie", 3);
			int revFiveKills = player.getSlayerBossKills("zombie", 4);

			int taraOneKills = player.getSlayerBossKills("spider", 0);
			int taraTwoKills = player.getSlayerBossKills("spider", 1);
			int taraThreeKills = player.getSlayerBossKills("spider", 2);
			int taraFourKills = player.getSlayerBossKills("spider", 3);

			int endermanOneKills = player.getSlayerBossKills("enderman", 0);
			int endermanTwoKills = player.getSlayerBossKills("enderman", 1);
			int endermanThreeKills = player.getSlayerBossKills("enderman", 2);
			int endermanFourKills = player.getSlayerBossKills("enderman", 3);

			int blazeOneKills = player.getSlayerBossKills("blaze", 0);
			int blazeTwoKills = player.getSlayerBossKills("blaze", 1);
			int blazeThreeKills = player.getSlayerBossKills("blaze", 2);
			int blazeFourKills = player.getSlayerBossKills("blaze", 3);

			String svenKills =
				"**Tier 1:** " +
				svenOneKills +
				"\n**Tier 2:** " +
				svenTwoKills +
				"\n**Tier 3:** " +
				svenThreeKills +
				"\n**Tier 4:** " +
				svenFourKills;

			String revKills =
				"**Tier 1:** " +
				revOneKills +
				"\n**Tier 2:** " +
				revTwoKills +
				"\n**Tier 3:** " +
				revThreeKills +
				"\n**Tier 4:** " +
				revFourKills +
				"\n**Tier 5:** " +
				revFiveKills;

			String taraKills =
				"**Tier 1:** " +
				taraOneKills +
				"\n**Tier 2:** " +
				taraTwoKills +
				"\n**Tier 3:** " +
				taraThreeKills +
				"\n**Tier 4:** " +
				taraFourKills;

			String endermanKills =
				"**Tier 1:** " +
				endermanOneKills +
				"\n**Tier 2:** " +
				endermanTwoKills +
				"\n**Tier 3:** " +
				endermanThreeKills +
				"\n**Tier 4:** " +
				endermanFourKills;

			String blazeKills =
				"**Tier 1:** " +
				blazeOneKills +
				"\n**Tier 2:** " +
				blazeTwoKills +
				"\n**Tier 3:** " +
				blazeThreeKills +
				"\n**Tier 4:** " +
				blazeFourKills;

			long coinsSpentOnSlayers =
				2000L *
				(svenOneKills + revOneKills + taraOneKills + endermanOneKills) +
				7500L *
				(svenTwoKills + revTwoKills + taraTwoKills + endermanTwoKills) +
				20000L *
				(svenThreeKills + revThreeKills + taraThreeKills + endermanThreeKills) +
				50000L *
				(svenFourKills + revFourKills + taraFourKills + endermanFourKills) +
				100000L *
				revFiveKills +
						5000L *
								blazeOneKills +
						20000L *
								blazeTwoKills +
						60000L *
								blazeThreeKills + 150000L * blazeFourKills; // TODO: check this
			eb.setDescription(
				"**Total Slayer:** " +
				formatNumber(player.getTotalSlayer()) +
				" XP\n**Total Coins Spent:** " +
				simplifyNumber(coinsSpentOnSlayers)
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("sven") + " Wolf (" + player.getSlayerLevel("sven") + ")",
				simplifyNumber(player.getSlayer("sven")) + " XP",
				true
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("rev") + " Zombie (" + player.getSlayerLevel("rev") + ")",
				simplifyNumber(player.getSlayer("rev")) + " XP",
				true
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("tara") + " Spider (" + player.getSlayerLevel("tara") + ")",
				simplifyNumber(player.getSlayer("tara")) + " XP",
				true
			);

			eb.addField("Boss Kills", svenKills, true);
			eb.addField("Boss Kills", revKills, true);
			eb.addField("Boss Kills", taraKills, true);

			eb.addField(
				SLAYER_EMOJI_MAP.get("enderman") + " Enderman (" + player.getSlayerLevel("enderman") + ")",
				simplifyNumber(player.getSlayer("enderman")) + " XP",
				true
			);
			eb.addField(
				SLAYER_EMOJI_MAP.get("blaze") + " Blaze (" + player.getSlayerLevel("blaze") + ")",
				simplifyNumber(player.getSlayer("blaze")) + " XP",
				true
			);
			eb.addBlankField(true);
			eb.addField("Boss Kills", endermanKills, true);
			eb.addField("Boss Kills", blazeKills, true);
			eb.addBlankField(true);
			eb.addBlankField(true);

			return eb;
		}
		return player.getFailEmbed();
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

					embed(getPlayerSlayer(player, args.length == 3 ? args[2] : null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
