package com.skyblockplus.dungeons;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class PartyFinderSlashCommand extends SlashCommand {

	public PartyFinderSlashCommand() {
		this.name = "partyfinder";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(PartyFinderCommand.getPartyFinderInfo(event.player, event.getOptionStr("profile")));
	}
}
