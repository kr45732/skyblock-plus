package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Emote;

public class EmojiMapServerCommand extends Command {
    public EmojiMapServerCommand() {
        this.name = "e-map";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        for(Emote emote: event.getGuild().getEmotes()){
            System.out.println("emojiMap.put(\"" + emote.getName() + "\", \"" + emote.getAsMention() + "\");");
        }
    }
}