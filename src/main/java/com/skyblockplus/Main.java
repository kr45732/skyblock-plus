package com.skyblockplus;

import static com.skyblockplus.utils.MainClassUtils.cacheApplyGuildUsers;
import static com.skyblockplus.utils.MainClassUtils.closeAsyncHttpClient;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.BOT_TOKEN;
import static com.skyblockplus.utils.Utils.setApplicationSettings;

import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.auctionbaz.AuctionCommand;
import com.skyblockplus.auctionbaz.AverageAuctionCommand;
import com.skyblockplus.auctionbaz.BazaarCommand;
import com.skyblockplus.auctionbaz.BidsCommand;
import com.skyblockplus.auctionbaz.BinCommand;
import com.skyblockplus.auctionbaz.QueryAuctionCommand;
import com.skyblockplus.dev.DeleteMessagesCommand;
import com.skyblockplus.dev.EmojiMapServerCommand;
import com.skyblockplus.dev.GetAllGuildsIn;
import com.skyblockplus.dev.GetEventListenersCommand;
import com.skyblockplus.dev.GetSettingsFile;
import com.skyblockplus.dev.InstantTimeNow;
import com.skyblockplus.dev.LinkedUserDev;
import com.skyblockplus.dev.PlaceholderCommand;
import com.skyblockplus.dev.QuickSetupTestCommand;
import com.skyblockplus.dev.ShutdownCommand;
import com.skyblockplus.dev.UuidCommand;
import com.skyblockplus.dungeons.CatacombsCommand;
import com.skyblockplus.dungeons.EssenceCommand;
import com.skyblockplus.dungeons.PartyFinderCommand;
import com.skyblockplus.eventlisteners.MainListener;
import com.skyblockplus.eventlisteners.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.guilds.GuildCommand;
import com.skyblockplus.guilds.GuildLeaderboardCommand;
import com.skyblockplus.inventory.EnderChestCommand;
import com.skyblockplus.inventory.InventoryCommand;
import com.skyblockplus.inventory.PetsCommand;
import com.skyblockplus.inventory.SacksCommand;
import com.skyblockplus.inventory.TalismanBagCommand;
import com.skyblockplus.inventory.WardrobeCommand;
import com.skyblockplus.link.LinkAccountCommand;
import com.skyblockplus.link.UnlinkAccountCommand;
import com.skyblockplus.miscellaneous.BaldCommand;
import com.skyblockplus.miscellaneous.BankCommand;
import com.skyblockplus.miscellaneous.CategoriesCommand;
import com.skyblockplus.miscellaneous.HelpCommand;
import com.skyblockplus.miscellaneous.HypixelCommand;
import com.skyblockplus.miscellaneous.InformationCommand;
import com.skyblockplus.miscellaneous.InviteCommand;
import com.skyblockplus.miscellaneous.NetworthCommand;
import com.skyblockplus.miscellaneous.ProfilesCommand;
import com.skyblockplus.miscellaneous.ReloadCommand;
import com.skyblockplus.miscellaneous.RoleCommands;
import com.skyblockplus.miscellaneous.SkyblockCommand;
import com.skyblockplus.miscellaneous.VersionCommand;
import com.skyblockplus.settings.SettingsCommand;
import com.skyblockplus.settings.SetupCommand;
import com.skyblockplus.settings.SpringDatabaseComponent;
import com.skyblockplus.skills.SkillsCommand;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.timeout.MessageTimeout;
import com.skyblockplus.weight.WeightCommand;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

@SpringBootApplication
public class Main {
    public static JDA jda;
    public static SpringDatabaseComponent database;
    public static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();

    public static void main(String[] args) throws LoginException, IllegalArgumentException {
        setApplicationSettings();

        Main.database = SpringApplication.run(Main.class, args).getBean(SpringDatabaseComponent.class);

        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.setActivity(Activity.watching(BOT_PREFIX + "help"));
        client.setOwnerId("385939031596466176");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix(BOT_PREFIX);

        client.addCommands(new InformationCommand(), new SlayerCommand(), new HelpCommand(waiter),
                new GuildCommand(waiter), new AuctionCommand(), new BinCommand(), new SkillsCommand(),
                new CatacombsCommand(), new ShutdownCommand(), new VersionCommand(), new RoleCommands(),
                new GuildLeaderboardCommand(), new EssenceCommand(), new BankCommand(waiter),
                new WardrobeCommand(waiter), new TalismanBagCommand(waiter), new InventoryCommand(waiter),
                new SacksCommand(waiter), new InviteCommand(), new WeightCommand(), new HypixelCommand(),
                new UuidCommand(), new SkyblockCommand(waiter), new BaldCommand(), new SettingsCommand(waiter),
                new ReloadCommand(), new SetupCommand(waiter), new CategoriesCommand(), new PartyFinderCommand(),
                new QuickSetupTestCommand(), new EmojiMapServerCommand(), new EnderChestCommand(), new InstantTimeNow(),
                new GetEventListenersCommand(), new GetAllGuildsIn(waiter), new LinkAccountCommand(),
                new GetSettingsFile(), new UnlinkAccountCommand(), new LinkedUserDev(), new BazaarCommand(),
                new AverageAuctionCommand(), new PetsCommand(waiter), new SkyblockEventCommand(),
                new DeleteMessagesCommand(), new PlaceholderCommand(), new ProfilesCommand(waiter),
                new NetworthCommand(), new QueryAuctionCommand(), new BidsCommand());

        if (BOT_PREFIX.equals("+")) {
            jda = JDABuilder.createDefault(BOT_TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                    .addEventListeners(waiter, client.build(), new MessageTimeout(), new MainListener()).build();
        } else {
            jda = JDABuilder.createDefault(BOT_TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                    .addEventListeners(waiter, client.build(), new MessageTimeout(), new MainListener()).build();
        }

        // scheduleUpdateLinkedAccounts();
    }

    @PreDestroy
    public void onExit() {
        System.out.println("== Stopping ==");

        System.out.println("== Caching Apply Users ==");
        cacheApplyGuildUsers();

        System.out.println("== Closing Async Http Client ==");
        closeAsyncHttpClient();

        System.out.println("== Finished ==");
    }

}
