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
import static com.skyblockplus.utils.Utils.client;

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
public class CheckApiSlashCommand extends SlashCommand {

	public CheckApiSlashCommand() {
		this.name = "check-api";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getCheckApi(event.player, event.getOptionStr("profile")));
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Get which Skyblock APIs a player has enabled or disabled")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(profilesCommandOption);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getCheckApi(String username, String profileName) {
		Player.Profile player = Player.create(username, profileName);
		if (player.isValid()) {
			EmbedBuilder eb = player.defaultPlayerEmbed();

			boolean invEnabled = player.isInventoryApiEnabled();
			boolean bankEnabled = player.isBankApiEnabled();
			boolean collectionsEnabled = player.isCollectionsApiEnabled();
			boolean vaultEnabled = player.isVaultApiEnabled();
			boolean skillsEnabled = player.isSkillsApiEnabled();

			eb.setDescription(
				"**All APIs Enabled:** " +
				(invEnabled && bankEnabled && collectionsEnabled && vaultEnabled && skillsEnabled) +
				"\n" +
				(invEnabled ? client.getSuccess() : client.getError()) +
				" Inventory API" +
				"\n" +
				(bankEnabled ? client.getSuccess() : client.getError()) +
				" Bank API" +
				"\n" +
				(collectionsEnabled ? client.getSuccess() : client.getError()) +
				" Collections API" +
				"\n" +
				(skillsEnabled ? client.getSuccess() : client.getError()) +
				" Skills API" +
				"\n" +
				(vaultEnabled ? client.getSuccess() : client.getError()) +
				" Personal Vault API"
			);

			return eb;
		}
		return player.getFailEmbed();
	}
}
