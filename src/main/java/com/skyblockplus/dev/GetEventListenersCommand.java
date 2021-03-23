package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.logCommand;

public class GetEventListenersCommand extends Command {
    public GetEventListenersCommand() {
        this.name = "d-listeners";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "d-listeners");

        StringBuilder ebString = new StringBuilder();
        for (Object i : jda.getRegisteredListeners()) {
            ebString.append("\n• ").append(i);
        }

        event.reply(ebString.toString());
    }
}
