package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class InviteSlashCommand extends SlashCommand {

	public InviteSlashCommand() {
		this.name = "invite";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				event.getHook().editOriginalEmbeds(InviteCommand.getInvite().build()).queue();
			}
		);
	}
}
