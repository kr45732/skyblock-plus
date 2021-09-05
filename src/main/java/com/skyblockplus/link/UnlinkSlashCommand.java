package com.skyblockplus.link;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class UnlinkSlashCommand extends SlashCommand {

	public UnlinkSlashCommand() {
		this.name = "unlink";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();
		event.embed(UnlinkCommand.unlinkAccount(event.getUser()));
	}
}
