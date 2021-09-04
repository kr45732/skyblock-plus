package com.skyblockplus.guilds;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class GuildSlashCommand extends SlashCommand {

	public GuildSlashCommand() {
		this.name = "guild";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> {
			event.logCommandGuildUserCommand();

			String subcommandName = event.getSubcommandName();
			EmbedBuilder eb;
			String username = event.getOptionStr("player");

			switch (subcommandName) {
				case "player":
					eb = GuildCommand.getGuildPlayer(username);
					break;
				case "information":
					eb = GuildCommand.getGuildInfo(username);
					break;
				case "members":
					eb = GuildCommand.getGuildMembersFromPlayer(username, event.getUser(), null, event.getHook());
					break;
				case "experience":
					OptionMapping numDays = event.getEvent().getOption("days");
					eb =
						GuildCommand.getGuildExpFromPlayer(
							username,
							numDays != null ? numDays.getAsLong() : 7,
							event.getUser(),
							null,
							event.getHook()
						);
					break;
				default:
					eb = event.invalidCommandMessage();
			}

			if (eb != null) {
				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		});
	}
}
