package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class BitsSlashCommand extends SlashCommand {

    public BitsSlashCommand() {
        this.name = "bits";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();

                    EmbedBuilder eb = BitsCommand.getBitPrices(event.getOptionStr("item"));

                    event.getHook().editOriginalEmbeds(eb.build()).queue();
                }
        )
                .start();
    }
}
