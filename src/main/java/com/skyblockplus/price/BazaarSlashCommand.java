package com.skyblockplus.price;

import static com.skyblockplus.utils.Utils.executor;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BazaarSlashCommand extends SlashCommand {

	public BazaarSlashCommand() {
		this.name = "bazaar";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(() -> {
			event.logCommandGuildUserCommand();

			EmbedBuilder eb = BazaarCommand.getBazaarItem(event.getOptionStr("item"));

			event.getHook().editOriginalEmbeds(eb.build()).queue();
		});
	}
}
