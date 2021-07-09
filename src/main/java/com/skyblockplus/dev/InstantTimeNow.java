package com.skyblockplus.dev;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.Instant;

public class InstantTimeNow extends Command {

	public InstantTimeNow() {
		this.name = "d-instant";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), getGuildPrefix(event.getGuild().getId()) + "d-instant");

				event.reply(defaultEmbed("Instant Time Now").setDescription(Instant.now().toString()).build());
			}
		);
	}
}
