package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class BinSlashCommand extends SlashCommand {

	public BinSlashCommand() {
		this.name = "bin";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(BinCommand.getLowestBin(event.getOptionStr("item")));
	}
}
