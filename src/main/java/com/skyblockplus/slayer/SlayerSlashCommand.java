/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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
import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.StringUtils.formatNumber;
import static com.skyblockplus.utils.utils.StringUtils.simplifyNumber;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class SlayerSlashCommand extends SlashCommand {

	public SlayerSlashCommand() {
		this.name = "slayer";
	}

	public static EmbedBuilder getPlayerSlayer(String username, String profileName) {
		Player.Profile player = Player.create(username, profileName);
		if (!player.isValid()) {
			return player.getErrorEmbed();
		}

		EmbedBuilder eb = player.defaultPlayerEmbed();

		int wolfOneKills = player.getSlayerBossKills("wolf", 0);
		int wolfTwoKills = player.getSlayerBossKills("wolf", 1);
		int wolfThreeKills = player.getSlayerBossKills("wolf", 2);
		int wolfFourKills = player.getSlayerBossKills("wolf", 3);

		int zombieOneKills = player.getSlayerBossKills("zombie", 0);
		int zombieTwoKills = player.getSlayerBossKills("zombie", 1);
		int zombieThreeKills = player.getSlayerBossKills("zombie", 2);
		int zombieFourKills = player.getSlayerBossKills("zombie", 3);
		int zombieFiveKills = player.getSlayerBossKills("zombie", 4);

		int spiderOneKills = player.getSlayerBossKills("spider", 0);
		int spiderTwoKills = player.getSlayerBossKills("spider", 1);
		int spiderThreeKills = player.getSlayerBossKills("spider", 2);
		int spiderFourKills = player.getSlayerBossKills("spider", 3);

		int endermanOneKills = player.getSlayerBossKills("enderman", 0);
		int endermanTwoKills = player.getSlayerBossKills("enderman", 1);
		int endermanThreeKills = player.getSlayerBossKills("enderman", 2);
		int endermanFourKills = player.getSlayerBossKills("enderman", 3);

		int blazeOneKills = player.getSlayerBossKills("blaze", 0);
		int blazeTwoKills = player.getSlayerBossKills("blaze", 1);
		int blazeThreeKills = player.getSlayerBossKills("blaze", 2);
		int blazeFourKills = player.getSlayerBossKills("blaze", 3);

		int vampireOneKills = player.getSlayerBossKills("vampire", 0);
		int vampireTwoKills = player.getSlayerBossKills("vampire", 1);
		int vampireThreeKills = player.getSlayerBossKills("vampire", 2);
		int vampireFourKills = player.getSlayerBossKills("vampire", 3);
		int vampireFiveKills = player.getSlayerBossKills("vampire", 4);

		String wolfKills =
			"**Tier 1:** " +
			formatNumber(wolfOneKills) +
			"\n**Tier 2:** " +
			formatNumber(wolfTwoKills) +
			"\n**Tier 3:** " +
			formatNumber(wolfThreeKills) +
			"\n**Tier 4:** " +
			formatNumber(wolfFourKills);

		String zombieKills =
			"**Tier 1:** " +
			formatNumber(zombieOneKills) +
			"\n**Tier 2:** " +
			formatNumber(zombieTwoKills) +
			"\n**Tier 3:** " +
			formatNumber(zombieThreeKills) +
			"\n**Tier 4:** " +
			formatNumber(zombieFourKills) +
			"\n**Tier 5:** " +
			formatNumber(zombieFiveKills);

		String spiderKills =
			"**Tier 1:** " +
			formatNumber(spiderOneKills) +
			"\n**Tier 2:** " +
			formatNumber(spiderTwoKills) +
			"\n**Tier 3:** " +
			formatNumber(spiderThreeKills) +
			"\n**Tier 4:** " +
			formatNumber(spiderFourKills);

		String endermanKills =
			"**Tier 1:** " +
			formatNumber(endermanOneKills) +
			"\n**Tier 2:** " +
			formatNumber(endermanTwoKills) +
			"\n**Tier 3:** " +
			formatNumber(endermanThreeKills) +
			"\n**Tier 4:** " +
			formatNumber(endermanFourKills);

		String blazeKills =
			"**Tier 1:** " +
			formatNumber(blazeOneKills) +
			"\n**Tier 2:** " +
			formatNumber(blazeTwoKills) +
			"\n**Tier 3:** " +
			formatNumber(blazeThreeKills) +
			"\n**Tier 4:** " +
			formatNumber(blazeFourKills);

		String vampireKills =
			"**Tier 1:** " +
			formatNumber(vampireOneKills) +
			"\n**Tier 2:** " +
			formatNumber(vampireTwoKills) +
			"\n**Tier 3:** " +
			formatNumber(vampireThreeKills) +
			"\n**Tier 4:** " +
			formatNumber(vampireFourKills) +
			"\n**Tier 5:** " +
			formatNumber(vampireFiveKills);

		long coinsSpentOnSlayers =
			2000L * (wolfOneKills + zombieOneKills + spiderOneKills + endermanOneKills) +
			7500L * (wolfTwoKills + zombieTwoKills + spiderTwoKills + endermanTwoKills) +
			20000L * (wolfThreeKills + zombieThreeKills + spiderThreeKills + endermanThreeKills) +
			50000L * (wolfFourKills + zombieFourKills + spiderFourKills + endermanFourKills) +
			100000L * zombieFiveKills +
			10000L * blazeOneKills +
			25000L * blazeTwoKills +
			60000L * blazeThreeKills +
			150000L * blazeFourKills;
		long motesSpentOnSlayers =
			2000L * vampireOneKills +
			4000L * vampireTwoKills +
			5000L * vampireThreeKills +
			7000L * vampireFourKills +
			10000L * vampireFiveKills;

		eb.setDescription(
			"**Total Slayer:** " +
			formatNumber(player.getTotalSlayerXp()) +
			" XP\n**Total Coins Spent:** " +
			simplifyNumber(coinsSpentOnSlayers) +
			"\n**Total Motes Spent:** " +
			simplifyNumber(motesSpentOnSlayers)
		);
		eb.addField(
			SLAYER_EMOJI_MAP.get("wolf") + " Wolf (" + player.getSlayerLevel("wolf") + ")",
			simplifyNumber(player.getSlayerXp("wolf")) + " XP",
			true
		);
		eb.addField(
			SLAYER_EMOJI_MAP.get("zombie") + " Zombie (" + player.getSlayerLevel("zombie") + ")",
			simplifyNumber(player.getSlayerXp("zombie")) + " XP",
			true
		);
		eb.addField(
			SLAYER_EMOJI_MAP.get("spider") + " Spider (" + player.getSlayerLevel("spider") + ")",
			simplifyNumber(player.getSlayerXp("spider")) + " XP",
			true
		);

		eb.addField("Boss Kills", wolfKills, true);
		eb.addField("Boss Kills", zombieKills, true);
		eb.addField("Boss Kills", spiderKills, true);

		eb.addField(
			SLAYER_EMOJI_MAP.get("enderman") + " Enderman (" + player.getSlayerLevel("enderman") + ")",
			simplifyNumber(player.getSlayerXp("enderman")) + " XP",
			true
		);
		eb.addField(
			SLAYER_EMOJI_MAP.get("blaze") + " Blaze (" + player.getSlayerLevel("blaze") + ")",
			simplifyNumber(player.getSlayerXp("blaze")) + " XP",
			true
		);
		eb.addField(
			SLAYER_EMOJI_MAP.get("vampire") + " Vampire (" + player.getSlayerLevel("vampire") + ")",
			simplifyNumber(player.getSlayerXp("vampire")) + " XP",
			true
		);
		eb.addField("Boss Kills", endermanKills, true);
		eb.addField("Boss Kills", blazeKills, true);
		eb.addField("Boss Kills", vampireKills, true);

		return eb;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getPlayerSlayer(event.player, event.getOptionStr("profile")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get the slayer data of a player")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
