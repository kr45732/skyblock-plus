package com.skyblockplus.link;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class LinkSlashCommand extends SlashCommand {

	public LinkSlashCommand() {
		this.name = "link";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> {
			event.logCommandGuildUserCommand();
			String linkOption = event.getOptionStr("player");
			EmbedBuilder eb;
			if (linkOption != null) {
				eb = LinkCommand.linkAccount(linkOption, event.getMember(), event.getGuild());
			} else {
				eb = LinkCommand.getLinkedAccount(event.getUser());
			}
			event.getHook().editOriginalEmbeds(eb.build()).queue();
		});
	}
}
