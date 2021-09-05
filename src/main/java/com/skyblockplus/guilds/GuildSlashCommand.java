package com.skyblockplus.guilds;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class GuildSlashCommand extends SlashCommand {

	public GuildSlashCommand() {
		this.name = "guild";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		switch (event.getSubcommandName()) {
			case "player":
				event.embed(GuildCommand.getGuildPlayer(event.player));
				break;
			case "information":
				event.embed(GuildCommand.getGuildInfo(event.player));
				break;
			case "members":
				event.paginate(GuildCommand.getGuildMembersFromPlayer(event.player, event.getUser(), null, event.getHook()));
				break;
			case "experience":
				OptionMapping numDays = event.getEvent().getOption("days");
				event.paginate(
					GuildCommand.getGuildExpFromPlayer(
						event.player,
						numDays != null ? numDays.getAsLong() : 7,
						event.getUser(),
						null,
						event.getHook()
					)
				);
				break;
			default:
				event.embed(event.invalidCommandMessage());
		}
	}
}
