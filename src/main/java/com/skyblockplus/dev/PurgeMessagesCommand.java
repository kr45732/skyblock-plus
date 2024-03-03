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

package com.skyblockplus.dev;

import static com.skyblockplus.utils.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.utils.Utils.defaultPerms;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PurgeMessagesCommand extends Command {

	public PurgeMessagesCommand() {
		this.name = "d-purge";
		this.ownerCommand = true;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(event) {
			@Override
			protected void execute() {
				if (args.length == 2) {
					try {
						int messageCount = Math.min(Integer.parseInt(args[1]), 100);
						event
							.getChannel()
							.getHistory()
							.retrievePast(messageCount)
							.queue(messages -> {
								event.getChannel().purgeMessages(messages);
								event
									.getChannel()
									.sendMessageEmbeds(defaultEmbed("Purged " + messageCount + " messages").build())
									.queue(m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
							});
					} catch (Exception ignored) {}
				}
			}
		};
	}
}
