package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class InformationSlashCommand extends SlashCommand {

	public InformationSlashCommand() {
		this.name = "information";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> {
			event.logCommandGuildUserCommand();
			event
				.getHook()
				.editOriginalEmbeds(InformationCommand.getInformation(event.getSlashCommandClient().getStartTime()).build())
				.setActionRows(InformationCommand.getInformationActionRow())
				.queue();
		});
	}
}
