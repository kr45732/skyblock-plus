package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BankSlashCommand extends SlashCommand {

	public BankSlashCommand() {
		this.name = "bank";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();

				String subcommandName = event.getEvent().getSubcommandName();
				String username = event.getOptionStr("player");
				String profileName = event.getOptionStr("profile");
				EmbedBuilder eb;

				if (subcommandName.equals("total")) {
					eb = BankCommand.getPlayerBalance(username, profileName);
				} else if (subcommandName.equals("history")) {
					eb = BankCommand.getPlayerBankHistory(username, profileName, event.getUser(), null, event.getHook());
				} else {
					eb = event.invalidCommandMessage();
				}

				if (eb != null) {
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
			}
		)
			.start();
	}
}
