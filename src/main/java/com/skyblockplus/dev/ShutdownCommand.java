package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.logCommand;

public class ShutdownCommand extends Command {

    public ShutdownCommand() {
        this.name = "d-shutdown";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(
                () -> {
                    logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "d-shutdown");
                    event.reactWarning();
                    event.getJDA().shutdown();
                }
        )
                .start();
    }
}
