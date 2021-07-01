package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class MissingTalismansSlashCommand extends SlashCommand {

	public MissingTalismansSlashCommand() {
		this.name = "missing";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				EmbedBuilder eb = MissingTalismansCommand.getMissingTalismans(event.getOptionStr("player"), event.getOptionStr("profile"));

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
