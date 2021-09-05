package com.skyblockplus.dungeons;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class EssenceSlashCommand extends SlashCommand {

	public EssenceSlashCommand() {
		this.name = "essence";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "upgrade":
				event.embed(event.disabledCommandMessage());
				break;
			case "information":
				event.embed(EssenceCommand.getEssenceInformation(event.getOptionStr("item")));
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
