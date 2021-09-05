package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class ProfilesSlashCommand extends SlashCommand {

	public ProfilesSlashCommand() {
		this.name = "profiles";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(ProfilesCommand.getPlayerProfiles(event.player, event.getUser(), null, event.getHook()));
	}
}
