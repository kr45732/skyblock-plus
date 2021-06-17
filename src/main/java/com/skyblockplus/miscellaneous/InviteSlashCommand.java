package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;

public class InviteSlashCommand extends SlashCommand {

    public InviteSlashCommand() {
        this.name = "invite";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();

                    event.getHook().editOriginalEmbeds(InviteCommand.getInvite().build()).queue();
                }
        )
                .start();
    }
}
