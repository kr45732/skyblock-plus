package com.SkyblockBot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.io.IOException;

import com.SkyblockBot.Auction.*;
import com.SkyblockBot.Dungeons.*;
import com.SkyblockBot.Slayer.*;
import com.SkyblockBot.Guild.*;
import com.SkyblockBot.Miscellaneous.*;
import com.SkyblockBot.Skills.*;
import com.SkyblockBot.Apply.*;
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

                client.addCommands(new AboutCommand(), new SlayerCommands(), new HelpCommand(waiter),
                                new GuildCommands(waiter), new AuctionCommands(), new AuctionCommandsAlias(),
                                new BinCommands(), new SkillsCommands(), new CatacombsCommand(),
                                new CatacombsCommandAlias()

                );

                setBotSettings();
                JDABuilder.createDefault(botToken).setStatus(OnlineStatus.DO_NOT_DISTURB)
                                .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                                .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                                .addEventListeners(waiter, client.build())
                                .addEventListeners(new Apply("apply", "React to apply", "✅", "application")).build();
        }

}