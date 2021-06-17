package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;

public class InformationSlashCommand extends SlashCommand {

    public InformationSlashCommand() {
        this.name = "information";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();
                    event
                            .getHook()
                            .editOriginalEmbeds(InformationCommand.getInformation(event.getSlashCommandClient().getStartTime()).build())
                            .setActionRows(InformationCommand.getInformationActionRow())
                            .queue();
                }
        )
                .start();
    }
}
