package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class HypixelSlashCommand extends SlashCommand {

	public HypixelSlashCommand() {
		this.name = "hypixel";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();
				String subcommandName = event.getSubcommandName();
				String username = event.getOptionStr("player");
				EmbedBuilder eb;

				if (subcommandName.equals("player")) {
					eb = HypixelCommand.getHypixelStats(username);
				} else if (subcommandName.equals("parkour")) {
					eb = HypixelCommand.getParkourStats(username);
				} else {
					eb = event.invalidCommandMessage();
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		)
			.start();
	}
}
