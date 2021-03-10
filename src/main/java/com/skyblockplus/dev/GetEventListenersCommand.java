package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import static com.skyblockplus.Main.jda;

public class GetEventListenersCommand extends Command {
    public GetEventListenersCommand() {
        this.name = "d-listeners";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        StringBuilder ebString = new StringBuilder();
        for (Object i : jda.getRegisteredListeners()) {
            ebString.append("\n• ").append(i);
        }

        event.getChannel().sendMessage(ebString.toString()).queue();
    }
}
