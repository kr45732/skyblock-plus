package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BidsSlashCommand extends SlashCommand {

	public BidsSlashCommand() {
		this.name = "bids";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();

				EmbedBuilder eb = BidsCommand.getPlayerBids(event.getOptionStr("player"));

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		)
			.start();
	}
}
