package com.skyblockplus.link;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class UnlinkAccountCommand extends Command {

    public UnlinkAccountCommand() {
        this.name = "unlink";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();

            logCommand(event.getGuild(), event.getAuthor(), content);

            ebMessage.editMessage(unlinkAccount(event).build()).queue();
        }).start();
    }

    private EmbedBuilder unlinkAccount(CommandEvent event) {
        database.deleteLinkedUserByDiscordId(event.getAuthor().getId());
        return defaultEmbed("Success").setDescription("You were unlinked");
    }
}
