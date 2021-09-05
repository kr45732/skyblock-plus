package com.skyblockplus.slayer;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class SlayerSlashCommand extends SlashCommand {

	public SlayerSlashCommand() {
		this.name = "slayer";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(SlayerCommand.getPlayerSlayer(event.player, event.getOptionStr("profile")));
	}
}
