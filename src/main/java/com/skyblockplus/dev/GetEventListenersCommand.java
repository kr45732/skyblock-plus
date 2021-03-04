package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import static com.skyblockplus.Main.jda;

public class GetEventListenersCommand extends Command {
    public GetEventListenersCommand() {
        this.name = "event-listeners";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        String ebString = "";
        for (Object i : jda.getRegisteredListeners()) {
            ebString += "\n• " + i;
        }

        event.getChannel().sendMessage(ebString).queue();
    }
}
