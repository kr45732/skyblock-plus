package com.skyblockplus.help;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class HelpSlashCommand extends SlashCommand {

	public HelpSlashCommand() {
		this.name = "help";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.paginate(HelpCommand.getHelp(event.getOptionStr("page"), event.getMember(), null, event.getHook(), event.getGuild().getId()));
	}
}
