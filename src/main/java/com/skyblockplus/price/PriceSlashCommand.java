package com.skyblockplus.price;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;

public class PriceSlashCommand extends SlashCommand {

    public PriceSlashCommand() {
        this.name = "price";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();

                    event.getHook().editOriginalEmbeds(PriceCommand.calculatePriceFromUuid(event.getOptionStr("uuid")).build()).queue();
                }
        )
                .start();
    }
}
