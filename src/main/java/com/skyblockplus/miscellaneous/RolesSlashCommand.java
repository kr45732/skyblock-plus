package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class RolesSlashCommand extends SlashCommand {

	public RolesSlashCommand() {
		this.name = "roles";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		event.embed(RoleCommand.updateRoles(event.getOptionStr("profile"), event.getGuild(), event.getMember()));
	}
}
