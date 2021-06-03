package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class ProfilesSlashCommand extends SlashCommand {

	public ProfilesSlashCommand() {
		this.name = "profiles";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();

				EmbedBuilder eb = ProfilesCommand.getPlayerProfiles(event.getOptionStr("player"), event.getUser(), null, event.getHook());

				if (eb != null) {
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
			}
		)
			.start();
	}
}
