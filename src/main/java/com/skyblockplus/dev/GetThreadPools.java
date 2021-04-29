package com.skyblockplus.dev;

import static com.skyblockplus.Main.*;

import java.util.Set;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class GetThreadPools extends Command {
    public GetThreadPools() {
        this.name = "d-threads";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();

        String replyStr = "";
        replyStr += "Total thread count: " + threads.size() + "\n";
        for (Thread thread : threads) {
            replyStr += "• " + thread.getName() + " | " + thread.getId() + " | " + thread.isAlive() + "\n";
        }

        event.reply(replyStr);
    }

}
