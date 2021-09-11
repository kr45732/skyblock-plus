package com.skyblockplus.inventory;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class SacksSlashCommand extends SlashCommand {

	public SacksSlashCommand() {
		this.name = "sacks";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(SacksCommand.getPlayerSacks(event.player, event.getOptionStr("profile"), event.getUser(), null, event.getHook()));
	}
}
