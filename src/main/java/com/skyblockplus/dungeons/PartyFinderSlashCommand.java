package com.skyblockplus.dungeons;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class PartyFinderSlashCommand extends SlashCommand {

	public PartyFinderSlashCommand() {
		this.name = "partyfinder";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();

				String profileName = event.getOptionStr("profile");
				EmbedBuilder eb;
				if (profileName != null) {
					eb = PartyFinderCommand.getPlayerDungeonInfo(event.getEvent().getOption("player").getAsString(), profileName);
				} else {
					eb = PartyFinderCommand.getPlayerDungeonInfo(event.getEvent().getOption("player").getAsString(), null);
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		)
			.start();
	}
}
