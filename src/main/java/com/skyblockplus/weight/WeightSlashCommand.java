package com.skyblockplus.weight;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class WeightSlashCommand extends SlashCommand {

	public WeightSlashCommand() {
		this.name = "weight";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		switch (event.getSubcommandName()) {
			case "player":
				if (event.invalidPlayerOption()) {
					return;
				}

				event.embed(WeightCommand.getPlayerWeight(event.player, event.getOptionStr("profile")));
				break;
			case "calculate":
				event.embed(
					WeightCommand.calculateWeight(
						event.getOptionStr("skill_average"),
						event.getOptionStr("slayer"),
						event.getOptionStr("catacombs"),
						event.getOptionStr("average_class")
					)
				);
				break;
			default:
				event.embed(event.invalidCommandMessage());
				break;
		}
	}
}
