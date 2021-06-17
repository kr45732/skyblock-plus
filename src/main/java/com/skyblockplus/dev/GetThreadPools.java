package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import static com.skyblockplus.utils.Utils.logCommand;

public class GetThreadPools extends Command {

    public GetThreadPools() {
        this.name = "d-threads";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

        event.getChannel().sendMessage("Total thread count: " + Thread.getAllStackTraces().size()).queue();
    }
}
