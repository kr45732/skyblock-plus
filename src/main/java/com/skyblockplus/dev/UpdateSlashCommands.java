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

package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class UpdateSlashCommands extends Command {

	public UpdateSlashCommands() {
		this.name = "d-slash";
		this.ownerCommand = true;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				if (args.length == 1) {
					jda
						.getGuildById(event.getGuild().getId())
						.updateCommands()
						.addCommands(generateSlashCommands())
						.queue(s ->
							event
								.getChannel()
								.sendMessageEmbeds(defaultEmbed("Success - added " + s.size() + " slash commands for this guild").build())
								.queue()
						);
				} else {
					if (args[1].equals("clear")) {
						jda.getGuildById(event.getGuild().getId()).updateCommands().queue();
						event.getChannel().sendMessageEmbeds(defaultEmbed("Success - cleared commands for this guild").build()).queue();
					} else if (args[1].equals("global")) {
						jda
							.getShardById(0)
							.updateCommands()
							.addCommands(generateSlashCommands())
							.queue(s ->
								event
									.getChannel()
									.sendMessageEmbeds(defaultEmbed("Success - added " + s.size() + " slash commands globally").build())
									.queue()
							);
					}
				}
			}
		}
			.queue();
	}

	private List<SlashCommandData> generateSlashCommands() {
		return slashCommandClient
			.getCommands()
			.stream()
			.map(c -> c.getFullCommandData().setGuildOnly(true))
			.collect(Collectors.toCollection(ArrayList::new));
	}
}
