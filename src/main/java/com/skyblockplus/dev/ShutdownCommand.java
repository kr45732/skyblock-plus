package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;

public class ShutdownCommand extends Command {

	public ShutdownCommand() {
		this.name = "d-shutdown";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				logCommand();
				event.reactWarning();
				event.getJDA().shutdown();
			}
		}
			.submit();
	}
}
