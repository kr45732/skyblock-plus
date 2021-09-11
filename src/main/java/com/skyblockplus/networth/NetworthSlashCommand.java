package com.skyblockplus.networth;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class NetworthSlashCommand extends SlashCommand {

	public NetworthSlashCommand() {
		this.name = "networth";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(
			new NetworthExecute()
				.setVerbose(event.getOptionBoolean("verbose", false))
				.getPlayerNetworth(event.player, event.getOptionStr("profile"))
		);
	}
}
