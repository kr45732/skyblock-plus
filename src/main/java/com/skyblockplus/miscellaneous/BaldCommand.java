package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.BotUtils.*;

public class BaldCommand extends Command {

    public BaldCommand() {
        this.name = "bald";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Checking if bald...", null);
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String[] args = event.getMessage().getContentRaw().split(" ");
        if(args.length != 2){
            eb = defaultEmbed("Invalid usage. Try `/bald [ign]`", null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        if(args[1].equalsIgnoreCase("crypticplasma") || args[1].equalsIgnoreCase("cryptlcplasma")) {
            ebMessage.editMessage(defaultEmbed(args[1] + " is not bald", null).build()).queueAfter(3, TimeUnit.SECONDS);
            return;
        }else if(args[1].equalsIgnoreCase("owlahk") || args[1].equalsIgnoreCase("owiahk") || args[1].equalsIgnoreCase("pinkishh") || args[1].equalsIgnoreCase("crowly") || args[1].equalsIgnoreCase("crowiy")){
            ebMessage.editMessage(defaultEmbed(args[1] + " is " + (new Random().nextDouble() >= 0.99? "not " : "") + "bald", null).build()).queueAfter(3, TimeUnit.SECONDS);
            return;
        }

        ebMessage.editMessage(defaultEmbed(args[1] + " is " + (new Random().nextDouble() >= 0.25? "not " : "") + "bald", null).build()).queueAfter(3, TimeUnit.SECONDS);

    }
}
