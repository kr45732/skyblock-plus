package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.io.IOException;

import static com.skyblockplus.utils.Utils.*;

public class PlaceholderCommand extends Command {
    public PlaceholderCommand() {
        this.name = "d-placeholder";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        if(BOT_PREFIX.equals("+")){
            return;
        }

        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            ebMessage.editMessage(defaultEmbed("Done").build()).queue();
        }).start();
    }
}
