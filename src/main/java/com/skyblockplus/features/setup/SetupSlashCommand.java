package com.skyblockplus.features.setup;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class SetupSlashCommand extends SlashCommand {

	public SetupSlashCommand() {
		this.name = "setup";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.getHook().editOriginalEmbeds(SetupCommand.getSetupEmbed().build()).setActionRows(SetupCommand.getSetupActionRow()).queue();
	}
}
