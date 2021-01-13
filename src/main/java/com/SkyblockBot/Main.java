package com.SkyblockBot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;

public class Main {
        public static void main(String[] args)
                        throws IOException, LoginException, IllegalArgumentException, RateLimitedException {
                EventWaiter waiter = new EventWaiter();
                CommandClientBuilder client = new CommandClientBuilder();

                client.useDefaultGame();
                client.setOwnerId("0280");
                client.setEmojis("✅", "⚠️", "❌");
                client.useHelpBuilder(false);
                client.setPrefix("!");

                client.addCommands(
                                new AboutCommand(new Color(9, 92, 13), "an all purpose skyblock bot",
                                                new String[] { "Skyblock Slayer", "Skyblock Skill Average",
                                                                "Skyblock Dungeons", "Skyblock Guild" }), // TODO: make
                                                                                                          // custom
                                                                                                          // about
                                                                                                          // command
                                new SlayerCommands(), // TODO: work on slayer commands
                                new Help(waiter), new GuildCommands(waiter), // TODO: work on guild commands
                                new AuctionCommands(), new AuctionCommandsAlias()

                );

                JDABuilder.createDefault(System.getenv("BOT_TOKEN"))// "Nzk2NzkxMTY3MzY2NTk0NTky.X_dDmQ.zOZSNLa6s1fBzyUD2jQtjOsX-a8")
                                .setStatus(OnlineStatus.DO_NOT_DISTURB).setActivity(Activity.playing("Loading..."))
                                .addEventListeners(waiter, client.build()).build();
        }
}