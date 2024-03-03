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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.profilesCommandOption;
import static com.skyblockplus.utils.utils.HypixelUtils.bestiaryTierFromKills;
import static com.skyblockplus.utils.utils.JsonUtils.getBestiaryJson;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SelectMenuPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.springframework.stereotype.Component;

@Component
public class BestiarySlashCommand extends SlashCommand {

	public BestiarySlashCommand() {
		this.name = "bestiary";
	}

	public static EmbedBuilder getBestiary(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			// default is true since new profiles already use the new format (hence don't have this field)
			if (!higherDepth(player.profileJson(), "bestiary.migration", true)) {
				return errorEmbed(
					player.getEscapedUsername() +
					"'s bestiary is not migrated. Make sure this player has logged on after the" +
					" <t:1690898400:D> Skyblock update"
				);
			}

			Map<SelectOption, EmbedBuilder> pages = new LinkedHashMap<>();

			String bestiaryMilestone = roundAndFormat(player.getBestiaryLevel());
			String bestiaryTier = roundAndFormat(player.getBestiaryTier());
			for (Map.Entry<String, JsonElement> entry : getBestiaryJson().entrySet()) {
				if (entry.getKey().equals("brackets")) {
					continue;
				}

				String locationName = higherDepth(entry.getValue(), "name").getAsString();
				EmbedBuilder eb = player
					.defaultPlayerEmbed()
					.setDescription(
						"**Bestiary Milestone:** " +
						bestiaryMilestone +
						"\n**Bestiary Tier:** " +
						bestiaryTier +
						"\n**Location:** " +
						locationName
					);

				for (JsonElement mob : higherDepth(entry.getValue(), "mobs").getAsJsonArray()) {
					int kills = 0;
					int deaths = 0;
					for (JsonElement bestiaryName : higherDepth(mob, "mobs").getAsJsonArray()) {
						kills += higherDepth(player.profileJson(), "bestiary.kills." + bestiaryName.getAsString(), 0);
						deaths += higherDepth(player.profileJson(), "bestiary.deaths." + bestiaryName.getAsString(), 0);
					}

					int tier = bestiaryTierFromKills(kills, higherDepth(mob, "bracket").getAsInt(), higherDepth(mob, "cap").getAsInt());

					eb.addField(
						cleanMcCodes(higherDepth(mob, "name").getAsString()) + " (" + roundAndFormat(tier) + ")",
						"Kills: " + formatNumber(kills) + "\nDeaths: " + formatNumber(deaths),
						true
					);
				}

				pages.put(SelectOption.of(locationName, entry.getKey()), eb);
			}

			new SelectMenuPaginator("dynamic", new PaginatorExtras().setSelectPages(pages), event);
			return null;
		}
		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getBestiary(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's bestiary stats")
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
