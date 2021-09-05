package com.skyblockplus.dungeons;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class DungeonsSlashCommand extends SlashCommand {

	public DungeonsSlashCommand() {
		this.name = "dungeons";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			DungeonsCommand.getPlayerDungeons(event.player, event.getOptionStr("profile"), event.getUser(), null, event.getHook())
		);
	}
}
