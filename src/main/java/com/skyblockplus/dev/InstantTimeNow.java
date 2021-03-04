package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Emote;

import java.time.Instant;

import static com.skyblockplus.utils.Utils.defaultEmbed;

public class InstantTimeNow extends Command {
    public InstantTimeNow() {
        this.name = "time-now";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        event.reply(defaultEmbed("Instant Time Now").setDescription(Instant.now().toString()).build());
    }
}