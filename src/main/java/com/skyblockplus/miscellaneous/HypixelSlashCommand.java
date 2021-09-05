package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class HypixelSlashCommand extends SlashCommand {

	public HypixelSlashCommand() {
		this.name = "hypixel";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "player":
				event.embed(HypixelCommand.getHypixelStats(event.player));
				break;
			case "parkour":
				event.embed(HypixelCommand.getParkourStats(event.player));
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
