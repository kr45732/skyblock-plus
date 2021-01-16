package com.SkyblockBot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.io.IOException;

import com.SkyblockBot.Auction.*;
import com.SkyblockBot.Slayer.*;
import com.SkyblockBot.Guild.*;
import com.SkyblockBot.Miscellaneous.*;
import static com.SkyblockBot.Miscellaneous.BotUtils.*;

public class Main {
        public static void main(String[] args)
                        throws IOException, LoginException, IllegalArgumentException, RateLimitedException {
                EventWaiter waiter = new EventWaiter();
                CommandClientBuilder client = new CommandClientBuilder();

                client.useDefaultGame();
                client.setOwnerId("385939031596466176");
                client.setEmojis("✅", "⚠️", "❌");
                client.useHelpBuilder(false);
                client.setPrefix("!");

                client.addCommands(new AboutCommand(), new SlayerCommands(), new Help(waiter),
                                new GuildCommands(waiter), new AuctionCommands(), new AuctionCommandsAlias(),
                                new BinCommands()

                );

                setBotSettings();
                JDABuilder.createDefault(botToken).setStatus(OnlineStatus.DO_NOT_DISTURB)
                                .setActivity(Activity.playing("Loading...")).addEventListeners(waiter, client.build())
                                .build();
        }

}