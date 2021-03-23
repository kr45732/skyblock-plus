package com.skyblockplus.skyblockevent;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.eventlisteners.AutomaticGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Map;

import static com.skyblockplus.eventlisteners.MainListener.getGuildMap;
import static com.skyblockplus.utils.Utils.*;

public class SkyblockEventCommand extends Command {

    public SkyblockEventCommand() {
        this.name = "event";
        this.cooldown = globalCooldown;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (args.length == 2) {
            if (args[1].equals("create")) {
                Map<String, AutomaticGuild> guildMap = getGuildMap();
                if (guildMap.containsKey(event.getGuild().getId())) {
                    eb = defaultEmbed("Create a Skyblock competition");
                    eb.setDescription("Please enter the name of the guild you want to track");
                    ebMessage.editMessage(eb.build()).queue();

                    guildMap.get(event.getGuild().getId()).createSkyblockEvent(event);
                } else {
                    ebMessage.editMessage(defaultEmbed("Error - no guild").build()).queue();

                }
                return;
            }else if(args[1].equals("list")){
                Map<String, AutomaticGuild> guildMap = getGuildMap();
                if (guildMap.containsKey(event.getGuild().getId())) {
                    if(guildMap.get(event.getGuild().getId()).skyblockEventEnabled()){
                        ebMessage.editMessage(defaultEmbed("Found event running").build()).queue();
                    }else{
                        ebMessage.editMessage(defaultEmbed("No events running").build()).queue();
                    }
                }else{
                    ebMessage.editMessage(defaultEmbed("Error - no guild").build()).queue();
                }
                return;
            }
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

}
