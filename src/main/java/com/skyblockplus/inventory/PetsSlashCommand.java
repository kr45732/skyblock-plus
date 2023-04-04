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

package com.skyblockplus.inventory;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.HypixelUtils.petLevelFromXp;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static com.skyblockplus.utils.utils.StringUtils.capitalizeString;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class PetsSlashCommand extends SlashCommand {

	public PetsSlashCommand() {
		this.name = "pets";
	}

	public static EmbedBuilder getPlayerPets(String username, String profileName, SlashCommandEvent event) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			List<String> missingPets = new ArrayList<>(PET_NAMES);
			List<String> petItems = new ArrayList<>();
			JsonArray playerPets = player.getPets();
			for (JsonElement pet : playerPets) {
				String petItem = null;
				try {
					petItem = higherDepth(pet, "heldItem").getAsString();
				} catch (Exception ignored) {}

				String petName = higherDepth(pet, "type").getAsString();
				missingPets.remove(petName);
				String rarity = higherDepth(pet, "tier").getAsString();

				petItems.add(
					getEmoji(petName + RARITY_TO_NUMBER_MAP.get(rarity)) +
					" " +
					capitalizeString(rarity) +
					" [Lvl " +
					petLevelFromXp(higherDepth(pet, "exp", 0L), rarity, petName) +
					"] " +
					capitalizeString(petName.toLowerCase().replace("_", " ")) +
					" " +
					(petItem != null ? getEmoji(petItem) : "")
				);
			}

			List<String> missingPetItems = new ArrayList<>();
			for (String missingPet : missingPets) {
				if (List.of("DROPLET_WISP", "FROST_WISP", "GLACIAL_WISP").contains(missingPet)) {
					continue;
				}

				String emoji = null;
				for (int i = 5; emoji == null && i >= 0; i--) {
					emoji = getEmoji(missingPet + ";" + i, null);
				}

				missingPetItems.add(emoji + " " + capitalizeString(missingPet.toLowerCase().replace("_", " ")));
			}

			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(25);
			paginateBuilder.addItems(petItems);

			if (!missingPetItems.isEmpty()) {
				paginateBuilder
					.getExtras()
					.addReactiveButtons(
						new PaginatorExtras.ReactiveButton(
							Button.primary("reactive_pets_show_missing", "Show Missing"),
							paginator -> {
								paginator.setStrings(missingPetItems);
								paginator
									.getExtras()
									.setEveryPageText("**Total Missing:** " + missingPetItems.size())
									.toggleReactiveButton("reactive_pets_show_missing", false)
									.toggleReactiveButton("reactive_pets_show_current", true);
							},
							true
						),
						new PaginatorExtras.ReactiveButton(
							Button.primary("reactive_pets_show_current", "Show Current"),
							paginator -> {
								paginator.setStrings(petItems);
								paginator
									.getExtras()
									.setEveryPageText(null)
									.toggleReactiveButton("reactive_pets_show_missing", true)
									.toggleReactiveButton("reactive_pets_show_current", false);
							},
							false
						)
					);
			}

			event.paginate(paginateBuilder);
			return null;
		}
		return player.getErrorEmbed();
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerPets(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's pets menu")
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
