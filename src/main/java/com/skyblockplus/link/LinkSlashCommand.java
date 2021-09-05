package com.skyblockplus.link;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class LinkSlashCommand extends SlashCommand {

	public LinkSlashCommand() {
		this.name = "link";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		String linkOption = event.getOptionStr("player");
		event.embed(
			linkOption != null
				? LinkCommand.linkAccount(linkOption, event.getMember(), event.getGuild())
				: LinkCommand.getLinkedAccount(event.getUser())
		);
	}
}
