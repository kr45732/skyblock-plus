package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class CategoriesCommand extends Command {

    public CategoriesCommand() {
        this.name = "categories";
        this.cooldown = globalCooldown;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "categories");

        StringBuilder ebString = new StringBuilder();
        for (net.dv8tion.jda.api.entities.Category category : event.getGuild().getCategories()) {
            ebString.append("\n• ").append(category.getName()).append(" --> ").append(category.getId());
        }

        eb = defaultEmbed("Guild Categories").setDescription(ebString.length() == 0 ? "None" : ebString.toString());
        ebMessage.editMessage(eb.build()).queue();
    }
}
