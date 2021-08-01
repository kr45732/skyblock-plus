package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BankSlashCommand extends SlashCommand {

	public BankSlashCommand() {
		this.name = "bank";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				String subcommandName = event.getSubcommandName();
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
		);
	}
}
