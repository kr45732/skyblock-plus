package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class FetchurSlashCommand extends SlashCommand {

	public FetchurSlashCommand() {
		this.name = "fetchur";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(FetchurCommand.getFetchurItem());
	}
}
