package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
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
