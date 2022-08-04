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
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

@Component
public class GuildKickerCommand extends Command {

	public GuildKickerCommand() {
		this.name = "guild-kicker";
		this.cooldown = globalCooldown + 2;
		this.aliases = new String[] { "g-kicker" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getGuildKicker(
		String username,
		String reqs,
		Player.Gamemode gamemode,
		boolean useKey,
		PaginatorEvent event
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

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				Player.Gamemode gamemode = getGamemodeOption("mode", Player.Gamemode.ALL);
				boolean useKey = getBooleanOption("--key");

				if (args.length == 3 && args[1].toLowerCase().startsWith("u:")) {
					paginate(getGuildKicker(args[1].split(":")[1], args[2], gamemode, useKey, getPaginatorEvent()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
