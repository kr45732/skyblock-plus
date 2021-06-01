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
import com.skyblockplus.dev.DeleteMessagesCommand;
import com.skyblockplus.dev.EmojiMapServerCommand;
import com.skyblockplus.dev.EvaluateCommand;
import com.skyblockplus.dev.GetAllGuildsIn;
import com.skyblockplus.dev.GetEventListenersCommand;
import com.skyblockplus.dev.GetSettingsFile;
import com.skyblockplus.dev.GetThreadPools;
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
import com.skyblockplus.guilds.GuildKickerCommand;
import com.skyblockplus.guilds.GuildLeaderboardCommand;
import com.skyblockplus.guilds.GuildRequirementsCommand;
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
import com.skyblockplus.miscellaneous.MissingTalismansCommand;
import com.skyblockplus.miscellaneous.ProfilesCommand;
import com.skyblockplus.miscellaneous.ReloadCommand;
import com.skyblockplus.miscellaneous.RoleCommands;
import com.skyblockplus.miscellaneous.SkyblockCommand;
import com.skyblockplus.miscellaneous.VersionCommand;
import com.skyblockplus.networth.NetworthCommand;
import com.skyblockplus.price.AuctionCommand;
import com.skyblockplus.price.AverageAuctionCommand;
import com.skyblockplus.price.BazaarCommand;
import com.skyblockplus.price.BidsCommand;
import com.skyblockplus.price.BinCommand;
import com.skyblockplus.price.BitsCommand;
import com.skyblockplus.price.QueryAuctionCommand;
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
    public static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
    public static JDA jda;
    public static SpringDatabaseComponent database;
    public static EventWaiter waiter;

    public static void main(String[] args) throws LoginException, IllegalArgumentException {
        setApplicationSettings();

        Main.database = SpringApplication.run(Main.class, args).getBean(SpringDatabaseComponent.class);

        Main.waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.setActivity(Activity.watching(BOT_PREFIX + "help"));
        client.setOwnerId("385939031596466176");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix(BOT_PREFIX);

        client.addCommands(new InformationCommand(), new SlayerCommand(), new HelpCommand(), new GuildCommand(),
                new AuctionCommand(), new BinCommand(), new SkillsCommand(), new CatacombsCommand(),
                new ShutdownCommand(), new VersionCommand(), new RoleCommands(), new GuildLeaderboardCommand(),
                new EssenceCommand(), new BankCommand(), new WardrobeCommand(), new TalismanBagCommand(),
                new InventoryCommand(), new SacksCommand(), new InviteCommand(), new WeightCommand(),
                new HypixelCommand(), new UuidCommand(), new SkyblockCommand(), new BaldCommand(),
                new SettingsCommand(), new ReloadCommand(), new SetupCommand(), new CategoriesCommand(),
                new PartyFinderCommand(), new QuickSetupTestCommand(), new EmojiMapServerCommand(),
                new EnderChestCommand(), new InstantTimeNow(), new GetEventListenersCommand(), new GetAllGuildsIn(),
                new LinkAccountCommand(), new GetSettingsFile(), new UnlinkAccountCommand(), new LinkedUserDev(),
                new BazaarCommand(), new AverageAuctionCommand(), new PetsCommand(), new SkyblockEventCommand(),
                new DeleteMessagesCommand(), new PlaceholderCommand(), new ProfilesCommand(), new NetworthCommand(),
                new QueryAuctionCommand(), new BidsCommand(), new GetThreadPools(), new BitsCommand(),
                new EvaluateCommand(), new GuildRequirementsCommand(), new GuildKickerCommand(),
                new MissingTalismansCommand());

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

        try {
            jda.awaitReady();
        } catch (Exception ignored) {
        }

        jda.getPresence().setActivity(Activity.watching(BOT_PREFIX + "help in " + jda.getGuilds().size() + " servers"));

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
