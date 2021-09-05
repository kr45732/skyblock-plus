package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class BidsSlashCommand extends SlashCommand {

	public BidsSlashCommand() {
		this.name = "bids";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(BidsCommand.getPlayerBids(event.getOptionStr("player")));
	}
}
