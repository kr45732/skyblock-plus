package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class CalculateSlashCommand extends SlashCommand {

	public CalculateSlashCommand() {
		this.name = "calculate";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(CalculateCommand.calculatePriceFromUuid(event.getOptionStr("uuid")));
	}
}
