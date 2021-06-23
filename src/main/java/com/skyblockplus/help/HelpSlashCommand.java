package com.skyblockplus.help;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class HelpSlashCommand extends SlashCommand {

	public HelpSlashCommand() {
		this.name = "help";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();
				EmbedBuilder eb = HelpCommand.getHelp(event.getOptionStr("page"), event.getMember(), null, event.getHook());
				if (eb != null) {
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
			}
		)
			.start();
	}
}
