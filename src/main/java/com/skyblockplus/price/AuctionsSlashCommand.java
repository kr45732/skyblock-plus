package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class AuctionsSlashCommand extends SlashCommand {

	public AuctionsSlashCommand() {
		this.name = "auctions";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();
				EmbedBuilder eb;
				String subcommandName = event.getSubcommandName();

				if (subcommandName.equals("player")) {
					eb = AuctionCommand.getPlayerAuction(event.getOptionStr("player"), event.getUser(), null, event.getHook());
				} else if (subcommandName.equals("uuid")) {
					eb = AuctionCommand.getAuctionByUuid(event.getOptionStr("uuid"));
				} else {
					eb = event.invalidCommandMessage();
				}

				if (eb != null) {
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
			}
		);
	}
}
