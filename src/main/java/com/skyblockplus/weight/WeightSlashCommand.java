package com.skyblockplus.weight;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class WeightSlashCommand extends SlashCommand {

	public WeightSlashCommand() {
		this.name = "weight";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();
				String subcommandName = event.getSubcommandName();
				EmbedBuilder eb;

				if (subcommandName.equals("player")) {
					eb = WeightCommand.getPlayerWeight(event.getOptionStr("player"), event.getOptionStr("profile"));
				} else if (subcommandName.equals("calculate")) {
					eb =
						WeightCommand.calculateWeight(
							event.getOptionStr("skill_average"),
							event.getOptionStr("slayer"),
							event.getOptionStr("catacombs"),
							event.getOptionStr("average_class")
						);
				} else {
					eb = event.invalidCommandMessage();
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
