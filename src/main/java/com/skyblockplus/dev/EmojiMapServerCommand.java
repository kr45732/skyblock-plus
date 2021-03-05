package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Emote;

import static com.skyblockplus.utils.Utils.defaultEmbed;

public class EmojiMapServerCommand extends Command {
    public EmojiMapServerCommand() {
        this.name = "e-map";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        String ebString = "";
        for (Emote emote : event.getGuild().getEmotes()) {
            ebString += ("emojiMap.put(\"" + emote.getName() + "\", \"\\" + emote.getAsMention() + "\");") + "\n";
        }
        event.reply(ebString);
    }
}