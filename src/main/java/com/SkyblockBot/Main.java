package com.SkyblockBot;

import static com.SkyblockBot.Miscellaneous.BotUtils.botToken;
import static com.SkyblockBot.Miscellaneous.BotUtils.getBotPrefix;
import static com.SkyblockBot.Miscellaneous.BotUtils.setBotSettings;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import com.SkyblockBot.Apply.Apply;
import com.SkyblockBot.Auction.AuctionCommands;
import com.SkyblockBot.Auction.BinCommands;
import com.SkyblockBot.Dungeons.CatacombsCommand;
import com.SkyblockBot.Guilds.GuildCommands;
import com.SkyblockBot.Miscellaneous.AboutCommand;
import com.SkyblockBot.Miscellaneous.ChannelDeleter;
import com.SkyblockBot.Miscellaneous.HelpCommand;
import com.SkyblockBot.Miscellaneous.ShutdownCommand;
import com.SkyblockBot.Miscellaneous.VersionCommand;
import com.SkyblockBot.Skills.SkillsCommands;
import com.SkyblockBot.Slayer.SlayerCommands;
import com.SkyblockBot.Verify.Verify;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Main {
    public static void main(String[] args)
            throws IOException, LoginException, IllegalArgumentException, RateLimitedException {
        String botPrefix = getBotPrefix();

        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.useDefaultGame();
        client.setOwnerId("385939031596466176");
        client.setCoOwnerIds("413716199751286784", "726329299895975948", "488019433240002565", "632708098657878028");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix(botPrefix);

        client.addCommands(new AboutCommand(), new SlayerCommands(), new HelpCommand(waiter), new GuildCommands(waiter),
                new AuctionCommands(), new BinCommands(), new SkillsCommands(), new CatacombsCommand(),
                new ShutdownCommand(), new VersionCommand()

        );
        setBotSettings(botPrefix);
        JDABuilder.createDefault(botToken).setStatus(OnlineStatus.DO_NOT_DISTURB).setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("Loading...")).addEventListeners(waiter, client.build())
                .addEventListeners(new Apply()).addEventListeners(new ChannelDeleter()).addEventListeners(new Verify())
                .build();

        // TODO: better bin command (parsing of string)
        // TODO: add unimplemented commands in HelpCommand.java
        // TODO: speed up commands/reduce duplicate code
        // TODO: improve !cata/catacombs command
        // TODO: use last played on profile rather than oldest profile
        // TODO: weight command/leaderboard
        // TODO: /g kick command (factoring in lowest g exp + lowest stats)
        // TODO: give guild member role automatically
    }
}

/*
 * @Skyblock God 🙏 Top 5 in guild slayers/skill/cata lb - 782154976616382493
 * 
 * @Skyblock King 👑 Top 15 in guild slayers/skill/cata lb - 782154976616382492
 * 
 * @SBF Guild Member Being a guild member of SBF - 782154976625426469
 * 
 * @SBG Guild Member Being a guild member of SBG - 782154976625426468
 * 
 * @$$$$$ Having 100 million coins in the bank - 782154976608387081
 * 
 * @Multi-Millionaire Having 10 million coins in the bank - 782154976608387080
 * 
 * @Millionaire Having 1 millions coins in the bank - 782154976608387079
 * 
 * @Pet Enthusiast Having a lvl 100 epic or legendary pet (excluding alch. Pets
 * and ench. pets) - 782154976608387072
 * 
 * @Fairy Having maximum fairy souls (no dungeon souls) - 782154976595935250
 * 
 * @Minion's collector Having all minions in the game maxed (excluding slayer
 * and flower minions) - 782154976595935249
 * 
 * @Slot collector Having 20 or more minion slots (not counting minion slot
 * upgrades) - 782154976595935248
 * 
 * @Collector Having all collections in the game maxed (except for slayer
 * recipes)
 * 
 * @Doom Slayer (One lvl 9 Slayer)
 * 
 * @Alchemy 30
 * 
 * @Combat 30
 * 
 * @Fishing 30
 * 
 * @Farming 30
 * 
 * @Foraging 30
 * 
 * @Mining 30
 * 
 * @Taming 30
 * 
 * @Carpentry 30
 * 
 * @Enchanting 30
 * 
 * @Dungeoneering 20
 * 
 * @Alchemy 40
 * 
 * @Combat 40
 * 
 * @Fishing 40
 * 
 * @Farming 40
 * 
 * @Foraging 40
 * 
 * @Farming 40
 * 
 * @Taming 40
 * 
 * @Carpentry 40
 * 
 * @Enchanting 40
 * 
 * @Dungeoneering 25
 * 
 * @Alchemy 50
 * 
 * @Combat 50
 * 
 * @Fishing 50
 * 
 * @Farming 50
 * 
 * @Foraging 50
 * 
 * @Mining 50
 * 
 * @Taming 50
 * 
 * @Carpentry 50
 * 
 * @Enchanting 50
 * 
 * @Dungeoneering 30
 * 
 * @Enchanting 60
 * 
 * @Farming 60
 * 
 * @Sven 7
 * 
 * @Tara 7
 * 
 * @Rev 7
 * 
 * @Sven 8
 * 
 * @Tara 8
 * 
 * @Rev 8
 * 
 * @Sven 9
 * 
 * @Tara 9
 * 
 * @Rev 9
 */