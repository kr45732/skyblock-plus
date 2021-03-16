package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class RemoveLinkedUserDev extends Command {
    public RemoveLinkedUserDev() {
        this.name = "d-unlink";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (args.length == 3) {
            ebMessage.editMessage(unlinkAccount(args[1], args[2]).build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }


    private EmbedBuilder unlinkAccount(String guildId, String discordId) {
        return defaultEmbed("API returned response code " + database.removeLinkedUser(guildId, discordId));
    }
}
