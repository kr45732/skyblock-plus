package com.skyblockplus.price;

import static com.skyblockplus.Main.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class QueryAuctionsSlashCommand extends SlashCommand {

	public QueryAuctionsSlashCommand() {
		this.name = "query";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				EmbedBuilder eb = QueryAuctionCommand.queryAuctions(event.getOptionStr("item"));

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
