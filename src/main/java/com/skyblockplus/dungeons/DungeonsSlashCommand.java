package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class DungeonsSlashCommand extends SlashCommand {

	public DungeonsSlashCommand() {
		this.name = "dungeons";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				String profileName = event.getOptionStr("profile");
				EmbedBuilder eb = DungeonsCommand.getPlayerDungeons(
					event.getOptionStr("player"),
					profileName,
					event.getUser(),
					null,
					event.getHook()
				);

				if (eb != null) {
					event.getHook().editOriginalEmbeds(eb.build()).queue();
				}
			}
		);
	}
}
