package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class AuctionsSlashCommand extends SlashCommand {

	public AuctionsSlashCommand() {
		this.name = "auctions";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "player":
				if (event.invalidPlayerOption()) {
					return;
				}

				event.paginate(AuctionCommand.getPlayerAuction(event.player, event.getUser(), null, event.getHook()));
				break;
			case "uuid":
				event.embed(AuctionCommand.getAuctionByUuid(event.getOptionStr("uuid")));
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
