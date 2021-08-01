package com.skyblockplus.help;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class HelpSlashCommand extends SlashCommand {

	public HelpSlashCommand() {
		this.name = "help";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();
				EmbedBuilder eb = HelpCommand.getHelp(
					event.getOptionStr("page"),
					event.getMember(),
					null,
					event.getHook(),
					event.getGuild().getId()
				);
				if (eb != null) {
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
			}
		);
	}
}
