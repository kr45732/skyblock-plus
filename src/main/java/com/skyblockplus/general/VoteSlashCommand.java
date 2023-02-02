/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
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

package com.skyblockplus.general;

import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

@Component
public class VoteSlashCommand extends SlashCommand {

	public VoteSlashCommand() {
		this.name = "vote";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(
			new MessageEditBuilder()
				.setEmbeds(defaultEmbed("Upvote Links").build())
				.setActionRow(
					Button.link("https://top.gg/bot/796791167366594592/vote", "Top.gg"),
					Button.link("https://discordbotlist.com/bots/skyblock-plus/upvote", "Discord Bot List"),
					Button.link("https://discords.com/bots/bot/796791167366594592/vote", "Discords.com")
				)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands.slash(name, "Get links to upvote the bot");
	}
}
