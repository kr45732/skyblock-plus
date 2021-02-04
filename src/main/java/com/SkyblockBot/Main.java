package com.SkyblockBot;

import com.SkyblockBot.Apply.Apply;
import com.SkyblockBot.Auction.AuctionCommands;
import com.SkyblockBot.Auction.BinCommands;
import com.SkyblockBot.Dungeons.CatacombsCommand;
import com.SkyblockBot.Dungeons.EssenceCommand;
import com.SkyblockBot.Guilds.GuildCommands;
import com.SkyblockBot.Guilds.GuildLeaderboardCommand;
import com.SkyblockBot.Miscellaneous.*;
import com.SkyblockBot.Roles.RoleCommands;
import com.SkyblockBot.Skills.SkillsCommands;
import com.SkyblockBot.Slayer.SlayerCommands;
import com.SkyblockBot.TimeoutHandler.ChannelDeleter;
import com.SkyblockBot.TimeoutHandler.MessageTimeout;
import com.SkyblockBot.Verify.Verify;
import com.SkyblockBot.Weight.WeightCommand;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.SkyblockBot.Utils.BotUtils.*;

//@SpringBootApplication
public class Main {
    public static JDA jda;

    public static void main(String[] args) throws LoginException, IllegalArgumentException {
//        SpringApplication.run(com.SkyblockBot.Main.class, args);

        String botPrefix = getBotPrefix();

        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.setActivity(Activity.watching(botPrefix + "help"));
        client.setOwnerId("385939031596466176");
        client.setCoOwnerIds("413716199751286784", "726329299895975948", "488019433240002565", "632708098657878028");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix(botPrefix);

        client.addCommands(new AboutCommand(), new SlayerCommands(), new HelpCommand(waiter), new GuildCommands(waiter),
                new AuctionCommands(), new BinCommands(), new SkillsCommands(), new CatacombsCommand(),
                new ShutdownCommand(), new VersionCommand(), new RoleCommands(), new GuildLeaderboardCommand(),
                new EssenceCommand(), new CoinsCommand(waiter), new WardrobeCommand(waiter),
                new TalismanBagCommand(waiter), new InventoryCommand(waiter), new SacksCommand(waiter),
                new InviteCommand(), new WeightCommand()
        );
        setBotSettings(botPrefix);

        if(botPrefix.equals("/")) {
            jda = JDABuilder.createDefault(botToken).setStatus(OnlineStatus.DO_NOT_DISTURB).setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setActivity(Activity.playing("Loading...")).addEventListeners(waiter, client.build())
                    .addEventListeners(new Apply()).addEventListeners(new Verify())
                    .addEventListeners(new ChannelDeleter()).addEventListeners(new MessageTimeout()).build();
        }else{
            jda = JDABuilder.createDefault(botToken).setStatus(OnlineStatus.DO_NOT_DISTURB).setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setActivity(Activity.playing("Loading...")).addEventListeners(waiter, client.build())
                    .addEventListeners(new ChannelDeleter()).addEventListeners(new MessageTimeout()).build();
        }
        
        // TODO: better bin command (parsing of string)
        // TODO: add unimplemented commands in HelpCommand.java
        // TODO: weight command/leaderboard
        // TODO: /g kick command (factoring in lowest g exp + lowest stats)
        // TODO: fix guild exp (DO NOT USE A MAP because of duplicated values)
        // TODO: finish stats command
    }
}
