package com.skyblockplus.link;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class LinkSlashCommand extends SlashCommand {

	public LinkSlashCommand() {
		this.name = "link";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();
				String linkOption = event.getOptionStr("player");
				EmbedBuilder eb = null;
				if (linkOption != null) {
					eb = LinkAccountCommand.linkAccount(linkOption, event.getUser(), event.getGuild());
				} else {
					eb = LinkAccountCommand.getLinkedAccount(event.getUser());
				}
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		)
			.start();
	}
}
