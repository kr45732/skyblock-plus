package com.skyblockplus.inventory;

import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class ArmorSlashCommand extends SlashCommand {

	public ArmorSlashCommand() {
		this.name = "armor";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(
			ArmorCommand.getPlayerEquippedArmor(event.player, event.getOptionStr("profile"), event.getUser(), null, event.getHook())
		);
	}
}
