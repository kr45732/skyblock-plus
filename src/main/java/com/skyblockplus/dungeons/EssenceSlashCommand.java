package com.skyblockplus.dungeons;

import static com.skyblockplus.utils.Utils.executor;
import static com.skyblockplus.utils.Utils.getEssenceCostsJson;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class EssenceSlashCommand extends SlashCommand {

	public EssenceSlashCommand() {
		this.name = "essence";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();
				String subcommandName = event.getSubcommandName();
				String itemName = event.getOptionStr("item");

				EmbedBuilder eb;
				if (subcommandName.equals("upgrade")) {
					eb = event.disabledCommandMessage();
				} else if (subcommandName.equals("information")) {
					eb = EssenceCommand.getEssenceInformation(itemName, getEssenceCostsJson());
				} else {
					eb = event.invalidCommandMessage();
				}

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
