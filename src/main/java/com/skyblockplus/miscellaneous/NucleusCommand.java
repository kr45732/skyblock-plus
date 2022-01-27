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

package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.HypixelPlayer;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import static com.skyblockplus.utils.Utils.*;

public class NucleusCommand extends Command {

	public NucleusCommand() {
		this.name = "nucleus";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getNuc(String username) {
		HypixelPlayer player = new HypixelPlayer(username);
		if (!player.isNotValid()) {
			EmbedBuilder eb = player.getDefaultEmbed();
			String nuc = "Not found api";
			try{nuc = "" + player.get("achievements.skyblock_crystal_nucleus").getAsInt();}catch (Exception ignored){};
			eb.addField("Skyblock Crystal Nucleus", nuc, false);

			return eb;
		}
		return defaultEmbed(player.getFailCause());
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

					embed(getNuc(player));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
