package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class QueryAuctionsSlashCommand extends SlashCommand {

	public QueryAuctionsSlashCommand() {
		this.name = "query";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(QueryAuctionCommand.queryAuctions(event.getOptionStr("item")));
	}
}
