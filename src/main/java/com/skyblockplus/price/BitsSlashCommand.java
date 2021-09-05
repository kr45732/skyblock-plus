package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class BitsSlashCommand extends SlashCommand {

	public BitsSlashCommand() {
		this.name = "bits";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(BitsCommand.getBitPrices(event.getOptionStr("item")));
	}
}
