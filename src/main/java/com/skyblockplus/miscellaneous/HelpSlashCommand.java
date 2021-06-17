package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;

public class HelpSlashCommand extends SlashCommand {

    public HelpSlashCommand() {
        this.name = "help";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();
                    HelpCommand.getHelp(event.getOptionStrNotNull("page"), event.getMember(), null, event.getHook());
                }
        )
                .start();
    }
}
