package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class CategoriesCommand extends Command {

    public CategoriesCommand() {
        this.name = "categories";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String ebString = "";
        for (net.dv8tion.jda.api.entities.Category category : event.getGuild().getCategories()) {
            ebString += "\n " + category.getName() + " - " + category.getId();
        }

        eb = defaultEmbed("Guild Categories").setDescription(ebString.length() == 0 ? "None" : ebString);
        ebMessage.editMessage(eb.build()).queue();
    }
}
