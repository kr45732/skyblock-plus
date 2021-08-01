package com.skyblockplus.slayer;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class SlayerSlashCommand extends SlashCommand {

	public SlayerSlashCommand() {
		this.name = "slayer";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				String profileName = event.getOptionStr("profile");
				EmbedBuilder eb;
				if (profileName != null) {
					eb = SlayerCommand.getPlayerSlayer(event.getOptionStr("player"), profileName);
				} else {
					eb = SlayerCommand.getPlayerSlayer(event.getOptionStr("player"), null);
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
