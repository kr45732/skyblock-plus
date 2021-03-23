package com.skyblockplus.dev;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.Utils.*;

public class DeleteMessagesCommand extends Command {
    public DeleteMessagesCommand() {
        this.name = "d-purge";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        if(args.length == 2){
            try {
                int messageCount = Math.min(Integer.parseInt(args[1]), 100);
                List<Message> toDelete =  event.getChannel().getHistory().retrievePast(messageCount).complete();
                event.getChannel().purgeMessages(toDelete);
                ebMessage = ebMessage.editMessage(defaultEmbed("Deleted " + messageCount + " messages").build()).complete();
                ebMessage.delete().queueAfter(5, TimeUnit.SECONDS);
                return;
            }catch (Exception e){
                ebMessage = ebMessage.editMessage(defaultEmbed("Invalid amount").build()).complete();
                ebMessage.delete().queueAfter(5, TimeUnit.SECONDS);
                return;
            }
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }
}
