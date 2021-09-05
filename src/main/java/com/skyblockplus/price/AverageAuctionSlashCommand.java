package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class AverageAuctionSlashCommand extends SlashCommand {

	public AverageAuctionSlashCommand() {
		this.name = "average";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(AverageAuctionCommand.getAverageAuctionPrice(event.getOptionStr("item")));
	}
}
