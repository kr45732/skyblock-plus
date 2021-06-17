package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.time.Instant;

import static com.skyblockplus.utils.Utils.*;

public class InstantTimeNow extends Command {

    public InstantTimeNow() {
        this.name = "d-instant";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(
                () -> {
                    logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "d-instant");

                    event.reply(defaultEmbed("Instant Time Now").setDescription(Instant.now().toString()).build());
                }
        )
                .start();
    }
}
