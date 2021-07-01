package com.skyblockplus.skills;

import static com.skyblockplus.Main.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class SkillsSlashCommand extends SlashCommand {

	public SkillsSlashCommand() {
		this.name = "skills";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				String profileName = event.getOptionStr("profile");
				EmbedBuilder eb;
				if (profileName != null) {
					eb = SkillsCommand.getPlayerSkill(event.getOptionStr("player"), profileName);
				} else {
					eb = SkillsCommand.getPlayerSkill(event.getOptionStr("player"), null);
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
