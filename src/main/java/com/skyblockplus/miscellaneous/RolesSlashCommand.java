package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class RolesSlashCommand extends SlashCommand {

	public RolesSlashCommand() {
		this.name = "roles";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				EmbedBuilder eb = RoleCommands.updateRoles(event.getOptionStr("profile"), event.getGuild(), event.getMember());

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
