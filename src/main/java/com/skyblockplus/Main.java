package com.skyblockplus;

import static com.skyblockplus.utils.BotUtils.BOT_PREFIX;
import static com.skyblockplus.utils.BotUtils.BOT_TOKEN;
import static com.skyblockplus.utils.BotUtils.setApplicationSettings;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.apply.Apply;
import com.skyblockplus.auction.AuctionCommands;
import com.skyblockplus.auction.BinCommands;
import com.skyblockplus.dungeons.CatacombsCommand;
import com.skyblockplus.dungeons.EssenceCommand;
import com.skyblockplus.guilds.GuildCommands;
import com.skyblockplus.guilds.GuildLeaderboardCommand;
import com.skyblockplus.miscellaneous.AboutCommand;
import com.skyblockplus.miscellaneous.CoinsCommand;
import com.skyblockplus.miscellaneous.HelpCommand;
import com.skyblockplus.miscellaneous.InventoryCommand;
import com.skyblockplus.miscellaneous.InviteCommand;
import com.skyblockplus.miscellaneous.SacksCommand;
import com.skyblockplus.miscellaneous.ShutdownCommand;
import com.skyblockplus.miscellaneous.TalismanBagCommand;
import com.skyblockplus.miscellaneous.VersionCommand;
import com.skyblockplus.miscellaneous.WardrobeCommand;
import com.skyblockplus.reload.ReloadEventWatcher;
import com.skyblockplus.roles.RoleCommands;
import com.skyblockplus.skills.SkillsCommands;
import com.skyblockplus.slayer.SlayerCommands;
import com.skyblockplus.timeout.ChannelDeleter;
import com.skyblockplus.timeout.MessageTimeout;
import com.skyblockplus.verify.Verify;
import com.skyblockplus.weight.WeightCommand;

//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

//@SpringBootApplication
public class Main {
    public static JDA jda;

    public static void main(String[] args) throws LoginException, IllegalArgumentException {
        setApplicationSettings();

        //SpringApplication.run(com.skyblockplus.Main.class, args);

        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.setActivity(Activity.watching(BOT_PREFIX + "help"));
        client.setOwnerId("385939031596466176");
        client.setCoOwnerIds("413716199751286784", "726329299895975948", "488019433240002565", "632708098657878028");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix(BOT_PREFIX);

        client.addCommands(new AboutCommand(), new SlayerCommands(), new HelpCommand(waiter), new GuildCommands(waiter),
                new AuctionCommands(), new BinCommands(), new SkillsCommands(), new CatacombsCommand(),
                new ShutdownCommand(), new VersionCommand(), new RoleCommands(), new GuildLeaderboardCommand(),
                new EssenceCommand(), new CoinsCommand(waiter), new WardrobeCommand(waiter),
                new TalismanBagCommand(waiter), new InventoryCommand(), new SacksCommand(waiter), new InviteCommand(),
                new WeightCommand());

        if (BOT_PREFIX.equals("/")) {
            jda = JDABuilder.createDefault(BOT_TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                    .addEventListeners(waiter, client.build(), new Apply(), new Verify(), new ChannelDeleter(),
                            new MessageTimeout())
                    .build();
        } else {
            jda = JDABuilder.createDefault(BOT_TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                    .addEventListeners(waiter, client.build(), new ChannelDeleter(), new MessageTimeout(),
                            new ReloadEventWatcher())
                    .build();
        }

        // TODO: /g kick command (factoring in lowest g exp + lowest stats)
        // TODO: finish stats command
        // TODO: stop heroku from idling
    }
}
