package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.time.Instant;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.logCommand;

public class InstantTimeNow extends Command {
    public InstantTimeNow() {
        this.name = "d-instant";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        logCommand(event.getGuild(), event.getAuthor(), "d-instant");

        event.reply(defaultEmbed("Instant Time Now").setDescription(Instant.now().toString()).build());
    }
}