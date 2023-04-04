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

import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.StringUtils.capitalizeString;
import static com.skyblockplus.utils.utils.StringUtils.formatNumber;
import static com.skyblockplus.utils.utils.Utils.*;

import com.skyblockplus.api.serversettings.automatedguild.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class CheckReqsSlashCommand extends SlashCommand {

	public CheckReqsSlashCommand() {
		this.name = "checkreqs";
	}

	public static EmbedBuilder getReqsCheck(String username, String profileName, SlashCommandEvent event) {
		List<AutomatedGuild> guilds = database.getAllGuildSettings(event.getGuild().getId());
		if (guilds != null) {
			guilds.removeIf(g -> g.getApplyReqs().isEmpty());
		}
		if (guilds == null || guilds.isEmpty()) {
			return errorEmbed("No automatic guilds with requirements setup");
		}

		Player.Profile player = Player.create(username, profileName);
		if (!player.isValid()) {
			return player.getErrorEmbed();
		}

		CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(PaginatorExtras.PaginatorType.EMBED_PAGES, event.getUser());

		for (AutomatedGuild guild : guilds) {
			EmbedBuilder eb = player.defaultPlayerEmbed(" | " + capitalizeString(guild.getGuildName().replace("_", " ")));
			for (ApplyRequirements applyReq : guild.getApplyReqs()) {
				long networthReq = 0;
				try {
					networthReq = Long.parseLong(applyReq.getNetworthReq());
				} catch (Exception ignored) {}

				String reqsStr =
					formatReq("Slayer", applyReq.getSlayerReq(), (double) player.getTotalSlayer()) +
					formatReq("Skills", applyReq.getSkillsReq(), player.getSkillAverage()) +
					formatReq(
						"Catacombs",
						applyReq.getCatacombsReq(),
						player.getCatacombs() != null ? player.getCatacombs().getProgressLevel() : null
					) +
					formatReq("Weight", applyReq.getWeightReq(), player.getWeight()) +
					formatReq("Lily Weight", applyReq.getLilyWeightReq(), player.getLilyWeight()) +
					formatReq("Level", applyReq.getLevelReq(), player.getLevel()) +
					formatReq("Networth", applyReq.getNetworthReq(), networthReq > 0 ? player.getNetworth() : null);

				eb.addField("Requirement", reqsStr, false);
			}
			paginateBuilder.getExtras().addEmbedPage(eb);
		}

		event.paginate(paginateBuilder);
		return null;
	}

	private static String formatReq(String name, String reqStr, Double playerValue) {
		long req = 0;
		try {
			req = Long.parseLong(reqStr);
		} catch (Exception ignored) {}

		return (
			req > 0
				? "\n" +
				(playerValue != null && playerValue >= req ? client.getSuccess() : client.getError()) +
				" " +
				name +
				" - " +
				formatNumber(req)
				: ""
		);
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getReqsCheck(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Check if a player meets any set automatic guild requirements")
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
