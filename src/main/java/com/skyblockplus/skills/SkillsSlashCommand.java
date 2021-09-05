package com.skyblockplus.skills;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class SkillsSlashCommand extends SlashCommand {

	public SkillsSlashCommand() {
		this.name = "skills";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(SkillsCommand.getPlayerSkill(event.player, event.getOptionStr("profile")));
	}
}
