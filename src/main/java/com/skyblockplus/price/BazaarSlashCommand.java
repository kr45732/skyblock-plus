package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BazaarSlashCommand extends SlashCommand {

    public BazaarSlashCommand() {
        this.name = "bazaar";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();

                    EmbedBuilder eb = BazaarCommand.getBazaarItem(event.getOptionStr("item"));

                    event.getHook().editOriginalEmbeds(eb.build()).queue();
                }
        )
                .start();
    }
}
