package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class PartyFinderSlashCommand extends SlashCommand {

	public PartyFinderSlashCommand() {
		this.name = "partyfinder";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> {
			event.logCommandGuildUserCommand();

			String profileName = event.getOptionStr("profile");
			EmbedBuilder eb;
			if (profileName != null) {
				eb = PartyFinderCommand.getPlayerDungeonInfo(event.getOptionStr("player"), profileName);
			} else {
				eb = PartyFinderCommand.getPlayerDungeonInfo(event.getOptionStr("player"), null);
			}

			event.getHook().editOriginalEmbeds(eb.build()).queue();
		});
	}
}
