package com.skyblockplus.link;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.linkedaccounts.LinkedAccount;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class LinkAccountCommand extends Command {
    CommandEvent event;

    public LinkAccountCommand() {
        this.name = "link";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");
        this.event = event;

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (args.length == 2) {
            ebMessage.editMessage(linkAccount(args[1], event.getAuthor(), event.getGuild()).build()).queue();
            return;
        }else if(args.length == 1){
            ebMessage.editMessage(getLinkedAccount().build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getLinkedAccount() {
        JsonElement userInfo = database.getLinkedUser(event.getGuild().getId(), event.getAuthor().getId());

        if(!userInfo.isJsonNull()){
            return defaultEmbed("You are linked to " + uuidToUsername(higherDepth(userInfo, "minecraftUuid").getAsString()));
        }else{
            return defaultEmbed("You are not linked");
        }
    }

    public static EmbedBuilder linkAccount(String username, User user, Guild guild) {
        String[] playerInfo = getPlayerDiscordInfo(username);
        if (playerInfo != null && playerInfo[0] != null) {
            if (!user.getAsTag().equals(playerInfo[0])) {
                EmbedBuilder eb = defaultEmbed("Discord tag mismatch");
                eb.setDescription("Account " + playerInfo[1] + " is linked with the discord tag "
                        + playerInfo[0] + "\nYour current discord tag is " + user.getAsTag());
                return eb;
            }

            LinkedAccount toAdd = new LinkedAccount(user.getId(), playerInfo[2]);

            if (database.addLinkedUser(guild.getId(), toAdd) == 200) {
if(!guild.getId().equals("794733014248587274")){

try{
                guild.getMember(user).modifyNickname(playerInfo[1]).queue();} catch (Exception ignored){}
}
                return defaultEmbed("Success").setDescription("Account " + playerInfo[1] + " linked with " + user.getAsTag());
            } else {
                return defaultEmbed("Error linking " + playerInfo[1]);
            }
        }

        return defaultEmbed("Error finding discord tag linked with account");
    }
}
