package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class PlaceholderCommand extends Command {
    public PlaceholderCommand() {
        this.name = "d-placeholder";
        this.ownerCommand = true;
        this.aliases = new String[]{"ph"};
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            switch (args[1]) {
                case "get": {
                    Guild guild = jda.getGuildById(args[2]);
                    List<TextChannel> channels = guild.getTextChannels();
                    System.out.println(guild.getName() + " - " + guild.getId() + " - " + guild.getMemberCount());
                    for (TextChannel channel : channels) {
                        System.out.println(channel.getName() + " - " + channel.getId());
                    }
                    break;
                }
                case "preview": {
                    Guild guild = jda.getGuildById("796790757947867156");
                    TextChannel channel = guild.getTextChannelById("799048209929863168");
                    eb = defaultEmbed("Important Information");
                    eb.setDescription("Hello,\n" + "I've noticed that this server is just you (the server owner) and me.\n"
                            + "While I am waiting for verification I would like to let servers with more members be able to use the bot\n"
                            + "Sorry for any inconvenience this may have caused.\n"
                            + "Feel free to invite me back once I'm verified!");
                    channel.sendMessage(guild.getOwner().getAsMention()).queue();
                    channel.sendMessage(eb.build()).queue();
                    break;
                }
                case "send": {
                    Guild guild = jda.getGuildById(args[2]);
                    TextChannel channel = guild.getTextChannelById(args[3]);
                    eb = defaultEmbed("Important Information");
                    eb.setDescription("Hello,\n" + "I've noticed that this server is just you (the server owner) and me.\n"
                            + "While I am waiting for verification I would like to let servers with more members be able to use the bot\n"
                            + "Sorry for any inconvenience this may have caused.\n"
                            + "Feel free to invite me back once I'm verified!");
                    channel.sendMessage(guild.getOwner().getAsMention()).queue();
                    channel.sendMessage(eb.build()).complete();
                    guild.leave().complete();
                    System.out.println("Left " + guild.getName() + " - " + guild.getId());
                    break;
                }
                case "send_silent": {
                    Guild guild = jda.getGuildById(args[2]);
                    guild.leave().complete();
                    System.out.println("Left " + guild.getName() + " - " + guild.getId());
                    break;
                }
                case "list":
                    List<Guild> guilds = new LinkedList<>(jda.getGuilds());

                    guilds.sort(Comparator.comparingInt(Guild::getMemberCount));

                    for (Guild guild : guilds) {
                        if (guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
                            continue;
                        }

                        System.out.println(guild.getName() + " (" + guild.getMemberCount() + ") | Id: " + guild.getId()
                                + " | Owner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")");
                    }

                    break;
            }

            ebMessage.editMessage(defaultEmbed("Done").build()).queue();
        }).start();
    }
}