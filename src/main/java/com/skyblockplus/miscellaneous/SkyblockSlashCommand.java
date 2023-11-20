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

import static com.skyblockplus.utils.ApiHandler.asyncUuidToUsername;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonObject;
import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class SkyblockSlashCommand extends SlashCommand {

	public SkyblockSlashCommand() {
		this.name = "skyblock";
	}

	public static EmbedBuilder getSkyblock(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginator = event.getPaginator(PaginatorExtras.PaginatorType.EMBED_PAGES);
			PaginatorExtras extras = paginator.getExtras();

			Map<String, CompletableFuture<String>> uuidToUsername = new HashMap<>();
			for (Player.Profile profile : player.getProfiles()) {
				EmbedBuilder eb = profile.defaultPlayerEmbed(" | " + capitalizeString(profile.getProfileName()));

				JsonObject membersObj = higherDepth(profile.getOuterProfileJson(), "members").getAsJsonObject();
				for (String uuid : membersObj.keySet()) {
					if (!uuidToUsername.containsKey(uuid)) {
						uuidToUsername.put(uuid, asyncUuidToUsername(uuid));
					}
				}

				long created = higherDepth(profile.profileJson(), "profile.first_join", -1L);
				eb.setDescription("Created: " + (created != -1 ? getRelativeTimestamp(created) : " Unknown"));
				if (profile.isSelected()) {
					eb.appendDescription("\nSelected: Yes");
				}
				eb.appendDescription("\nGamemode: " + profile.getGamemode().getName() + profile.getGamemode().getSymbol(" "));
				if (membersObj.size() > 1) {
					List<String> members = new ArrayList<>();
					for (String uuid : membersObj.keySet()) {
						try {
							members.add(escapeText(uuidToUsername.get(uuid).get()));
						} catch (Exception ignored) {}
					}
					eb.appendDescription("\nMembers: " + String.join(", ", members));
				}

				SenitherWeight weight = new SenitherWeight(profile, true);
				LilyWeight lilyWeight = new LilyWeight(profile, true);
				eb.addField(
					getEmoji("SAPLING") + " Skill Average",
					profile.getSkillAverage() == -1 ? "Skills API disabled" : roundAndFormat(profile.getSkillAverage()),
					true
				);
				eb.addField(getEmoji("OVERFLUX_CAPACITOR") + " Total Slayer XP", formatNumber(profile.getTotalSlayerXp()), true);
				eb.addField(
					DUNGEON_EMOJI_MAP.get("catacombs") + " Catacombs",
					roundAndFormat(profile.getCatacombs().getProgressLevel()),
					true
				);

				eb.addField("<:levels:1067859971221499954> Level", roundAndFormat(profile.getLevel()), true);
				eb.addField(
					getEmoji("TRAINING_WEIGHTS") + " Senither Weight",
					weight.getTotalWeight().getFormatted(false) + " (" + weight.getStage() + ")",
					true
				);
				eb.addField(
					getEmoji("TRAINING_WEIGHTS") + " Lily weight",
					lilyWeight.getTotalWeight().getFormatted(false) + " (" + lilyWeight.getStage() + ")",
					true
				);

				double profileNetworth = profile.getNetworth();
				eb.addField(
					getEmoji("ENCHANTED_GOLD") + " Networth",
					profileNetworth == -1 ? "Inventory API is disabled" : formatNumber((long) profileNetworth),
					true
				);
				eb.addField(
					getEmoji("PIGGY_BANK") + " Bank Balance",
					profile.getBankBalance() == -1 ? "Banking API disabled" : simplifyNumber(profile.getBankBalance()),
					true
				);
				eb.addField(getEmoji("ENCHANTED_GOLD") + " Purse Coins", simplifyNumber(profile.getPurseCoins()), true);

				eb.addField(getEmoji("COBBLESTONE") + " Maxed Collections", profile.getNumMaxedCollections() + "/73", true);
				eb.addField(SKILLS_EMOJI_MAP.get("combat") + " Bestiary", roundAndFormat(profile.getBestiaryLevel()), true);
				eb.addField(getEmoji("COBBLESTONE_GENERATOR_11") + " Minion Slots", "" + profile.getNumberMinionSlots() + "/26", true);

				if (profile.isSelected()) {
					extras.getEmbedPages().add(0, eb);
				} else {
					extras.addEmbedPage(eb);
				}
			}

			event.paginate(paginator);
			return null;
		}

		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getSkyblock(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get an overview of a player's Skyblock profiles")
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
