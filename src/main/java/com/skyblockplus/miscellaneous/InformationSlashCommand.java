package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class InformationSlashCommand extends SlashCommand {

	public InformationSlashCommand() {
		this.name = "information";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event
			.getHook()
			.editOriginalEmbeds(InformationCommand.getInformation(event.getSlashCommandClient().getStartTime()).build())
			.setActionRows(InformationCommand.getInformationActionRow())
			.queue();
	}
}
