package com.skyblockplus.link;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;

public class UnlinkSlashCommand extends SlashCommand {

	public UnlinkSlashCommand() {
		this.name = "unlink";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();
				event.getHook().editOriginalEmbeds(UnlinkAccountCommand.unlinkAccount(event.getUser()).build()).queue();
			}
		);
	}
}
