package com.skyblockplus.skills;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class SkillsSlashCommand extends SlashCommand {

	public SkillsSlashCommand() {
		this.name = "skills";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		new Thread(
			() -> {
				event.logCommandGuildUserCommand();

				String profileName = event.getOptionStr("profile");
				EmbedBuilder eb;
				if (profileName != null) {
					eb = SkillsCommand.getPlayerSkill(event.getEvent().getOption("player").getAsString(), profileName);
				} else {
					eb = SkillsCommand.getPlayerSkill(event.getEvent().getOption("player").getAsString(), null);
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		)
			.start();
	}
}
