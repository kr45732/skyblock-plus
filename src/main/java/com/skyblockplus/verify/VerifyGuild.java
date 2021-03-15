package com.skyblockplus.verify;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;

public class VerifyGuild {
    private final TextChannel messageChannel;
    private final Message originalMessage;

    public VerifyGuild(TextChannel messageChannel, Message originalMessage) {
        this.messageChannel = messageChannel;
        this.originalMessage = originalMessage;
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (!event.getChannel().getId().equals(messageChannel.getId())) {
            return;
        }

        if (event.getMessage().getId().equals(originalMessage.getId())) {
            return;
        }

        if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
            if (event.getAuthor().isBot()) {
                return;
            }

            if (!event.getMessage().getContentRaw().startsWith(BOT_PREFIX + "link ")) {
                event.getMessage().delete().queue();
                return;
            }
        }

        event.getMessage().delete().queueAfter(7, TimeUnit.SECONDS);
    }
}
