package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.loadingEmbed;

public class GetAllGuildsIn extends Command {
    public GetAllGuildsIn() {
        this.name = "d-servers";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        if(args.length == 2){
            if(args[1].equals("list")){
                EmbedBuilder eb1 = defaultEmbed("Server List");
                for (Guild guild: jda.getGuilds()) {
                    try{
                        List<Invite> invites = guild.retrieveInvites().complete();

                        if(invites.size() > 0){
                            eb1.addField(guild.getName(), "Invite Link: " + invites.get(0).getUrl() + "\nId: " + guild.getId() + "\nOwner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")", false);
                        }else{
                            eb1.addField(guild.getName(), "Invite Link: " + guild.getChannels().get(0).createInvite().setMaxAge(0)
                                    .complete().getUrl() + "\nId: " +guild.getId() + "\nOwner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")", false);
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                        eb1.addField(guild.getName(), "Id: " +guild.getId() + "\nOwner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")", false);
                    }
                }
                ebMessage.editMessage(eb1.build()).queue();
                return;
            }
        }

        ebMessage.editMessage(defaultEmbed("Invalid input").build()).queue();
    }
}
