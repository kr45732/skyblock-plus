package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class BankSlashCommand extends SlashCommand {

	public BankSlashCommand() {
		this.name = "bank";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "total":
				event.embed(BankCommand.getPlayerBalance(event.player, event.getOptionStr("profile")));
				break;
			case "history":
				event.paginate(
					BankCommand.getPlayerBankHistory(event.player, event.getOptionStr("profile"), event.getUser(), null, event.getHook())
				);
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
