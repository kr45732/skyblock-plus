package com.skyblockplus.dev;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.executor;
import static com.skyblockplus.utils.Utils.logCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class ShutdownCommand extends Command {

	public ShutdownCommand() {
		this.name = "d-shutdown";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), getGuildPrefix(event.getGuild().getId()) + "d-shutdown");
				event.reactWarning();
				event.getJDA().shutdown();
			}
		);
	}
}
