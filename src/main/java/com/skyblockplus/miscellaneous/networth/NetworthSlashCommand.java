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

package com.skyblockplus.miscellaneous.networth;

import static com.skyblockplus.utils.Utils.globalCooldown;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class NetworthSlashCommand extends SlashCommand {

	public NetworthSlashCommand() {
		this.name = "networth";
		this.cooldown = globalCooldown + 1;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			new NetworthExecute()
				.setVerbose(event.getOptionBoolean("verbose", false))
				.getPlayerNetworth(event.player, event.getOptionStr("profile"), event)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Calculate a player's networth")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(Constants.profilesCommandOption)
			.addOption(OptionType.BOOLEAN, "verbose", "Links a detailed JSON with a breakdown of value of each item");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}
}
