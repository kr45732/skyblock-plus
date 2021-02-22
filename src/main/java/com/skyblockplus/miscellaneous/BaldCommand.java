package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.Random;

import static com.skyblockplus.utils.BotUtils.*;

public class BaldCommand extends Command {

    public BaldCommand() {
        this.name = "bald";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Checking if bald...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String[] args = event.getMessage().getContentRaw().split(" ");
        if (args.length != 2) {
            eb = defaultEmbed("Invalid usage. Try `" + BOT_PREFIX + "bald @mention`");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        try {
            String id = args[1].replaceAll("[<@!>]", "");
            User user = event.getJDA().getUserById(id);
            if (user != null && !user.isBot()) {
                eb = defaultEmbed("Baldness Checker");
                if (user.getId().equals("385939031596466176")) {
                    eb.setDescription(user.getName() + " is not bald!");
                    eb.setImage(user.getAvatarUrl());
                    ebMessage.editMessage(eb.build()).queue();
                    event.getMessage().addReaction(":green_check_custom:799774962394988574").queue();
                    return;
                } else if (user.getId().equals("273873363926253568") || user.getId().equals("726329299895975948")
                        || user.getId().equals("370888656803594240")) {
                    if (new Random().nextDouble() >= 0.99) {
                        eb.setDescription(user.getName() + " is not bald!");
                        eb.setImage(user.getAvatarUrl());
                        ebMessage.editMessage(eb.build()).queue();
                        event.getMessage().addReaction(":green_check_custom:799774962394988574").queue();
                        return;
                    } else {
                        eb.setDescription("**WARNING** - " + user.getName() + " is bald!!!");
                        eb.setImage(user.getAvatarUrl());
                        eb.setColor(Color.red);
                        ebMessage.editMessage(eb.build()).queue();
                        event.getMessage().addReaction("⚠️").queue();
                        return;
                    }
                } else {
                    if (new Random().nextDouble() >= 0.5) {
                        eb.setDescription(user.getName() + " is not bald!");
                        eb.setImage(user.getAvatarUrl());
                        ebMessage.editMessage(eb.build()).queue();
                        event.getMessage().addReaction(":green_check_custom:799774962394988574").queue();
                        return;
                    } else {
                        eb.setDescription("**WARNING** - " + user.getName() + " is bald!!!");
                        eb.setImage(user.getAvatarUrl());
                        eb.setColor(Color.red);
                        ebMessage.editMessage(eb.build()).queue();
                        event.getMessage().addReaction("⚠️").queue();
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        ebMessage.editMessage(defaultEmbed("Invalid usage. Try `" + BOT_PREFIX + "bald @mention`").build()).queue();
    }
}
