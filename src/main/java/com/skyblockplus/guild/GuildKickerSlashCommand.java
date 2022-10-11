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

package com.skyblockplus.guild;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class GuildKickerSlashCommand extends SlashCommand {

	public GuildKickerSlashCommand() {
		this.name = "guild-kicker";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			getGuildKicker(
				event.player,
				event.getOptionStr("requirements"),
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event.getOptionBoolean("key", false),
				event
			)
		);
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get helper which shows who to promote or demote in your guild")
			.addOption(OptionType.STRING, "requirements", "The requirements a player must meet", true)
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(
				new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
					.addChoice("All", "all")
					.addChoice("Ironman", "ironman")
					.addChoice("Stranded", "stranded")
			)
			.addOption(OptionType.BOOLEAN, "key", "If the API key for this server should be used for more updated results");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getGuildKicker(
		String username,
		String reqs,
		Player.Gamemode gamemode,
		boolean useKey,
		SlashCommandEvent event
	) {
		String[] reqsArr = reqs.split("] \\[");
		if (reqsArr.length > 5) {
			return invalidEmbed("You can only enter a maximum of 5 sets of requirements");
		}
		for (int i = 0; i < reqsArr.length; i++) {
			String[] indvReqs = reqsArr[i].replace("[", "").replace("]", "").split("\\s+");
			for (String indvReq : indvReqs) {
				String[] reqDashSplit = indvReq.split(":");
				if (reqDashSplit.length != 2) {
					return invalidEmbed(indvReq + " is an invalid requirement format");
				}

				if (
					!reqDashSplit[0].equals("slayer") &&
					!reqDashSplit[0].equals("skills") &&
					!reqDashSplit[0].equals("catacombs") &&
					!reqDashSplit[0].equals("weight")
				) {
					return invalidEmbed(indvReq + " is an invalid requirement type");
				}

				try {
					Double.parseDouble(reqDashSplit[1]);
				} catch (Exception e) {
					return invalidEmbed(indvReq + " is an invalid requirement value");
				}
			}

			reqsArr[i] = reqsArr[i].replace("[", "").replace("]", "");
		}

		String hypixelKey = null;
		if (useKey) {
			hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

			EmbedBuilder eb = checkHypixelKey(hypixelKey);
			if (eb != null) {
				return eb;
			}
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (!usernameUuidStruct.isValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}
		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuidStruct.uuid());
		if (!guildResponse.isValid()) {
			return invalidEmbed(guildResponse.failCause());
		}
		JsonElement guildJson = guildResponse.response();
		String guildId = higherDepth(guildJson, "_id").getAsString();

		if (hypixelGuildQueue.contains(guildId)) {
			return invalidEmbed("This guild is currently updating, please try again in a couple of seconds");
		}
		hypixelGuildQueue.add(guildId);
		List<DataObject> playerList = leaderboardDatabase.getCachedPlayers(
			List.of("slayer", "skills", "catacombs", "weight"),
			gamemode,
			streamJsonArray(higherDepth(guildJson, "members")).map(u -> higherDepth(u, "uuid", "")).collect(Collectors.toList()),
			hypixelKey,
			event
		);
		hypixelGuildQueue.remove(guildId);

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(20);

		for (DataObject guildMember : playerList) {
			double slayer = guildMember.getDouble("slayer");
			double skills = guildMember.getDouble("skills");
			double catacombs = guildMember.getDouble("catacombs");
			double weight = guildMember.getDouble("weight");

			boolean meetsReqs = false;

			for (String req : reqsArr) {
				String[] reqSplit = req.split("\\s+");
				double slayerReq = 0;
				double skillsReq = 0;
				double catacombsReq = 0;
				double weightReq = 0;
				for (String reqIndividual : reqSplit) {
					switch (reqIndividual.split(":")[0]) {
						case "slayer" -> slayerReq = Double.parseDouble(reqIndividual.split(":")[1]);
						case "skills" -> skillsReq = Double.parseDouble(reqIndividual.split(":")[1]);
						case "catacombs" -> catacombsReq = Double.parseDouble(reqIndividual.split(":")[1]);
						case "weight" -> weightReq = Double.parseDouble(reqIndividual.split(":")[1]);
					}
				}

				if (slayer >= slayerReq && Math.max(0, skills) >= skillsReq && catacombs >= catacombsReq && weight >= weightReq) {
					meetsReqs = true;
					break;
				}
			}

			if (!meetsReqs) {
				paginateBuilder.addItems(
					"• **" +
					guildMember.getString("username") +
					"** | Slayer: " +
					formatNumber(slayer) +
					" | Skills: " +
					roundAndFormat(skills) +
					" | Cata: " +
					roundAndFormat(catacombs) +
					" | Weight: " +
					roundAndFormat(weight)
				);
			}
		}

		paginateBuilder
			.getPaginatorExtras()
			.setEveryPageTitle("Guild Kick Helper")
			.setEveryPageText("**Total missing requirements:** " + paginateBuilder.size());

		event.paginate(paginateBuilder);
		return null;
	}
}
